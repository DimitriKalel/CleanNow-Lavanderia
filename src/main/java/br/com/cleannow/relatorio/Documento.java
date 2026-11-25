package br.com.cleannow.relatorio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Documento {
    public interface Bloco {
    }

    public record Secao(String titulo) implements Bloco {
    }

    public record Paragrafo(String texto) implements Bloco {
    }

    public record Destaque(String rotulo, String valor) implements Bloco {
    }

    public record Tabela(List<String> cabecalho, List<List<String>> linhas) implements Bloco {
    }

    private final String titulo;
    private final String subtitulo;
    private final List<Bloco> blocos = new ArrayList<>();

    public Documento(String titulo, String subtitulo) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
    }

    public Documento secao(String titulo) {
        blocos.add(new Secao(titulo));
        return this;
    }

    public Documento paragrafo(String texto) {
        blocos.add(new Paragrafo(texto));
        return this;
    }

    public Documento destaque(String rotulo, String valor) {
        blocos.add(new Destaque(rotulo, valor));
        return this;
    }

    public Documento campos(String... paresCampoValor) {
        List<List<String>> linhas = new ArrayList<>();
        for (int i = 0; i + 1 < paresCampoValor.length; i += 2) {
            linhas.add(List.of(paresCampoValor[i], paresCampoValor[i + 1]));
        }
        blocos.add(new Tabela(List.of(), linhas));
        return this;
    }

    public Documento tabela(List<String> cabecalho, List<List<String>> linhas) {
        blocos.add(new Tabela(cabecalho, linhas));
        return this;
    }

    public static List<String> linha(String... celulas) {
        return Arrays.asList(celulas);
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public List<Bloco> getBlocos() {
        return Collections.unmodifiableList(blocos);
    }
}
