package ui.rh.painel;

import dao.RhFeriasDAO;
import dao.RhFuncionarioDAO;
import model.RhFeriasModel;
import model.RhFuncionarioModel;
import ui.rh.dialog.RhFeriasDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RhFeriasPanel extends JPanel {

    private final RhFeriasDAO dao = new RhFeriasDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();
    private final DefaultTableModel model;
    private final JTable table;

    public RhFeriasPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Ferias e Abonos"));

        JButton btnAdd = UiKit.primary("Adicionar");
        JButton btnEdit = UiKit.ghost("Editar");
        JButton btnDel = UiKit.ghost("Excluir");
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnDel);

        model = new DefaultTableModel(new Object[]{"ID", "Funcionario", "Inicio", "Fim", "Abono", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        add(top, BorderLayout.NORTH);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhFeriasDialog((Frame) w, null).setVisible(true);
            carregar();
        });
        btnEdit.addActionListener(e -> editar());
        btnDel.addActionListener(e -> excluir());

        carregar();
    }

    private void carregar() {
        model.setRowCount(0);
        try {
            List<RhFuncionarioModel> funcs = funcDAO.listar(false);
            Map<String, String> nomes = new HashMap<>();
            for (RhFuncionarioModel f : funcs) nomes.put(f.getId(), f.getNome());

            List<RhFeriasModel> lista = dao.listar(null);
            for (RhFeriasModel f : lista) {
                model.addRow(new Object[]{f.getId(), nomes.getOrDefault(f.getFuncionarioId(), f.getFuncionarioId()),
                        formatDateBr(f.getDataInicio()), formatDateBr(f.getDataFim()), f.getAbono(), f.getStatus()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar ferias: " + ex.getMessage());
        }
    }

    private void editar() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        try {
            RhFeriasModel fer = null;
            for (RhFeriasModel m : dao.listar(null)) {
                if (m.getId() == id) { fer = m; break; }
            }
            if (fer != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new RhFeriasDialog((Frame) w, fer).setVisible(true);
                carregar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao editar: " + ex.getMessage());
        }
    }

    private void excluir() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        int ok = JOptionPane.showConfirmDialog(this, "Excluir ferias?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.excluir(id);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }

    private void aplicarRenderers() {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(4).setCellRenderer(moneyRenderer(zebra));
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

    private String formatDateBr(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            String s = iso.trim();
            if (s.length() >= 10) s = s.substring(0, 10);
            LocalDate d = LocalDate.parse(s);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }
}
