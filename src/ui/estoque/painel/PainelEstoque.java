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
    private final JTable tabela;
    private final JList<String> listaCategorias;
    private final JComboBox<JogoModel> comboJogo;
    private final JButton botaoAdicionar;

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
                "ID", "Nome", "Tipo", "Qtd", "R$ Compra", "R$ Venda"
        }, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        tabela = new JTable(modeloTabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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

        add(painelRodape, BorderLayout.SOUTH);

        // Carrega os produtos ao iniciar
        listar();
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
                    produto.getId(),
                    produto.getNome(),
                    tipoExibido,
                    produto.getQuantidade(),
                    produto.getPrecoCompra(),
                    produto.getPrecoVenda()
            });
        }
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

        String idSelecionado = (String) modeloTabela.getValueAt(linhaSelecionada, 0);
        ProdutoModel produtoSelecionado = ctrl.listar("").stream()
                .filter(produto -> produto.getId().equals(idSelecionado))
                .findFirst().orElse(null);
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
                try {
                    model.EtbModel etb = new dao.EtbDAO().buscarPorId(produtoSelecionado.getId());
                    if (etb != null) {
                        new ui.estoque.dialog.CadastroEtbDialog((JFrame) owner, etb).setVisible(true);
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
        String idSelecionado = modeloTabela.getValueAt(linhaSelecionada, 0).toString();
        String nomeSelecionado = modeloTabela.getValueAt(linhaSelecionada, 1).toString();

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
