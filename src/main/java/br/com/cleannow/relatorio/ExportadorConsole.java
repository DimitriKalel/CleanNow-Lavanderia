package br.com.cleannow.relatorio;

import java.util.List;

public class ExportadorConsole implements ExportadorRelatorio {
    private static final int LARGURA = 78;

    @Override
    public String getFormato() {
        return "Console";
    }

    @Override
    public String exportar(Documento doc, String nomeBase) {
        StringBuilder sb = new StringBuilder();
        String linha = "=".repeat(LARGURA);
        sb.append(linha).append('\n').append(centralizar(doc.getTitulo())).append('\n');
        if (doc.getSubtitulo() != null && !doc.getSubtitulo().isBlank()) {
            sb.append(centralizar(doc.getSubtitulo())).append('\n');
        }
        sb.append(linha).append('\n');
        for (Documento.Bloco b : doc.getBlocos()) {
            if (b instanceof Documento.Secao s) {
                sb.append('\n').append(s.titulo().toUpperCase()).append('\n')
                        .append("-".repeat(Math.min(LARGURA, s.titulo().length()))).append('\n');
            } else if (b instanceof Documento.Paragrafo p) {
                sb.append(p.texto()).append('\n');
            } else if (b instanceof Documento.Destaque d) {
                sb.append(String.format(">> %s: %s%n", d.rotulo(), d.valor()));
            } else if (b instanceof Documento.Tabela t) {
                sb.append(tabela(t));
            }
        }
        return sb.toString();
    }

    private String tabela(Documento.Tabela t) {
        int colunas = t.cabecalho().isEmpty() ? (t.linhas().isEmpty() ? 0 : t.linhas().get(0).size())
                : t.cabecalho().size();
        int[] larg = new int[colunas];
        medir(t.cabecalho(), larg);
        for (List<String> l : t.linhas()) medir(l, larg);
        StringBuilder sb = new StringBuilder();
        if (!t.cabecalho().isEmpty()) {
            sb.append(formatar(t.cabecalho(), larg));
            int total = 0;
            for (int w : larg) total += w + 2;
            sb.append("-".repeat(Math.max(0, total - 2))).append('\n');
        }
        if (t.linhas().isEmpty()) {
            sb.append("(nenhum registro)\n");
        }
        for (List<String> l : t.linhas()) sb.append(formatar(l, larg));
        return sb.toString();
    }

    private static void medir(List<String> celulas, int[] larg) {
        for (int i = 0; i < celulas.size() && i < larg.length; i++) {
            larg[i] = Math.max(larg[i], texto(celulas.get(i)).length());
        }
    }

    private static String formatar(List<String> celulas, int[] larg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < larg.length; i++) {
            String c = i < celulas.size() ? texto(celulas.get(i)) : "";
            sb.append(String.format("%-" + Math.max(1, larg[i]) + "s", c));
            if (i < larg.length - 1) sb.append("  ");
        }
        return sb.toString().stripTrailing() + "\n";
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }

    private static String centralizar(String s) {
        int esp = Math.max(0, (LARGURA - s.length()) / 2);
        return " ".repeat(esp) + s;
    }
}
