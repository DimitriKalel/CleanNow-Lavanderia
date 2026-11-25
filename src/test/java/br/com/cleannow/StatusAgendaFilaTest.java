package br.com.cleannow;

import br.com.cleannow.excecao.AgendaLotadaException;
import br.com.cleannow.excecao.TransicaoStatusInvalidaException;
import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.MeioPagamento;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.modelo.pedido.TipoAgendamento;
import br.com.cleannow.servico.PedidoService.Horario;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StatusAgendaFilaTest extends BaseTeste {
    @Test
    public void statusSegueAOrdemEGeraNotificacaoACadaMudanca() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV"), 3, 0, null, null);
        int antes = p.getHistorico().size();
        assertEquals(StatusPedido.COLETADO, s.pedidos().avancarStatus(p.getCodigo()));
        assertEquals(StatusPedido.EM_PROCESSO, s.pedidos().avancarStatus(p.getCodigo()));
        assertEquals(StatusPedido.PRONTO, s.pedidos().avancarStatus(p.getCodigo()));
        s.pagamentos().pagarPedido(p.getCodigo(), MeioPagamento.PIX);
        assertEquals(StatusPedido.ENTREGUE, s.pedidos().avancarStatus(p.getCodigo()));
        assertEquals(antes + 5, p.getHistorico().size());
    }

    @Test(expected = TransicaoStatusInvalidaException.class)
    public void naoEntregaPedidoSemPagamento() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV"), 3, 0, null, null);
        for (int i = 0; i < 4; i++) s.pedidos().avancarStatus(p.getCodigo());
    }

    @Test(expected = TransicaoStatusInvalidaException.class)
    public void pedidoEntregueNaoMudaMais() {
        StatusPedido.ENTREGUE.proximo();
    }

    @Test(expected = TransicaoStatusInvalidaException.class)
    public void naoCancelaDepoisDaColeta() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV"), 3, 0, null, null);
        s.pedidos().avancarStatus(p.getCodigo());
        s.pedidos().cancelar(p.getCodigo(), null);
    }

    @Test
    public void sextaColetaNoMesmoPeriodoEhRecusada() {
        Cliente c = comum();
        for (int i = 0; i < 5; i++) {
            s.pedidos().criar(c.getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA), null);
        }
        try {
            s.pedidos().criar(c.getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA), null);
            fail("deveria recusar a 6ª coleta");
        } catch (AgendaLotadaException esperado) {
            s.pedidos().criar(c.getId(), List.of("LAV"), 2, 0, new Horario(SEGUNDA.plusDays(1), Periodo.MANHA), null);
        }
        assertEquals(5, s.agenda().ocupacao(TipoAgendamento.COLETA, SEGUNDA, Periodo.TARDE));
    }

    @Test
    public void cancelamentoLiberaAVaga() {
        Cliente c = comum();
        Pedido p = s.pedidos().criar(c.getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA), null);
        s.pedidos().cancelar(p.getCodigo(), "teste");
        assertEquals(5, s.agenda().vagas(TipoAgendamento.COLETA, SEGUNDA, Periodo.TARDE));
    }

    @Test
    public void agendaLotadaNaoConsomeBonusNemFranquia() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        Cliente outro = comum();
        for (int i = 0; i < 5; i++) s.pedidos().criar(outro.getId(), List.of("LAV"), 1, 0, tarde(SEGUNDA), null);
        try {
            s.pedidos().criar(c.getId(), List.of("LAV"), 10, 0, tarde(SEGUNDA), null);
            fail();
        } catch (AgendaLotadaException esperado) {
            assertEquals(20.0, s.clientes().buscar(c.getId()).getAssinatura().getFranquiaRestante(), 0.001);
        }
    }

    @Test(expected = ValidacaoException.class)
    public void naoAgendaNoDomingo() {
        s.pedidos().criar(comum().getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA.plusDays(6)), null);
    }

    @Test(expected = ValidacaoException.class)
    public void naoAgendaNoPassado() {
        s.pedidos().criar(comum().getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA.minusDays(2)), null);
    }

    @Test(expected = ValidacaoException.class)
    public void naoAgendaPeriodoQueJaPassouHoje() {
        relogio.fixar(LocalDateTime.of(SEGUNDA, java.time.LocalTime.of(13, 0)));
        s.pedidos().criar(comum().getId(), List.of("LAV"), 2, 0, new Horario(SEGUNDA, Periodo.MANHA), null);
    }

    @Test(expected = ValidacaoException.class)
    public void entregaAntesDaColetaEhRecusada() {
        s.pedidos().criar(comum().getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA.plusDays(2)), tarde(SEGUNDA.plusDays(1)));
    }

    @Test
    public void vipPassaNaFrenteNaFilaDeServico() {
        Pedido doComum = s.pedidos().criar(comum().getId(), List.of("LAV"), 3, 0, null, null);
        relogio.fixar(LocalDateTime.of(SEGUNDA, java.time.LocalTime.of(10, 0)));
        Pedido doVip = s.pedidos().criar(vip().getId(), List.of("LAV"), 3, 0, null, null);
        s.pedidos().avancarStatus(doComum.getCodigo());
        s.pedidos().avancarStatus(doVip.getCodigo());
        assertEquals(doVip.getCodigo(), s.fila().filaDeEspera().get(0).getCodigo());
        assertEquals(doVip.getCodigo(), s.fila().iniciarProximo().getCodigo());
        assertEquals(StatusPedido.EM_PROCESSO, s.pedidos().buscar(doVip.getCodigo()).getStatus());
    }
}
