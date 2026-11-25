package br.com.cleannow.servico;

import br.com.cleannow.excecao.EntidadeNaoEncontradaException;
import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.Assinatura;
import br.com.cleannow.modelo.pagamento.Orcamento;
import br.com.cleannow.modelo.pedido.Agendamento;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.modelo.pedido.TipoAgendamento;
import br.com.cleannow.modelo.servico.Pacote;
import br.com.cleannow.modelo.servico.Servico;
import br.com.cleannow.repositorio.Repositorio;
import br.com.cleannow.servico.cobranca.CobrancaAssinatura;
import br.com.cleannow.servico.cobranca.CobrancaAvulsa;
import br.com.cleannow.servico.cobranca.EstrategiaCobranca;
import br.com.cleannow.util.Formatador;
import br.com.cleannow.util.Validador;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoService {
    public static final double PESO_MAXIMO_KG = 50.0;
    public static final int PECAS_MAXIMO = 200;

    private final Repositorio<Pedido> pedidos;
    private final ClienteService clienteService;
    private final CatalogoService catalogo;
    private final AgendaService agenda;
    private final Repositorio<Cliente> clientes;
    private final Clock relogio;

    public PedidoService(Repositorio<Pedido> pedidos, Repositorio<Cliente> clientes, ClienteService clienteService,
                         CatalogoService catalogo, AgendaService agenda, Clock relogio) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.clienteService = clienteService;
        this.catalogo = catalogo;
        this.agenda = agenda;
        this.relogio = relogio;
    }

    public record Horario(LocalDate data, Periodo periodo) {
    }

    public Pedido criar(String clienteId, List<String> codigosServicos, double pesoKg, int quantidadePecas,
                        Horario coleta, Horario entrega) {
        Cliente cliente = clienteService.buscar(clienteId);
        List<String> codigos = normalizarCodigos(codigosServicos);
        List<Servico> servicos = catalogo.buscarServicos(codigos);
        validarQuantidades(servicos, pesoKg, quantidadePecas);

        LocalDateTime agora = LocalDateTime.now(relogio);

        Agendamento agColeta = coleta == null ? null
                : agenda.reservar(TipoAgendamento.COLETA, coleta.data(), coleta.periodo(), cliente, null);
        LocalDate minimoEntrega = agColeta != null ? agColeta.getData() : agora.toLocalDate();
        Agendamento agEntrega = entrega == null ? null
                : agenda.reservar(TipoAgendamento.ENTREGA, entrega.data(), entrega.periodo(), cliente, minimoEntrega);

        String avisoMensalidade = renovarAssinaturaSeNecessario(cliente, YearMonth.from(agora));
        Pacote pacote = catalogo.melhorPacotePara(codigos);
        Orcamento orc = escolherCobranca(cliente).calcular(cliente, servicos, pesoKg, quantidadePecas, pacote);

        Pedido pedido = new Pedido(proximoCodigo(agora), cliente.getId(), agora, codigos, pesoKg, quantidadePecas,
                orc, pacote == null ? null : pacote.getNome());
        if (avisoMensalidade != null) pedido.notificar(agora, avisoMensalidade);
        if (agColeta != null) pedido.definirColeta(agColeta, agora);
        if (agEntrega != null) pedido.definirEntrega(agEntrega, agora);
        if (agColeta == null) pedido.notificar(agora, "Cliente vai levar as roupas à loja.");

        pedidos.salvar(pedido);
        clientes.salvar(cliente);
        return pedido;
    }

    private EstrategiaCobranca escolherCobranca(Cliente cliente) {
        return cliente.temAssinaturaAtiva() ? new CobrancaAssinatura() : new CobrancaAvulsa();
    }

    private String renovarAssinaturaSeNecessario(Cliente cliente, YearMonth mes) {
        if (cliente.temAssinaturaAtiva() && cliente.getAssinatura().atualizarMes(mes)) {
            return "Assinatura renovada para " + Formatador.mes(mes) + ": mensalidade de "
                    + Formatador.moeda(Assinatura.MENSALIDADE) + " registrada e franquia de "
                    + Formatador.kg(Assinatura.FRANQUIA_KG) + " liberada.";
        }
        return null;
    }

    private List<String> normalizarCodigos(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            throw new ValidacaoException("Escolha pelo menos um serviço.");
        }
        return new ArrayList<>(codigos.stream().map(c -> c.trim().toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private void validarQuantidades(List<Servico> servicos, double pesoKg, int pecas) {
        boolean porKg = servicos.stream().anyMatch(s -> s.quantidadeCobrada(1, 0) > 0);
        boolean porPeca = servicos.stream().anyMatch(s -> s.quantidadeCobrada(0, 1) > 0);
        if (porKg) {
            Validador.positivo(pesoKg, "Peso das roupas");
            if (pesoKg > PESO_MAXIMO_KG) {
                throw new ValidacaoException("Peso máximo por pedido: " + Formatador.kg(PESO_MAXIMO_KG) + ".");
            }
        } else {
            Validador.naoNegativo(pesoKg, "Peso das roupas");
        }
        if (porPeca && pecas <= 0) {
            throw new ValidacaoException("Informe a quantidade de peças para passar.");
        }
        if (pecas < 0 || pecas > PECAS_MAXIMO) {
            throw new ValidacaoException("Quantidade de peças deve estar entre 0 e " + PECAS_MAXIMO + ".");
        }
    }

    public Pedido buscar(String codigo) {
        return pedidos.buscarPorId(codigo)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pedido não encontrado: " + codigo));
    }

    public Pedido rastrear(String codigo) {
        return buscar(codigo);
    }

    public StatusPedido avancarStatus(String codigo) {
        Pedido p = buscar(codigo);
        StatusPedido novo = p.avancarStatus(LocalDateTime.now(relogio));
        pedidos.salvar(p);
        return novo;
    }

    public Pedido agendarEntrega(String codigo, LocalDate data, Periodo periodo) {
        Pedido p = buscar(codigo);
        if (p.getEntrega() != null) {
            throw new ValidacaoException("O pedido já tem entrega agendada: " + p.getEntrega());
        }
        Cliente c = clienteService.buscar(p.getClienteId());
        LocalDate minimo = p.getColeta() != null ? p.getColeta().getData() : LocalDate.now(relogio);
        Agendamento ag = agenda.reservar(TipoAgendamento.ENTREGA, data, periodo, c, minimo);
        p.definirEntrega(ag, LocalDateTime.now(relogio));
        pedidos.salvar(p);
        return p;
    }

    public void cancelar(String codigo, String motivo) {
        Pedido p = buscar(codigo);
        p.cancelar(LocalDateTime.now(relogio), motivo);
        Cliente c = clienteService.buscar(p.getClienteId());
        Orcamento o = p.getOrcamento();
        if (o.getKgBonusUsados() > 0) {
            c.devolverBonus(o.getKgBonusUsados());
        }
        if (o.getKgFranquiaUsados() > 0 && c.getAssinatura() != null) {
            c.getAssinatura().devolverFranquia(o.getKgFranquiaUsados(), YearMonth.from(p.getCriadoEm()));
        }
        pedidos.salvar(p);
        clientes.salvar(c);
    }

    public List<Pedido> listarTodos() {
        return pedidos.listarTodos();
    }

    public List<Pedido> listarPorCliente(String clienteId) {
        return pedidos.listarTodos().stream()
                .filter(p -> p.getClienteId().equalsIgnoreCase(clienteId))
                .sorted(Comparator.comparing(Pedido::getCriadoEm))
                .collect(Collectors.toList());
    }

    public List<Pedido> listarPorStatus(StatusPedido status) {
        return pedidos.listarTodos().stream().filter(p -> p.getStatus() == status).collect(Collectors.toList());
    }

    private String proximoCodigo(LocalDateTime agora) {
        String prefixo = "CN" + agora.getYear() + "-";
        int maior = 0;
        for (Pedido p : pedidos.listarTodos()) {
            if (p.getCodigo().startsWith(prefixo)) {
                try {
                    maior = Math.max(maior, Integer.parseInt(p.getCodigo().substring(prefixo.length())));
                } catch (NumberFormatException ignorado) {
                }
            }
        }
        return prefixo + String.format("%04d", maior + 1);
    }
}
