package ui.estoque.painel;

import controller.ProdutoEstoqueController;
import dao.JogoDAO;
import model.JogoModel;
import model.ProdutoModel;
import ui.estoque.dialog.*;
import ui.dialog.SelecionarCategoriaDialog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Painel de estoque repaginado.
 * <p>
 * Novidades:
 * <ul>
 * <li>Menu lateral com JList de categorias;</li>
 * <li>Barra superior com busca e filtro por Jogo (TCG);</li>
 * <li>Botão flutuante único “➕ Adicionar Produto” que abre JPopupMenu;</li>
 * <li>Visual mais espaçado (padding e gaps) e uso de emojis como ícones
 * nativos;</li>
 * <li>Filtro feito do lado do painel, sem tocar no controller.</li>
 * </ul>
 */
public class PainelEstoque extends JPanel {

    private static final String[] CATEGORIAS = {
            "Todos", "Carta", "Booster", "Deck", "Selados", "Acessório", "Alimento", "Promo", "Outro"
    };

    private String categoriaFiltro = "Todos";
    private String jogoFiltroId = null; // null significa “Todos Jogos”

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();
    private final JTextField campoBusca = new JTextField();
    private final DefaultTableModel modeloTabela;
    // Lista para armazenar os produtos atualmente exibidos na tabela
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
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        /*
         * =============================== PAINEL LATERAL: CATEGORIAS
         * ===============================
         */
        listaCategorias = new JList<>(CATEGORIAS);
        listaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaCategorias.setSelectedIndex(0);
        listaCategorias.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selecionada = listaCategorias.getSelectedValue();
                    categoriaFiltro = selecionada;
                    listar();
                }
            }
        });
        JScrollPane scrollCategorias = new JScrollPane(listaCategorias);
        scrollCategorias.setPreferredSize(new Dimension(120, 0));
        add(scrollCategorias, BorderLayout.WEST);

        /* =============================== CABEÇALHO =============================== */
        JPanel painelSuperior = new JPanel(new BorderLayout(10, 10));

        // ——— Subpainel de filtros (busca + filtro por jogo) ———
        JPanel painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        // Filtro por Jogo
        painelFiltros.add(new JLabel("Jogo:"));
        comboJogo = new JComboBox<>();
        comboJogo.setPreferredSize(new Dimension(160, 24));
        carregarJogosNoCombo();
        comboJogo.addActionListener(e -> {
            JogoModel selecionado = (JogoModel) comboJogo.getSelectedItem();
            if (selecionado != null && selecionado.getId() != null) {
                jogoFiltroId = selecionado.getId();
            } else {
                jogoFiltroId = null;
            }
            listar();
        });
        painelFiltros.add(comboJogo);

        // Espaço entre filtros
        painelFiltros.add(Box.createHorizontalStrut(12));

        // Campo de busca por texto
        painelFiltros.add(new JLabel("🔍"));
        campoBusca.setPreferredSize(new Dimension(160, 24));
        painelFiltros.add(campoBusca);

        JButton botaoBuscar = new JButton("OK");
        botaoBuscar.addActionListener(e -> listar());
        painelFiltros.add(botaoBuscar);

        JButton botaoLimpar = new JButton("⟳");
        botaoLimpar.addActionListener(e -> {
            campoBusca.setText("");
            listar();
        });
        painelFiltros.add(botaoLimpar);

        painelSuperior.add(painelFiltros, BorderLayout.WEST);

        // ——— Botão flutuante “Adicionar Produto” com JPopupMenu ———
        botaoAdicionar = new JButton("➕ Adicionar Produto");
        botaoAdicionar.setToolTipText("Clique para adicionar novo produto");
        botaoAdicionar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarMenuAdicionarProduto(botaoAdicionar);
            }
        });
        JPanel painelBotaoAdicionar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        painelBotaoAdicionar.add(botaoAdicionar);
        painelSuperior.add(painelBotaoAdicionar, BorderLayout.EAST);

        add(painelSuperior, BorderLayout.NORTH);

        /* =============================== TABELA =============================== */
        modeloTabela = new DefaultTableModel(new String[] {
                "Nome", "Tipo", "Quantidade", "R$ Compra", "R$ Venda", "Fornecedor"
        }, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        tabela = new JTable(modeloTabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Centralizar colunas de números e formatar moeda
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer moeda = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof Number) {
                    setText(brl.format(((Number) value).doubleValue()));
                } else {
                    super.setValue(value);
                }
            }
        };
        moeda.setHorizontalAlignment(SwingConstants.CENTER);

        // Quantidade = col 2
        tabela.getColumnModel().getColumn(2).setCellRenderer(center);
        // R$ Compra = col 3
        tabela.getColumnModel().getColumn(3).setCellRenderer(moeda);
        // R$ Venda = col 4
        tabela.getColumnModel().getColumn(4).setCellRenderer(moeda);

        // Duplo-clique = editar
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2 && tabela.getSelectedRow() != -1) {
                    abrirEditar();
                }
            }
        });

        JScrollPane scrollTabela = new JScrollPane(tabela);
        add(scrollTabela, BorderLayout.CENTER);

        /* =============================== RODAPÉ =============================== */
        JPanel painelRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));

        JButton botaoEditar = new JButton("✏️ Editar");
        JButton botaoExcluir = new JButton("🗑️ Excluir");
        painelRodape.add(botaoEditar);
        painelRodape.add(botaoExcluir);

        botaoEditar.addActionListener(e -> abrirEditar());
        botaoExcluir.addActionListener(e -> deletarSelecionado());

        // Botão de criar pedido
        JButton botaoCriarPedido = new JButton("📦 Criar Pedido");
        botaoCriarPedido.addActionListener(e -> abrirCriarPedido());
        painelRodape.add(botaoCriarPedido);

        // Botão para visualizar pedidos
        JButton botaoVerPedidos = new JButton("📄 Ver Pedidos");
        botaoVerPedidos.addActionListener(e -> {
            JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ui.estoque.painel.PainelPedidosEstoque(owner).setVisible(true);
            listar();
        });
        painelRodape.add(botaoVerPedidos);

        // Botão para abrir movimentações de estoque
        JButton botaoMovimentacoes = new JButton("📊 Movimentações");
        botaoMovimentacoes.addActionListener(e -> {
            JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ui.estoque.dialog.MovimentacaoEstoqueDialog(owner).setVisible(true);
        });
        painelRodape.add(botaoMovimentacoes);

        // painel de resumo abaixo da tabela
        JPanel painelResumo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        painelResumo.add(lblTotalEstoqueCusto);
        painelResumo.add(lblTotalEstoqueVenda);
        painelResumo.add(lblPmz);
        painelResumo.add(lblItensEstoque);
        painelResumo.add(lblPrecoMedioVenda);

        JPanel wrapperSouth = new JPanel(new BorderLayout());
        wrapperSouth.add(painelResumo, BorderLayout.NORTH);
        wrapperSouth.add(painelRodape, BorderLayout.SOUTH);
        add(wrapperSouth, BorderLayout.SOUTH);

        // Carrega os produtos ao iniciar
        listar();
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
     * tipo de cadastro.
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
     * Atualiza a tabela de produtos aplicando filtros de pesquisa, categoria e
     * jogo.
     */
    private void listar() {
        modeloTabela.setRowCount(0);
        List<ProdutoModel> produtos = ctrl.listar(campoBusca.getText().trim());

        // Preencher o nome do fornecedor a partir do ID
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

            // Pré-filtro bruto para evitar NullPointer
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

        // Filtro por jogo (jogoFiltroId == null significa “Todos Jogos”)
        List<ProdutoModel> filtradosPorJogo = filtradosPorCategoria.stream().filter(produto -> {
            if (jogoFiltroId == null) {
                return true;
            }
            return jogoFiltroId.equals(produto.getJogoId());
        }).collect(Collectors.toList());

        // Salva a lista filtrada para uso em editar
        produtosFiltrados = filtradosPorJogo;

        // Agora preenche a tabela conforme antiga lógica, mas apenas com
        // filtradosPorJogo
        for (ProdutoModel produto : filtradosPorJogo) {
            String tipo = produto.getTipo();
            String tipoExibido = tipo;

            // Alimento → exibe o subtipo (Salgadinho, Suco, etc.)
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

            // Acessórios → mostrar categoria
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

            // ETBs → mostrar subtipo visual como Booster Box ou Pokémon Center
            if ("ETB".equalsIgnoreCase(tipo)) {
                try {
                    model.EtbModel etbModel = new dao.EtbDAO().buscarPorId(produto.getId());
                    if (etbModel != null && etbModel.getTipo() != null && !etbModel.getTipo().isBlank()) {
                        tipoExibido = etbModel.getTipo(); // Booster Box, Pokémon Center, etc.
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            modeloTabela.addRow(new Object[] {
                    produto.getNome(),
                    tipoExibido,
                    produto.getQuantidade(),
                    produto.getPrecoCompra(),
                    produto.getPrecoVenda(),
                    produto.getFornecedorNome()
            });
        }

        // ======================= RESUMO (CONSISTENTE COM RELATÓRIOS)
        // =======================
        // Regras:
        // - Estoque (Custo): soma(qtd * precoCompra)
        // - Estoque (Venda): soma(qtd * precoVenda)
        // - Itens em Estoque: soma(qtd)
        // - PMZ: (Estoque Custo) / (Itens) [se itens > 0]
        // - Preço Médio Venda (ponderado): (Estoque Venda) / (Itens) [se itens > 0]

        int itensEstoque = 0;
        double estoqueCusto = 0.0;
        double estoqueVenda = 0.0;

        for (ProdutoModel p : filtradosPorJogo) {
            int qtd = Math.max(0, p.getQuantidade());

            double compra = p.getPrecoCompra();
            double venda = p.getPrecoVenda();

            // evita ruído: se vier null/NaN/negativo, zera (melhor do que quebrar KPI)
            if (Double.isNaN(compra) || Double.isInfinite(compra) || compra < 0)
                compra = 0.0;
            if (Double.isNaN(venda) || Double.isInfinite(venda) || venda < 0)
                venda = 0.0;

            itensEstoque += qtd;
            estoqueCusto += (qtd * compra);
            estoqueVenda += (qtd * venda);
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
                new CadastroAcessorioDialog((JFrame) owner).setVisible(true);
                break;
            case "Alimento":
                new CadastroProdutoAlimenticioDialog((JFrame) owner).setVisible(true);
                break;
            default:
                // fallback genérico
                new ProdutoCadastroDialog((JFrame) owner, null).setVisible(true);
                break;
        }
        listar();
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
        if (produtoSelecionado == null) {
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        String tipo = produtoSelecionado.getTipo();

        switch (tipo) {
            case "Carta":
                model.Carta carta = new dao.CartaDAO().buscarPorId(produtoSelecionado.getId());
                if (carta != null) {
                    new ui.estoque.dialog.CadastroCartaDialog(owner, carta).setVisible(true);
                }
                break;

            case "Booster":
                model.BoosterModel booster = new dao.BoosterDAO().buscarPorId(produtoSelecionado.getId());
                if (booster != null) {
                    new ui.estoque.dialog.CadastroBoosterDialog((JFrame) owner, booster).setVisible(true);
                }
                break;

            case "Deck":
                try {
                    model.DeckModel deck = new dao.DeckDAO().buscarPorId(produtoSelecionado.getId());
                    if (deck != null) {
                        new ui.estoque.dialog.CadastroDeckDialog((JFrame) owner, deck).setVisible(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar deck: " + e.getMessage());
                }
                break;

            case "ETB":
            case "Booster Box":
            case "Pokémon Center":
            case "Mini ETB":
            case "Collection Box":
            case "Special Collection":
            case "Latas":
            case "Box colecionáveis":
            case "Mini Booster Box":
            case "Trainer Kit":
                try {
                    model.EtbModel etb = new dao.EtbDAO().buscarPorId(produtoSelecionado.getId());
                    if (etb != null) {
                        new CadastroEtbDialog((JFrame) owner, etb).setVisible(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar ETB: " + e.getMessage());
                }
                break;

            case "Acessório":
                try {
                    model.AcessorioModel acessorio = new dao.AcessorioDAO().buscarPorId(produtoSelecionado.getId());
                    if (acessorio != null) {
                        new ui.estoque.dialog.CadastroAcessorioDialog((JFrame) owner, acessorio).setVisible(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar acessório: " + e.getMessage());
                    e.printStackTrace();
                }
                break;

            case "Alimento":
                try {
                    model.AlimentoModel alimento = new dao.AlimentoDAO().buscarPorId(produtoSelecionado.getId());
                    if (alimento != null) {
                        new ui.estoque.dialog.CadastroProdutoAlimenticioDialog((JFrame) owner, alimento)
                                .setVisible(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar alimento: " + e.getMessage());
                    e.printStackTrace();
                }
                break;

            default:
                new ui.estoque.dialog.ProdutoCadastroDialog((JFrame) owner, produtoSelecionado).setVisible(true);
                break;
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
            if ("Todos".equalsIgnoreCase(categoriaFiltro)) {
                return true;
            }
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
        new ui.estoque.dialog.CriarPedidoEstoqueDialog(owner, filtradosPorCategoria).setVisible(true);
    }
}
