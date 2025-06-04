package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import model.PedidoCompraModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;


/**
 * Permite visualizar e editar as quantidades pedidas de cada item
 * (antes da entrada), mantendo a consistência do pedido.
 */
public class ProdutosDoPedidoDialog extends JDialog {

    private final PedidoCompraModel pedido;
    private final PedidoEstoqueProdutoDAO pedProdDAO = new PedidoEstoqueProdutoDAO();
    private final PedidoCompraDAO pedDAO = new PedidoCompraDAO();

    private JTable table;
    private DefaultTableModel tm;

    public ProdutosDoPedidoDialog(Frame parent, PedidoCompraModel pedido) {
        super(parent, "Itens do Pedido • " + pedido.getNome(), true);
        this.pedido = pedido;
        initComponents();
        loadData();
        pack();
        setLocationRelativeTo(parent);
    }

    /** UI: Item ID (oculto), Produto ID, Qtd Pedida (editável) */
    private void initComponents() {
        tm = new DefaultTableModel(
                new Object[] { "Item ID", "Produto ID", "Qtd Pedida" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };

        table = new JTable(tm);
        table.getColumnModel().getColumn(0).setMinWidth(0); // esconde Item ID
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        JScrollPane scroll = new JScrollPane(table);

        JButton btnSave = new JButton("Salvar Alterações");
        btnSave.addActionListener(e -> onSave());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(btnSave, BorderLayout.SOUTH);
    }

    /** Carrega itens existentes */
    private void loadData() {
        try {
            List<PedidoEstoqueProdutoModel> itens = pedProdDAO.listarPorPedido(pedido.getId());
            ProdutoDAO prodDAO = new ProdutoDAO();

            for (PedidoEstoqueProdutoModel it : itens) {
                String nomeProduto = it.getProdutoId(); // fallback padrão

                try {
                    ProdutoModel prod = prodDAO.findById(it.getProdutoId());
                    if (prod != null) {
                        nomeProduto = prod.getNome();
                    }
                } catch (Exception e) {
                    // se der erro, mantém produtoId
                }

                tm.addRow(new Object[] {
                        it.getId(),
                        nomeProduto,
                        it.getQuantidadePedida()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar itens: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /** Salva novas quantidades pedidas */
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
                // Revalida status do item
                String st = (item.getQuantidadeRecebida() == 0) ? "pendente"
                        : (item.getQuantidadeRecebida() >= qtdPedida) ? "completo" : "parcial";
                item.setStatus(st);
                pedProdDAO.atualizar(item);

                totalPedida += qtdPedida;
                totalRecebida += item.getQuantidadeRecebida();
            }

            // Reavalie status do pedido
            String novoStatus = (totalRecebida == 0) ? "enviado"
                    : (totalRecebida >= totalPedida) ? "recebido" : "parcialmente recebido";
            pedido.setStatus(novoStatus);
            pedDAO.atualizar(pedido);

            JOptionPane.showMessageDialog(this, "Alterações salvas!");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
