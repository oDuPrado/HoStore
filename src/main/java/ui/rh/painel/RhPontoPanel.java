package ui.rh.painel;

import dao.RhFuncionarioDAO;
import dao.RhPontoDAO;
import model.RhFuncionarioModel;
import model.RhPontoModel;
import ui.rh.dialog.RhPontoDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RhPontoPanel extends JPanel {

    private final RhPontoDAO dao = new RhPontoDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();
    private final DefaultTableModel model;
    private final JTable table;

    public RhPontoPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Controle de Ponto"));

        JButton btnAdd = UiKit.primary("Registrar");
        JButton btnEdit = UiKit.ghost("Editar");
        JButton btnDel = UiKit.ghost("Excluir");
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnDel);

        model = new DefaultTableModel(new Object[]{"ID", "Funcionario", "Data", "Entrada", "Saida", "Horas"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        add(top, BorderLayout.NORTH);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhPontoDialog((Frame) w, null).setVisible(true);
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

            List<RhPontoModel> lista = dao.listar(null, null, null);
            for (RhPontoModel p : lista) {
                model.addRow(new Object[]{p.getId(), nomes.getOrDefault(p.getFuncionarioId(), p.getFuncionarioId()), formatDateBr(p.getData()), p.getEntrada(), p.getSaida(), p.getHorasTrabalhadas()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar ponto: " + ex.getMessage());
        }
    }

    private void editar() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        try {
            RhPontoModel p = null;
            for (RhPontoModel m : dao.listar(null, null, null)) {
                if (m.getId() == id) { p = m; break; }
            }
            if (p != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new RhPontoDialog((Frame) w, p).setVisible(true);
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
        int ok = JOptionPane.showConfirmDialog(this, "Excluir registro de ponto?", "Confirmar", JOptionPane.YES_NO_OPTION);
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
