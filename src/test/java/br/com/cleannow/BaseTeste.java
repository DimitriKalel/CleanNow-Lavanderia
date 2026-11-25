package br.com.cleannow;

import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.servico.PedidoService.Horario;
import br.com.cleannow.util.RelogioAjustavel;
import org.junit.Before;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class BaseTeste {
    protected static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 16);
    protected RelogioAjustavel relogio;
    protected Sistema s;

    @Before
    public void preparar() {
        relogio = new RelogioAjustavel(ZoneId.of("America/Fortaleza"));
        relogio.fixar(LocalDateTime.of(SEGUNDA, java.time.LocalTime.of(9, 0)));
        s = Sistema.emMemoria(relogio, Path.of("target", "relatorios-teste"));
    }

    protected Cliente comum() {
        return s.clientes().cadastrar("Cliente Comum", "86999990000", "comum" + System.nanoTime() + "@x.com", "Rua A, 1", false);
    }

    protected Cliente vip() {
        return s.clientes().cadastrar("Cliente VIP", "86999990001", "vip" + System.nanoTime() + "@x.com", "Rua B, 2", true);
    }

    protected static Horario tarde(LocalDate d) {
        return new Horario(d, Periodo.TARDE);
    }
}
