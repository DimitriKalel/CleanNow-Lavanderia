package br.com.cleannow.modelo.pagamento;

import br.com.cleannow.util.Formatador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Orcamento {
    public static class Linha {
        private final String descricao;
        private final double valor;

        public Linha(String descricao, double valor) {
            this.descricao = descricao;
            this.valor = Formatador.centavos(valor);
        }

        public String getDescricao() {
            return descricao;
        }

        public double getValor() {
            return valor;
        }
    }

    private final List<Linha> linhas = new ArrayList<>();
    private double kgFranquiaUsados;
    private double kgBonusUsados;
    private String modalidade;

    public void adicionar(String descricao, double valor) {
        linhas.add(new Linha(descricao, valor));
    }

    public double getTotal() {
        double total = 0;
        for (Linha l : linhas) {
            total += l.getValor();
        }
        return Formatador.centavos(Math.max(0, total));
    }

    public double getValorBruto() {
        double bruto = 0;
        for (Linha l : linhas) {
            if (l.getValor() > 0) bruto += l.getValor();
        }
        return Formatador.centavos(bruto);
    }

    public double getTotalAbatimentos() {
        return Formatador.centavos(getValorBruto() - getTotal());
    }

    public List<Linha> getLinhas() {
        return Collections.unmodifiableList(linhas);
    }

    public double getKgFranquiaUsados() {
        return kgFranquiaUsados;
    }

    public void setKgFranquiaUsados(double kg) {
        this.kgFranquiaUsados = kg;
    }

    public double getKgBonusUsados() {
        return kgBonusUsados;
    }

    public void setKgBonusUsados(double kg) {
        this.kgBonusUsados = kg;
    }

    public String getModalidade() {
        return modalidade;
    }

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    }
}
