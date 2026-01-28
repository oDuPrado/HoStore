// src/ui/estoque/painel/PainelEstoque.java
package ui.estoque.painel;

import controller.ProdutoEstoqueController;
import dao.JogoDAO;
import model.JogoModel;
import model.ProdutoModel;
import ui.estoque.dialog.*;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PainelEstoque extends JPanel {

    private static final String[] CATEGORIAS = {
            "Todos", "Carta", "Booster", "Deck", "Selados", "Acessório", "Alimento", "Promo", "Outro"
    };

    private String categoriaFiltro = "Todos";
    private String jogoFiltroId = null; // null = “Todos Jogos”

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    private final JTextField campoBusca = new JTextField();
    private final DefaultTableModel modeloTabela;
    private List<ProdutoModel> produtosFiltrados;

    private final JTable tabela;
    private final JList<String> listaCategorias;
    private final JComboBox<JogoModel> comboJogo;
    private final JButton botaoAdicionar;

    // Summary labels
    private final JLabel lblTotalEstoqueCusto = new JLabel("Estoque (Custo): R$ 0,00");
    private final JLabel lblTotalEstoqueVenda = new JLabel("Estoque (Venda): R$ 0,00");
    private final JLabel lblPmz = new JLabel("PMZ: R$ 0,00");
    private final JLabel lblItensEstoque = new JLabel("Itens em Estoque: 0");
    private final JLabel lblPrecoMedioVenda = new JLabel("Preço Médio Venda: R$ 0,00");

    private final NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public PainelEstoque() {
        UiKit.applyPanelBase(this);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        /*
         * =============================== LATERAL (CARD)
         * ===============================
         */
        JPanel cardCategorias = UiKit.card();
        cardCategorias.setLayout(new BorderLayout(8, 8));
        cardCategorias.add(UiKit.title("Categorias"), BorderLayout.NORTH);

        listaCategorias = new JList<>(CATEGORIAS);
        listaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaCategorias.setSelectedIndex(0);
        listaCategorias.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                categoriaFiltro = listaCategorias.getSelectedValue();
                listar();
            }
        });

        JScrollPane scrollCategorias = UiKit.scroll(listaCategorias);
        scrollCategorias.setPreferredSize(new Dimension(170, 0));
        cardCategorias.add(scrollCategorias, BorderLayout.CENTER);

        add(cardCategorias, BorderLayout.WEST);

        /*
         * =============================== TOPO (CARD) ===============================
         */
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        painelFiltros.setOpaque(false);

        // Filtro por Jogo
        painelFiltros.add(new JLabel("Jogo:"));
        comboJogo = new JComboBox<>();
        comboJogo.setPreferredSize(new Dimension(180, 28));
        carregarJogosNoCombo();
        comboJogo.addActionListener(e -> {
            JogoModel selecionado = (JogoModel) comboJogo.getSelectedItem();
            jogoFiltroId = (selecionado != null && selecionado.getId() != null) ? selecionado.getId() : null;
            listar();
        });
        painelFiltros.add(comboJogo);

        painelFiltros.add(Box.createHorizontalStrut(10));

        // Busca
        painelFiltros.add(new JLabel("🔍"));
        campoBusca.setPreferredSize(new Dimension(220, 28));
        painelFiltros.add(campoBusca);

        JButton botaoBuscar = UiKit.primary("Filtrar");
        botaoBuscar.addActionListener(e -> listar());
        painelFiltros.add(botaoBuscar);

        JButton botaoLimpar = UiKit.ghost("Limpar");
        botaoLimpar.addActionListener(e -> {
            campoBusca.setText("");
            listar();
        });
        painelFiltros.add(botaoLimpar);

        topCard.add(painelFiltros, BorderLayout.WEST);

        // Botão “Adicionar Produto” com popup
        botaoAdicionar = UiKit.primary("➕ Adicionar Produto");
        botaoAdicionar.setToolTipText("Clique para adicionar novo produto");
        botaoAdicionar.addActionListener(e -> mostrarMenuAdicionarProduto(botaoAdicionar));

        JPanel painelBotaoAdicionar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        painelBotaoAdicionar.setOpaque(false);
        painelBotaoAdicionar.add(botaoAdicionar);

        topCard.add(painelBotaoAdicionar, BorderLayout.EAST);

        add(topCard, BorderLayout.NORTH);

        /*
         * =============================== TABELA (CARD) ===============================
         */
        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.add(UiKit.title("Estoque"), BorderLayout.NORTH);

        modeloTabela = new DefaultTableModel(new String[] {
                "Nome", "Tipo", "Quantidade", "R$ Compra", "R$ Venda (min-max)", "Fornecedor"
        }, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };

        tabela = new JTable(modeloTabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        UiKit.tableDefaults(tabela);

        // Zebra em todas as colunas
        var zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Duplo clique = editar
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2 && tabela.getSelectedRow() != -1) {
                    abrirEditar();
                }
            }
        });

        JScrollPane scrollTabela = UiKit.scroll(tabela);
        tableCard.add(scrollTabela, BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        /*
         * =============================== RODAPÉ (CARD) ===============================
         */
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        JPanel painelResumo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        painelResumo.setOpaque(false);
        painelResumo.add(lblTotalEstoqueCusto);
        painelResumo.add(lblTotalEstoqueVenda);
        painelResumo.add(lblPmz);
        painelResumo.add(lblItensEstoque);
        painelResumo.add(lblPrecoMedioVenda);
        bottomCard.add(painelResumo, BorderLayout.NORTH);

        JPanel painelRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        painelRodape.setOpaque(false);

        JButton botaoEditar = UiKit.ghost("✏️ Editar");
        JButton botaoExcluir = UiKit.ghost("🗑️ Excluir");
        JButton botaoLotes = UiKit.ghost("Lotes");
        JButton botaoCriarPedido = UiKit.ghost("📦 Criar Pedido");
        JButton botaoVerPedidos = UiKit.ghost("📄 Ver Pedidos");
        JButton botaoMovimentacoes = UiKit.ghost("📊 Movimentações");

        botaoEditar.addActionListener(e -> abrirEditar());
        botaoExcluir.addActionListener(e -> deletarSelecionado());
        botaoLotes.addActionListener(e -> abrirLotes());
        botaoCriarPedido.addActionListener(e -> abrirCriarPedido());
        botaoVerPedidos.addActionListener(e -> {
            JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ui.estoque.painel.PainelPedidosEstoque(owner).setVisible(true);
            listar();
        });
        botaoMovimentacoes.addActionListener(e -> {
            JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ui.estoque.dialog.MovimentacaoEstoqueDialog(owner).setVisible(true);
        });

        painelRodape.add(botaoEditar);
        painelRodape.add(botaoExcluir);
        painelRodape.add(botaoLotes);
        painelRodape.add(botaoCriarPedido);
        painelRodape.add(botaoVerPedidos);
        painelRodape.add(botaoMovimentacoes);

        bottomCard.add(painelRodape, BorderLayout.SOUTH);

        add(bottomCard, BorderLayout.SOUTH);

        // Carrega ao iniciar
        listar();

        // Atalhos
        SwingUtilities.invokeLater(() -> {
            JRootPane root = SwingUtilities.getRootPane(this);
            if (root == null)
                return;

            InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = root.getActionMap();

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focoBusca");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focoTabela");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluirSelecionado");

            am.put("focoBusca", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    campoBusca.requestFocusInWindow();
                }
            });
            am.put("focoTabela", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    tabela.requestFocusInWindow();
                }
            });
            am.put("excluirSelecionado", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    deletarSelecionado();
                }
            });
        });
    }

    /**
     * Carrega todos os jogos do banco no combo, incluindo a opção “Todos Jogos”.
     */
    private void carregarJogosNoCombo() {
        comboJogo.addItem(new JogoModel(null, "Todos Jogos"));
        try {
            JogoDAO dao = new JogoDAO();
            List<JogoModel> jogos = dao.listarTodos();
            for (JogoModel jogo : jogos) {
                comboJogo.addItem(jogo);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar lista de jogos: " + e.getMessage());
        }
    }

    /**
     * Exibe um JPopupMenu ancorado no botão “➕ Adicionar Produto” com opções de
     * tipo.
     */
    private void mostrarMenuAdicionarProduto(Component componente) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemCarta = new JMenuItem("Carta 🃏");
        JMenuItem itemBooster = new JMenuItem("Booster 📦");
        JMenuItem itemDeck = new JMenuItem("Deck 🎴");
        JMenuItem itemSelados = new JMenuItem("Selados 📚");
        JMenuItem itemAcessorio = new JMenuItem("Acessório 🛠️");
        JMenuItem itemAlimento = new JMenuItem("Alimento 🍫");
        JMenuItem itemOutro = new JMenuItem("Outro ➕");

        itemCarta.addActionListener(e -> abrirDialog("Carta"));
        itemBooster.addActionListener(e -> abrirDialog("Booster"));
        itemDeck.addActionListener(e -> abrirDialog("Deck"));
        itemSelados.addActionListener(e -> abrirDialog("ETB"));
        itemAcessorio.addActionListener(e -> abrirDialog("Acessório"));
        itemAlimento.addActionListener(e -> abrirDialog("Alimento"));
        itemOutro.addActionListener(e -> abrirDialog("Outro"));

        menu.add(itemCarta);
        menu.add(itemBooster);
        menu.add(itemDeck);
        menu.add(itemSelados);
        menu.add(itemAcessorio);
        menu.add(itemAlimento);
        menu.addSeparator();
        menu.add(itemOutro);

        menu.show(componente, 0, componente.getHeight());
    }

    /**
     * Atualiza a tabela de produtos aplicando filtros: busca, categoria e jogo.
     */
    private void listar() {
        modeloTabela.setRowCount(0);
        List<ProdutoModel> produtos = ctrl.listar(campoBusca.getText().trim());

        // Preencher nome do fornecedor a partir do ID
        for (ProdutoModel p : produtos) {
            try {
                if (p.getFornecedorId() != null) {
                    model.FornecedorModel fornecedor = new dao.FornecedorDAO().buscarPorId(p.getFornecedorId());
                    if (fornecedor != null) {
                        p.setFornecedorNome(fornecedor.getNome());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<ProdutoModel> filtradosPorCategoria = produtos.stream().filter(produto -> {
            String tipo = produto.getTipo();
            if (tipo == null)
                return false;

            if ("Todos".equalsIgnoreCase(categoriaFiltro)) {
                return true;
            }

            if ("Selados".equalsIgnoreCase(categoriaFiltro)) {
                return tipo.equalsIgnoreCase("ETB") || List.of(
                        "Booster Box",
                        "Pokémon Center",
                        "Mini ETB",
                        "Collection Box",
                        "Special Collection",
                        "Latas",
                        "Box colecionáveis",
                        "Mini Booster Box",
                        "Trainer Kit").contains(tipo);
            }

            return tipo.equalsIgnoreCase(categoriaFiltro);
        }).collect(Collectors.toList());

        // Filtro por jogo
        List<ProdutoModel> filtradosPorJogo = filtradosPorCategoria.stream().filter(produto -> {
            if (jogoFiltroId == null)
                return true;
            return jogoFiltroId.equals(produto.getJogoId());
        }).collect(Collectors.toList());

        produtosFiltrados = filtradosPorJogo;

        // Preenche tabela
        int itensEstoque = 0;
        double estoqueCusto = 0.0;
        double estoqueVenda = 0.0;
        try (java.sql.Connection c = util.DB.get()) {
            dao.EstoqueLoteDAO loteDAO = new dao.EstoqueLoteDAO();
            for (ProdutoModel produto : filtradosPorJogo) {
            String tipo = produto.getTipo();
            String tipoExibido = tipo;

            if ("Alimento".equalsIgnoreCase(tipo)) {
                try {
                    model.AlimentoModel alimentoModel = new dao.AlimentoDAO().buscarPorId(produto.getId());
                    if (alimentoModel != null && alimentoModel.getSubtipo() != null
                            && !alimentoModel.getSubtipo().isBlank()) {
                        tipoExibido = alimentoModel.getSubtipo();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if ("Acessório".equalsIgnoreCase(tipo)) {
                try {
                    model.AcessorioModel acessorioModel = new dao.AcessorioDAO().buscarPorId(produto.getId());
                    if (acessorioModel != null && acessorioModel.getCategoria() != null
                            && !acessorioModel.getCategoria().isBlank()) {
                        tipoExibido = acessorioModel.getCategoria();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if ("ETB".equalsIgnoreCase(tipo)) {
                try {
                    model.EtbModel etbModel = new dao.EtbDAO().buscarPorId(produto.getId());
                    if (etbModel != null && etbModel.getTipo() != null && !etbModel.getTipo().isBlank()) {
                        tipoExibido = etbModel.getTipo();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            dao.EstoqueLoteDAO.LoteResumo resumo;
            dao.EstoqueLoteDAO.LoteFaixa faixa;
            try {
                resumo = loteDAO.obterResumoProduto(produto.getId(), c);
                faixa = loteDAO.obterFaixaPrecoVenda(produto.getId(), c);
            } catch (Exception ex) {
                int qtdFallback = Math.max(0, produto.getQuantidade());
                resumo = new dao.EstoqueLoteDAO.LoteResumo(
                        qtdFallback,
                        qtdFallback * Math.max(0.0, produto.getPrecoCompra()),
                        qtdFallback * Math.max(0.0, produto.getPrecoVenda()));
                faixa = new dao.EstoqueLoteDAO.LoteFaixa(produto.getPrecoVenda(), produto.getPrecoVenda());
            }

            String faixaVenda = formatFaixaVenda(faixa);
            modeloTabela.addRow(new Object[] {
                    produto.getNome(),
                    tipoExibido,
                    resumo.qtdDisponivel,
                    produto.getPrecoCompra(),
                    faixaVenda,
                    produto.getFornecedorNome()
            });

            // acumula por lote
            itensEstoque += Math.max(0, resumo.qtdDisponivel);
            estoqueCusto += Math.max(0.0, resumo.custoTotal);
            estoqueVenda += Math.max(0.0, resumo.vendaTotal);
        }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        double pmz = itensEstoque > 0 ? (estoqueCusto / itensEstoque) : 0.0;
        double precoMedioVenda = itensEstoque > 0 ? (estoqueVenda / itensEstoque) : 0.0;

        lblTotalEstoqueCusto.setText("Estoque (Custo): " + brl.format(estoqueCusto));
        lblTotalEstoqueVenda.setText("Estoque (Venda): " + brl.format(estoqueVenda));
        lblPmz.setText("PMZ: " + brl.format(pmz));
        lblItensEstoque.setText("Itens em Estoque: " + itensEstoque);
        lblPrecoMedioVenda.setText("Preço Médio Venda: " + brl.format(precoMedioVenda));
    }

    /* ------------------------------ AÇÕES CRUD ------------------------------ */

    private void abrirDialog(String categoria) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        switch (categoria) {
            case "Carta":
                new CadastroCartaDialog((JFrame) owner, null).setVisible(true);
                break;
            case "Booster":
                new CadastroBoosterDialog((JFrame) owner).setVisible(true);
                break;
            case "Deck":
                new CadastroDeckDialog((JFrame) owner).setVisible(true);
                break;
            case "ETB":
                new CadastroEtbDialog((JFrame) owner).setVisible(true);
                break;
            case "Acessório":
                new CadastroAcessorioDialog((JFrame) owner, null).setVisible(true);
                break;
            case "Alimento":
                new CadastroProdutoAlimenticioDialog((JFrame) owner, null).setVisible(true);
                break;
            default:
                new ProdutoCadastroDialog((JFrame) owner, null).setVisible(true);
                break;
        }
        listar();
    }

    private ProdutoModel obterSelecionado() {
        int linhaSelecionada = tabela.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um item.");
            return null;
        }

        if (produtosFiltrados == null || linhaSelecionada >= produtosFiltrados.size()) {
            JOptionPane.showMessageDialog(this, "Selecao invalida.");
            return null;
        }

        return produtosFiltrados.get(linhaSelecionada);
    }

    private void abrirLotes() {
        ProdutoModel selecionado = obterSelecionado();
        if (selecionado == null)
            return;

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        new ui.estoque.dialog.LotesProdutoDialog(owner, selecionado).setVisible(true);
        listar();
    }

    private String formatFaixaVenda(dao.EstoqueLoteDAO.LoteFaixa faixa) {
        if (faixa == null || (faixa.min == null && faixa.max == null))
            return "-";
        double min = (faixa.min != null) ? faixa.min : 0.0;
        double max = (faixa.max != null) ? faixa.max : min;
        if (Double.isNaN(min) || Double.isInfinite(min))
            min = 0.0;
        if (Double.isNaN(max) || Double.isInfinite(max))
            max = min;
        if (Math.abs(max - min) < 0.000001) {
            return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(min);
        }
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(min) + "–"
                + NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(max);
    }

    private void abrirEditar() {
        int linhaSelecionada = tabela.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um item.");
            return;
        }

        if (produtosFiltrados == null || linhaSelecionada >= produtosFiltrados.size()) {
            JOptionPane.showMessageDialog(this, "Erro ao localizar produto selecionado.");
            return;
        }

        ProdutoModel produtoSelecionado = produtosFiltrados.get(linhaSelecionada);
        if (produtoSelecionado == null)
            return;

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        String tipo = produtoSelecionado.getTipo();

        switch (tipo) {
            case "Carta" -> {
                model.Carta carta = new dao.CartaDAO().buscarPorId(produtoSelecionado.getId());
                if (carta != null)
                    new CadastroCartaDialog((JFrame) owner, carta).setVisible(true);
            }
            case "Booster" -> {
                model.BoosterModel booster = new dao.BoosterDAO().buscarPorId(produtoSelecionado.getId());
                if (booster != null)
                    new CadastroBoosterDialog((JFrame) owner, booster).setVisible(true);
            }
            case "Deck" -> {
                try {
                    model.DeckModel deck = new dao.DeckDAO().buscarPorId(produtoSelecionado.getId());
                    if (deck != null)
                        new CadastroDeckDialog((JFrame) owner, deck).setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar deck: " + e.getMessage());
                }
            }
            case "ETB", "Booster Box", "Pokémon Center", "Mini ETB", "Collection Box", "Special Collection",
                    "Latas", "Box colecionáveis", "Mini Booster Box", "Trainer Kit" -> {
                try {
                    model.EtbModel etb = new dao.EtbDAO().buscarPorId(produtoSelecionado.getId());
                    if (etb != null)
                        new CadastroEtbDialog((JFrame) owner, etb).setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar ETB: " + e.getMessage());
                }
            }
            case "Acessório" -> {
                try {
                    model.AcessorioModel acessorio = new dao.AcessorioDAO().buscarPorId(produtoSelecionado.getId());
                    if (acessorio != null)
                        new CadastroAcessorioDialog((JFrame) owner, acessorio).setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar acessório: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            case "Alimento" -> {
                try {
                    model.AlimentoModel alimento = new dao.AlimentoDAO().buscarPorId(produtoSelecionado.getId());
                    if (alimento != null)
                        new CadastroProdutoAlimenticioDialog((JFrame) owner, alimento).setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar alimento: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            default -> new ProdutoCadastroDialog((JFrame) owner, produtoSelecionado).setVisible(true);
        }

        listar();
    }

    private void deletarSelecionado() {
        int linhaSelecionada = tabela.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um item.");
            return;
        }

        if (produtosFiltrados == null || linhaSelecionada >= produtosFiltrados.size()) {
            JOptionPane.showMessageDialog(this, "Erro ao localizar produto selecionado.");
            return;
        }

        ProdutoModel produtoSelecionado = produtosFiltrados.get(linhaSelecionada);
        if (produtoSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Produto não encontrado.");
            return;
        }

        String idSelecionado = produtoSelecionado.getId();
        String nomeSelecionado = produtoSelecionado.getNome();

        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Excluir o produto \"" + nomeSelecionado + "\"?",
                "Confirmação",
                JOptionPane.OK_CANCEL_OPTION);

        if (opcao == JOptionPane.OK_OPTION) {
            ctrl.remover(idSelecionado);
            listar();
        }
    }

    private void abrirCriarPedido() {
        List<ProdutoModel> produtos = ctrl.listar(campoBusca.getText().trim());

        List<ProdutoModel> filtradosPorCategoria = produtos.stream().filter(produto -> {
            String tipo = produto.getTipo();
            if (tipo == null)
                return false;

            if ("Todos".equalsIgnoreCase(categoriaFiltro))
                return true;

            if ("Selados".equalsIgnoreCase(categoriaFiltro)) {
                return tipo.equalsIgnoreCase("ETB");
            }
            return tipo.equalsIgnoreCase(categoriaFiltro);
        }).collect(Collectors.toList());

        if (filtradosPorCategoria.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum produto encontrado para essa categoria.");
            return;
        }

        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        new CriarPedidoEstoqueDialog(owner, filtradosPorCategoria).setVisible(true);
    }
}
