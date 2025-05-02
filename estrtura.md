# 📁 Estrutura do Projeto

```
HOSTORE/
├── .vscode/
│   └── settings.json
│
├── bin/
│   ├── app/
│   │   └── Main.class
│   └── ui/
│       └── TelaPrincipal.class
│
├── data/
│   ├── export/
│   │   └── comprovante_2.pdf
│   ├── fonts/
│   │   ├── Roboto-Bold.ttf
│   │   └── Roboto-Regular.ttf
│   └── hostore.db
│
├── Estruturas/
│   ├── Estoque.md
│   └── vendas.md
│
├── lib/
│   ├── commons-lang3-3.17.0.jar
│   ├── gson-2.13.0.jar
│   ├── jcalendar-1.4.jar
│   ├── opencsv-5.10.jar
│   ├── pdfbox-app-3.0.4.jar
│   └── sqlite-jdbc-3.49.1.0.jar
│
├── scripts/
│
└── src/
    ├── api/
    │   └── PokeTcgApi.java
    │
    ├── app/
    │   └── Main.java
    │
    ├── controller/
    │   ├── EstoqueController.java
    │   ├── ProdutoEstoqueController.java
    │   └── VendaController.java
    │
    ├── dao/
    │   ├── AcessorioDAO.java
    │   ├── BoosterDAO.java
    │   ├── CadastroGenericoDAO.java
    │   ├── CartaDAO.java
    │   ├── ClienteDAO.java
    │   ├── ColecaoDAO.java
    │   ├── DeckDAO.java
    │   ├── EstoqueDAO.java
    │   ├── EtbDAO.java
    │   ├── FornecedorDAO.java
    │   ├── ProdutoAlimenticioDAO.java
    │   ├── ProdutoDAO.java
    │   ├── PromocaoProdutoDAO.java
    │   ├── SetDAO.java
    │   ├── TipoPromocaoDAO.java
    │   ├── UsuarioDAO.java
    │   ├── VendaDAO.java
    │   └── VendaItemDAO.java
    │
    ├── factory/
    │   └── VendaFactory.java
    │
    ├── model/
    │   ├── AcessorioModel.java
    │   ├── BoosterModel.java
    │   ├── Carta.java
    │   ├── ClienteModel.java
    │   ├── ColecaoModel.java
    │   ├── DeckModel.java
    │   ├── EtbModel.java
    │   ├── FornecedorModel.java
    │   ├── MovimentacaoEstoque.java
    │   ├── ProdutoAlimenticioModel.java
    │   ├── ProdutoModel.java
    │   ├── PromocaoProdutoModel.java
    │   ├── SetModel.java
    │   ├── TipoPromocaoModel.java
    │   ├── UsuarioModel.java
    │   ├── VendaItemModel.java
    │   └── VendaModel.java
    │
    ├── service/
    │   ├── ClienteService.java
    │   ├── ColecaoService.java
    │   ├── EstoqueService.java
    │   ├── ProdutoEstoqueService.java
    │   ├── SessaoService.java
    │   ├── SetService.java
    │   └── VendaService.java
    │
    ├── ui/ 
    │   └── financeiro/
    │   │ ├── painel/
    │   │ │   ├── PainelFinanceiro.java
    │   │ │   ├── PainelContasPagar.java
    │   │ │   ├── PainelContasReceber.java
    │   │ │   └── PainelResumoFinanceiro.java
    │   │ └── dialog/
    │   │     ├── ContaPagarDialog.java
    │   │     └── ContaReceberDialog.java
    │   │
    │   │─── ajustes/
    │   │   ├── dialog/
    │   │   │   ├── AbstractCrudPainel.java
    │   │   │   ├── CategoriaProdutoDialog.java
    │   │   │   ├── ClienteVipDialog.java
    │   │   │   ├── CondicaoDialog.java
    │   │   │   ├── ConfigFinanceiroDialog.java
    │   │   │   ├── ConfigImpressoaDialog.java
    │   │   │   ├── ConfigLojaDialog.java
    │   │   │   ├── ConfigSistemaDialog.java
    │   │   │   ├── FornecedorDialog.java
    │   │   │   ├── IdiomaDialog.java
    │   │   │   ├── LoginDialog.java
    │   │   │   ├── PromocaoDialog.java
    │   │   │   ├── TipoCartaDialog.java
    │   │   │   ├── TipoPromocaoDialog.java
    │   │   │   ├── UsuarioDialog.java
    │   │   │   ├── VerProdutosVinculadosDialog.java
    │   │   │   └── VincularProdutosDialog.java
    │   │   │
    │   │   └── painel/
    │   │       ├── AbstractCrudPainel.java
    │   │       ├── CategoriaProdutoPainel.java
    │   │       ├── ClienteVipPainel.java
    │   │       ├── CondicaoPainel.java
    │   │       ├── FornecedorPainel.java
    │   │       ├── IdiomaPainel.java
    │   │       ├── PromocaoPainel.java
    │   │       ├── TipoCartaPainel.java
    │   │       ├── TipoPromocaoPainel.java
    │   │       ├── UsuarioPainel.java
    │   │       └── AjustesPanel.java
    │   │
    │   ├── clientes/
    │   │   ├── dialog/
    │   │   │   └── ClienteCadastroDialog.java
    │   │   └── painel/
    │   │       └── PainelClientes.java
    │   │
    │   ├── dash/
    │   │   ├── dialog/
    │   │   └── painel/
    │   │       └── DashboardPanel.java
    │   │
    │   ├── estoque/
    │   │   ├── dialog/
    │   │   │   ├── CadastroAcessorioDialog.java
    │   │   │   ├── CadastroBoosterDialog.java
    │   │   │   ├── CadastroDeckDialog.java
    │   │   │   ├── CadastroEtbDialog.java
    │   │   │   ├── CadastroProdutoAlimenticioDialog.java
    │   │   │   ├── CartaCadastroDialog.java
    │   │   │   ├── MovimentacaoDialog.java
    │   │   │   └── ProdutoCadastroDialog.java
    │   │   └── painel/
    │   │       └── PainelEstoque.java
    │   │
    │   ├── venda/
    │   │   ├── dialog/
    │   │   │   ├── VendaDetalhesDialog.java
    │   │   │   ├── VendaFinalizarDialog.java
    │   │   │   └── VendaNovaDialog.java
    │   │   └── painel/
    │   │       └── PainelVendas.java
    │   │
    │   └── TelaPrincipal.java
    │
    └── util/
        ├── AlertUtils.java
        ├── DateUtils.java
        ├── DB.java
        ├── FormatterFactory.java
        ├── LogService.java
        ├── MaskUtils.java
        ├── PDFGenerator.java
        └── PythonCaller.java

