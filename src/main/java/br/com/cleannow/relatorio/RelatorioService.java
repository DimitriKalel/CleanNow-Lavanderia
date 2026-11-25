package br.com.cleannow.relatorio;

import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.Assinatura;
import br.com.cleannow.modelo.pagamento.Orcamento;
import br.com.cleannow.modelo.pedido.Notificacao;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.modelo.pedido.TipoAgendamento;
import br.com.cleannow.servico.AgendaService;
import br.com.cleannow.servico.CatalogoService;
import br.com.cleannow.servico.ClienteService;
import br.com.cleannow.servico.HistoricoService;
import br.com.cleannow.servico.PedidoService;
import br.com.cleannow.util.Formatador;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static br.com.cleannow.relatorio.Documento.linha;

public class RelatorioService {
    private final PedidoService pedidos;
    private final ClienteService clientes;
    private final HistoricoService historico;
    private final CatalogoService catalogo;
    private final AgendaService agenda;
    private final Clock relogio;

    public RelatorioService(PedidoService pedidos, ClienteService clientes, HistoricoService historico,
                            CatalogoService catalogo, AgendaService agenda, Clock relogio) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.historico = historico;
        this.catalogo = catalogo;
        this.agenda = agenda;
        this.relogio = relogio;
    }

    public Documento comprovantePedido(String codigo) {
        Pedido p = pedidos.buscar(codigo);
        Cliente c = clientes.buscar(p.getClienteId());
        Documento d = new Documento("Pedido " + p.getCodigo(), "Comprovante e rastreio do pedido");
        d.secao("Dados do pedido").campos(
                "Cliente", c.getNome() + " (" + c.getId() + " - " + c.getTipo() + ")",
                "Telefone", c.getTelefoneFormatado(),
                "Endereço", c.getEndereco(),
                "Criado em", Formatador.dataHora(p.getCriadoEm()),
                "Status atual", p.getStatus().getDescricao(),
                "Peso / peças", Formatador.kg(p.getPesoKg()) + " / " + p.getQuantidadePecas() + " peça(s)",
                "Modalidade", p.getOrcamento().getModalidade(),
                "Preferências", c.getPreferencias().resumo(),
                "Coleta", p.getColeta() == null ? "Cliente leva à loja" : p.getColeta().toString(),
                "Entrega", p.getEntrega() == null ? "Cliente busca na loja" : p.getEntrega().toString());

        d.secao("Memória de cálculo");
        List<List<String>> linhas = new ArrayList<>();
        for (Orcamento.Linha l : p.getOrcamento().getLinhas()) {
            linhas.add(linha(l.getDescricao(), Formatador.moeda(l.getValor())));
        }
        if (p.getColeta() != null) linhas.add(linha("Taxa de coleta", Formatador.moeda(p.getColeta().getTaxa())));
        if (p.getEntrega() != null) linhas.add(linha("Taxa de entrega", Formatador.moeda(p.getEntrega().getTaxa())));
        d.tabela(linha("Item", "Valor"), linhas);
        d.destaque("Total do pedido", Formatador.moeda(p.getTotal()));
        d.paragrafo(p.isPago() ? "Pago: " + p.getPagamento() : "Pagamento pendente.");

        d.secao("Linha do tempo (notificações)");
        List<List<String>> eventos = new ArrayList<>();
        for (Notificacao n : p.getHistorico()) {
            eventos.add(linha(Formatador.dataHora(n.getDataHora()), n.getMensagem()));
        }
        d.tabela(linha("Data/hora", "Evento"), eventos);
        return d;
    }

    public Documento historicoCliente(String clienteId) {
        Cliente c = clientes.buscar(clienteId);
        HistoricoService.Estatisticas e = historico.calcular(c);
        Documento d = new Documento("Histórico de " + c.getNome(), "Cliente " + c.getId() + " - " + c.getTipo()
                + " desde " + Formatador.data(c.getDataCadastro()));

        d.secao("Resumo").campos(
                "Pedidos (total / entregues / cancelados)",
                e.totalPedidos() + " / " + e.pedidosConcluidos() + " / " + e.pedidosCancelados(),
                "Total gasto em pedidos", Formatador.moeda(e.totalGasto()),
                "Ticket médio", Formatador.moeda(e.ticketMedio()),
                "Roupas processadas", Formatador.kg(e.kgTotal()) + " e " + e.pecasTotal() + " peças passadas",
                "Frequência", Formatador.decimal(e.pedidosPorMes(), 1) + " pedido(s) por mês",
                "Intervalo médio entre pedidos", e.mediaDiasEntrePedidos() == 0 ? "-"
                        : Formatador.decimal(e.mediaDiasEntrePedidos(), 0) + " dias",
                "Último pedido", e.ultimoPedido() == null ? "-"
                        : Formatador.data(e.ultimoPedido()) + " (há " + e.diasDesdeUltimo() + " dias)",
                "Serviço mais usado", e.servicoMaisUsado(),
                "Período de coleta mais usado", e.periodoMaisUsado());

        d.secao("Fidelidade e plano").campos(
                "Pedidos pagos", String.valueOf(c.getPedidosPagos()),
                "Bônus disponível", Formatador.kg(c.getKgBonusDisponivel()) + " de lavagem grátis",
                "Faltam para o próximo bônus", c.pedidosAteProximoBonus() + " pedido(s) pago(s)",
                "Assinatura", descricaoAssinatura(c));

        d.secao("Preferências").paragrafo(c.getPreferencias().resumo());

        d.secao("Uso por serviço");
        List<List<String>> uso = new ArrayList<>();
        e.usoPorServico().forEach((s, n) -> uso.add(linha(s, n + " pedido(s)")));
        d.tabela(linha("Serviço", "Vezes"), uso);

        d.secao("Pedidos");
        List<List<String>> lista = new ArrayList<>();
        for (Pedido p : pedidos.listarPorCliente(c.getId())) {
            lista.add(linha(p.getCodigo(), Formatador.data(p.getCriadoEm().toLocalDate()), servicos(p),
                    Formatador.kg(p.getPesoKg()), p.getStatus().getDescricao(), Formatador.moeda(p.getTotal()),
                    p.isPago() ? "sim" : "não"));
        }
        d.tabela(linha("Código", "Data", "Serviços", "Peso", "Status", "Total", "Pago"), lista);
        return d;
    }

    private String descricaoAssinatura(Cliente c) {
        Assinatura a = c.getAssinatura();
        if (a == null) return "Não assinante";
        if (!a.isAtiva()) return "Cancelada (assinante de " + Formatador.mes(a.getInicio()) + ")";
        return String.format("Ativa desde %s | franquia de %s: %s usados, %s restantes | %d mensalidade(s) = %s",
                Formatador.mes(a.getInicio()), Formatador.mes(a.getMesReferencia()), Formatador.kg(a.getKgUsadosNoMes()),
                Formatador.kg(a.getFranquiaRestante()), a.getMensalidadesPagas().size(), Formatador.moeda(a.getTotalPago()));
    }

    private String servicos(Pedido p) {
        return String.join("+", p.getCodigosServicos());
    }

    public Documento gerencial(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ValidacaoException("Período inválido: a data final deve ser igual ou posterior à inicial.");
        }
        Documento d = new Documento("Relatório gerencial",
                "Período de " + Formatador.data(inicio) + " a " + Formatador.data(fim));

        List<Pedido> doPeriodo = new ArrayList<>();
        for (Pedido p : pedidos.listarTodos()) {
            LocalDate dia = p.getCriadoEm().toLocalDate();
            if (!dia.isBefore(inicio) && !dia.isAfter(fim)) doPeriodo.add(p);
        }

        double faturamentoPedidos = 0, taxas = 0, descontos = 0, kg = 0;
        int pagos = 0;
        Map<StatusPedido, Integer> porStatus = new EnumMap<>(StatusPedido.class);
        Map<String, Integer> porServico = new LinkedHashMap<>();
        Map<String, Double> gastoPorCliente = new LinkedHashMap<>();
        for (Pedido p : doPeriodo) {
            porStatus.merge(p.getStatus(), 1, Integer::sum);
            if (p.getStatus() == StatusPedido.CANCELADO) continue;
            kg += p.getPesoKg();
            for (String s : p.getCodigosServicos()) porServico.merge(s, 1, Integer::sum);
            if (p.isPago()) {
                pagos++;
                faturamentoPedidos += p.getPagamento().getValor();
                taxas += p.getTotalTaxas();
                descontos += p.getOrcamento().getTotalAbatimentos();
                gastoPorCliente.merge(p.getClienteId(), p.getPagamento().getValor(), Double::sum);
            }
        }

        double mensalidades = 0;
        int assinantesAtivos = 0, vips = 0;
        for (Cliente c : clientes.listar()) {
            if (c.getPrioridade() == 1) vips++;
            Assinatura a = c.getAssinatura();
            if (a == null) continue;
            if (a.isAtiva()) assinantesAtivos++;
            for (YearMonth m : a.getMensalidadesPagas()) {
                if (!m.isBefore(YearMonth.from(inicio)) && !m.isAfter(YearMonth.from(fim))) {
                    mensalidades += Assinatura.MENSALIDADE;
                }
            }
        }

        d.secao("Faturamento").campos(
                "Pedidos pagos", pagos + " de " + doPeriodo.size() + " pedidos criados",
                "Receita de pedidos", Formatador.moeda(faturamentoPedidos),
                "   dos quais taxas de coleta/entrega", Formatador.moeda(taxas),
                "Receita de mensalidades", Formatador.moeda(mensalidades),
                "Descontos, franquias e bônus concedidos", Formatador.moeda(descontos),
                "Roupas processadas", Formatador.kg(kg));
        d.destaque("Receita total", Formatador.moeda(faturamentoPedidos + mensalidades));

        d.secao("Pedidos por status");
        List<List<String>> st = new ArrayList<>();
        for (StatusPedido s : StatusPedido.values()) {
            st.add(linha(s.getDescricao(), String.valueOf(porStatus.getOrDefault(s, 0))));
        }
        d.tabela(linha("Status", "Pedidos"), st);

        d.secao("Serviços mais pedidos");
        List<List<String>> sv = new ArrayList<>();
        porServico.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sv.add(linha(nomeServico(e.getKey()), String.valueOf(e.getValue()))));
        d.tabela(linha("Serviço", "Pedidos"), sv);

        d.secao("Top 5 clientes por valor pago");
        List<List<String>> top = new ArrayList<>();
        gastoPorCliente.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .forEach(e -> {
                    Cliente c = clientes.buscar(e.getKey());
                    top.add(linha(c.getNome(), c.getTipo(), Formatador.moeda(e.getValue())));
                });
        d.tabela(linha("Cliente", "Tipo", "Total pago"), top);

        d.secao("Base de clientes").campos(
                "Clientes cadastrados", String.valueOf(clientes.listar().size()),
                "Clientes VIP", String.valueOf(vips),
                "Assinaturas ativas", String.valueOf(assinantesAtivos));

        d.secao("Ocupação da agenda - próximos 6 dias úteis");
        List<List<String>> ag = new ArrayList<>();
        LocalDate dia = LocalDate.now(relogio);
        int dias = 0;
        while (dias < 6) {
            if (dia.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                List<String> l = new ArrayList<>();
                l.add(Formatador.data(dia));
                for (Periodo per : Periodo.values()) {
                    l.add(agenda.ocupacao(TipoAgendamento.COLETA, dia, per) + "/" + AgendaService.LIMITE_POR_PERIODO);
                    l.add(agenda.ocupacao(TipoAgendamento.ENTREGA, dia, per) + "/" + AgendaService.LIMITE_POR_PERIODO);
                }
                ag.add(l);
                dias++;
            }
            dia = dia.plusDays(1);
        }
        d.tabela(linha("Dia", "Coletas manhã", "Entregas manhã", "Coletas tarde", "Entregas tarde"), ag);
        return d;
    }

    private String nomeServico(String codigo) {
        try {
            return catalogo.buscarServico(codigo).getNome();
        } catch (RuntimeException e) {
            return codigo;
        }
    }
}
