package br.com.cleannow.modelo.servico;

import br.com.cleannow.excecao.ValidacaoException;
import br.com.cleannow.modelo.Identificavel;
import br.com.cleannow.util.Validador;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Pacote implements Identificavel {
    private final String id;
    private String nome;
    private final List<String> codigosServicos;
    private double desconto;

    public Pacote(String id, String nome, Collection<String> codigosServicos, double desconto) {
        this.id = Validador.textoObrigatorio(id, "Código do pacote").toUpperCase();
        this.nome = Validador.textoObrigatorio(nome, "Nome do pacote");
        if (codigosServicos == null || codigosServicos.size() < 2) {
            throw new ValidacaoException("Um pacote precisa ter pelo menos 2 serviços.");
        }
        this.codigosServicos = new ArrayList<>(codigosServicos);
        this.desconto = Validador.percentual(desconto, "Desconto do pacote");
    }

    public boolean aplicavelA(Collection<String> codigosDoPedido) {
        return codigosDoPedido.containsAll(codigosServicos);
    }

    public boolean contemServico(String codigo) {
        return codigosServicos.contains(codigo);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<String> getCodigosServicos() {
        return Collections.unmodifiableList(codigosServicos);
    }

    public double getDesconto() {
        return desconto;
    }

    public void setDesconto(double desconto) {
        this.desconto = Validador.percentual(desconto, "Desconto do pacote");
    }

    @Override
    public String toString() {
        return String.format("%s - %s %s (%.0f%% de desconto)", id, nome, codigosServicos, desconto * 100);
    }
}
