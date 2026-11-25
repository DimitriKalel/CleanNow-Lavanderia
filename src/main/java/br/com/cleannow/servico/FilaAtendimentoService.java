package br.com.cleannow.servico;

import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.repositorio.Repositorio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class FilaAtendimentoService {
    private final Repositorio<Pedido> pedidos;
    private final ClienteService clientes;
    private final Clock relogio;

    public FilaAtendimentoService(Repositorio<Pedido> pedidos, ClienteService clientes, Clock relogio) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.relogio = relogio;
    }

    public List<Pedido> filaDeEspera() {
        Comparator<Pedido> ordem = Comparator
                .comparingInt((Pedido p) -> clientes.buscar(p.getClienteId()).getPrioridade())
                .thenComparing(Pedido::getCriadoEm);
        PriorityQueue<Pedido> fila = new PriorityQueue<>(ordem);
        for (Pedido p : pedidos.listarTodos()) {
            if (p.getStatus() == StatusPedido.COLETADO) fila.add(p);
        }
        List<Pedido> ordenada = new ArrayList<>();
        while (!fila.isEmpty()) ordenada.add(fila.poll());
        return ordenada;
    }

    public List<Pedido> emProcesso() {
        List<Pedido> lista = new ArrayList<>();
        for (Pedido p : pedidos.listarTodos()) {
            if (p.getStatus() == StatusPedido.EM_PROCESSO) lista.add(p);
        }
        return lista;
    }

    public Pedido iniciarProximo() {
        List<Pedido> fila = filaDeEspera();
        if (fila.isEmpty()) {
            throw new ValidacaoException("Não há pedidos coletados aguardando serviço.");
        }
        Pedido proximo = fila.get(0);
        proximo.avancarStatus(LocalDateTime.now(relogio));
        pedidos.salvar(proximo);
        return proximo;
    }
}
