package br.com.cleannow;

import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pedido.Pedido;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CalculoPrecoTest extends BaseTeste {
    @Test
    public void lavagemESecagemSemPacote() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV", "SEC"), 5, 0, null, null);
        assertEquals(5 * 8 + 5 * 5, p.getTotal(), 0.001);
        assertNull(p.getPacoteAplicado());
    }

    @Test
    public void passarRoupaCobraPorPeca() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("PAS"), 0, 12, null, null);
        assertEquals(36.0, p.getTotal(), 0.001);
    }

    @Test
    public void pacoteCompletoDaDezPorCento() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV", "SEC", "PAS"), 4, 10, null, null);
        double bruto = 4 * 8 + 4 * 5 + 10 * 3;
        assertEquals(bruto * 0.90, p.getTotal(), 0.001);
        assertEquals("Pacote Completo", p.getPacoteAplicado());
    }

    @Test
    public void vipTemQuinzePorCentoSobreOValorComPacote() {
        Pedido p = s.pedidos().criar(vip().getId(), List.of("LAV", "SEC", "PAS"), 4, 10, null, null);
        assertEquals(82 * 0.90 * 0.85, p.getTotal(), 0.01);
    }

    @Test
    public void taxaDeColetaEEntregaParaComum() {
        Pedido p = s.pedidos().criar(comum().getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA), tarde(SEGUNDA.plusDays(1)));
        assertEquals(16 + 10 + 10, p.getTotal(), 0.001);
    }

    @Test
    public void coletaEEntregaGratisParaVip() {
        Cliente v = vip();
        Pedido p = s.pedidos().criar(v.getId(), List.of("LAV"), 2, 0, tarde(SEGUNDA), tarde(SEGUNDA.plusDays(1)));
        assertEquals(16 * 0.85, p.getTotal(), 0.001);
        assertEquals(0.0, p.getTotalTaxas(), 0.001);
    }

    @Test(expected = br.com.cleannow.excecao.ValidacaoException.class)
    public void pesoZeroComServicoPorKgEhRecusado() {
        s.pedidos().criar(comum().getId(), List.of("LAV"), 0, 0, null, null);
    }

    @Test(expected = br.com.cleannow.excecao.EntidadeNaoEncontradaException.class)
    public void servicoInexistenteEhRecusado() {
        s.pedidos().criar(comum().getId(), List.of("XYZ"), 3, 0, null, null);
    }
}
