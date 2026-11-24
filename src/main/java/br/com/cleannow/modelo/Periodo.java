package br.com.cleannow.modelo;

import java.time.LocalTime;

public enum Periodo {
    MANHA("Manhã (08h-12h)", LocalTime.of(12, 0)),
    TARDE("Tarde (13h-18h)", LocalTime.of(18, 0));

    private final String descricao;
    private final LocalTime fim;

    Periodo(String descricao, LocalTime fim) {
        this.descricao = descricao;
        this.fim = fim;
    }

    public LocalTime getFim() {
        return fim;
    }

    public String getDescricao() {
        return descricao;
    }
}
