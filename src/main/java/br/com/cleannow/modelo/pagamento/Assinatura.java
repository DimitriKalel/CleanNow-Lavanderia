package br.com.cleannow.modelo.pagamento;

import br.com.cleannow.excecao.PagamentoException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Assinatura {
    public static final double MENSALIDADE = 150.00;
    public static final double FRANQUIA_KG = 20.0;

    private boolean ativa;
    private YearMonth inicio;
    private YearMonth mesReferencia;
    private double kgUsadosNoMes;
    private final List<YearMonth> mensalidadesPagas = new ArrayList<>();

    public Assinatura(YearMonth mesInicio) {
        this.ativa = true;
        this.inicio = mesInicio;
        this.mesReferencia = mesInicio;
        this.mensalidadesPagas.add(mesInicio);
    }

    public boolean atualizarMes(YearMonth atual) {
        if (!ativa || !atual.isAfter(mesReferencia)) {
            return false;
        }
        mesReferencia = atual;
        kgUsadosNoMes = 0;
        mensalidadesPagas.add(atual);
        return true;
    }

    public double getFranquiaRestante() {
        return ativa ? Math.max(0, FRANQUIA_KG - kgUsadosNoMes) : 0;
    }

    public double consumirFranquia(double kg) {
        if (!ativa) {
            throw new PagamentoException("A assinatura está cancelada.");
        }
        double coberto = Math.min(kg, getFranquiaRestante());
        kgUsadosNoMes += coberto;
        return coberto;
    }

    public void devolverFranquia(double kg, YearMonth mesDoPedido) {
        if (mesDoPedido.equals(mesReferencia)) {
            kgUsadosNoMes = Math.max(0, kgUsadosNoMes - kg);
        }
    }

    public void cancelar() {
        if (!ativa) {
            throw new PagamentoException("A assinatura já está cancelada.");
        }
        ativa = false;
    }

    public double getTotalPago() {
        return mensalidadesPagas.size() * MENSALIDADE;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public YearMonth getInicio() {
        return inicio;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public double getKgUsadosNoMes() {
        return kgUsadosNoMes;
    }

    public List<YearMonth> getMensalidadesPagas() {
        return Collections.unmodifiableList(mensalidadesPagas);
    }
}
