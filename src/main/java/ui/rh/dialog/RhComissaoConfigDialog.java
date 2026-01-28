package ui.rh.dialog;

import dao.RhFuncionarioDAO;
import model.RhFuncionarioModel;
import util.FormatterFactory;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RhComissaoConfigDialog extends JDialog {

    private final RhFuncionarioDAO dao = new RhFuncionarioDAO();
    private final DefaultTableModel model;
    private final JTable table;
    private final Map<String, RhFuncionarioModel> cache = new HashMap<>();

    public RhComissaoConfigDialog(Frame owner) {
        super(owner, "Comissao por funcionario", true);
        UiKit.applyDialogBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Comissao por funcionario"));
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Funcionario", "Comissao %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(2).setCellRenderer(percentRenderer(zebra));
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(FormatterFactory.getFormattedDoubleField(0.0)));

        add(UiKit.scroll(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnSalvar = UiKit.primary("Salvar");
        JButton btnCancelar = UiKit.ghost("Cancelar");
        actions.add(btnCancelar);
        actions.add(btnSalvar);
        add(actions, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        carregar();
        setSize(620, 420);
        setLocationRelativeTo(owner);
    }

    private void carregar() {
        model.setRowCount(0);
        cache.clear();
        try {
            List<RhFuncionarioModel> lista = dao.listar(false);
            for (RhFuncionarioModel f : lista) {
                cache.put(f.getId(), f);
                model.addRow(new Object[]{f.getId(), f.getNome(), f.getComissaoPct()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar comissoes: " + ex.getMessage());
        }
    }

    private void salvar() {
        try {
            for (int i = 0; i < model.getRowCount(); i++) {
                String id = model.getValueAt(i, 0).toString();
                double pct = parseDouble(model.getValueAt(i, 2));
                RhFuncionarioModel f = cache.get(id);
                if (f != null) {
                    f.setComissaoPct(pct);
                    dao.atualizar(f);
                }
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar comissoes: " + ex.getMessage());
        }
    }

    private static double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        String s = value.toString().trim().replace("%", "").replace(" ", "");
        if (s.contains(",") && s.contains(".")) {
            s = s.replace(".", "").replace(",", ".");
        } else {
            s = s.replace(",", ".");
        }
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private static DefaultTableCellRenderer percentRenderer(DefaultTableCellRenderer zebraBase) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double v = (value instanceof Number n) ? n.doubleValue() : parseDouble(value);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setText(nf.format(v) + "%");
                return l;
            }
        };
    }
}
