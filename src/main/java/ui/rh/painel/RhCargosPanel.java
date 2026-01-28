package ui.rh.painel;

import dao.RhCargoDAO;
import model.RhCargoModel;
import ui.rh.dialog.RhCargoDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class RhCargosPanel extends JPanel {

    private final RhCargoDAO dao = new RhCargoDAO();
    private final DefaultTableModel model;
    private final JTable table;

    public RhCargosPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Cargos"));

        JButton btnAdd = UiKit.primary("Adicionar");
        JButton btnEdit = UiKit.ghost("Editar");
        JButton btnDel = UiKit.ghost("Excluir");
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnDel);

        model = new DefaultTableModel(new Object[]{"ID", "Nome", "Salario Base", "Ativo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        add(top, BorderLayout.NORTH);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhCargoDialog((Frame) w, null).setVisible(true);
            carregar();
        });
        btnEdit.addActionListener(e -> editarSelecionado());
        btnDel.addActionListener(e -> excluirSelecionado());

        carregar();
    }

    private void carregar() {
        model.setRowCount(0);
        try {
            List<RhCargoModel> lista = dao.listar();
            for (RhCargoModel c : lista) {
                model.addRow(new Object[]{c.getId(), c.getNome(), c.getSalarioBase(), c.getAtivo()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar cargos: " + ex.getMessage());
        }
    }

    private void aplicarRenderers() {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(2).setCellRenderer(moneyRenderer(zebra));
    }

    private DefaultTableCellRenderer moneyRenderer(DefaultTableCellRenderer zebraBase) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double v = (value instanceof Number n) ? n.doubleValue() : 0.0;
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setText(nf.format(v));
                return l;
            }
        };
    }

    private void editarSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = model.getValueAt(row, 0).toString();
        try {
            RhCargoModel c = dao.buscarPorId(id);
            if (c != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new RhCargoDialog((Frame) w, c).setVisible(true);
                carregar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao editar: " + ex.getMessage());
        }
    }

    private void excluirSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = model.getValueAt(row, 0).toString();
        int ok = JOptionPane.showConfirmDialog(this, "Excluir cargo selecionado?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.excluir(id);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }
}
