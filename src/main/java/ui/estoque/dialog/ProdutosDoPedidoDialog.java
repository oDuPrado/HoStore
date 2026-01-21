// src/ui/estoque/dialog/ProdutosDoPedidoDialog.java
package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import javax.swing.border.*;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import model.PedidoCompraModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class ProdutosDoPedidoDialog extends JDialog {

    private final PedidoCompraModel pedido;
    private final PedidoEstoqueProdutoDAO pedProdDAO = new PedidoEstoqueProdutoDAO();
    private final PedidoCompraDAO pedDAO = new PedidoCompraDAO();

    private JTable table;
    private DefaultTableModel tm;

    public ProdutosDoPedidoDialog(Frame parent, PedidoCompraModel pedido) {
        super(parent, "📦 Itens do Pedido", true);
        UiKit.applyDialogBase(this);
        this.pedido = pedido;
        initComponents();
        loadData();
        setSize(760, 520);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));

        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        String dataVis = pedido.getData();
        try {
            dataVis = new SimpleDateFormat("dd/MM/yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(pedido.getData()));
        } catch (Exception ignore) {
        }

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Itens do Pedido • " + pedido.getNome()));
        left.add(UiKit.hint("Data: " + dataVis + " • Status: " + pedido.getStatus()));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        tm = new DefaultTableModel(new Object[] { "Item ID", "Produto", "Qtd Pedida" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };

        table = new JTable(tm);
        UiKit.tableDefaults(table);

        // esconder Item ID
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setMinWidth(0);
        cm.getColumn(0).setMaxWidth(0);
        cm.getColumn(0).setPreferredWidth(0);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        cm.getColumn(1).setCellRenderer(zebra);
        cm.getColumn(2).setCellRenderer(zebra);
        cm.getColumn(2).setMaxWidth(130);

        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));
        JScrollPane scroll = UiKit.scroll(table);
        scroll.setBorder(new LineBorder(new Color(0xEEF0F3), 1, true));
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());
        footer.add(UiKit.hint("Edite apenas a quantidade pedida. O status do pedido será recalculado."),
                BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnCancel = UiKit.ghost("Cancelar");
        JButton btnSave = UiKit.primary("Salvar Alterações");
        actions.add(btnCancel);
        actions.add(btnSave);
        footer.add(actions, BorderLayout.EAST);

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> onSave());

        add(footer, BorderLayout.SOUTH);
    }

    private void loadData() {
        try {
            tm.setRowCount(0);
            var itens = pedProdDAO.listarPorPedido(pedido.getId());
            ProdutoDAO prodDAO = new ProdutoDAO();

            for (PedidoEstoqueProdutoModel it : itens) {
                String nomeProduto = it.getProdutoId();
                try {
                    ProdutoModel prod = prodDAO.findById(it.getProdutoId());
                    if (prod != null)
                        nomeProduto = prod.getNome();
                } catch (Exception ignore) {
                }

                tm.addRow(new Object[] { it.getId(), nomeProduto, it.getQuantidadePedida() });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar itens: " + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void onSave() {
        try {
            int totalPedida = 0;
            int totalRecebida = 0;

            for (int r = 0; r < tm.getRowCount(); r++) {
                String itemId = tm.getValueAt(r, 0).toString();
                int qtdPedida = Integer.parseInt(tm.getValueAt(r, 2).toString());

                PedidoEstoqueProdutoModel item = pedProdDAO.buscarPorId(itemId);
                if (item == null)
                    continue;

                item.setQuantidadePedida(qtdPedida);
                String st = (item.getQuantidadeRecebida() == 0) ? "pendente"
                        : (item.getQuantidadeRecebida() >= qtdPedida) ? "completo" : "parcial";
                item.setStatus(st);
                pedProdDAO.atualizar(item);

                totalPedida += qtdPedida;
                totalRecebida += item.getQuantidadeRecebida();
            }

            String novoStatus = (totalRecebida == 0) ? "enviado"
                    : (totalRecebida >= totalPedida) ? "recebido" : "parcialmente recebido";
            pedido.setStatus(novoStatus);
            pedDAO.atualizar(pedido);

            JOptionPane.showMessageDialog(this, "Alterações salvas!");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
