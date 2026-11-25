package br.com.cleannow;

import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.MeioPagamento;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.servico.PedidoService.Horario;
import br.com.cleannow.util.RelogioAjustavel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class DadosDemonstracao {
    private DadosDemonstracao() {
    }

    public static String carregar(Sistema s) {
        if (!s.estaVazio()) {
            return "Já existem dados cadastrados; a demonstração só pode ser carregada num sistema vazio.";
        }
        RelogioAjustavel relogio = s.relogio();
        LocalDate hoje = LocalDate.now(relogio);
        try {
            relogio.fixar(LocalDateTime.of(hoje.minusDays(120), LocalTime.of(9, 0)));
            Cliente maria = s.clientes().cadastrar("Maria Fernanda Lima", "(86) 99812-3344",
                    "maria.lima@email.com", "Rua Areolino de Abreu, 1200 - Centro, Teresina", false);
            Cliente carlos = s.clientes().cadastrar("Carlos Eduardo Rocha", "(86) 98877-1020",
                    "carlos.rocha@email.com", "Av. Nossa Senhora de Fátima, 455 - Fátima, Teresina", true);
            Cliente ana = s.clientes().cadastrar("Ana Beatriz Sousa", "(86) 99901-2233",
                    "ana.sousa@email.com", "Rua Jônatas Batista, 88 - Jóquei, Teresina", false);
            Cliente pedro = s.clientes().cadastrar("Pedro Henrique Alves", "(86) 99456-7788",
                    "pedro.alves@email.com", "Rua Desembargador Freitas, 300 - Ilhotas, Teresina", true);
            Cliente juliana = s.clientes().cadastrar("Juliana Castro", "(86) 98123-4567",
                    "juliana.castro@email.com", "Rua Coelho Rodrigues, 1500 - Centro, Teresina", false);
            Cliente rafael = s.clientes().cadastrar("Rafael Nogueira", "(86) 3221-4455",
                    "rafael.nogueira@email.com", "Av. Frei Serafim, 2100 - Centro, Teresina", false);

            s.clientes().atualizarPreferencias(maria.getId(), p -> {
                p.setPeriodoPreferido(Periodo.MANHA);
                p.setAmaciante(false);
            });
            s.clientes().atualizarPreferencias(carlos.getId(), p -> {
                p.setPeriodoPreferido(Periodo.TARDE);
                p.setRoupasDobradas(false);
                p.setObservacoes("Camisas sociais em cabide");
            });
            s.clientes().atualizarPreferencias(ana.getId(), p -> p.setSabaoHipoalergenico(true));

            for (int i = 9; i >= 1; i--) {
                LocalDate dia = diaUtil(hoje.minusDays(i * 12L));
                relogio.fixar(LocalDateTime.of(dia, LocalTime.of(8, 30)));
                Pedido p = s.pedidos().criar(maria.getId(), List.of("LAV", "SEC"), 4 + (i % 3), 0,
                        new Horario(dia, Periodo.MANHA), new Horario(dia.plusDays(diaUtilSeguinte(dia)), Periodo.TARDE));
                concluir(s, relogio, p.getCodigo(), dia, i % 2 == 0 ? MeioPagamento.PIX : MeioPagamento.CARTAO_DEBITO);
            }

            LocalDate d1 = diaUtil(hoje.minusDays(20));
            relogio.fixar(LocalDateTime.of(d1, LocalTime.of(10, 0)));
            Pedido c1 = s.pedidos().criar(carlos.getId(), List.of("LAV", "SEC", "PAS"), 7, 12,
                    new Horario(d1, Periodo.TARDE), null);
            concluir(s, relogio, c1.getCodigo(), d1, MeioPagamento.CARTAO_CREDITO);

            LocalDate coletaHoje = diaUtil(hoje);
            relogio.fixar(LocalDateTime.of(coletaHoje, LocalTime.of(7, 40)));
            Pedido jul = s.pedidos().criar(juliana.getId(), List.of("LAV"), 4, 0,
                    new Horario(coletaHoje, Periodo.MANHA), null);
            s.pedidos().avancarStatus(jul.getCodigo());

            relogio.fixar(LocalDateTime.of(coletaHoje, LocalTime.of(7, 55)));
            Pedido car = s.pedidos().criar(carlos.getId(), List.of("LAV", "SEC", "PAS"), 6, 10,
                    new Horario(coletaHoje, Periodo.MANHA), new Horario(diaUtil(coletaHoje.plusDays(2)), Periodo.TARDE));
            s.pedidos().avancarStatus(car.getCodigo());

            relogio.fixar(LocalDateTime.of(coletaHoje, LocalTime.of(8, 10)));
            Pedido ped = s.pedidos().criar(pedro.getId(), List.of("SEC"), 3, 0,
                    new Horario(coletaHoje, Periodo.MANHA), null);
            s.pedidos().avancarStatus(ped.getCodigo());
            s.pedidos().avancarStatus(ped.getCodigo());

            relogio.fixar(LocalDateTime.of(coletaHoje, LocalTime.of(8, 20)));
            Pedido raf = s.pedidos().criar(rafael.getId(), List.of("PAS"), 0, 15, null, null);
            s.pedidos().avancarStatus(raf.getCodigo());
            s.pedidos().avancarStatus(raf.getCodigo());
            s.pedidos().avancarStatus(raf.getCodigo());
            s.pagamentos().pagarPedido(raf.getCodigo(), MeioPagamento.DINHEIRO);

            relogio.fixar(LocalDateTime.of(coletaHoje, LocalTime.of(9, 0)));
            s.pagamentos().assinarPlano(ana.getId());
            LocalDate amanha = diaUtil(coletaHoje.plusDays(1));
            s.pedidos().criar(ana.getId(), List.of("LAV", "SEC"), 12, 0,
                    new Horario(amanha, Periodo.MANHA), new Horario(diaUtil(amanha.plusDays(1)), Periodo.MANHA));

            relogio.fixar(LocalDateTime.of(coletaHoje, LocalTime.of(9, 30)));
            Pedido canc = s.pedidos().criar(juliana.getId(), List.of("LAV", "SEC"), 5, 0,
                    new Horario(amanha, Periodo.TARDE), null);
            s.pedidos().cancelar(canc.getCodigo(), "cliente viajou");
        } finally {
            relogio.voltarAoNormal();
        }
        return "Dados de demonstração carregados: 6 clientes (2 VIP, 1 assinante) e "
                + s.pedidos().listarTodos().size() + " pedidos.";
    }

    private static void concluir(Sistema s, RelogioAjustavel relogio, String codigo, LocalDate dia, MeioPagamento meio) {
        relogio.fixar(LocalDateTime.of(dia, LocalTime.of(11, 0)));
        s.pedidos().avancarStatus(codigo);
        relogio.fixar(LocalDateTime.of(dia, LocalTime.of(14, 0)));
        s.pedidos().avancarStatus(codigo);
        relogio.fixar(LocalDateTime.of(dia.plusDays(1), LocalTime.of(10, 0)));
        s.pedidos().avancarStatus(codigo);
        s.pagamentos().pagarPedido(codigo, meio);
        relogio.fixar(LocalDateTime.of(dia.plusDays(1), LocalTime.of(16, 0)));
        s.pedidos().avancarStatus(codigo);
    }

    private static LocalDate diaUtil(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    private static long diaUtilSeguinte(LocalDate d) {
        return d.plusDays(1).getDayOfWeek() == DayOfWeek.SUNDAY ? 2 : 1;
    }
}
