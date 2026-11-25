package br.com.cleannow.modelo.cliente;

import java.time.LocalDate;

public class ClienteComum extends Cliente {
    public static final String TIPO = "COMUM";
    public static final double TAXA_AGENDAMENTO = 10.00;

    public ClienteComum(String id, String nome, String telefone, String email, String endereco, LocalDate dataCadastro) {
        super(id, nome, telefone, email, endereco, dataCadastro);
    }

    public static ClienteComum aPartirDe(Cliente outro) {
        ClienteComum c = new ClienteComum(outro.getId(), outro.getNome(), outro.getTelefone(),
                outro.getEmail(), outro.getEndereco(), outro.getDataCadastro());
        c.copiarHistoricoDe(outro);
        return c;
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    @Override
    public double getDescontoServicos() {
        return 0.0;
    }

    @Override
    public double getTaxaPorAgendamento() {
        return TAXA_AGENDAMENTO;
    }

    @Override
    public int getPrioridade() {
        return 2;
    }
}
