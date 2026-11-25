package br.com.cleannow.modelo.cliente;

import br.com.cleannow.excecao.PagamentoException;
import br.com.cleannow.modelo.Identificavel;
import br.com.cleannow.modelo.pagamento.Assinatura;
import br.com.cleannow.util.Validador;

import java.time.LocalDate;
import java.time.YearMonth;

public abstract class Cliente implements Identificavel {
    public static final int PEDIDOS_PARA_BONUS = 10;
    public static final double KG_BONUS = 5.0;

    private String id;
    private String nome;
    private String telefone;
    private String email;
    private String endereco;
    private LocalDate dataCadastro;
    private Preferencias preferencias = new Preferencias();
    private Assinatura assinatura;
    private int pedidosPagos;
    private double kgBonusDisponivel;

    protected Cliente(String id, String nome, String telefone, String email, String endereco, LocalDate dataCadastro) {
        this.id = id;
        setNome(nome);
        setTelefone(telefone);
        setEmail(email);
        setEndereco(endereco);
        this.dataCadastro = dataCadastro;
    }

    public abstract String getTipo();

    public abstract double getDescontoServicos();

    public abstract double getTaxaPorAgendamento();

    public abstract int getPrioridade();

    public double registrarPedidoPago() {
        pedidosPagos++;
        if (pedidosPagos % PEDIDOS_PARA_BONUS == 0) {
            kgBonusDisponivel += KG_BONUS;
            return KG_BONUS;
        }
        return 0;
    }

    public double usarBonus(double kgDesejados) {
        double usado = Math.min(kgDesejados, kgBonusDisponivel);
        kgBonusDisponivel -= usado;
        return usado;
    }

    public void devolverBonus(double kg) {
        kgBonusDisponivel += kg;
    }

    public int pedidosAteProximoBonus() {
        return PEDIDOS_PARA_BONUS - (pedidosPagos % PEDIDOS_PARA_BONUS);
    }

    public boolean temAssinaturaAtiva() {
        return assinatura != null && assinatura.isAtiva();
    }

    public void assinarPlano(YearMonth mes) {
        if (temAssinaturaAtiva()) {
            throw new PagamentoException(nome + " já possui uma assinatura ativa.");
        }
        this.assinatura = new Assinatura(mes);
    }

    public void cancelarPlano() {
        if (!temAssinaturaAtiva()) {
            throw new PagamentoException(nome + " não possui assinatura ativa.");
        }
        assinatura.cancelar();
    }

    protected void copiarHistoricoDe(Cliente outro) {
        this.preferencias = outro.preferencias;
        this.assinatura = outro.assinatura;
        this.pedidosPagos = outro.pedidosPagos;
        this.kgBonusDisponivel = outro.kgBonusDisponivel;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        this.nome = Validador.textoObrigatorio(nome, "Nome");
    }

    public String getTelefone() {
        return telefone;
    }

    public final void setTelefone(String telefone) {
        this.telefone = Validador.telefone(telefone);
    }

    public String getTelefoneFormatado() {
        String t = telefone;
        return t.length() == 11
                ? String.format("(%s) %s-%s", t.substring(0, 2), t.substring(2, 7), t.substring(7))
                : String.format("(%s) %s-%s", t.substring(0, 2), t.substring(2, 6), t.substring(6));
    }

    public String getEmail() {
        return email;
    }

    public final void setEmail(String email) {
        this.email = Validador.email(email);
    }

    public String getEndereco() {
        return endereco;
    }

    public final void setEndereco(String endereco) {
        this.endereco = Validador.textoObrigatorio(endereco, "Endereço");
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public Preferencias getPreferencias() {
        if (preferencias == null) {
            preferencias = new Preferencias();
        }
        return preferencias;
    }

    public Assinatura getAssinatura() {
        return assinatura;
    }

    public int getPedidosPagos() {
        return pedidosPagos;
    }

    public double getKgBonusDisponivel() {
        return kgBonusDisponivel;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", id, nome, getTipo());
    }
}
