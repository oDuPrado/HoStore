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
 *   <li>Barra de filtros com <b>toggle‑buttons</b> para cada categoria;</li>
 *   <li>Barra de atalhos “Adicionar” – um botão ➕ para cada tipo de produto;</li>
 *   <li>Visual mais espaçado (padding e gaps) e uso de emojis como ícones nativos;</li>
 *   <li>Filtro por categoria feito do lado do painel, sem tocar no controller.</li>
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
            if ("Todos".equals(cat)) bt.setSelected(true);
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
        model = new DefaultTableModel(new String[]{
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
                if (e.getClickCount() == 2 && tabela.getSelectedRow() != -1) abrirEditar();
            }
        });

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        /* =============================== RODAPÉ =============================== */
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        JButton btEditar = new JButton("✏️ Editar");
        JButton btDel = new JButton("🗑️ Excluir");
        rodape.add(btEditar);
        rodape.add(btDel);
        btEditar.addActionListener(e -> abrirEditar());
        btDel.addActionListener(e -> deletarSelecionado());
        add(rodape, BorderLayout.SOUTH);

        listar();
    }

    /* =============================== MÉTODOS AUXILIARES =============================== */

    private void addShortcut(JPanel parent, String texto, String emoji, Runnable action) {
        JButton b = new JButton(emoji + " " + texto);
        b.addActionListener(e -> action.run());
        parent.add(b);
    }

    private void listar() {
        model.setRowCount(0);
        List<ProdutoModel> data = ctrl.listar(tfBusca.getText().trim());
        for (ProdutoModel p : data) {
            if (!"Todos".equals(categoriaFiltro) && !p.getTipo().equalsIgnoreCase(categoriaFiltro)) continue;
            model.addRow(new Object[]{
                p.getId(), p.getNome(), p.getTipo(),
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
        if (p == null) return;
    
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
        String id   = model.getValueAt(row, 0).toString();
String nome = model.getValueAt(row, 1).toString();

if (JOptionPane.showConfirmDialog(
        this,
        "Excluir o produto \"" + nome + "\"?",
        "Confirmação",
        JOptionPane.OK_CANCEL_OPTION
) == JOptionPane.OK_OPTION) {
    ctrl.remover(id);
    listar();
}

    }
}
