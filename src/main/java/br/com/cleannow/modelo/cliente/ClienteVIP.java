package br.com.cleannow.modelo.cliente;

import java.time.LocalDate;

public class ClienteVIP extends Cliente {
    public static final String TIPO = "VIP";
    public static final double DESCONTO = 0.15;

    public ClienteVIP(String id, String nome, String telefone, String email, String endereco, LocalDate dataCadastro) {
        super(id, nome, telefone, email, endereco, dataCadastro);
    }

    public static ClienteVIP aPartirDe(Cliente outro) {
        ClienteVIP c = new ClienteVIP(outro.getId(), outro.getNome(), outro.getTelefone(),
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
        return DESCONTO;
    }

    @Override
    public double getTaxaPorAgendamento() {
        return 0.0;
    }

    @Override
    public int getPrioridade() {
        return 1;
    }
}
