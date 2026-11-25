package br.com.cleannow.relatorio;

public interface ExportadorRelatorio {
    String getFormato();

    String exportar(Documento documento, String nomeBase);
}
