Listagem de caminhos de pasta para o volume OS
O número de série do volume é 1631-E65F
C:.
¦   .gitattributes
¦   .gitignore
¦   estrtura.md
¦   estrutura_projeto.md
¦   estrutura_projeto.txt
¦   hostore.db
¦   LICENSE
¦   README.md
¦   src.zip
¦   
+---.vscode
¦       settings.json
¦       
+---bin
¦   +---app
¦   ¦       Main.class
¦   ¦       
¦   +---ui
¦           TelaPrincipal.class
¦           
+---data
¦   ¦   hostore.db
¦   ¦   hos_logs.txt
¦   ¦   
¦   +---export
¦   ¦       comprovante_1.pdf
¦   ¦       comprovante_2.pdf
¦   ¦       
¦   +---fonts
¦           Roboto-Bold.ttf
¦           Roboto-Regular.ttf
¦           
+---Estrturas
¦       Estoque.md
¦       vendas.md
¦       
+---lib
¦       commons-lang3-3.17.0.jar
¦       gson-2.13.0.jar
¦       jcalendar-1.4.jar
¦       opencsv-5.10.jar
¦       pdfbox-app-3.0.4.jar
¦       postgresql-42.7.5.jar
¦       sqlite-jdbc-3.49.1.0.jar
¦       
+---scripts
+---src
    ¦   ConsultaSQLiteSimples.java
    ¦   TesteConexao.java
    ¦   
    +---api
    ¦       PokeTcgApi.java
    ¦       
    +---app
    ¦       Main.java
    ¦       
    +---controller
    ¦       EstoqueController.java
    ¦       ProdutoEstoqueController.java
    ¦       VendaController.java
    ¦       
    +---dao
    ¦       AcessorioDAO.java
    ¦       AlimentoDAO.java
    ¦       BancoDAO.java
    ¦       BoosterDAO.java
    ¦       CadastroGenericoDAO.java
    ¦       CartaDAO.java
    ¦       ClienteDAO.java
    ¦       ColecaoDAO.java
    ¦       ContaPagarPedidoDAO.java
    ¦       DeckDAO.java
    ¦       EstoqueDAO.java
    ¦       EtbDAO.java
    ¦       FornecedorDAO.java
    ¦       MovimentacaoEstoqueDAO.java
    ¦       PagamentoContaPagarDAO.java
    ¦       ParcelaContaPagarDAO.java
    ¦       PedidoCompraDAO.java
    ¦       PedidoEstoqueProdutoDAO.java
    ¦       PlanoContaDAO.java
    ¦       ProdutoDAO.java
    ¦       PromocaoProdutoDAO.java
    ¦       SetDAO.java
    ¦       TipoPromocaoDAO.java
    ¦       TituloContaPagarDAO.java
    ¦       UsuarioDAO.java
    ¦       VendaDAO.java
    ¦       VendaItemDAO.java
    ¦       
    +---factory
    ¦       VendaFactory.java
    ¦       
    +---model
    ¦       AcessorioModel.java
    ¦       AlimentoModel.java
    ¦       BancoModel.java
    ¦       BoosterModel.java
    ¦       Carta.java
    ¦       ClienteModel.java
    ¦       ColecaoModel.java
    ¦       DeckModel.java
    ¦       EtbModel.java
    ¦       FornecedorModel.java
    ¦       MovimentacaoEstoqueModel.java
    ¦       PagamentoContaPagarModel.java
    ¦       ParcelaContaPagarModel.java
    ¦       PedidoCompraModel.java
    ¦       PedidoEstoqueProdutoModel.java
    ¦       PlanoContaModel.java
    ¦       ProdutoEstoqueDTO.java
    ¦       ProdutoModel.java
    ¦       PromocaoProdutoModel.java
    ¦       SetModel.java
    ¦       TipoPromocaoModel.java
    ¦       TituloContaPagarModel.java
    ¦       UsuarioModel.java
    ¦       VendaItemModel.java
    ¦       VendaModel.java
    ¦       
    +---service
    ¦       BancoService.java
    ¦       ClienteService.java
    ¦       ColecaoService.java
    ¦       ContaPagarService.java
    ¦       EstoqueService.java
    ¦       MovimentacaoEstoqueService.java
    ¦       PlanoContaService.java
    ¦       ProdutoEstoqueService.java
    ¦       SessaoService.java
    ¦       SetService.java
    ¦       VendaService.java
    ¦       
    +---ui
    ¦   ¦   TelaPrincipal.java
    ¦   ¦   
    ¦   +---ajustes
    ¦   ¦   ¦   AjustesPanel.java
    ¦   ¦   ¦   
    ¦   ¦   +---dialog
    ¦   ¦   ¦       AbstractCrudPainel.java
    ¦   ¦   ¦       BancoDialog.java
    ¦   ¦   ¦       CategoriaProdutoDialog.java
    ¦   ¦   ¦       ClienteVipDialog.java
    ¦   ¦   ¦       CondicaoDialog.java
    ¦   ¦   ¦       ConfigFinanceiroDialog.java
    ¦   ¦   ¦       ConfigImpressaoDialog.java
    ¦   ¦   ¦       ConfigLojaDialog.java
    ¦   ¦   ¦       ConfigSistemaDialog.java
    ¦   ¦   ¦       FornecedorDialog.java
    ¦   ¦   ¦       IdiomaDialog.java
    ¦   ¦   ¦       LoginDialog.java
    ¦   ¦   ¦       PlanoContaDialog.java
    ¦   ¦   ¦       PromocaoDialog.java
    ¦   ¦   ¦       SelecionarPlanoContaDialog.java
    ¦   ¦   ¦       TipoCartaDialog.java
    ¦   ¦   ¦       TipoPromocaoDialog.java
    ¦   ¦   ¦       UsuarioDialog.java
    ¦   ¦   ¦       VerProdutosVinculadosDialog.java
    ¦   ¦   ¦       VincularProdutosDialog.java
    ¦   ¦   ¦       
    ¦   ¦   +---painel
    ¦   ¦           AbstractCrudPainel.java
    ¦   ¦           BancoPainel.java
    ¦   ¦           CategoriaProdutoPainel.java
    ¦   ¦           ClienteVipPainel.java
    ¦   ¦           CondicaoPainel.java
    ¦   ¦           FornecedorPainel.java
    ¦   ¦           IdiomaPainel.java
    ¦   ¦           PlanoContaPainel.java
    ¦   ¦           PromocaoPainel.java
    ¦   ¦           TipoCartaPainel.java
    ¦   ¦           TipoPromocaoPainel.java
    ¦   ¦           UsuarioPainel.java
    ¦   ¦           
    ¦   +---clientes
    ¦   ¦   +---dialog
    ¦   ¦   ¦       ClienteCadastroDialog.java
    ¦   ¦   ¦       
    ¦   ¦   +---painel
    ¦   ¦           PainelClientes.java
    ¦   ¦           
    ¦   +---dash
    ¦   ¦   +---dialog
    ¦   ¦   +---painel
    ¦   ¦           DashboardPanel.java
    ¦   ¦           
    ¦   +---dialog
    ¦   ¦       CartaFormDialog.java
    ¦   ¦       SelecionarCategoriaDialog.java
    ¦   ¦       SelectCartaDialog.java
    ¦   ¦       
    ¦   +---estoque
    ¦   ¦   +---dialog
    ¦   ¦   ¦       BuscarCartaDialog.java
    ¦   ¦   ¦       CadastroAcessorioDialog.java
    ¦   ¦   ¦       CadastroBoosterDialog.java
    ¦   ¦   ¦       CadastroCartaDialog.java
    ¦   ¦   ¦       CadastroDeckDialog.java
    ¦   ¦   ¦       CadastroEtbDialog.java
    ¦   ¦   ¦       CadastroProdutoAlimenticioDialog.java
    ¦   ¦   ¦       CriarPedidoEstoqueDialog.java
    ¦   ¦   ¦       EntradaPedidoDialog.java
    ¦   ¦   ¦       EntradaProdutosDialog.java
    ¦   ¦   ¦       FornecedorSelectionDialog.java
    ¦   ¦   ¦       MovimentacaoDialog.java
    ¦   ¦   ¦       NovoPedidoEstoqueDialog.java
    ¦   ¦   ¦       ProdutoCadastroDialog.java
    ¦   ¦   ¦       ProdutosDoPedidoDialog.java
    ¦   ¦   ¦       
    ¦   ¦   +---painel
    ¦   ¦           PainelEstoque.java
    ¦   ¦           PainelPedidosEstoque.java
    ¦   ¦           
    ¦   +---financeiro
    ¦   ¦   +---dialog
    ¦   ¦   ¦       ContaPagarDialog.java
    ¦   ¦   ¦       PagamentoContaPagarDialog.java
    ¦   ¦   ¦       ParcelasTituloDialog.java
    ¦   ¦   ¦       PedidosCompraDialog.java
    ¦   ¦   ¦       VincularPedidosDialog.java
    ¦   ¦   ¦       
    ¦   ¦   +---painel
    ¦   ¦           PainelFinanceiro.java
    ¦   ¦           
    ¦   +---venda
    ¦       +---dialog
    ¦       ¦       SelectProdutoDialog.java
    ¦       ¦       VendaDetalhesDialog.java
    ¦       ¦       VendaFinalizarDialog.java
    ¦       ¦       VendaNovaDialog.java
    ¦       ¦       
    ¦       +---painel
    ¦               PainelVendas.java
    ¦               
    +---util
            AlertUtils.java
            DateUtils.java
            DB.java
            DBPostgres.java
            FormatterFactory.java
            LogService.java
            MaskUtils.java
            PDFGenerator.java
            PythonCaller.java
            
