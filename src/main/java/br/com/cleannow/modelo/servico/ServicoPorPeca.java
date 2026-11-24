package br.com.cleannow.modelo.servico;

public class ServicoPorPeca extends Servico {
    public static final String TIPO = "POR_PECA";

    public ServicoPorPeca(String codigo, String nome, double precoPorPeca) {
        super(codigo, nome, precoPorPeca);
    }

    @Override
    public double quantidadeCobrada(double pesoKg, int quantidadePecas) {
        return quantidadePecas;
    }

    @Override
    public String getUnidade() {
        return "peça";
    }

    @Override
    public String getUnidadePlural() {
        return "peças";
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
