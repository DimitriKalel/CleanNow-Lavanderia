package br.com.cleannow.modelo.pagamento;

import br.com.cleannow.util.Formatador;

import java.time.LocalDateTime;

public class Pagamento {
    private final double valor;
    private final MeioPagamento meio;
    private final LocalDateTime dataHora;

    public Pagamento(double valor, MeioPagamento meio, LocalDateTime dataHora) {
        this.valor = valor;
        this.meio = meio;
        this.dataHora = dataHora;
    }

    public double getValor() {
        return valor;
    }

    public MeioPagamento getMeio() {
        return meio;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        return Formatador.moeda(valor) + " via " + meio.getDescricao() + " em " + Formatador.dataHora(dataHora);
    }
}
