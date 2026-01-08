// src/ui/estoque/dialog/EntradaProdutosDialog.java
package ui.estoque.dialog;

import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class EntradaProdutosDialog extends JDialog {

    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    private final DefaultTableModel modelItens;
    private final JTable tabelaItens;
    private final String pedidoId;

    public EntradaProdutosDialog(Frame owner, String pedidoId) {
        super(owner, "📥 Entrada de Produtos", true);
        this.pedidoId = pedidoId;

        UiKit.applyDialogBase(this);
        setLayout(new BorderLayout(12, 12));

        // ===== Header =====
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Entrada • Pedido " + pedidoId));
        left.add(UiKit.hint("Edite apenas 'Qtd Recebida' e clique em Confirmar."));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // ===== Tabela =====
        modelItens = new DefaultTableModel(
                new String[] { "LINK_ID", "PRODUTO_ID", "Produto", "Qtd Pedida", "Qtd Recebida", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 4; // só Qtd Recebida
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3 || columnIndex == 4)
                    return Integer.class;
                return super.getColumnClass(columnIndex);
            }
        };

        tabelaItens = new JTable(modelItens);
        UiKit.tableDefaults(tabelaItens);

        // Renderers
        TableColumnModel cm = tabelaItens.getColumnModel();

        // Zebra só nas colunas visíveis
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(zebra);
        }

        // Status como badge (coluna 5)
        cm.getColumn(5).setCellRenderer(UiKit.badgeStatusRenderer());

        // Editor numérico simples pra "Qtd Recebida"
        JTextField tfNum = new JTextField();
        tfNum.setBorder(new EmptyBorder(0, 8, 0, 8));
        DefaultCellEditor numEditor = new DefaultCellEditor(tfNum) {
            @Override
            public boolean stopCellEditing() {
                String v = (String) getCellEditorValue();
                try {
                    if (v == null || v.isBlank())
                        v = "0";
                    Integer.parseInt(v.trim());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EntradaProdutosDialog.this,
                            "Digite um número inteiro válido para 'Qtd Recebida'.",
                            "Valor inválido", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                return super.stopCellEditing();
            }
        };
        cm.getColumn(4).setCellEditor(numEditor);

        // Ajustes de largura
        cm.getColumn(3).setMaxWidth(110); // Qtd Pedida
        cm.getColumn(4).setMaxWidth(120); // Qtd Recebida
        cm.getColumn(5).setMaxWidth(160); // Status

        // Esconder colunas técnicas
        hideColumn(0); // LINK_ID
        hideColumn(1); // PRODUTO_ID

        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.add(UiKit.scroll(tabelaItens), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // ===== Footer =====
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Isso irá atualizar os itens do pedido e ajustar estoque (como seu código atual faz)."),
                BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btCancelar = UiKit.ghost("Cancelar");
        JButton btSalvar = UiKit.primary("Confirmar Entrada");

        actions.add(btCancelar);
        actions.add(btSalvar);

        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        btCancelar.addActionListener(e -> dispose());
        btSalvar.addActionListener(e -> salvar());

        carregarItens();

        setSize(820, 520);
        setLocationRelativeTo(owner);
    }

    private void hideColumn(int idx) {
        TableColumnModel cm = tabelaItens.getColumnModel();
        cm.getColumn(idx).setMinWidth(0);
        cm.getColumn(idx).setMaxWidth(0);
        cm.getColumn(idx).setPreferredWidth(0);
    }

    private void carregarItens() {
        try {
            List<PedidoEstoqueProdutoModel> itens = itemDAO.listarPorPedido(pedidoId);
            modelItens.setRowCount(0);

            for (PedidoEstoqueProdutoModel it : itens) {
                ProdutoModel p = produtoDAO.findById(it.getProdutoId());

                String nome = (p != null) ? p.getNome() : it.getProdutoId();
                String status = it.getStatus();

                modelItens.addRow(new Object[] {
                        it.getId(), // LINK_ID (hidden)
                        it.getProdutoId(), // PRODUTO_ID (hidden)
                        nome, // Produto
                        it.getQuantidadePedida(),
                        it.getQuantidadeRecebida(),
                        status
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar itens:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void salvar() {
        try {
            if (tabelaItens.isEditing()) {
                tabelaItens.getCellEditor().stopCellEditing();
            }

            for (int i = 0; i < modelItens.getRowCount(); i++) {
                String linkId = modelItens.getValueAt(i, 0).toString();
                String produtoId = modelItens.getValueAt(i, 1).toString();

                int ped = Integer.parseInt(modelItens.getValueAt(i, 3).toString());
                int rec = Integer.parseInt(modelItens.getValueAt(i, 4).toString());

                String status = rec >= ped ? "completo" : (rec > 0 ? "parcial" : "pendente");

                // atualiza link (mesma lógica, só com IDs corretos)
                PedidoEstoqueProdutoModel m = new PedidoEstoqueProdutoModel(
                        linkId, pedidoId, produtoId, ped, rec, status);
                itemDAO.atualizar(m);

                // atualiza estoque (mantive seu comportamento, mas isso aqui é arriscado)
                ProdutoModel p = produtoDAO.findById(produtoId);
                if (p != null) {
                    p.setQuantidade(p.getQuantidade() + rec);
                    produtoDAO.update(p);
                }
            }

            JOptionPane.showMessageDialog(this, "Recebimento registrado!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
