package br.com.cleannow.modelo.servico;

public class PassarRoupa extends ServicoPorPeca {
    public static final String TIPO = "PASSAR";
    public static final String CODIGO = "PAS";

    public PassarRoupa(double precoPorPeca) {
        super(CODIGO, "Passar roupa", precoPorPeca);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
