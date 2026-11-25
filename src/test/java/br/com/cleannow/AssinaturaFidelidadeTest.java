package br.com.cleannow;

import br.com.cleannow.excecao.PagamentoException;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.MeioPagamento;
import br.com.cleannow.modelo.pedido.Pedido;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssinaturaFidelidadeTest extends BaseTeste {
    @Test
    public void pedidoDentroDaFranquiaSoPagaOQueNaoEstaNoPlano() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        Pedido p = s.pedidos().criar(c.getId(), List.of("LAV", "SEC"), 8, 0, null, null);
        assertEquals(0.0, p.getTotal(), 0.001);
        assertEquals(12.0, s.clientes().buscar(c.getId()).getAssinatura().getFranquiaRestante(), 0.001);
    }

    @Test
    public void excedenteDaFranquiaEhCobradoPeloPrecoNormal() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        s.pedidos().criar(c.getId(), List.of("LAV", "SEC"), 15, 0, null, null);
        Pedido p = s.pedidos().criar(c.getId(), List.of("LAV", "SEC"), 8, 0, null, null);

        assertEquals(3 * 13, p.getTotal(), 0.001);
    }

    @Test
    public void viradaDoMesRenovaFranquiaERegistraMensalidade() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        s.pedidos().criar(c.getId(), List.of("LAV"), 20, 0, null, null);
        relogio.fixar(LocalDateTime.of(2026, 12, 2, 9, 0));
        Pedido dezembro = s.pedidos().criar(c.getId(), List.of("LAV"), 6, 0, null, null);
        assertEquals(0.0, dezembro.getTotal(), 0.001);
        assertEquals(2, s.clientes().buscar(c.getId()).getAssinatura().getMensalidadesPagas().size());
    }

    @Test
    public void planoCanceladoVoltaParaPagamentoPorServico() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        s.pagamentos().cancelarPlano(c.getId());
        Pedido p = s.pedidos().criar(c.getId(), List.of("LAV"), 2, 0, null, null);
        assertEquals(16.0, p.getTotal(), 0.001);
    }

    @Test(expected = PagamentoException.class)
    public void naoPodeAssinarDuasVezes() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        s.pagamentos().assinarPlano(c.getId());
    }

    @Test
    public void decimoPedidoPagoDaCincoKgDeLavagemGratis() {
        Cliente c = comum();
        double bonus = 0;
        for (int i = 0; i < 10; i++) {
            Pedido p = s.pedidos().criar(c.getId(), List.of("SEC"), 1, 0, null, null);
            bonus = s.pagamentos().pagarPedido(p.getCodigo(), MeioPagamento.PIX).kgBonusGanhos();
        }
        assertEquals(5.0, bonus, 0.001);
        Pedido comBonus = s.pedidos().criar(c.getId(), List.of("LAV"), 7, 0, null, null);
        assertEquals(2 * 8, comBonus.getTotal(), 0.001);
        assertEquals(0.0, s.clientes().buscar(c.getId()).getKgBonusDisponivel(), 0.001);
    }

    @Test
    public void cancelarDevolveBonusEFranquia() {
        Cliente c = comum();
        for (int i = 0; i < 10; i++) {
            Pedido p = s.pedidos().criar(c.getId(), List.of("SEC"), 1, 0, null, null);
            s.pagamentos().pagarPedido(p.getCodigo(), MeioPagamento.PIX);
        }
        Pedido p = s.pedidos().criar(c.getId(), List.of("LAV"), 3, 0, null, null);
        assertEquals(2.0, s.clientes().buscar(c.getId()).getKgBonusDisponivel(), 0.001);
        s.pedidos().cancelar(p.getCodigo(), "teste");
        assertEquals(5.0, s.clientes().buscar(c.getId()).getKgBonusDisponivel(), 0.001);
    }

    @Test
    public void promoverAVipMantemBonusEAssinatura() {
        Cliente c = comum();
        s.pagamentos().assinarPlano(c.getId());
        Cliente v = s.clientes().alterarTipo(c.getId(), true);
        assertEquals("VIP", v.getTipo());
        assertTrue(v.temAssinaturaAtiva());
        assertFalse(s.clientes().listar().stream().anyMatch(x -> x.getTipo().equals("COMUM")));
    }

    @Test(expected = PagamentoException.class)
    public void naoPagaDuasVezes() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV"), 2, 0, null, null);
        s.pagamentos().pagarPedido(p.getCodigo(), MeioPagamento.PIX);
        relogio.fixar(LocalDateTime.of(SEGUNDA, LocalTime.of(10, 0)));
        s.pagamentos().pagarPedido(p.getCodigo(), MeioPagamento.DINHEIRO);
    }
}
