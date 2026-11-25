# CleanNow – Sistema de Gestão de Lavanderia

Trabalho da P2 de Programação Orientada a Objetos. Sistema de console em **Java 17**
para a lavanderia CleanNow: clientes comuns e VIP, serviços e pacotes, coleta e
entrega com agenda limitada, rastreio e status do pedido, histórico e preferências,
pagamentos, plano de assinatura e bônus de fidelidade.

**Equipe:** Douglas Miranda Carcará · João Lucas Amadeus de Oliveira Muniz ·
Artur Afonso Bezerra Caetano · Dimitri Kalel Soares Silva · Raimundo Nonato Gomes Ferreira

---

## 1. Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| Java (JDK) | 17 ou mais novo | `java -version` |
| Maven | 3.8 ou mais novo | `mvn -version` |

Na primeira compilação o Maven baixa as dependências (Gson, iText e JUnit), então é
preciso estar conectado à internet.

## 2. Como executar (passo a passo)

```bash
# 1. clonar o repositório
git clone <url-do-repositorio>
cd cleannow

# 2. compilar, rodar os testes e gerar o jar
mvn package

# 3. executar
java -jar target/cleannow.jar
```

Atalhos: `./rodar.sh` (Linux/macOS) ou `CleanNow.bat` (Windows) compilam e executam.
Também é possível rodar direto pelo Maven: `mvn compile exec:java`.

Na primeira execução, escolha a opção **11 – Carregar dados de demonstração** para
ter clientes e pedidos de exemplo (6 clientes, 16 pedidos em várias etapas).

> **Windows:** se os acentos aparecerem errados, rode `chcp 65001` antes ou use o `CleanNow.bat`.

### Testes

```bash
mvn test      # 38 testes JUnit cobrindo preços, plano, bônus, status, agenda, fila e persistência
```

## 3. Onde ficam os dados

| Pasta | Conteúdo |
|---|---|
| `dados/clientes.json`, `pedidos.json`, `servicos.json`, `pacotes.json` | Persistência em JSON, gravada a cada alteração |
| `relatorios/` | PDFs gerados pelo menu de relatórios |

Para usar outra pasta de dados: `java -jar target/cleannow.jar minhaPasta`.
Se um arquivo estiver ausente, o sistema começa vazio; se estiver corrompido, ele é
renomeado para `*.corrompido-DATA`, um aviso é exibido e o sistema continua funcionando.

O projeto não usa credenciais nem chaves de API, por isso não há `.env.example`.

## 4. Funcionalidades

| Requisito | Onde está |
|---|---|
| Cadastro de clientes (comum/VIP), edição, busca, remoção, promoção a VIP | Menu 1 · `ClienteService` |
| Serviços: lavagem R$ 8/kg, secagem R$ 5/kg, passar R$ 3/peça; pacote completo com 10% | Menu 2 · `CatalogoService`, `Servico`, `Pacote` |
| VIP: 15% de desconto, coleta/entrega grátis e prioridade na fila | `ClienteVIP`, `FilaAtendimentoService` |
| Coleta e entrega por período (manhã/tarde), máx. 5 por período, taxa R$ 10 (grátis VIP) | Menus 3, 4 e 10 · `AgendaService` |
| Rastreio pelo código do pedido | Menu 6 |
| Status na ordem coletado → em processo → pronto → entregue, sem pular etapas, com notificação a cada mudança | Menu 5 · `Pedido`, `StatusPedido` |
| Histórico, frequência e preferências do cliente | Menu 8 · `HistoricoService`, `Preferencias` |
| Pagamento por serviço ou plano mensal (R$ 150, franquia 20 kg, excedente cobrado) | Menu 7 · `EstrategiaCobranca`, `Assinatura` |
| Bônus: a cada 10 pedidos pagos, 5 kg de lavagem grátis | `Cliente.registrarPedidoPago()` |
| Relatórios no console e em PDF (pedido, histórico do cliente, gerencial por período) | Menu 9 · `RelatorioService`, `ExportadorPdf` |

**Extras:** fila de serviço com prioridade VIP, cancelamento antes da coleta (devolve
franquia e bônus e libera a vaga), cadastro de novos serviços e pacotes, relatório
gerencial com faturamento e ocupação da agenda, busca de clientes sem acentos, não há
coleta aos domingos nem em período que já terminou, dados de demonstração.

### Regras que o enunciado deixou em aberto (decisões da equipe)

- VIP com pacote completo: os descontos se acumulam (10% do pacote e depois 15% VIP).
- A franquia de 20 kg conta o **peso das roupas**: 5 kg lavados e secos consomem 5 kg.
- O bônus de fidelidade vale para lavagem; é usado automaticamente no próximo pedido.
- A taxa de R$ 10 vale para cada coleta e para cada entrega agendada.
- O pedido só pode ser marcado como entregue depois de pago.
- O limite de 5 por período vale separadamente para coletas e para entregas.

## 5. Estrutura do código

```
src/main/java/br/com/cleannow/
├── Main.java, Sistema.java        ponto de entrada e montagem dos objetos
├── modelo/                        entidades (cliente, servico, pedido, pagamento)
├── servico/                       regras de negócio (+ cobranca/: estratégias de preço)
├── repositorio/                   persistência (interface, memória, JSON com Gson)
├── relatorio/                     montagem de relatórios e exportadores console/PDF
├── ui/                            menus e leitura segura do teclado
├── excecao/                       exceções de negócio
└── util/                          validação, formatação e relógio ajustável
src/test/java/                     testes JUnit 4
CleanNow.bat, rodar.sh             compilar (se preciso) e executar

```

### POO aplicada

- **Abstração:** `Cliente`, `Servico` e `EstrategiaCobranca` são abstratas; `Repositorio` e `ExportadorRelatorio` são interfaces.
- **Herança:** `ClienteComum`/`ClienteVIP`; `Lavagem`/`Secagem` → `ServicoPorKg`, `PassarRoupa` → `ServicoPorPeca`; `RepositorioJson` → `RepositorioMemoria`.
- **Polimorfismo:** desconto, taxa e prioridade vêm do tipo do cliente; cada serviço calcula sua quantidade; cobrança avulsa ou por assinatura (Template Method); relatório em console ou PDF.
- **Encapsulamento:** atributos privados com setters validados; o status do pedido só muda por `avancarStatus()`.

## 6. Bibliotecas

| Biblioteca | Versão | Uso |
|---|---|---|
| Gson | 2.11.0 | Ler e gravar os dados em JSON |
| iText | 5.5.13.4 | Gerar os relatórios em PDF (licença AGPL, uso acadêmico) |
| JUnit | 4.13.2 | Testes automatizados |
