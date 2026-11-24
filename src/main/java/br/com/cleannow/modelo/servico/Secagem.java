package br.com.cleannow.modelo.servico;

public class Secagem extends ServicoPorKg {
    public static final String TIPO = "SECAGEM";
    public static final String CODIGO = "SEC";

    public Secagem(double precoPorKg) {
        super(CODIGO, "Secagem", precoPorKg);
    }

    @Override
    public boolean isCobertoPelaFranquia() {
        return true;
    }

    @Override
    public String getTipo() {
        return TIPO;
    }
}
