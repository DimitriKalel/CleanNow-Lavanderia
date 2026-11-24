package br.com.cleannow.modelo.servico;

public class Lavagem extends ServicoPorKg {
    public static final String TIPO = "LAVAGEM";
    public static final String CODIGO = "LAV";

    public Lavagem(double precoPorKg) {
        super(CODIGO, "Lavagem", precoPorKg);
    }

    @Override
    public boolean isCobertoPelaFranquia() {
        return true;
    }

    @Override
    public boolean isElegivelBonusFidelidade() {
        return true;
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
