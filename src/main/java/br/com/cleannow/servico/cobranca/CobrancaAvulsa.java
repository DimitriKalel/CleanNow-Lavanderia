package br.com.cleannow.servico.cobranca;

import br.com.cleannow.modelo.cliente.Cliente;

public class CobrancaAvulsa extends EstrategiaCobranca {
    @Override
    public String getNome() {
        return "Pagamento por serviço";
    }

    @Override
    protected double kgCobertosPeloPlano(Cliente cliente, double pesoKg) {
        return 0;
    }
}
