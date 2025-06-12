package ui.ajustes.dialog;

import dao.PromocaoProdutoDAO;
import model.PromocaoProdutoModel;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * @TODO: AJUSTAR_VER_PRODUTOS_VINCULADOS
 * Mostra produtos já vinculados e permite desvincular ou vincular novos.
 */
public class VerProdutosVinculadosDialog extends JDialog {

    private final String promocaoId;
    private final JComboBox<String> cbCategoria = new JComboBox<>();
    private final JTextField tfBusca             = new JTextField();
    private final DefaultListModel<String> listAvailable = new DefaultListModel<>();
    private final JList<String> lstAvailable     = new JList<>(listAvailable);
    private final DefaultListModel<String> listLinked    = new DefaultListModel<>();
    private final JList<String> lstLinked        = new JList<>(listLinked);

    // mapeia nome → id
    private final Map<String, String> prodIdMap = new HashMap<>();
    private final Map<String, String> linkIdMap = new HashMap<>();

    public VerProdutosVinculadosDialog(String promocaoId) {
        super((Frame) null, "Produtos da Promoção", true);
        this.promocaoId = promocaoId;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // topo: categoria + busca
        JPanel top = new JPanel(new GridLayout(2, 1, 5, 5));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JPanel cat   = new JPanel(new BorderLayout(5, 5));
        cat.add(new JLabel("Categoria:"), BorderLayout.WEST);
        cat.add(cbCategoria, BorderLayout.CENTER);
        JPanel busca = new JPanel(new BorderLayout(5, 5));
        busca.add(new JLabel("Buscar:"), BorderLayout.WEST);
        busca.add(tfBusca, BorderLayout.CENTER);
        top.add(cat);
        top.add(busca);
        add(top, BorderLayout.NORTH);

        // listas disponíveis e vinculadas
        JPanel centro = new JPanel(new GridLayout(2, 1, 8, 8));
        centro.add(criarPanel("Disponíveis", lstAvailable, "➕ Vincular", this::vincular));
        centro.add(criarPanel("Vinculados",  lstLinked,    "➖ Desvincular", this::desvincular));
        add(centro, BorderLayout.CENTER);

        // listeners
        cbCategoria.addActionListener(e -> carregarDisponiveis());
        tfBusca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { carregarDisponiveis(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { carregarDisponiveis(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { carregarDisponiveis(); }
        });

        carregarCategorias();
        carregarLinks();
    }

    // cria cada painel de lista
    private JPanel criarPanel(String title, JList<String> list, String btnText, Runnable action) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder(title));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        JButton btn = new JButton(btnText);
        btn.addActionListener(e -> action.run());
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    // carrega categorias distintas
    private void carregarCategorias() {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT DISTINCT categoria FROM produtos ORDER BY categoria");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cbCategoria.addItem(rs.getString("categoria"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // carrega produtos disponíveis com filtro de busca
    private void carregarDisponiveis() {
        listAvailable.clear();
        prodIdMap.clear();
        String categoria = (String) cbCategoria.getSelectedItem();
        String termo     = tfBusca.getText().trim().toLowerCase();
        if (categoria == null) return;

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id,nome FROM produtos WHERE categoria=? ORDER BY nome")) {
            ps.setString(1, categoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id   = rs.getString("id");
                    String nome = rs.getString("nome");
                    // só adiciona se não estiver vinculado e bater no termo
                    if (!linkIdMap.containsKey(id) && nome.toLowerCase().contains(termo)) {
                        listAvailable.addElement(nome);
                        prodIdMap.put(nome, id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // carrega lista de vínculos existentes
    private void carregarLinks() {
        listLinked.clear();
        linkIdMap.clear();
        try {
            List<PromocaoProdutoModel> lst = new PromocaoProdutoDAO().listarPorPromocao(promocaoId);
            for (PromocaoProdutoModel m : lst) {
                try (Connection c = DB.get();
                     PreparedStatement ps = c.prepareStatement(
                         "SELECT nome FROM produtos WHERE id=?")) {
                    ps.setString(1, m.getProdutoId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String nome = rs.getString("nome");
                            listLinked.addElement(nome);
                            linkIdMap.put(nome, m.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        carregarDisponiveis();
    }

    // vincula seleção
    private void vincular() {
        for (String nome : lstAvailable.getSelectedValuesList()) {
            String idProd = prodIdMap.get(nome);
            try {
                new PromocaoProdutoDAO().vincularProduto(
                    new PromocaoProdutoModel(UUID.randomUUID().toString(),
                                             promocaoId, idProd)
                );
            } catch (Exception e) { e.printStackTrace(); }
        }
        carregarLinks();
    }

    // desvincula seleção
    private void desvincular() {
        for (String nome : lstLinked.getSelectedValuesList()) {
            String linkId = linkIdMap.get(nome);
            try {
                new PromocaoProdutoDAO().desvincularProduto(linkId);
            } catch (Exception e) { e.printStackTrace(); }
        }
        carregarLinks();
    }
}
