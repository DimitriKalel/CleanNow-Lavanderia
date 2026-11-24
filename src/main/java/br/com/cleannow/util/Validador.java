package br.com.cleannow.util;

import br.com.cleannow.excecao.ValidacaoException;

import java.util.regex.Pattern;

public final class Validador {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validador() {
    }

    public static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException(campo + " é obrigatório.");
        }
        return valor.trim();
    }

    public static String email(String valor) {
        String v = textoObrigatorio(valor, "E-mail").toLowerCase();
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidacaoException("E-mail inválido: " + valor);
        }
        return v;
    }

    public static String telefone(String valor) {
        String digitos = textoObrigatorio(valor, "Telefone").replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new ValidacaoException("Telefone deve ter DDD + 8 ou 9 dígitos: " + valor);
        }
        return digitos;
    }

    public static double positivo(double valor, String campo) {
        if (Double.isNaN(valor) || valor <= 0) {
            throw new ValidacaoException(campo + " deve ser maior que zero.");
        }
        return valor;
    }

    public static double naoNegativo(double valor, String campo) {
        if (Double.isNaN(valor) || valor < 0) {
            throw new ValidacaoException(campo + " não pode ser negativo.");
        }
        return valor;
    }

    public static double percentual(double valor, String campo) {
        if (Double.isNaN(valor) || valor < 0 || valor >= 1) {
            throw new ValidacaoException(campo + " deve estar entre 0% e 99%.");
        }
        return valor;
    }
}
