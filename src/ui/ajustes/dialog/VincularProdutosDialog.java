package ui.ajustes.dialog;

import dao.PromocaoProdutoDAO;
import model.PromocaoProdutoModel;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class VincularProdutosDialog extends JDialog {

    private final String promocaoId;
    private final JComboBox<String> cbCategoria = new JComboBox<>();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> listProdutos = new JList<>(listModel);
    private final Map<String, String> produtoIdMap = new HashMap<>();

    public VincularProdutosDialog(String promocaoId) {
        super((Frame) null, "Vincular Produtos à Promoção", true);
        this.promocaoId = promocaoId;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // topo: categoria
        JPanel painelTop = new JPanel(new BorderLayout(5, 5));
        painelTop.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        painelTop.add(new JLabel("Categoria de Produto:"), BorderLayout.NORTH);
        painelTop.add(cbCategoria, BorderLayout.CENTER);
        add(painelTop, BorderLayout.NORTH);

        // lista de produtos
        JPanel painelLista = new JPanel(new BorderLayout(5, 5));
        painelLista.setBorder(BorderFactory.createTitledBorder("Produtos Disponíveis"));
        listProdutos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        painelLista.add(new JScrollPane(listProdutos), BorderLayout.CENTER);
        add(painelLista, BorderLayout.CENTER);

        // botão vincular
        JButton btnVincular = new JButton("✅ Vincular Selecionados");
        btnVincular.addActionListener(e -> vincularSelecionados());
        add(btnVincular, BorderLayout.SOUTH);

        // carregar categorias e produtos
        carregarCategorias();
        cbCategoria.addActionListener(e -> carregarProdutos());
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
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar categorias:\n" + e.getMessage());
        }
    }

    private void carregarProdutos() {
        listModel.clear();
        produtoIdMap.clear();
        String categoria = (String) cbCategoria.getSelectedItem();
        if (categoria == null) return;

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id, nome FROM produtos WHERE categoria = ? ORDER BY nome")) {
            ps.setString(1, categoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String nome = rs.getString("nome");
                    listModel.addElement(nome);
                    produtoIdMap.put(nome, id);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar produtos:\n" + e.getMessage());
        }
    }

    private void vincularSelecionados() {
        List<String> selecionados = listProdutos.getSelectedValuesList();
        if (selecionados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Selecione pelo menos um produto.");
            return;
        }

        try {
            PromocaoProdutoDAO dao = new PromocaoProdutoDAO();
            // pega os já vinculados
            List<PromocaoProdutoModel> existentes = dao.listarPorPromocao(promocaoId);
            Set<String> idsExistentes = existentes.stream()
                .map(PromocaoProdutoModel::getProdutoId)
                .collect(Collectors.toSet());

            List<String> vinculadosAgora = new ArrayList<>();
            for (String nome : selecionados) {
                String pid = produtoIdMap.get(nome);
                if (!idsExistentes.contains(pid)) {
                    dao.vincularProduto(new PromocaoProdutoModel(
                        UUID.randomUUID().toString(),
                        promocaoId,
                        pid
                    ));
                    vinculadosAgora.add(nome);
                }
            }

            if (vinculadosAgora.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Nenhum produto novo para vincular (já existem todos).");
            } else {
                JOptionPane.showMessageDialog(this,
                    vinculadosAgora.size() + " produto(s) vinculados com sucesso!");
            }
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erro ao vincular:\n" + e.getMessage());
        }
    }
}
