package br.com.cleannow.servico;

import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.Assinatura;
import br.com.cleannow.modelo.pagamento.MeioPagamento;
import br.com.cleannow.modelo.pagamento.Pagamento;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.repositorio.Repositorio;
import br.com.cleannow.util.Formatador;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;

public class PagamentoService {
    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final Repositorio<Pedido> pedidos;
    private final Repositorio<Cliente> clientes;
    private final Clock relogio;

    public PagamentoService(PedidoService pedidoService, ClienteService clienteService,
                            Repositorio<Pedido> pedidos, Repositorio<Cliente> clientes, Clock relogio) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.relogio = relogio;
    }

    public record Recibo(Pedido pedido, double valorPago, double kgBonusGanhos, int pedidosAteProximoBonus) {
    }

    public Recibo pagarPedido(String codigo, MeioPagamento meio) {
        Pedido p = pedidoService.buscar(codigo);
        Cliente c = clienteService.buscar(p.getClienteId());
        LocalDateTime agora = LocalDateTime.now(relogio);
        double valor = p.getTotal();
        p.registrarPagamento(new Pagamento(valor, meio, agora));

        double bonus = c.registrarPedidoPago();
        if (bonus > 0) {
            p.notificar(agora, "Parabéns! " + c.getPedidosPagos() + " pedidos pagos: você ganhou "
                    + Formatador.kg(bonus) + " de lavagem grátis no próximo pedido.");
        }
        pedidos.salvar(p);
        clientes.salvar(c);
        return new Recibo(p, valor, bonus, c.pedidosAteProximoBonus());
    }

    public double assinarPlano(String clienteId) {
        Cliente c = clienteService.buscar(clienteId);
        c.assinarPlano(YearMonth.now(relogio));
        clientes.salvar(c);
        return Assinatura.MENSALIDADE;
    }

    public void cancelarPlano(String clienteId) {
        Cliente c = clienteService.buscar(clienteId);
        c.cancelarPlano();
        clientes.salvar(c);
    }
}
