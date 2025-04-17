/lib                      ← dependências externas (ex: JDBC, PDFBox, etc.)
/scripts                  ← scripts auxiliares e integração Python (futuro)
/data                     ← banco de dados SQLite, PDFs, exports, backups etc.

/src
│
├── app/
│   └── Main.java
│
├── controller/
│   ├── EstoqueController.java          ← gerenciamento da lógica UI do estoque (planejado)
│   └── VendaController.java            ← lógica UI da venda
│
├── dao/
│   ├── CartaDAO.java                   ← CRUD e buscas de cartas
│   ├── ClienteDAO.java                 ← CRUD clientes
│   ├── VendaDAO.java                   ← CRUD vendas
│   ├── VendaItemDAO.java               ← CRUD itens de venda
│   ├── ProdutoDAO.java                 ← CRUD genérico (comidas, bebidas, etc.)
│   ├── BoosterDAO.java                 ← CRUD boosters
│   ├── DeckDAO.java                    ← CRUD decks
│   ├── AcessorioDAO.java               ← CRUD acessórios
│   └── EtbDAO.java                     ← CRUD ETBs
│
├── model/
│   ├── CartaModel.java                 ← entidade cartas
│   ├── ClienteModel.java
│   ├── VendaModel.java
│   ├── VendaItemModel.java
│   ├── ProdutoModel.java               ← genérico (comidas, bebidas)
│   ├── BoosterModel.java               ← boosters individuais
│   ├── DeckModel.java                  ← decks completos
│   ├── AcessorioModel.java             ← acessórios diversos
│   └── EtbModel.java                   ← Elite Trainer Boxes
│
├── service/
│   ├── CartaService.java               ← lógica de cadastro e validação cartas
│   ├── ClienteService.java
│   ├── EstoqueService.java             ← consultas e validações do estoque geral
│   ├── VendaService.java
│   ├── ProdutoService.java             ← lógica genérica produtos
│   ├── BoosterService.java
│   ├── DeckService.java
│   ├── AcessorioService.java
│   └── EtbService.java
│
├── ui/
│   ├── dialog/
│   │   ├── SelecionarCategoriaDialog.java ← seleção de categoria para cadastro
│   │   ├── CartaCadastroDialog.java       ← modal cartas
│   │   ├── ProdutoCadastroDialog.java     ← modal produtos genéricos
│   │   ├── BoosterCadastroDialog.java     ← modal boosters
│   │   ├── DeckCadastroDialog.java        ← modal decks
│   │   ├── AcessorioCadastroDialog.java   ← modal acessórios
│   │   ├── EtbCadastroDialog.java         ← modal ETBs
│   │   ├── VendaNovaDialog.java
│   │   ├── VendaFinalizarDialog.java
│   │   └── VendaDetalhesDialog.java
│   │
│   ├── PainelEstoque.java             ← painel principal estoque
│   ├── DashboardPanel.java            ← dashboard inicial/resumo geral
│   ├── PainelClientes.java
│   ├── PainelVendas.java
│   └── TelaPrincipal.java
│
├── util/
│   ├── DB.java                        ← conexão SQLite
│   ├── DateUtils.java                 ← utilitários de data
│   ├── AlertUtils.java                ← utilitários para alertas visuais
│   ├── LogService.java                ← serviço de logging
│   ├── PDFGenerator.java              ← gera PDFs (vendas, relatórios)
│   └── PythonCaller.java              ← integração futura scrapper
