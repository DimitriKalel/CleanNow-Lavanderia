package br.com.cleannow.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatador {
    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private Formatador() {
    }

    public static String moeda(double valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor).replace(' ', ' ');
    }

    public static String data(LocalDate d) {
        return d == null ? "-" : d.format(DATA);
    }

    public static String dataHora(LocalDateTime d) {
        return d == null ? "-" : d.format(DATA_HORA);
    }

    public static String mes(YearMonth m) {
        return m == null ? "-" : m.format(MES);
    }

    public static String kg(double kg) {
        return String.format(BR, "%.1f kg", kg);
    }

    public static String decimal(double valor, int casas) {
        return String.format(BR, "%." + casas + "f", valor);
    }

    public static String percentual(double fracao) {
        return String.format(BR, "%.0f%%", fracao * 100);
    }

    public static double centavos(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
