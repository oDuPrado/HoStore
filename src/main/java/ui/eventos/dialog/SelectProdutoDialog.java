package ui.eventos.dialog;

import dao.ProdutoDAO;
import model.ProdutoModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;

public class SelectProdutoDialog extends JDialog {

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Nome", "Tipo", "Estoque", "Preco" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private ProdutoModel selecionado;

    public SelectProdutoDialog(Window owner, String tipo) {
        super(owner, "Selecionar Produto", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);
        setSize(820, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        add(buildTopCard(tipo), BorderLayout.NORTH);
        add(buildCenterCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        configurarTabela();
        carregar(tipo);
    }

    public ProdutoModel getSelecionado() {
        return selecionado;
    }

    private JPanel buildTopCard(String tipo) {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Selecionar Produto"));
        left.add(UiKit.hint("Filtro: " + tipo));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JPanel buildCenterCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));
        card.add(UiKit.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomCard() {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnSelecionar = UiKit.primary("Selecionar");
        btnSelecionar.addActionListener(e -> selecionar());

        right.add(btnCancelar);
        right.add(btnSelecionar);
        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void configurarTabela() {
        UiKit.tableDefaults(table);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);
        table.getColumnModel().getColumn(4).setCellRenderer(right);

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);
    }

    private void carregar(String tipo) {
        try {
            model.setRowCount(0);
            List<ProdutoModel> produtos = new ProdutoDAO().listAll(true);
            NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

            for (ProdutoModel p : produtos) {
                if (p.getQuantidade() <= 0) {
                    continue;
                }
                if ("BOOSTER".equalsIgnoreCase(tipo) && !"Booster".equalsIgnoreCase(p.getTipo())) {
                    continue;
                }
                if ("PRODUTO".equalsIgnoreCase(tipo) && "SERVICO".equalsIgnoreCase(p.getTipo())) {
                    continue;
                }
                model.addRow(new Object[] {
                        p.getId(),
                        p.getNome(),
                        p.getTipo(),
                        p.getQuantidade(),
                        moeda.format(p.getPrecoVenda())
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selecionar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(modelRow, 0);
        selecionado = new ProdutoDAO().findById(id, true);
        dispose();
    }
}
