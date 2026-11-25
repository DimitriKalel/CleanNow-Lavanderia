package br.com.cleannow.modelo.pedido;

import br.com.cleannow.util.Formatador;

import java.time.LocalDateTime;

public class Notificacao {
    private final LocalDateTime dataHora;
    private final String mensagem;

    public Notificacao(LocalDateTime dataHora, String mensagem) {
        this.dataHora = dataHora;
        this.mensagem = mensagem;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getMensagem() {
        return mensagem;
    }

    @Override
    public String toString() {
        return Formatador.dataHora(dataHora) + "  " + mensagem;
    }
}
