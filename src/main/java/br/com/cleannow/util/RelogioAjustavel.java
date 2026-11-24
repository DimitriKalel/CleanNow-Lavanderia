package br.com.cleannow.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class RelogioAjustavel extends Clock {
    private final ZoneId zona;
    private Instant fixo;

    public RelogioAjustavel(ZoneId zona) {
        this.zona = zona;
    }

    public static RelogioAjustavel sistema() {
        return new RelogioAjustavel(ZoneId.systemDefault());
    }

    public void fixar(LocalDateTime dataHora) {
        this.fixo = dataHora.atZone(zona).toInstant();
    }

    public void voltarAoNormal() {
        this.fixo = null;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        RelogioAjustavel r = new RelogioAjustavel(zone);
        r.fixo = this.fixo;
        return r;
    }

    @Override
    public Instant instant() {
        return fixo != null ? fixo : Instant.now();
    }
}
