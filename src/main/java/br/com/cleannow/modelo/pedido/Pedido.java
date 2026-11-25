package br.com.cleannow.modelo.pedido;

import br.com.cleannow.excecao.PagamentoException;
import br.com.cleannow.excecao.TransicaoStatusInvalidaException;
import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.Identificavel;
import br.com.cleannow.modelo.pagamento.Orcamento;
import br.com.cleannow.modelo.pagamento.Pagamento;
import br.com.cleannow.util.Formatador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pedido implements Identificavel {
    private final String codigo;
    private final String clienteId;
    private final LocalDateTime criadoEm;
    private final List<String> codigosServicos;
    private final double pesoKg;
    private final int quantidadePecas;
    private final Orcamento orcamento;
    private final String pacoteAplicado;
    private StatusPedido status = StatusPedido.AGUARDANDO_COLETA;
    private final List<Notificacao> historico = new ArrayList<>();
    private Agendamento coleta;
    private Agendamento entrega;
    private Pagamento pagamento;

    public Pedido(String codigo, String clienteId, LocalDateTime criadoEm, List<String> codigosServicos,
                  double pesoKg, int quantidadePecas, Orcamento orcamento, String pacoteAplicado) {
        if (codigosServicos == null || codigosServicos.isEmpty()) {
            throw new ValidacaoException("O pedido precisa ter pelo menos um serviço.");
        }
        this.codigo = codigo;
        this.clienteId = clienteId;
        this.criadoEm = criadoEm;
        this.codigosServicos = new ArrayList<>(codigosServicos);
        this.pesoKg = pesoKg;
        this.quantidadePecas = quantidadePecas;
        this.orcamento = orcamento;
        this.pacoteAplicado = pacoteAplicado;
        notificar(criadoEm, "Pedido criado. Total dos serviços: " + Formatador.moeda(orcamento.getTotal()));
    }

    public StatusPedido avancarStatus(LocalDateTime agora) {
        StatusPedido novo = status.proximo();
        if (novo == StatusPedido.ENTREGUE && !isPago()) {
            throw new TransicaoStatusInvalidaException(
                    "O pedido " + codigo + " só pode ser entregue depois de pago.");
        }
        status = novo;
        notificar(agora, "Status atualizado para: " + novo.getDescricao().toUpperCase());
        return novo;
    }

    public void cancelar(LocalDateTime agora, String motivo) {
        if (status != StatusPedido.AGUARDANDO_COLETA) {
            throw new TransicaoStatusInvalidaException(
                    "Só é possível cancelar antes da coleta. Status atual: " + status.getDescricao());
        }
        if (isPago()) {
            throw new PagamentoException("Pedido já pago não pode ser cancelado pelo sistema; procure o atendimento.");
        }
        status = StatusPedido.CANCELADO;
        notificar(agora, "Pedido cancelado" + (motivo == null || motivo.isBlank() ? "." : ": " + motivo));
    }

    public void definirColeta(Agendamento ag, LocalDateTime agora) {
        validarAlteravel();
        if (status != StatusPedido.AGUARDANDO_COLETA) {
            throw new TransicaoStatusInvalidaException("O pedido já foi coletado.");
        }
        this.coleta = ag;
        notificar(agora, "Coleta agendada: " + ag);
    }

    public void definirEntrega(Agendamento ag, LocalDateTime agora) {
        validarAlteravel();
        if (status == StatusPedido.ENTREGUE) {
            throw new TransicaoStatusInvalidaException("O pedido já foi entregue.");
        }
        this.entrega = ag;
        notificar(agora, "Entrega agendada: " + ag);
    }

    private void validarAlteravel() {
        if (status == StatusPedido.CANCELADO) {
            throw new TransicaoStatusInvalidaException("O pedido " + codigo + " está cancelado.");
        }
        if (isPago()) {
            throw new PagamentoException("O pedido já foi pago; agendamentos com taxa devem ser feitos antes do pagamento.");
        }
    }

    public void registrarPagamento(Pagamento pagamento) {
        if (status == StatusPedido.CANCELADO) {
            throw new PagamentoException("Não é possível pagar um pedido cancelado.");
        }
        if (isPago()) {
            throw new PagamentoException("O pedido " + codigo + " já foi pago.");
        }
        this.pagamento = pagamento;
        notificar(pagamento.getDataHora(), "Pagamento confirmado: " + pagamento);
    }

    public double getTotal() {
        return Formatador.centavos(orcamento.getTotal() + getTotalTaxas());
    }

    public double getTotalTaxas() {
        double t = 0;
        if (coleta != null) t += coleta.getTaxa();
        if (entrega != null) t += entrega.getTaxa();
        return t;
    }

    public final void notificar(LocalDateTime quando, String mensagem) {
        historico.add(new Notificacao(quando, mensagem));
    }

    @Override
    public String getId() {
        return codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getClienteId() {
        return clienteId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<String> getCodigosServicos() {
        return Collections.unmodifiableList(codigosServicos);
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public int getQuantidadePecas() {
        return quantidadePecas;
    }

    public Orcamento getOrcamento() {
        return orcamento;
    }

    public String getPacoteAplicado() {
        return pacoteAplicado;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public List<Notificacao> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    public Agendamento getColeta() {
        return coleta;
    }

    public Agendamento getEntrega() {
        return entrega;
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public boolean isPago() {
        return pagamento != null;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s%s", codigo, Formatador.dataHora(criadoEm),
                status.getDescricao(), Formatador.moeda(getTotal()), isPago() ? " (pago)" : "");
    }
}
