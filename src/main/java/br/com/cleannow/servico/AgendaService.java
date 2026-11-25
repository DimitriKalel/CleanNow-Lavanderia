package br.com.cleannow.servico;

import br.com.cleannow.excecao.AgendaLotadaException;
import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pedido.Agendamento;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.modelo.pedido.TipoAgendamento;
import br.com.cleannow.repositorio.Repositorio;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AgendaService {
    public static final int LIMITE_POR_PERIODO = 5;

    private final Repositorio<Pedido> pedidos;
    private final Clock relogio;

    public AgendaService(Repositorio<Pedido> pedidos, Clock relogio) {
        this.pedidos = pedidos;
        this.relogio = relogio;
    }

    public int ocupacao(TipoAgendamento tipo, LocalDate data, Periodo periodo) {
        int total = 0;
        for (Pedido p : pedidos.listarTodos()) {
            if (p.getStatus() == StatusPedido.CANCELADO) continue;
            Agendamento ag = tipo == TipoAgendamento.COLETA ? p.getColeta() : p.getEntrega();
            if (ag != null && ag.mesmoHorario(tipo, data, periodo)) total++;
        }
        return total;
    }

    public int vagas(TipoAgendamento tipo, LocalDate data, Periodo periodo) {
        return LIMITE_POR_PERIODO - ocupacao(tipo, data, periodo);
    }

    public Agendamento reservar(TipoAgendamento tipo, LocalDate data, Periodo periodo, Cliente cliente,
                                LocalDate dataMinima) {
        validarData(data, dataMinima);
        validarPeriodo(data, periodo);
        if (vagas(tipo, data, periodo) <= 0) {
            throw new AgendaLotadaException(String.format(
                    "%s lotada em %s (%s): já há %d agendamentos. Escolha outro período.",
                    tipo.getDescricao(), data, periodo.getDescricao(), LIMITE_POR_PERIODO));
        }
        return new Agendamento(tipo, data, periodo, cliente.getTaxaPorAgendamento());
    }

    public void validarData(LocalDate data, LocalDate dataMinima) {
        if (data == null) {
            throw new ValidacaoException("Informe a data do agendamento.");
        }
        LocalDate hoje = LocalDate.now(relogio);
        if (data.isBefore(hoje)) {
            throw new ValidacaoException("Não é possível agendar para uma data passada.");
        }
        if (dataMinima != null && data.isBefore(dataMinima)) {
            throw new ValidacaoException("A data deve ser igual ou posterior a " + dataMinima + ".");
        }
        if (data.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ValidacaoException("A CleanNow não faz coletas nem entregas aos domingos.");
        }
        if (data.isAfter(hoje.plusDays(60))) {
            throw new ValidacaoException("Agendamentos só podem ser feitos com até 60 dias de antecedência.");
        }
    }

    public void validarPeriodo(LocalDate data, Periodo periodo) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (data.equals(agora.toLocalDate()) && agora.toLocalTime().isAfter(periodo.getFim())) {
            throw new ValidacaoException("O período " + periodo.getDescricao() + " de hoje já terminou.");
        }
    }

    public List<Pedido> pedidosDoDia(TipoAgendamento tipo, LocalDate data, Periodo periodo) {
        List<Pedido> lista = new ArrayList<>();
        for (Pedido p : pedidos.listarTodos()) {
            if (p.getStatus() == StatusPedido.CANCELADO) continue;
            Agendamento ag = tipo == TipoAgendamento.COLETA ? p.getColeta() : p.getEntrega();
            if (ag != null && ag.mesmoHorario(tipo, data, periodo)) lista.add(p);
        }
        return lista;
    }
}
