package br.com.cleannow.servico;

import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.modelo.servico.Servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoricoService {
    private final PedidoService pedidos;
    private final CatalogoService catalogo;
    private final Clock relogio;

    public HistoricoService(PedidoService pedidos, CatalogoService catalogo, Clock relogio) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.relogio = relogio;
    }

    public record Estatisticas(
            int totalPedidos, int pedidosConcluidos, int pedidosCancelados, double totalGasto,
            double ticketMedio, double kgTotal, int pecasTotal, String servicoMaisUsado,
            Map<String, Integer> usoPorServico, String periodoMaisUsado, LocalDate primeiroPedido,
            LocalDate ultimoPedido, long diasDesdeUltimo, double mediaDiasEntrePedidos,
            double pedidosPorMes) {
    }

    public Estatisticas calcular(Cliente cliente) {
        List<Pedido> todos = pedidos.listarPorCliente(cliente.getId());
        int concluidos = 0, cancelados = 0, pecas = 0;
        double gasto = 0, kg = 0;
        Map<String, Integer> uso = new LinkedHashMap<>();
        Map<Periodo, Integer> periodos = new LinkedHashMap<>();
        LocalDate primeiro = null, ultimo = null;
        int validos = 0;

        for (Pedido p : todos) {
            if (p.getStatus() == StatusPedido.CANCELADO) {
                cancelados++;
                continue;
            }
            validos++;
            if (p.getStatus() == StatusPedido.ENTREGUE) concluidos++;
            if (p.isPago()) gasto += p.getPagamento().getValor();
            kg += p.getPesoKg();
            pecas += p.getQuantidadePecas();
            for (String cod : p.getCodigosServicos()) {
                uso.merge(nomeServico(cod), 1, Integer::sum);
            }
            if (p.getColeta() != null) periodos.merge(p.getColeta().getPeriodo(), 1, Integer::sum);
            LocalDate d = p.getCriadoEm().toLocalDate();
            if (primeiro == null || d.isBefore(primeiro)) primeiro = d;
            if (ultimo == null || d.isAfter(ultimo)) ultimo = d;
        }

        String maisUsado = maior(uso);
        Periodo periodoTop = maior(periodos);
        LocalDate hoje = LocalDate.now(relogio);
        long diasDesde = ultimo == null ? -1 : ChronoUnit.DAYS.between(ultimo, hoje);
        double mediaEntre = (validos > 1) ? ChronoUnit.DAYS.between(primeiro, ultimo) / (double) (validos - 1) : 0;
        double meses = primeiro == null ? 0 : Math.max(1, ChronoUnit.DAYS.between(primeiro, hoje) / 30.0);
        double porMes = meses == 0 ? 0 : validos / meses;
        int pagos = (int) todos.stream().filter(Pedido::isPago).count();

        return new Estatisticas(todos.size(), concluidos, cancelados, gasto, pagos == 0 ? 0 : gasto / pagos, kg,
                pecas, maisUsado == null ? "-" : maisUsado, uso,
                periodoTop == null ? "-" : periodoTop.getDescricao(), primeiro, ultimo, diasDesde, mediaEntre, porMes);
    }

    private String nomeServico(String codigo) {
        try {
            Servico s = catalogo.buscarServico(codigo);
            return s.getNome();
        } catch (RuntimeException e) {
            return codigo + " (removido)";
        }
    }

    private static <K> K maior(Map<K, Integer> contagem) {
        K melhor = null;
        int max = 0;
        for (Map.Entry<K, Integer> e : contagem.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                melhor = e.getKey();
            }
        }
        return melhor;
    }
}
