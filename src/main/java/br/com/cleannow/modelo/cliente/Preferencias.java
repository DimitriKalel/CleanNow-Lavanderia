package br.com.cleannow.modelo.cliente;

import br.com.cleannow.modelo.Periodo;

public class Preferencias {
    private Periodo periodoPreferido;
    private boolean amaciante = true;
    private boolean roupasDobradas = true;
    private boolean sabaoHipoalergenico;
    private String observacoes = "";

    public Periodo getPeriodoPreferido() {
        return periodoPreferido;
    }

    public void setPeriodoPreferido(Periodo periodoPreferido) {
        this.periodoPreferido = periodoPreferido;
    }

    public boolean isAmaciante() {
        return amaciante;
    }

    public void setAmaciante(boolean amaciante) {
        this.amaciante = amaciante;
    }

    public boolean isRoupasDobradas() {
        return roupasDobradas;
    }

    public void setRoupasDobradas(boolean roupasDobradas) {
        this.roupasDobradas = roupasDobradas;
    }

    public boolean isSabaoHipoalergenico() {
        return sabaoHipoalergenico;
    }

    public void setSabaoHipoalergenico(boolean sabaoHipoalergenico) {
        this.sabaoHipoalergenico = sabaoHipoalergenico;
    }

    public String getObservacoes() {
        return observacoes == null ? "" : observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes == null ? "" : observacoes.trim();
    }

    public String resumo() {
        StringBuilder sb = new StringBuilder();
        sb.append(roupasDobradas ? "dobrar roupas" : "roupas em cabide");
        sb.append(", amaciante: ").append(amaciante ? "sim" : "não");
        if (sabaoHipoalergenico) sb.append(", sabão hipoalergênico");
        if (periodoPreferido != null) sb.append(", prefere ").append(periodoPreferido.getDescricao());
        if (!getObservacoes().isEmpty()) sb.append(", obs.: ").append(observacoes);
        return sb.toString();
    }
}
