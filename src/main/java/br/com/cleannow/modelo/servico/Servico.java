package br.com.cleannow.modelo.servico;

import br.com.cleannow.modelo.Identificavel;
import br.com.cleannow.util.Formatador;
import br.com.cleannow.util.Validador;

public abstract class Servico implements Identificavel {
    private final String codigo;
    private String nome;
    private double precoUnitario;

    protected Servico(String codigo, String nome, double precoUnitario) {
        this.codigo = Validador.textoObrigatorio(codigo, "Código do serviço").toUpperCase();
        setNome(nome);
        setPrecoUnitario(precoUnitario);
    }

    public abstract double quantidadeCobrada(double pesoKg, int quantidadePecas);

    public abstract String getUnidade();

    public String getUnidadePlural() {
        return getUnidade();
    }

    public abstract String getTipo();

    public boolean isCobertoPelaFranquia() {
        return false;
    }

    public boolean isElegivelBonusFidelidade() {
        return false;
    }

    public double calcularValor(double pesoKg, int quantidadePecas) {
        return precoUnitario * quantidadeCobrada(pesoKg, quantidadePecas);
    }

    @Override
    public String getId() {
        return codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        this.nome = Validador.textoObrigatorio(nome, "Nome do serviço");
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public final void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = Validador.positivo(precoUnitario, "Preço do serviço");
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s/%s)", codigo, nome, Formatador.moeda(precoUnitario), getUnidade());
    }
}
