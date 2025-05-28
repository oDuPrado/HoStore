package ui.estoque.painel;

import controller.ProdutoEstoqueController;
import model.ProdutoModel;
import ui.estoque.dialog.*;
import ui.dialog.SelecionarCategoriaDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Painel de estoque repaginado.
 * <p>
 * Novidades:
 * <ul>
 * <li>Barra de filtros com <b>toggle‑buttons</b> para cada categoria;</li>
 * <li>Barra de atalhos “Adicionar” – um botão ➕ para cada tipo de produto;</li>
 * <li>Visual mais espaçado (padding e gaps) e uso de emojis como ícones
 * nativos;</li>
 * <li>Filtro por categoria feito do lado do painel, sem tocar no
 * controller.</li>
 * </ul>
 */
public class PainelEstoque extends JPanel {

    private static final String[] CATEGORIAS = {
            "Todos", "Carta", "Booster", "Deck", "ETB", "Acessório", "Alimento", "Promo", "Outro"
    };

    private String categoriaFiltro = "Todos";

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();
    private final JTextField tfBusca = new JTextField();
    private final DefaultTableModel model;
    private final JTable tabela;

    public PainelEstoque() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        /* =============================== CABEÇALHO =============================== */
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        // ——— Linha de filtros (categoria + busca) ———
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filtros.setAlignmentX(LEFT_ALIGNMENT);

        ButtonGroup grupoCategorias = new ButtonGroup();
        for (String cat : CATEGORIAS) {
            JToggleButton bt = new JToggleButton(cat);
            if ("Todos".equals(cat))
                bt.setSelected(true);
            bt.addActionListener(e -> {
                categoriaFiltro = bt.getText();
                listar();
            });
            grupoCategorias.add(bt);
            filtros.add(bt);
        }

        filtros.add(Box.createHorizontalStrut(12));
        filtros.add(new JLabel("🔍"));
        tfBusca.setPreferredSize(new Dimension(160, 24));
        filtros.add(tfBusca);
        JButton btBuscar = new JButton("OK");
        btBuscar.addActionListener(e -> listar());
        filtros.add(btBuscar);

        JButton btClear = new JButton("⟳");
        btClear.addActionListener(e -> {
            tfBusca.setText("");
            listar();
        });
        filtros.add(btClear);

        header.add(filtros);

        // ——— Linha de atalhos de adição ———
        JPanel atalhos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        atalhos.setAlignmentX(LEFT_ALIGNMENT);

        addShortcut(atalhos, "Carta", "🃏", () -> abrirDialog("Carta"));
        addShortcut(atalhos, "Booster", "📦", () -> abrirDialog("Booster"));
        addShortcut(atalhos, "Deck", "🎴", () -> abrirDialog("Deck"));
        addShortcut(atalhos, "ETB", "📚", () -> abrirDialog("ETB"));
        addShortcut(atalhos, "Acessório", "🛠️", () -> abrirDialog("Acessório"));
        addShortcut(atalhos, "Alimento", "🍫", () -> abrirDialog("Alimento"));
        addShortcut(atalhos, "Outro", "➕", () -> abrirDialog("Outro"));

        header.add(atalhos);

        add(header, BorderLayout.NORTH);

        /* =============================== TABELA =============================== */
        model = new DefaultTableModel(new String[] {
                "ID", "Nome", "Tipo", "Qtd", "R$ Compra", "R$ Venda"
        }, 0) {

            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabela = new JTable(model);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Duplo‑clique = editar
        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() != -1)
                    abrirEditar();
            }
        });

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        /* =============================== RODAPÉ =============================== */
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        JButton btEditar = new JButton("✏️ Editar");
        JButton btDel = new JButton("🗑️ Excluir");
        rodape.add(btEditar);
        rodape.add(btDel);
        // Botão de criar pedido
        JButton btPedido = new JButton("📦 Criar Pedido");
        btPedido.addActionListener(e -> abrirCriarPedido());
        rodape.add(btPedido);

        JButton btVerPedidos = new JButton("📄 Ver Pedidos");
        btVerPedidos.addActionListener(e -> {
            JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ui.estoque.painel.PainelPedidosEstoque(owner).setVisible(true);
            listar();
        });
        rodape.add(btVerPedidos);

        btEditar.addActionListener(e -> abrirEditar());
        btDel.addActionListener(e -> deletarSelecionado());
        add(rodape, BorderLayout.SOUTH);

        listar();
    }

    /*
     * =============================== MÉTODOS AUXILIARES
     * ===============================
     */

    private void addShortcut(JPanel parent, String texto, String emoji, Runnable action) {
        JButton b = new JButton(emoji + " " + texto);
        b.addActionListener(e -> action.run());
        parent.add(b);
    }

    private void listar() {
        model.setRowCount(0);
        List<ProdutoModel> data = ctrl.listar(tfBusca.getText().trim());
        for (ProdutoModel p : data) {
            String tipo = p.getTipo();
            String tipoExibido = tipo;

            // Alimento → exibe o subtipo (Salgadinho, Suco, etc.)
            if ("Alimento".equalsIgnoreCase(tipo)) {
                try {
                    model.AlimentoModel a = new dao.AlimentoDAO().buscarPorId(p.getId());
                    if (a != null && a.getSubtipo() != null && !a.getSubtipo().isBlank()) {
                        tipoExibido = a.getSubtipo();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Acessórios → mostrar categoria
            if ("Acessório".equalsIgnoreCase(tipo)) {
                try {
                    model.AcessorioModel ac = new dao.AcessorioDAO().buscarPorId(p.getId());
                    if (ac != null && ac.getCategoria() != null && !ac.getCategoria().isBlank()) {
                        tipoExibido = ac.getCategoria();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // ETBs → mostrar subtipo visual como Booster Box ou Pokémon Center
            if ("ETB".equalsIgnoreCase(tipo)) {
                try {
                    model.EtbModel etb = new dao.EtbDAO().buscarPorId(p.getId());
                    if (etb != null && etb.getTipo() != null && !etb.getTipo().isBlank()) {
                        tipoExibido = etb.getTipo(); // Booster Box, Pokémon Center, etc.
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Filtro inteligente (categoria visual pode diferir do tipo armazenado)
            boolean corresponde = switch (categoriaFiltro) {
                case "ETB" -> tipo.equalsIgnoreCase("ETB")
                        || tipoExibido.equalsIgnoreCase("Booster Box")
                        || tipoExibido.equalsIgnoreCase("Pokémon Center");
                case "Alimento" -> tipo.equalsIgnoreCase("Alimento");
                default -> "Todos".equalsIgnoreCase(categoriaFiltro)
                        || tipo.equalsIgnoreCase(categoriaFiltro);
            };

            if (!corresponde)
                continue;

            model.addRow(new Object[] {
                    p.getId(), p.getNome(), tipoExibido,
                    p.getQuantidade(), p.getPrecoCompra(),
                    p.getPrecoVenda()
            });
        }

    }

    /* ------------------------------ CRUD ------------------------------ */

    private void abrirDialog(String cat) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        switch (cat) {
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
        }
        listar();
    }

    private void abrirEditar() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um item.");
            return;
        }

        String id = (String) model.getValueAt(row, 0);
        ProdutoModel p = ctrl.listar("").stream()
                .filter(prod -> prod.getId().equals(id))
                .findFirst().orElse(null);
        if (p == null)
            return;

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);

        switch (p.getTipo()) {
            case "Carta":
                model.Carta carta = new dao.CartaDAO().buscarPorId(p.getId());
                if (carta != null)
                    new ui.estoque.dialog.CadastroCartaDialog(owner, carta).setVisible(true);
                break;

            case "Booster":
                model.BoosterModel booster = new dao.BoosterDAO().buscarPorId(p.getId());
                if (booster != null)
                    new ui.estoque.dialog.CadastroBoosterDialog((JFrame) owner, booster).setVisible(true);
                break;

            case "Deck":
                try {
                    model.DeckModel deck = new dao.DeckDAO().buscarPorId(p.getId());
                    if (deck != null)
                        new ui.estoque.dialog.CadastroDeckDialog((JFrame) owner, deck).setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar deck: " + e.getMessage());
                }
                break;

            // 🧠 Aqui agrupamos todos os tipos visuais de ETB
            case "ETB":
            case "Booster Box":
            case "Pokémon Center":
                try {
                    model.EtbModel etb = new dao.EtbDAO().buscarPorId(p.getId());
                    if (etb != null)
                        new ui.estoque.dialog.CadastroEtbDialog((JFrame) owner, etb).setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar ETB: " + e.getMessage());
                }
                break;

            case "Acessório":
                try {
                    model.AcessorioModel ac = new dao.AcessorioDAO().buscarPorId(p.getId());
                    if (ac != null)
                        new ui.estoque.dialog.CadastroAcessorioDialog((JFrame) owner, ac)
                                .setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar acessório: " + e.getMessage());
                    e.printStackTrace();
                }
                break;

            case "Alimento":
                try {
                    model.AlimentoModel alimento = new dao.AlimentoDAO().buscarPorId(p.getId());
                    if (alimento != null)
                        new ui.estoque.dialog.CadastroProdutoAlimenticioDialog((JFrame) owner, alimento)
                                .setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Erro ao buscar alimento: " + e.getMessage());
                    e.printStackTrace();
                }
                break;

            default:
                new ui.estoque.dialog.ProdutoCadastroDialog((JFrame) owner, p).setVisible(true);
        }

        listar();
    }

    private void deletarSelecionado() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um item.");
            return;
        }
        String id = model.getValueAt(row, 0).toString();
        String nome = model.getValueAt(row, 1).toString();

        if (JOptionPane.showConfirmDialog(
                this,
                "Excluir o produto \"" + nome + "\"?",
                "Confirmação",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            ctrl.remover(id);
            listar();
        }
    }

    // Abre dialog para criar pedido com os produtos da categoria atual
    private void abrirCriarPedido() {
        List<ProdutoModel> produtos = ctrl.listar(tfBusca.getText().trim());

        List<ProdutoModel> filtrados = produtos.stream().filter(p -> {
            String tipo = p.getTipo();
            return "Todos".equalsIgnoreCase(categoriaFiltro) || tipo.equalsIgnoreCase(categoriaFiltro);
        }).toList();

        if (filtrados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum produto encontrado para essa categoria.");
            return;
        }

        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        new ui.estoque.dialog.CriarPedidoEstoqueDialog(owner, filtrados).setVisible(true);
    }

}
