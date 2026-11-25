package br.com.cleannow.modelo.pedido;

public enum TipoAgendamento {
    COLETA("Coleta"),
    ENTREGA("Entrega");

    private final String descricao;

    TipoAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
