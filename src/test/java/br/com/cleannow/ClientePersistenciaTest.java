package br.com.cleannow;

import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.cliente.ClienteVIP;
import br.com.cleannow.modelo.pagamento.MeioPagamento;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientePersistenciaTest extends BaseTeste {
    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    @Test(expected = ValidacaoException.class)
    public void emailInvalidoEhRecusado() {
        s.clientes().cadastrar("Fulano", "86999990000", "fulano-sem-arroba", "Rua X", false);
    }

    @Test(expected = ValidacaoException.class)
    public void telefoneSemDddEhRecusado() {
        s.clientes().cadastrar("Fulano", "9999-0000", "f@x.com", "Rua X", false);
    }

    @Test(expected = ValidacaoException.class)
    public void emailRepetidoEhRecusado() {
        s.clientes().cadastrar("A", "86999990000", "a@x.com", "Rua X", false);
        s.clientes().cadastrar("B", "86999990001", "A@X.COM", "Rua Y", false);
    }

    @Test(expected = ValidacaoException.class)
    public void naoRemoveClienteComPedidoEmAndamento() {
        Cliente c = comum();
        s.pedidos().criar(c.getId(), List.of("LAV"), 3, 0, null, null);
        s.clientes().remover(c.getId());
    }

    @Test
    public void buscaPorNomeIgnoraAcentoEMaiuscula() {
        s.clientes().cadastrar("João Câmara", "86999990000", "joao@x.com", "Rua X", false);
        assertEquals(1, s.clientes().buscarPorNome("joao cam").size());
    }

    @Test
    public void dadosSobrevivemAoReiniciarOPrograma() throws Exception {
        Path dados = pasta.newFolder("dados").toPath();
        Sistema primeiro = Sistema.comArquivos(dados, pasta.getRoot().toPath(), relogio);
        Cliente v = primeiro.clientes().cadastrar("Vip Persistido", "86988887777", "vp@x.com", "Rua Z", true);
        primeiro.pagamentos().assinarPlano(v.getId());
        Pedido p = primeiro.pedidos().criar(v.getId(), List.of("LAV", "SEC", "PAS"), 25, 5, tarde(SEGUNDA), null);
        primeiro.pedidos().avancarStatus(p.getCodigo());
        primeiro.pagamentos().pagarPedido(p.getCodigo(), MeioPagamento.CARTAO_CREDITO);

        Sistema segundo = Sistema.comArquivos(dados, pasta.getRoot().toPath(), relogio);
        Cliente lido = segundo.clientes().buscar(v.getId());
        assertTrue(lido instanceof ClienteVIP);
        assertTrue(lido.temAssinaturaAtiva());
        assertEquals(0.0, lido.getAssinatura().getFranquiaRestante(), 0.001);
        Pedido pl = segundo.pedidos().buscar(p.getCodigo());
        assertEquals(StatusPedido.COLETADO, pl.getStatus());
        assertTrue(pl.isPago());
        assertEquals(p.getTotal(), pl.getTotal(), 0.001);
        assertEquals(p.getHistorico().size(), pl.getHistorico().size());
        assertEquals(SEGUNDA, pl.getColeta().getData());
        assertEquals(3, segundo.catalogo().listarServicos().size());
    }

    @Test
    public void arquivoCorrompidoNaoDerrubaOSistema() throws Exception {
        Path dados = pasta.newFolder("dados").toPath();
        Files.writeString(dados.resolve("clientes.json"), "{ isso nao e json [");
        Sistema sis = Sistema.comArquivos(dados, pasta.getRoot().toPath(), relogio);
        assertEquals(0, sis.clientes().listar().size());
        assertFalse(sis.avisos().isEmpty());
        try (var arquivos = Files.list(dados)) {
            assertTrue(arquivos.anyMatch(f -> f.getFileName().toString().startsWith("clientes.json.corrompido")));
        }
        sis.clientes().cadastrar("Novo", "86999990000", "novo@x.com", "Rua X", false);
    }

    @Test
    public void pastaSemArquivosComecaComCatalogoPadrao() throws Exception {
        Sistema sis = Sistema.comArquivos(pasta.newFolder("vazia").toPath(), pasta.getRoot().toPath(), relogio);
        assertTrue(sis.estaVazio());
        assertEquals(1, sis.catalogo().listarPacotes().size());
    }

    @Test
    public void relatoriosSaoMontadosSemErro() {
        DadosDemonstracao.carregar(s);
        String c = s.exportadorConsole().exportar(s.relatorios().gerencial(SEGUNDA.minusDays(150), SEGUNDA), null);
        assertTrue(c.contains("Receita total"));
        String h = s.exportadorConsole().exportar(s.relatorios().historicoCliente("C001"), null);
        assertTrue(h.contains("Maria Fernanda Lima"));
    }
}
