package br.com.cleannow;

import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.servico.Pacote;
import br.com.cleannow.modelo.servico.Servico;
import br.com.cleannow.relatorio.ExportadorConsole;
import br.com.cleannow.relatorio.ExportadorPdf;
import br.com.cleannow.relatorio.RelatorioService;
import br.com.cleannow.repositorio.GsonFactory;
import br.com.cleannow.repositorio.Repositorio;
import br.com.cleannow.repositorio.RepositorioJson;
import br.com.cleannow.repositorio.RepositorioMemoria;
import br.com.cleannow.servico.AgendaService;
import br.com.cleannow.servico.CatalogoService;
import br.com.cleannow.servico.ClienteService;
import br.com.cleannow.servico.FilaAtendimentoService;
import br.com.cleannow.servico.HistoricoService;
import br.com.cleannow.servico.PagamentoService;
import br.com.cleannow.servico.PedidoService;
import br.com.cleannow.util.RelogioAjustavel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Sistema {
    private final RelogioAjustavel relogio;
    private final List<String> avisos = new ArrayList<>();
    private final Repositorio<Cliente> repoClientes;
    private final Repositorio<Pedido> repoPedidos;

    private final CatalogoService catalogo;
    private final ClienteService clientes;
    private final AgendaService agenda;
    private final PedidoService pedidos;
    private final FilaAtendimentoService fila;
    private final PagamentoService pagamentos;
    private final HistoricoService historico;
    private final RelatorioService relatorios;
    private final ExportadorConsole exportadorConsole = new ExportadorConsole();
    private final ExportadorPdf exportadorPdf;

    private Sistema(Repositorio<Cliente> c, Repositorio<Pedido> p, Repositorio<Servico> s, Repositorio<Pacote> pc,
                    Path pastaRelatorios, RelogioAjustavel relogio) {
        this.relogio = relogio;
        this.repoClientes = c;
        this.repoPedidos = p;
        this.catalogo = new CatalogoService(s, pc);
        this.clientes = new ClienteService(c, p, relogio);
        this.agenda = new AgendaService(p, relogio);
        this.pedidos = new PedidoService(p, c, clientes, catalogo, agenda, relogio);
        this.fila = new FilaAtendimentoService(p, clientes, relogio);
        this.pagamentos = new PagamentoService(pedidos, clientes, p, c, relogio);
        this.historico = new HistoricoService(pedidos, catalogo, relogio);
        this.relatorios = new RelatorioService(pedidos, clientes, historico, catalogo, agenda, relogio);
        this.exportadorPdf = new ExportadorPdf(pastaRelatorios);
    }

    public static Sistema comArquivos(Path pastaDados, Path pastaRelatorios, RelogioAjustavel relogio) {
        Gson gson = GsonFactory.criar();
        RepositorioJson<Cliente> c = new RepositorioJson<>(pastaDados.resolve("clientes.json"), gson,
                new TypeToken<List<Cliente>>() { }.getType());
        RepositorioJson<Pedido> p = new RepositorioJson<>(pastaDados.resolve("pedidos.json"), gson,
                new TypeToken<List<Pedido>>() { }.getType());
        RepositorioJson<Servico> s = new RepositorioJson<>(pastaDados.resolve("servicos.json"), gson,
                new TypeToken<List<Servico>>() { }.getType());
        RepositorioJson<Pacote> pc = new RepositorioJson<>(pastaDados.resolve("pacotes.json"), gson,
                new TypeToken<List<Pacote>>() { }.getType());
        Sistema sis = new Sistema(c, p, s, pc, pastaRelatorios, relogio);
        for (RepositorioJson<?> r : List.of(c, p, s, pc)) {
            sis.avisos.addAll(r.getAvisos());
        }
        return sis;
    }

    public static Sistema emMemoria(RelogioAjustavel relogio, Path pastaRelatorios) {
        return new Sistema(new RepositorioMemoria<>(), new RepositorioMemoria<>(), new RepositorioMemoria<>(),
                new RepositorioMemoria<>(), pastaRelatorios, relogio);
    }

    public boolean estaVazio() {
        return repoClientes.quantidade() == 0 && repoPedidos.quantidade() == 0;
    }

    public RelogioAjustavel relogio() { return relogio; }
    public List<String> avisos() { return avisos; }
    public CatalogoService catalogo() { return catalogo; }
    public ClienteService clientes() { return clientes; }
    public AgendaService agenda() { return agenda; }
    public PedidoService pedidos() { return pedidos; }
    public FilaAtendimentoService fila() { return fila; }
    public PagamentoService pagamentos() { return pagamentos; }
    public HistoricoService historico() { return historico; }
    public RelatorioService relatorios() { return relatorios; }
    public ExportadorConsole exportadorConsole() { return exportadorConsole; }
    public ExportadorPdf exportadorPdf() { return exportadorPdf; }
}
