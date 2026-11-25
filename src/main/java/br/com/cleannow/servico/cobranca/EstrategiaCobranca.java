package br.com.cleannow.servico.cobranca;

import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.Orcamento;
import br.com.cleannow.modelo.servico.Pacote;
import br.com.cleannow.modelo.servico.Servico;
import br.com.cleannow.util.Formatador;

import java.util.List;

public abstract class EstrategiaCobranca {
    public abstract String getNome();

    protected abstract double kgCobertosPeloPlano(Cliente cliente, double pesoKg);

    public final Orcamento calcular(Cliente cliente, List<Servico> servicos, double pesoKg,
                                    int quantidadePecas, Pacote pacote) {
        Orcamento orc = new Orcamento();
        orc.setModalidade(getNome());

        boolean temServicoDaFranquia = servicos.stream().anyMatch(Servico::isCobertoPelaFranquia);
        boolean temServicoDoBonus = servicos.stream().anyMatch(Servico::isElegivelBonusFidelidade);

        double kgFranquia = temServicoDaFranquia ? kgCobertosPeloPlano(cliente, pesoKg) : 0;
        double kgBonus = temServicoDoBonus ? cliente.usarBonus(pesoKg - kgFranquia) : 0;

        for (Servico s : servicos) {
            double qtd = s.quantidadeCobrada(pesoKg, quantidadePecas);
            orc.adicionar(String.format("%s (%s %s x %s)", s.getNome(), formatarQtd(qtd), qtd == 1 ? s.getUnidade() : s.getUnidadePlural(),
                    Formatador.moeda(s.getPrecoUnitario())), s.calcularValor(pesoKg, quantidadePecas));
            if (s.isCobertoPelaFranquia() && kgFranquia > 0) {
                orc.adicionar("  Franquia do plano - " + s.getNome() + " (" + Formatador.kg(kgFranquia) + ")",
                        -s.getPrecoUnitario() * kgFranquia);
            }
            if (s.isElegivelBonusFidelidade() && kgBonus > 0) {
                orc.adicionar("  Bônus fidelidade - " + s.getNome() + " (" + Formatador.kg(kgBonus) + ")",
                        -s.getPrecoUnitario() * kgBonus);
            }
        }

        double subtotal = orc.getTotal();
        if (pacote != null && subtotal > 0) {
            orc.adicionar("Desconto " + pacote.getNome() + " (" + Formatador.percentual(pacote.getDesconto()) + ")",
                    -subtotal * pacote.getDesconto());
        }

        double descontoCliente = cliente.getDescontoServicos();
        subtotal = orc.getTotal();
        if (descontoCliente > 0 && subtotal > 0) {
            orc.adicionar("Desconto cliente " + cliente.getTipo() + " (" + Formatador.percentual(descontoCliente) + ")",
                    -subtotal * descontoCliente);
        }

        orc.setKgFranquiaUsados(kgFranquia);
        orc.setKgBonusUsados(kgBonus);
        return orc;
    }

    private static String formatarQtd(double qtd) {
        return qtd == Math.rint(qtd) ? String.valueOf((long) qtd) : Formatador.decimal(qtd, 1);
    }
}
