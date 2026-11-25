package br.com.cleannow.servico;

import br.com.cleannow.excecao.EntidadeNaoEncontradaException;
import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.servico.Lavagem;
import br.com.cleannow.modelo.servico.Pacote;
import br.com.cleannow.modelo.servico.PassarRoupa;
import br.com.cleannow.modelo.servico.Secagem;
import br.com.cleannow.modelo.servico.Servico;
import br.com.cleannow.modelo.servico.ServicoPorKg;
import br.com.cleannow.modelo.servico.ServicoPorPeca;
import br.com.cleannow.repositorio.Repositorio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CatalogoService {
    public static final String PACOTE_COMPLETO = "COMPLETO";
    private static final Set<String> SERVICOS_BASE = Set.of(Lavagem.CODIGO, Secagem.CODIGO, PassarRoupa.CODIGO);

    private final Repositorio<Servico> servicos;
    private final Repositorio<Pacote> pacotes;

    public CatalogoService(Repositorio<Servico> servicos, Repositorio<Pacote> pacotes) {
        this.servicos = servicos;
        this.pacotes = pacotes;
        inicializarPadrao();
    }

    private void inicializarPadrao() {
        if (!servicos.existe(Lavagem.CODIGO)) servicos.salvar(new Lavagem(8.00));
        if (!servicos.existe(Secagem.CODIGO)) servicos.salvar(new Secagem(5.00));
        if (!servicos.existe(PassarRoupa.CODIGO)) servicos.salvar(new PassarRoupa(3.00));
        if (!pacotes.existe(PACOTE_COMPLETO)) {
            pacotes.salvar(new Pacote(PACOTE_COMPLETO, "Pacote Completo",
                    List.of(Lavagem.CODIGO, Secagem.CODIGO, PassarRoupa.CODIGO), 0.10));
        }
    }

    public List<Servico> listarServicos() {
        return servicos.listarTodos();
    }

    public Servico buscarServico(String codigo) {
        return servicos.buscarPorId(codigo)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Serviço não encontrado: " + codigo));
    }

    public List<Servico> buscarServicos(Collection<String> codigos) {
        List<Servico> lista = new ArrayList<>();
        for (String c : codigos) {
            lista.add(buscarServico(c));
        }
        return lista;
    }

    public Servico cadastrarServico(String codigo, String nome, double preco, boolean porKg) {
        String cod = codigo == null ? "" : codigo.trim().toUpperCase();
        if (servicos.existe(cod)) {
            throw new ValidacaoException("Já existe um serviço com o código " + cod + ".");
        }
        Servico s = porKg ? new ServicoPorKg(cod, nome, preco) : new ServicoPorPeca(cod, nome, preco);
        servicos.salvar(s);
        return s;
    }

    public void alterarPreco(String codigo, double novoPreco) {
        Servico s = buscarServico(codigo);
        s.setPrecoUnitario(novoPreco);
        servicos.salvar(s);
    }

    public void removerServico(String codigo) {
        Servico s = buscarServico(codigo);
        if (SERVICOS_BASE.contains(s.getCodigo())) {
            throw new ValidacaoException("Os serviços básicos (lavagem, secagem e passar) não podem ser removidos.");
        }
        for (Pacote p : pacotes.listarTodos()) {
            if (p.contemServico(s.getCodigo())) {
                throw new ValidacaoException("O serviço faz parte do pacote " + p.getNome() + "; remova o pacote antes.");
            }
        }
        servicos.remover(s.getCodigo());
    }

    public List<Pacote> listarPacotes() {
        return pacotes.listarTodos();
    }

    public Pacote cadastrarPacote(String codigo, String nome, List<String> codigosServicos, double desconto) {
        String cod = codigo == null ? "" : codigo.trim().toUpperCase();
        if (pacotes.existe(cod)) {
            throw new ValidacaoException("Já existe um pacote com o código " + cod + ".");
        }
        buscarServicos(codigosServicos);
        Pacote p = new Pacote(cod, nome, codigosServicos, desconto);
        pacotes.salvar(p);
        return p;
    }

    public void removerPacote(String codigo) {
        if (!pacotes.remover(codigo)) {
            throw new EntidadeNaoEncontradaException("Pacote não encontrado: " + codigo);
        }
    }

    public Pacote melhorPacotePara(Collection<String> codigosDoPedido) {
        Pacote melhor = null;
        for (Pacote p : pacotes.listarTodos()) {
            if (p.aplicavelA(codigosDoPedido) && (melhor == null || p.getDesconto() > melhor.getDesconto())) {
                melhor = p;
            }
        }
        return melhor;
    }
}
