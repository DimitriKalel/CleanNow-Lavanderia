package br.com.cleannow.servico;

import br.com.cleannow.excecao.EntidadeNaoEncontradaException;
import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.cliente.ClienteComum;
import br.com.cleannow.modelo.cliente.ClienteVIP;
import br.com.cleannow.modelo.cliente.Preferencias;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.repositorio.Repositorio;
import br.com.cleannow.util.Validador;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClienteService {
    private final Repositorio<Cliente> clientes;
    private final Repositorio<Pedido> pedidos;
    private final Clock relogio;

    public ClienteService(Repositorio<Cliente> clientes, Repositorio<Pedido> pedidos, Clock relogio) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.relogio = relogio;
    }

    public Cliente cadastrar(String nome, String telefone, String email, String endereco, boolean vip) {
        validarEmailUnico(email, null);
        String id = proximoId();
        LocalDate hoje = LocalDate.now(relogio);
        Cliente c = vip
                ? new ClienteVIP(id, nome, telefone, email, endereco, hoje)
                : new ClienteComum(id, nome, telefone, email, endereco, hoje);
        clientes.salvar(c);
        return c;
    }

    public Cliente buscar(String id) {
        return clientes.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não encontrado: " + id));
    }

    public List<Cliente> listar() {
        return clientes.listarTodos();
    }

    public List<Cliente> buscarPorNome(String trecho) {
        String alvo = normalizar(trecho);
        return clientes.listarTodos().stream()
                .filter(c -> normalizar(c.getNome()).contains(alvo))
                .collect(Collectors.toList());
    }

    public Cliente editar(String id, Consumer<Cliente> alteracao) {
        Cliente c = buscar(id);
        String emailAntes = c.getEmail();
        alteracao.accept(c);
        if (!c.getEmail().equals(emailAntes)) {
            try {
                validarEmailUnico(c.getEmail(), id);
            } catch (ValidacaoException e) {
                c.setEmail(emailAntes);
                throw e;
            }
        }
        clientes.salvar(c);
        return c;
    }

    public Cliente atualizarDados(String id, String nome, String telefone, String email, String endereco) {
        Cliente c = buscar(id);
        String n = Validador.textoObrigatorio(nome, "Nome");
        String t = Validador.telefone(telefone);
        String e = Validador.email(email);
        String end = Validador.textoObrigatorio(endereco, "Endereço");
        validarEmailUnico(e, id);
        c.setNome(n);
        c.setTelefone(t);
        c.setEmail(e);
        c.setEndereco(end);
        clientes.salvar(c);
        return c;
    }

    public void atualizarPreferencias(String id, Consumer<Preferencias> alteracao) {
        Cliente c = buscar(id);
        alteracao.accept(c.getPreferencias());
        clientes.salvar(c);
    }

    public Cliente alterarTipo(String id, boolean vip) {
        Cliente atual = buscar(id);
        if (vip == (atual instanceof ClienteVIP)) {
            throw new ValidacaoException(atual.getNome() + " já é cliente " + atual.getTipo() + ".");
        }
        Cliente novo = vip ? ClienteVIP.aPartirDe(atual) : ClienteComum.aPartirDe(atual);
        clientes.salvar(novo);
        return novo;
    }

    public void remover(String id) {
        Cliente c = buscar(id);
        boolean temAberto = pedidos.listarTodos().stream()
                .anyMatch(p -> p.getClienteId().equals(c.getId()) && !p.getStatus().isFinalizado());
        if (temAberto) {
            throw new ValidacaoException(c.getNome() + " tem pedidos em andamento e não pode ser removido.");
        }
        clientes.remover(c.getId());
    }

    private void validarEmailUnico(String email, String idIgnorado) {
        if (email == null) return;
        String e = email.trim().toLowerCase();
        boolean repetido = clientes.listarTodos().stream()
                .anyMatch(c -> c.getEmail().equals(e) && !c.getId().equals(idIgnorado));
        if (repetido) {
            throw new ValidacaoException("Já existe um cliente com o e-mail " + e + ".");
        }
    }

    private String proximoId() {
        int maior = 0;
        for (Cliente c : clientes.listarTodos()) {
            try {
                maior = Math.max(maior, Integer.parseInt(c.getId().substring(1)));
            } catch (NumberFormatException | IndexOutOfBoundsException ignorado) {
            }
        }
        return String.format("C%03d", maior + 1);
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }
}
