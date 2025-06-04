package ui.ajustes.dialog;

import dao.PromocaoProdutoDAO;
import model.PromocaoProdutoModel;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class VerProdutosVinculadosDialog extends JDialog {

    private final String promocaoId;
    private final JComboBox<String> cbCategoria = new JComboBox<>();
    private final JTextField tfBusca = new JTextField();
    private final DefaultListModel<String> listAvailable = new DefaultListModel<>();
    private final JList<String> lstAvailable = new JList<>(listAvailable);
    private final DefaultListModel<String> listLinked = new DefaultListModel<>();
    private final JList<String> lstLinked = new JList<>(listLinked);

    private final Map<String, String> prodIdMap = new HashMap<>();
    private final Map<String, String> linkIdMap = new HashMap<>();

    public VerProdutosVinculadosDialog(String promocaoId) {
        super((Frame) null, "Produtos da Promoção", true);
        this.promocaoId = promocaoId;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // === TOPO (categoria + busca)
        JPanel top = new JPanel(new GridLayout(2, 1, 5, 5));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JPanel cat = new JPanel(new BorderLayout(5, 5));
        cat.add(new JLabel("Categoria:"), BorderLayout.WEST);
        cat.add(cbCategoria, BorderLayout.CENTER);
        JPanel busca = new JPanel(new BorderLayout(5, 5));
        busca.add(new JLabel("Buscar:"), BorderLayout.WEST);
        busca.add(tfBusca, BorderLayout.CENTER);
        top.add(cat);
        top.add(busca);
        add(top, BorderLayout.NORTH);

        // === LISTAS
        JPanel centro = new JPanel(new GridLayout(2, 1, 8, 8));

        // Disponíveis
        JPanel pa = new JPanel(new BorderLayout(4, 4));
        pa.setBorder(BorderFactory.createTitledBorder("Disponíveis"));
        lstAvailable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pa.add(new JScrollPane(lstAvailable), BorderLayout.CENTER);
        JButton btnLink = new JButton("➕ Vincular");
        btnLink.addActionListener(e -> vincular());
        pa.add(btnLink, BorderLayout.SOUTH);
        centro.add(pa);

        // Vinculados
        JPanel pl = new JPanel(new BorderLayout(4, 4));
        pl.setBorder(BorderFactory.createTitledBorder("Vinculados"));
        lstLinked.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pl.add(new JScrollPane(lstLinked), BorderLayout.CENTER);
        JButton btnUnlink = new JButton("➖ Desvincular");
        btnUnlink.addActionListener(e -> desvincular());
        pl.add(btnUnlink, BorderLayout.SOUTH);
        centro.add(pl);

        add(centro, BorderLayout.CENTER);

        // Eventos
        cbCategoria.addActionListener(e -> carregarDisponiveis());
        tfBusca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { carregarDisponiveis(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { carregarDisponiveis(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { carregarDisponiveis(); }
        });

        // Carregar tudo
        carregarCategorias();
        carregarLinks();
    }

    private void carregarCategorias() {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT DISTINCT categoria FROM produtos ORDER BY categoria");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cbCategoria.addItem(rs.getString("categoria"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarDisponiveis() {
        listAvailable.clear();
        prodIdMap.clear();
        String categoria = (String) cbCategoria.getSelectedItem();
        String termoBusca = tfBusca.getText().trim().toLowerCase();

        if (categoria == null) return;

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id,nome FROM produtos WHERE categoria=? ORDER BY nome")) {
            ps.setString(1, categoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String nome = rs.getString("nome");
                    if (!linkIdMap.containsKey(id) && nome.toLowerCase().contains(termoBusca)) {
                        listAvailable.addElement(nome);
                        prodIdMap.put(nome, id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarLinks() {
        listLinked.clear();
        linkIdMap.clear();
        try {
            List<PromocaoProdutoModel> lst = new PromocaoProdutoDAO().listarPorPromocao(promocaoId);
            try (Connection c = DB.get()) {
                for (PromocaoProdutoModel m : lst) {
                    try (PreparedStatement ps = c.prepareStatement(
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        carregarDisponiveis();
    }

    private void vincular() {
        List<String> selecionados = lstAvailable.getSelectedValuesList();
        if (selecionados.isEmpty()) return;
        PromocaoProdutoDAO dao = new PromocaoProdutoDAO();
        for (String nome : selecionados) {
            String id = prodIdMap.get(nome);
            try {
                dao.vincularProduto(new PromocaoProdutoModel(
                        UUID.randomUUID().toString(),
                        promocaoId,
                        id
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        carregarLinks();
    }

    private void desvincular() {
        List<String> selecionados = lstLinked.getSelectedValuesList();
        if (selecionados.isEmpty()) return;
        PromocaoProdutoDAO dao = new PromocaoProdutoDAO();
        for (String nome : selecionados) {
            String id = linkIdMap.get(nome);
            try {
                dao.desvincularProduto(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        carregarLinks();
    }
}
