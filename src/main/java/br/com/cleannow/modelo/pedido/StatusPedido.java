package br.com.cleannow.modelo.pedido;

import br.com.cleannow.excecao.TransicaoStatusInvalidaException;

public enum StatusPedido {
    AGUARDANDO_COLETA("Aguardando coleta"),
    COLETADO("Coletado"),
    EM_PROCESSO("Em processo"),
    PRONTO("Pronto"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusPedido proximo() {
        if (this == ENTREGUE || this == CANCELADO) {
            throw new TransicaoStatusInvalidaException(
                    "O pedido está " + descricao.toLowerCase() + " e não pode mais mudar de status.");
        }
        return values()[ordinal() + 1];
    }

    public boolean isFinalizado() {
        return this == ENTREGUE || this == CANCELADO;
    }

    public boolean isNaFilaDeServico() {
        return this == COLETADO || this == EM_PROCESSO;
    }
}
