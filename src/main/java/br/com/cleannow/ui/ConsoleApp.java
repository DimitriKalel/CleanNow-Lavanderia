package br.com.cleannow.ui;

import br.com.cleannow.Sistema;
import br.com.cleannow.excecao.CleanNowException;
import br.com.cleannow.excecao.EntidadeNaoEncontradaException;
import br.com.cleannow.modelo.Periodo;
import br.com.cleannow.modelo.cliente.Cliente;
import br.com.cleannow.modelo.pagamento.Assinatura;
import br.com.cleannow.modelo.pagamento.MeioPagamento;
import br.com.cleannow.modelo.pedido.Notificacao;
import br.com.cleannow.modelo.pedido.Pedido;
import br.com.cleannow.modelo.pedido.StatusPedido;
import br.com.cleannow.modelo.pedido.TipoAgendamento;
import br.com.cleannow.modelo.servico.Pacote;
import br.com.cleannow.modelo.servico.Servico;
import br.com.cleannow.relatorio.Documento;
import br.com.cleannow.relatorio.ExportadorRelatorio;
import br.com.cleannow.servico.AgendaService;
import br.com.cleannow.servico.PagamentoService;
import br.com.cleannow.servico.PedidoService.Horario;
import br.com.cleannow.util.Formatador;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConsoleApp {
    @FunctionalInterface
    private interface Acao {
        void rodar();
    }

    private final Sistema sistema;
    private final Entrada in;
    private final PrintStream out;

    public ConsoleApp(Sistema sistema, Entrada entrada, PrintStream out) {
        this.sistema = sistema;
        this.in = entrada;
        this.out = out;
    }

    public void iniciar() {
        cabecalho();
        sistema.avisos().forEach(a -> out.println("AVISO: " + a));
        try {
            boolean continuar = true;
            while (continuar) {
                continuar = menuPrincipal();
            }
        } catch (Entrada.EntradaEncerradaException e) {
            out.println();
        }
        out.println("Dados salvos. Até logo!");
    }

    private void cabecalho() {
        out.println("==============================================");
        out.println("   CleanNow Lavanderia - Sistema de Gestão");
        out.println("   Lavagem | Secagem | Passadoria | Entrega");
        out.println("==============================================");
    }

    private boolean menuPrincipal() {
        out.println("\n----------- MENU PRINCIPAL -----------");
        out.println(" 1) Clientes");
        out.println(" 2) Serviços e pacotes");
        out.println(" 3) Novo pedido");
        out.println(" 4) Pedidos (consultar, agendar entrega, cancelar)");
        out.println(" 5) Status e fila de serviço");
        out.println(" 6) Rastrear pedido pelo código");
        out.println(" 7) Pagamentos e plano de assinatura");
        out.println(" 8) Histórico e preferências do cliente");
        out.println(" 9) Relatórios (console / PDF)");
        out.println("10) Agenda de coletas e entregas");
        out.println(" 0) Sair");
        int op = in.inteiro("Escolha: ", 0, 10);
        switch (op) {
            case 1 -> menuClientes();
            case 2 -> menuCatalogo();
            case 3 -> executar(this::novoPedido);
            case 4 -> menuPedidos();
            case 5 -> menuStatus();
            case 6 -> executar(this::rastrear);
            case 7 -> menuPagamentos();
            case 8 -> menuHistorico();
            case 9 -> menuRelatorios();
            case 10 -> executar(this::verAgenda);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void executar(Acao acao) {
        try {
            acao.rodar();
        } catch (Entrada.EntradaEncerradaException e) {
            throw e;
        } catch (CleanNowException e) {
            out.println("\n[!] " + e.getMessage());
        } catch (RuntimeException e) {
            out.println("\n[!] Erro inesperado: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            out.println("    A operação foi cancelada; os dados anteriores foram preservados.");
        }
    }

    private void menuClientes() {
        out.println("\n--- CLIENTES ---");
        out.println("1) Cadastrar  2) Listar  3) Buscar por nome  4) Editar  5) Promover/rebaixar VIP  6) Remover  0) Voltar");
        switch (in.inteiro("Escolha: ", 0, 6)) {
            case 1 -> executar(this::cadastrarCliente);
            case 2 -> executar(this::listarClientes);
            case 3 -> executar(() -> {
                List<Cliente> achados = sistema.clientes().buscarPorNome(in.texto("Nome ou parte do nome: "));
                if (achados.isEmpty()) out.println("Nenhum cliente encontrado.");
                achados.forEach(this::imprimirClienteResumo);
            });
            case 4 -> executar(this::editarCliente);
            case 5 -> executar(() -> {
                Cliente c = selecionarCliente();
                boolean paraVip = c.getPrioridade() != 1;
                if (in.simNao((paraVip ? "Promover " : "Rebaixar ") + c.getNome() + (paraVip ? " a VIP?" : " a cliente comum?"))) {
                    Cliente novo = sistema.clientes().alterarTipo(c.getId(), paraVip);
                    out.println("Pronto: " + novo);
                }
            });
            case 6 -> executar(() -> {
                Cliente c = selecionarCliente();
                if (in.simNao("Remover " + c.getNome() + " definitivamente?")) {
                    sistema.clientes().remover(c.getId());
                    out.println("Cliente removido.");
                }
            });
            default -> { }
        }
    }

    private void cadastrarCliente() {
        out.println("\nNovo cliente");
        String nome = in.texto("Nome: ");
        String tel = in.texto("Telefone com DDD: ");
        String email = in.texto("E-mail: ");
        String end = in.texto("Endereço (para coleta/entrega): ");
        boolean vip = in.simNao("Cliente VIP?");
        Cliente c = sistema.clientes().cadastrar(nome, tel, email, end, vip);
        out.println("Cliente cadastrado: " + c);
        if (in.simNao("Deseja registrar as preferências agora?")) {
            editarPreferencias(c);
        }
    }

    private void listarClientes() {
        List<Cliente> lista = sistema.clientes().listar();
        if (lista.isEmpty()) {
            out.println("Nenhum cliente cadastrado.");
            return;
        }
        out.printf("%n%-5s %-26s %-6s %-16s %-28s %s%n", "ID", "Nome", "Tipo", "Telefone", "E-mail", "Plano");
        for (Cliente c : lista) {
            out.printf("%-5s %-26s %-6s %-16s %-28s %s%n", c.getId(), corta(c.getNome(), 26), c.getTipo(),
                    c.getTelefoneFormatado(), corta(c.getEmail(), 28), c.temAssinaturaAtiva() ? "assinante" : "-");
        }
    }

    private void imprimirClienteResumo(Cliente c) {
        out.printf("%s | %s | %s | %s%n", c, c.getTelefoneFormatado(), c.getEmail(), c.getEndereco());
    }

    private void editarCliente() {
        Cliente c = selecionarCliente();
        out.println("O que deseja alterar?  1) Nome  2) Telefone  3) E-mail  4) Endereço");
        int campo = in.inteiro("Campo: ", 1, 4);
        switch (campo) {
            case 1 -> sistema.clientes().editar(c.getId(), x -> x.setNome(in.textoOuManter("Nome", x.getNome())));
            case 2 -> sistema.clientes().editar(c.getId(), x -> x.setTelefone(in.textoOuManter("Telefone", x.getTelefone())));
            case 3 -> sistema.clientes().editar(c.getId(), x -> x.setEmail(in.textoOuManter("E-mail", x.getEmail())));
            default -> sistema.clientes().editar(c.getId(), x -> x.setEndereco(in.textoOuManter("Endereço", x.getEndereco())));
        }
        out.println("Cliente atualizado: " + sistema.clientes().buscar(c.getId()));
    }

    private Cliente selecionarCliente() {
        if (sistema.clientes().listar().isEmpty()) {
            throw new EntidadeNaoEncontradaException("Nenhum cliente cadastrado ainda.");
        }
        String busca = in.linha("ID do cliente ou parte do nome (Enter lista todos): ");
        if (!busca.isEmpty()) {
            try {
                return sistema.clientes().buscar(busca);
            } catch (EntidadeNaoEncontradaException naoEhId) {
                List<Cliente> achados = sistema.clientes().buscarPorNome(busca);
                if (achados.isEmpty()) throw new EntidadeNaoEncontradaException("Nenhum cliente encontrado para: " + busca);
                if (achados.size() == 1) return achados.get(0);
                return in.escolher("Vários clientes encontrados:", achados, Cliente::toString);
            }
        }
        return in.escolher("Clientes:", sistema.clientes().listar(), Cliente::toString);
    }

    private void menuCatalogo() {
        out.println("\n--- SERVIÇOS E PACOTES ---");
        out.println("1) Listar  2) Alterar preço  3) Cadastrar serviço  4) Remover serviço  5) Cadastrar pacote  6) Remover pacote  0) Voltar");
        switch (in.inteiro("Escolha: ", 0, 6)) {
            case 1 -> executar(this::listarCatalogo);
            case 2 -> executar(() -> {
                Servico s = in.escolher("Serviço:", sistema.catalogo().listarServicos(), Servico::toString);
                double preco = in.decimal("Novo preço (R$ por " + s.getUnidade() + "): ", 0.01, 10000);
                sistema.catalogo().alterarPreco(s.getCodigo(), preco);
                out.println("Preço atualizado: " + sistema.catalogo().buscarServico(s.getCodigo()));
            });
            case 3 -> executar(() -> {
                String cod = in.texto("Código (ex.: EDR): ");
                String nome = in.texto("Nome (ex.: Lavagem de edredom): ");
                boolean porKg = in.simNao("Cobrado por kg? (n = por peça)");
                double preco = in.decimal("Preço: R$ ", 0.01, 10000);
                out.println("Cadastrado: " + sistema.catalogo().cadastrarServico(cod, nome, preco, porKg));
            });
            case 4 -> executar(() -> {
                Servico s = in.escolher("Serviço:", sistema.catalogo().listarServicos(), Servico::toString);
                sistema.catalogo().removerServico(s.getCodigo());
                out.println("Serviço removido.");
            });
            case 5 -> executar(() -> {
                String cod = in.texto("Código do pacote: ");
                String nome = in.texto("Nome do pacote: ");
                List<Servico> servs = in.escolherVarios("Serviços do pacote:", sistema.catalogo().listarServicos(), Servico::toString);
                double desc = in.decimal("Desconto em % (ex.: 10): ", 1, 99) / 100.0;
                Pacote p = sistema.catalogo().cadastrarPacote(cod, nome,
                        servs.stream().map(Servico::getCodigo).collect(Collectors.toList()), desc);
                out.println("Pacote cadastrado: " + p);
            });
            case 6 -> executar(() -> {
                Pacote p = in.escolher("Pacote:", sistema.catalogo().listarPacotes(), Pacote::toString);
                sistema.catalogo().removerPacote(p.getId());
                out.println("Pacote removido.");
            });
            default -> { }
        }
    }

    private void listarCatalogo() {
        out.println("\nServiços:");
        for (Servico s : sistema.catalogo().listarServicos()) {
            out.printf("  %-4s %-26s %10s/%-4s %s%n", s.getCodigo(), s.getNome(), Formatador.moeda(s.getPrecoUnitario()),
                    s.getUnidade(), s.isCobertoPelaFranquia() ? "(entra na franquia do plano)" : "");
        }
        out.println("Pacotes:");
        sistema.catalogo().listarPacotes().forEach(p -> out.println("  " + p));
        out.printf("Regras: VIP %s de desconto e coleta/entrega grátis | taxa comum %s por coleta e por entrega%n",
                Formatador.percentual(0.15), Formatador.moeda(10));
        out.printf("Plano mensal %s com franquia de %s (lavagem e secagem) | bônus: %s grátis a cada %d pedidos pagos%n",
                Formatador.moeda(Assinatura.MENSALIDADE), Formatador.kg(Assinatura.FRANQUIA_KG),
                Formatador.kg(Cliente.KG_BONUS), Cliente.PEDIDOS_PARA_BONUS);
    }

    private void novoPedido() {
        out.println("\n--- NOVO PEDIDO ---");
        Cliente c = selecionarCliente();
        out.println("Cliente: " + c + (c.temAssinaturaAtiva()
                ? " | assinante, franquia restante " + Formatador.kg(c.getAssinatura().getFranquiaRestante()) : "")
                + (c.getKgBonusDisponivel() > 0 ? " | bônus " + Formatador.kg(c.getKgBonusDisponivel()) : ""));
        out.println("Preferências: " + c.getPreferencias().resumo());

        List<Servico> servicos = in.escolherVarios("Serviços desejados:", sistema.catalogo().listarServicos(), Servico::toString);
        boolean temKg = servicos.stream().anyMatch(s -> s.quantidadeCobrada(1, 0) > 0);
        boolean temPeca = servicos.stream().anyMatch(s -> s.quantidadeCobrada(0, 1) > 0);
        double peso = temKg ? in.decimal("Peso das roupas (kg): ", 0.1, 50) : 0;
        int pecas = temPeca ? in.inteiro("Quantidade de peças: ", 1, 200) : 0;

        Horario coleta = null;
        if (in.simNao("Agendar coleta? (n = cliente leva à loja)")) {
            coleta = lerHorario(TipoAgendamento.COLETA, c);
        }
        Horario entrega = null;
        if (in.simNao("Agendar entrega? (n = cliente busca na loja)")) {
            entrega = lerHorario(TipoAgendamento.ENTREGA, c);
        }
        Pedido p = sistema.pedidos().criar(c.getId(), servicos.stream().map(Servico::getCodigo).collect(Collectors.toList()),
                peso, pecas, coleta, entrega);
        out.println("\nPedido criado com sucesso! Código de rastreio: " + p.getCodigo());
        imprimir(sistema.relatorios().comprovantePedido(p.getCodigo()));
    }

    private Horario lerHorario(TipoAgendamento tipo, Cliente c) {
        LocalDate data;
        while (true) {
            data = in.data("Data da " + tipo.getDescricao().toLowerCase());
            try {
                sistema.agenda().validarData(data, null);
                break;
            } catch (CleanNowException e) {
                out.println("  " + e.getMessage());
            }
        }
        Periodo pref = c.getPreferencias().getPeriodoPreferido();
        out.println("Vagas nesse dia: manhã " + sistema.agenda().vagas(tipo, data, Periodo.MANHA)
                + " | tarde " + sistema.agenda().vagas(tipo, data, Periodo.TARDE)
                + (pref != null ? " | preferência do cliente: " + pref.getDescricao() : ""));
        while (true) {
            Periodo periodo = in.escolher("Período:", Arrays.asList(Periodo.values()), Periodo::getDescricao);
            try {
                sistema.agenda().validarPeriodo(data, periodo);
                if (sistema.agenda().vagas(tipo, data, periodo) <= 0) {
                    out.println("  Esse período está lotado. Escolha outro.");
                    continue;
                }
                return new Horario(data, periodo);
            } catch (CleanNowException e) {
                out.println("  " + e.getMessage());
                if (!in.simNao("Escolher outro período nesse dia?")) {
                    throw e;
                }
            }
        }
    }

    private void menuPedidos() {
        out.println("\n--- PEDIDOS ---");
        out.println("1) Listar todos  2) Listar por status  3) Ver detalhes/comprovante  4) Agendar entrega  5) Cancelar  0) Voltar");
        switch (in.inteiro("Escolha: ", 0, 5)) {
            case 1 -> executar(() -> listarPedidos(sistema.pedidos().listarTodos()));
            case 2 -> executar(() -> {
                StatusPedido st = in.escolher("Status:", Arrays.asList(StatusPedido.values()), StatusPedido::getDescricao);
                listarPedidos(sistema.pedidos().listarPorStatus(st));
            });
            case 3 -> executar(() -> imprimir(sistema.relatorios().comprovantePedido(selecionarPedido().getCodigo())));
            case 4 -> executar(() -> {
                Pedido p = selecionarPedido();
                Horario h = lerHorario(TipoAgendamento.ENTREGA, sistema.clientes().buscar(p.getClienteId()));
                Pedido atualizado = sistema.pedidos().agendarEntrega(p.getCodigo(), h.data(), h.periodo());
                out.println("Entrega agendada: " + atualizado.getEntrega() + " | novo total: "
                        + Formatador.moeda(atualizado.getTotal()));
            });
            case 5 -> executar(() -> {
                Pedido p = selecionarPedido();
                String motivo = in.linha("Motivo (opcional): ");
                if (in.simNao("Confirmar cancelamento de " + p.getCodigo() + "?")) {
                    sistema.pedidos().cancelar(p.getCodigo(), motivo);
                    out.println("Pedido cancelado. Franquia/bônus usados foram devolvidos e a vaga na agenda foi liberada.");
                }
            });
            default -> { }
        }
    }

    private void listarPedidos(List<Pedido> lista) {
        if (lista.isEmpty()) {
            out.println("Nenhum pedido.");
            return;
        }
        out.printf("%n%-12s %-17s %-22s %-12s %-18s %12s %s%n", "Código", "Criado em", "Cliente", "Serviços",
                "Status", "Total", "Pago");
        for (Pedido p : lista) {
            String nome = nomeCliente(p.getClienteId());
            out.printf("%-12s %-17s %-22s %-12s %-18s %12s %s%n", p.getCodigo(), Formatador.dataHora(p.getCriadoEm()),
                    corta(nome, 22), String.join("+", p.getCodigosServicos()), p.getStatus().getDescricao(),
                    Formatador.moeda(p.getTotal()), p.isPago() ? "sim" : "não");
        }
    }

    private Pedido selecionarPedido() {
        String cod = in.texto("Código do pedido (ex.: CN2026-0001 ou só 1): ");
        if (cod.matches("\\d+")) {
            String sufixo = String.format("-%04d", Integer.parseInt(cod));
            List<Pedido> achados = sistema.pedidos().listarTodos().stream()
                    .filter(p -> p.getCodigo().endsWith(sufixo)).collect(Collectors.toList());
            if (achados.size() == 1) return achados.get(0);
            if (achados.size() > 1) return in.escolher("Qual deles?", achados, Pedido::toString);
        }
        return sistema.pedidos().buscar(cod);
    }

    private void menuStatus() {
        out.println("\n--- STATUS E FILA DE SERVIÇO ---");
        out.println("1) Avançar status de um pedido  2) Ver fila de serviço (VIP primeiro)  3) Iniciar próximo da fila  0) Voltar");
        switch (in.inteiro("Escolha: ", 0, 3)) {
            case 1 -> executar(() -> {
                Pedido p = selecionarPedido();
                out.println("Status atual: " + p.getStatus().getDescricao());
                StatusPedido proximo = p.getStatus().proximo();
                if (in.simNao("Avançar para " + proximo.getDescricao().toUpperCase() + "?")) {
                    StatusPedido novo = sistema.pedidos().avancarStatus(p.getCodigo());
                    out.println("Status atualizado para " + novo.getDescricao() + ". Notificação registrada no histórico.");
                }
            });
            case 2 -> executar(this::verFila);
            case 3 -> executar(() -> {
                Pedido p = sistema.fila().iniciarProximo();
                out.println("Iniciado: " + p.getCodigo() + " de " + nomeCliente(p.getClienteId()) + " -> EM PROCESSO");
            });
            default -> { }
        }
    }

    private void verFila() {
        List<Pedido> fila = sistema.fila().filaDeEspera();
        out.println("\nAguardando serviço (ordem de atendimento):");
        if (fila.isEmpty()) out.println("  (vazia)");
        int i = 1;
        for (Pedido p : fila) {
            Cliente c = sistema.clientes().buscar(p.getClienteId());
            out.printf("  %d. %s  %-22s %-5s chegou %s%n", i++, p.getCodigo(), corta(c.getNome(), 22), c.getTipo(),
                    Formatador.dataHora(p.getCriadoEm()));
        }
        out.println("Em processo:");
        List<Pedido> emProc = sistema.fila().emProcesso();
        if (emProc.isEmpty()) out.println("  (nenhum)");
        emProc.forEach(p -> out.println("  " + p.getCodigo() + "  " + nomeCliente(p.getClienteId())));
    }

    private void rastrear() {
        Pedido p = selecionarPedido();
        out.println("\nPedido " + p.getCodigo() + " - " + nomeCliente(p.getClienteId()));
        StringBuilder linha = new StringBuilder("  ");
        for (StatusPedido s : List.of(StatusPedido.AGUARDANDO_COLETA, StatusPedido.COLETADO, StatusPedido.EM_PROCESSO,
                StatusPedido.PRONTO, StatusPedido.ENTREGUE)) {
            boolean atingido = p.getStatus() != StatusPedido.CANCELADO && s.ordinal() <= p.getStatus().ordinal();
            linha.append(atingido ? "[x] " : "[ ] ").append(s.getDescricao()).append("   ");
        }
        out.println(p.getStatus() == StatusPedido.CANCELADO ? "  *** PEDIDO CANCELADO ***" : linha.toString());
        if (p.getEntrega() != null) out.println("  " + p.getEntrega());
        out.println("  Total: " + Formatador.moeda(p.getTotal()) + (p.isPago() ? " (pago)" : " (pendente)"));
        out.println("Histórico de notificações:");
        for (Notificacao n : p.getHistorico()) out.println("  " + n);
    }

    private void menuPagamentos() {
        out.println("\n--- PAGAMENTOS E ASSINATURA ---");
        out.println("1) Pagar pedido  2) Pedidos pendentes de pagamento  3) Assinar plano mensal  4) Cancelar plano  5) Ver plano do cliente  0) Voltar");
        switch (in.inteiro("Escolha: ", 0, 5)) {
            case 1 -> executar(this::pagarPedido);
            case 2 -> executar(() -> listarPedidos(sistema.pedidos().listarTodos().stream()
                    .filter(p -> !p.isPago() && p.getStatus() != StatusPedido.CANCELADO).collect(Collectors.toList())));
            case 3 -> executar(() -> {
                Cliente c = selecionarCliente();
                out.printf("Plano mensal: %s com franquia de %s de lavagem e secagem; excedente cobrado à parte.%n",
                        Formatador.moeda(Assinatura.MENSALIDADE), Formatador.kg(Assinatura.FRANQUIA_KG));
                if (in.simNao("Confirmar assinatura para " + c.getNome() + "?")) {
                    double v = sistema.pagamentos().assinarPlano(c.getId());
                    out.println("Assinatura ativada. Primeira mensalidade registrada: " + Formatador.moeda(v));
                }
            });
            case 4 -> executar(() -> {
                Cliente c = selecionarCliente();
                if (in.simNao("Cancelar o plano de " + c.getNome() + "?")) {
                    sistema.pagamentos().cancelarPlano(c.getId());
                    out.println("Plano cancelado. Os próximos pedidos serão cobrados por serviço.");
                }
            });
            case 5 -> executar(() -> {
                Cliente c = selecionarCliente();
                Assinatura a = c.getAssinatura();
                if (a == null) {
                    out.println(c.getNome() + " não é assinante.");
                } else {
                    out.printf("Situação: %s | início %s | mês %s | usados %s | restantes %s | mensalidades pagas: %d (%s)%n",
                            a.isAtiva() ? "ativa" : "cancelada", Formatador.mes(a.getInicio()), Formatador.mes(a.getMesReferencia()),
                            Formatador.kg(a.getKgUsadosNoMes()), Formatador.kg(a.getFranquiaRestante()),
                            a.getMensalidadesPagas().size(), Formatador.moeda(a.getTotalPago()));
                }
            });
            default -> { }
        }
    }

    private void pagarPedido() {
        Pedido p = selecionarPedido();
        out.println("Total a pagar: " + Formatador.moeda(p.getTotal()));
        MeioPagamento meio = in.escolher("Forma de pagamento:", Arrays.asList(MeioPagamento.values()), MeioPagamento::getDescricao);
        PagamentoService.Recibo r = sistema.pagamentos().pagarPedido(p.getCodigo(), meio);
        out.println("Pagamento de " + Formatador.moeda(r.valorPago()) + " confirmado.");
        if (r.kgBonusGanhos() > 0) {
            out.println("*** O cliente completou mais 10 pedidos pagos e ganhou " + Formatador.kg(r.kgBonusGanhos())
                    + " de lavagem grátis! ***");
        } else {
            out.println("Faltam " + r.pedidosAteProximoBonus() + " pedido(s) pago(s) para o próximo bônus de fidelidade.");
        }
    }

    private void menuHistorico() {
        out.println("\n--- HISTÓRICO E PREFERÊNCIAS ---");
        out.println("1) Ver histórico completo do cliente  2) Editar preferências  0) Voltar");
        switch (in.inteiro("Escolha: ", 0, 2)) {
            case 1 -> executar(() -> imprimir(sistema.relatorios().historicoCliente(selecionarCliente().getId())));
            case 2 -> executar(() -> editarPreferencias(selecionarCliente()));
            default -> { }
        }
    }

    private void editarPreferencias(Cliente c) {
        out.println("Preferências atuais: " + c.getPreferencias().resumo());
        boolean amaciante = in.simNao("Amaciante?");
        boolean dobrar = in.simNao("Entregar roupas dobradas? (n = em cabide)");
        boolean hipo = in.simNao("Usar sabão hipoalergênico?");
        List<String> opcoes = new ArrayList<>(List.of("Sem preferência"));
        for (Periodo p : Periodo.values()) opcoes.add(p.getDescricao());
        String per = in.escolher("Período preferido para coleta/entrega:", opcoes, s -> s);
        String obs = in.linha("Observações (opcional): ");
        sistema.clientes().atualizarPreferencias(c.getId(), p -> {
            p.setAmaciante(amaciante);
            p.setRoupasDobradas(dobrar);
            p.setSabaoHipoalergenico(hipo);
            p.setPeriodoPreferido(Arrays.stream(Periodo.values()).filter(x -> x.getDescricao().equals(per))
                    .findFirst().orElse(null));
            p.setObservacoes(obs);
        });
        out.println("Preferências salvas: " + sistema.clientes().buscar(c.getId()).getPreferencias().resumo());
    }

    private void menuRelatorios() {
        out.println("\n--- RELATÓRIOS ---");
        out.println("1) Comprovante de pedido  2) Histórico do cliente  3) Relatório gerencial por período  0) Voltar");
        int op = in.inteiro("Escolha: ", 0, 3);
        if (op == 0) return;
        executar(() -> {
            Documento doc;
            String nome;
            if (op == 1) {
                Pedido p = selecionarPedido();
                doc = sistema.relatorios().comprovantePedido(p.getCodigo());
                nome = "pedido_" + p.getCodigo();
            } else if (op == 2) {
                Cliente c = selecionarCliente();
                doc = sistema.relatorios().historicoCliente(c.getId());
                nome = "historico_" + c.getId();
            } else {
                LocalDate ini = in.data("Data inicial");
                LocalDate fim = in.data("Data final");
                doc = sistema.relatorios().gerencial(ini, fim);
                nome = "gerencial";
            }
            List<ExportadorRelatorio> formatos = List.of(sistema.exportadorConsole(), sistema.exportadorPdf());
            ExportadorRelatorio exp = in.escolher("Formato:", formatos, ExportadorRelatorio::getFormato);
            out.println(exp.exportar(doc, nome));
        });
    }

    private void imprimir(Documento doc) {
        out.println(sistema.exportadorConsole().exportar(doc, null));
    }

    private void verAgenda() {
        LocalDate data = in.data("Dia da agenda");
        out.println("\nAgenda de " + Formatador.data(data) + " (limite " + AgendaService.LIMITE_POR_PERIODO + " por período)");
        for (TipoAgendamento tipo : TipoAgendamento.values()) {
            for (Periodo per : Periodo.values()) {
                List<Pedido> lista = sistema.agenda().pedidosDoDia(tipo, data, per);
                out.printf("%s - %s: %d/%d%n", tipo.getDescricao(), per.getDescricao(), lista.size(),
                        AgendaService.LIMITE_POR_PERIODO);
                for (Pedido p : lista) {
                    Cliente c = sistema.clientes().buscar(p.getClienteId());
                    out.printf("   %s  %-22s %s%n", p.getCodigo(), corta(c.getNome(), 22), c.getEndereco());
                }
            }
        }
    }

    private String nomeCliente(String id) {
        try {
            return sistema.clientes().buscar(id).getNome();
        } catch (EntidadeNaoEncontradaException e) {
            return id + " (removido)";
        }
    }

    private static String corta(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
