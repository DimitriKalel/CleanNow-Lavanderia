package br.com.cleannow.modelo.servico;

public class ServicoPorKg extends Servico {
    public static final String TIPO = "POR_KG";

    public ServicoPorKg(String codigo, String nome, double precoPorKg) {
        super(codigo, nome, precoPorKg);
    }

    @Override
    public double quantidadeCobrada(double pesoKg, int quantidadePecas) {
        return pesoKg;
    }

    @Override
    public String getUnidade() {
        return "kg";
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
