package br.com.cleannow.servico.cobranca;

import br.com.cleannow.modelo.cliente.Cliente;

public class CobrancaAssinatura extends EstrategiaCobranca {
    @Override
    public String getNome() {
        return "Plano de assinatura mensal";
    }

    @Override
    protected double kgCobertosPeloPlano(Cliente cliente, double pesoKg) {
        return cliente.getAssinatura().consumirFranquia(pesoKg);
    }
}
