// src/ui/estoque/dialog/ProdutosDoPedidoDialog.java
package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import javax.swing.border.*;
import dao.PedidoEstoqueProdutoDAO;
import dao.FornecedorDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import model.PedidoCompraModel;
import model.FornecedorModel;
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
    private final java.util.List<PedidoEstoqueProdutoModel> itens = new java.util.ArrayList<>();

    private JTable table;
    private DefaultTableModel tm;

    public ProdutosDoPedidoDialog(Frame parent, PedidoCompraModel pedido) {
        super(parent, "📦 Itens do Pedido", true);
        UiKit.applyDialogBase(this);
        this.pedido = pedido;
        initComponents();
        loadData();
        setSize(920, 560);
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

        String fornecedorNome = "-";
        try {
            if (pedido.getFornecedorId() != null) {
                FornecedorModel forn = new FornecedorDAO().buscarPorId(pedido.getFornecedorId());
                if (forn != null && forn.getNome() != null && !forn.getNome().isBlank()) {
                    fornecedorNome = forn.getNome();
                }
            }
        } catch (Exception ignore) {
        }

        JPanel left = new JPanel(new GridLayout(3, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Itens do Pedido • " + pedido.getNome()));
        left.add(UiKit.hint("Data: " + dataVis + " - Status: " + pedido.getStatus()));
        left.add(UiKit.hint("Pedido ID: " + pedido.getId() + " - Fornecedor: " + fornecedorNome));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        tm = new DefaultTableModel(new Object[] {
                "Item ID", "Produto", "Qtd Pedida", "Qtd Recebida", "Fornecedor", "Custo", "Preco", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (col != 2)
                    return false;
                if (row < 0 || row >= itens.size())
                    return false;
                return itens.get(row).getQuantidadeRecebida() == 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 2, 3 -> Integer.class;
                    case 5, 6 -> Double.class;
                    default -> String.class;
                };
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
        cm.getColumn(3).setCellRenderer(zebra);
        cm.getColumn(4).setCellRenderer(zebra);
        cm.getColumn(5).setCellRenderer(currencyRenderer(zebra));
        cm.getColumn(6).setCellRenderer(currencyRenderer(zebra));
        cm.getColumn(7).setCellRenderer(UiKit.badgeStatusRenderer());

        cm.getColumn(2).setMaxWidth(120);
        cm.getColumn(3).setMaxWidth(120);
        cm.getColumn(7).setMaxWidth(160);

        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));
        JScrollPane scroll = UiKit.scroll(table);
        scroll.setBorder(new LineBorder(new Color(0xEEF0F3), 1, true));
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());
        footer.add(UiKit.hint("Itens recebidos ficam bloqueados. Edite apenas a quantidade pedida."),
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

    private DefaultTableCellRenderer currencyRenderer(DefaultTableCellRenderer base) {
        java.text.NumberFormat cf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"));
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setText(cf.format(v));
                return l;
            }
        };
    }

    private void loadData() {
        try {
            tm.setRowCount(0);
            itens.clear();
            var lista = pedProdDAO.listarPorPedido(pedido.getId());
            ProdutoDAO prodDAO = new ProdutoDAO();
            FornecedorDAO fornecedorDAO = new FornecedorDAO();

            for (PedidoEstoqueProdutoModel it : lista) {
                String nomeProduto = it.getProdutoId();
                try {
                    ProdutoModel prod = prodDAO.findById(it.getProdutoId());
                    if (prod != null)
                        nomeProduto = prod.getNome();
                } catch (Exception ignore) {
                }

                String fornecedorNome = "-";
                try {
                    if (it.getFornecedorId() != null) {
                        FornecedorModel forn = fornecedorDAO.buscarPorId(it.getFornecedorId());
                        if (forn != null && forn.getNome() != null && !forn.getNome().isBlank()) {
                            fornecedorNome = forn.getNome();
                        }
                    }
                } catch (Exception ignore) {
                }

                itens.add(it);
                tm.addRow(new Object[] {
                        it.getId(),
                        nomeProduto,
                        it.getQuantidadePedida(),
                        it.getQuantidadeRecebida(),
                        fornecedorNome,
                        it.getCustoUnit() != null ? it.getCustoUnit() : 0.0,
                        it.getPrecoVendaUnit() != null ? it.getPrecoVendaUnit() : 0.0,
                        it.getStatus()
                });
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
                int qtdPedida = Integer.parseInt(tm.getValueAt(r, 2).toString());
                if (r >= itens.size())
                    continue;
                PedidoEstoqueProdutoModel item = itens.get(r);

                if (item.getQuantidadeRecebida() > 0 && qtdPedida != item.getQuantidadePedida()) {
                    JOptionPane.showMessageDialog(this,
                            "Nao e permitido alterar quantidade pedida em item ja recebido.",
                            "Bloqueado", JOptionPane.WARNING_MESSAGE);
                    return;
                }

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

            JOptionPane.showMessageDialog(this, "Alteracoes salvas!");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
