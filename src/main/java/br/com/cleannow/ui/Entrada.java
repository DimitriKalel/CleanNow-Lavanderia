package br.com.cleannow.ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

public class Entrada {
    public static class EntradaEncerradaException extends RuntimeException {
        public EntradaEncerradaException() {
            super("Entrada encerrada.");
        }
    }

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Scanner scanner;
    private final PrintStream out;
    private final java.time.Clock relogio;

    public Entrada(Scanner scanner, PrintStream out, java.time.Clock relogio) {
        this.scanner = scanner;
        this.out = out;
        this.relogio = relogio;
    }

    public String linha(String pergunta) {
        out.print(pergunta);
        out.flush();
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            throw new EntradaEncerradaException();
        }
    }

    public String texto(String pergunta) {
        while (true) {
            String s = linha(pergunta);
            if (!s.isEmpty()) return s;
            out.println("  Campo obrigatório.");
        }
    }

    public String textoOuManter(String pergunta, String atual) {
        String s = linha(pergunta + " [" + atual + "]: ");
        return s.isEmpty() ? atual : s;
    }

    public int inteiro(String pergunta, int min, int max) {
        while (true) {
            String s = linha(pergunta);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignorado) {
            }
            out.printf("  Digite um número inteiro entre %d e %d.%n", min, max);
        }
    }

    public double decimal(String pergunta, double min, double max) {
        while (true) {
            String s = linha(pergunta).replace(',', '.');
            try {
                double v = Double.parseDouble(s);
                if (!Double.isNaN(v) && v >= min && v <= max) return v;
            } catch (NumberFormatException ignorado) {
            }
            out.printf("  Digite um número entre %s e %s (use vírgula ou ponto).%n", fmt(min), fmt(max));
        }
    }

    public boolean simNao(String pergunta) {
        while (true) {
            String s = linha(pergunta + " (s/n): ").toLowerCase();
            if (s.startsWith("s")) return true;
            if (s.startsWith("n")) return false;
            out.println("  Responda s ou n.");
        }
    }

    public LocalDate data(String pergunta) {
        while (true) {
            String s = linha(pergunta + " (dd/mm/aaaa, 'hoje' ou 'amanha'): ").toLowerCase();
            LocalDate hoje = LocalDate.now(relogio);
            if (s.equals("hoje")) return hoje;
            if (s.equals("amanha") || s.equals("amanhã")) return hoje.plusDays(1);
            try {
                return LocalDate.parse(s, DATA);
            } catch (DateTimeParseException e) {
                out.println("  Data inválida. Exemplo: " + hoje.format(DATA));
            }
        }
    }

    public <T> T escolher(String titulo, List<T> opcoes, Function<T, String> rotulo) {
        out.println(titulo);
        for (int i = 0; i < opcoes.size(); i++) {
            out.printf("  %d) %s%n", i + 1, rotulo.apply(opcoes.get(i)));
        }
        return opcoes.get(inteiro("Opção: ", 1, opcoes.size()) - 1);
    }

    public <T> List<T> escolherVarios(String titulo, List<T> opcoes, Function<T, String> rotulo) {
        out.println(titulo);
        for (int i = 0; i < opcoes.size(); i++) {
            out.printf("  %d) %s%n", i + 1, rotulo.apply(opcoes.get(i)));
        }
        while (true) {
            String s = linha("Números separados por vírgula (ex.: 1,2,3): ");
            List<T> escolhidos = new ArrayList<>();
            boolean ok = !s.isBlank();
            for (String parte : s.split("[,;\\s]+")) {
                if (parte.isBlank()) continue;
                try {
                    int n = Integer.parseInt(parte);
                    if (n < 1 || n > opcoes.size()) {
                        ok = false;
                        break;
                    }
                    T item = opcoes.get(n - 1);
                    if (!escolhidos.contains(item)) escolhidos.add(item);
                } catch (NumberFormatException e) {
                    ok = false;
                    break;
                }
            }
            if (ok && !escolhidos.isEmpty()) return escolhidos;
            out.printf("  Use números de 1 a %d separados por vírgula.%n", opcoes.size());
        }
    }

    public void pausar() {
        linha("\nPressione Enter para continuar...");
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
