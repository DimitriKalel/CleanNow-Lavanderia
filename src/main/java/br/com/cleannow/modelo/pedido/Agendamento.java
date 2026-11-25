package br.com.cleannow.modelo.pedido;

import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.util.Formatador;

import java.time.LocalDate;

public class Agendamento {
    private final TipoAgendamento tipo;
    private final LocalDate data;
    private final Periodo periodo;
    private final double taxa;

    public Agendamento(TipoAgendamento tipo, LocalDate data, Periodo periodo, double taxa) {
        this.tipo = tipo;
        this.data = data;
        this.periodo = periodo;
        this.taxa = taxa;
    }

    public boolean mesmoHorario(TipoAgendamento tipo, LocalDate data, Periodo periodo) {
        return this.tipo == tipo && this.data.equals(data) && this.periodo == periodo;
    }

    public TipoAgendamento getTipo() {
        return tipo;
    }

    public LocalDate getData() {
        return data;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public double getTaxa() {
        return taxa;
    }

    @Override
    public String toString() {
        return String.format("%s em %s, %s (taxa %s)", tipo.getDescricao(), Formatador.data(data),
                periodo.getDescricao(), taxa == 0 ? "grátis" : Formatador.moeda(taxa));
    }
}
