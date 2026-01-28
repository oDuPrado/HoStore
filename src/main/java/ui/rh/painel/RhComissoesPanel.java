package ui.rh.painel;

import dao.RhComissaoDAO;
import dao.RhFuncionarioDAO;
import model.RhComissaoModel;
import model.RhFuncionarioModel;
import service.RhService;
import ui.rh.dialog.RhComissaoDialog;
import ui.rh.dialog.RhComissaoConfigDialog;
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

public class RhComissoesPanel extends JPanel {

    private final RhComissaoDAO dao = new RhComissaoDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();
    private final RhService service = new RhService();
    private final DefaultTableModel model;
    private final JTable table;

    public RhComissoesPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Comissoes"));

        JButton btnAdd = UiKit.primary("Adicionar manual");
        JButton btnConfig = UiKit.ghost("Comissao por vendedor");
        JButton btnGerar = UiKit.ghost("Gerar do periodo");
        JButton btnDel = UiKit.ghost("Excluir");
        top.add(btnAdd);
        top.add(btnConfig);
        top.add(btnGerar);
        top.add(btnDel);

        model = new DefaultTableModel(new Object[]{"ID", "Funcionario", "Venda", "%", "Valor", "Data"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        add(top, BorderLayout.NORTH);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhComissaoDialog((Frame) w, null).setVisible(true);
            carregar();
        });
        btnConfig.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhComissaoConfigDialog((Frame) w).setVisible(true);
            carregar();
        });
        btnGerar.addActionListener(e -> gerarPeriodo());
        btnDel.addActionListener(e -> excluir());

        carregar();
    }

    private void carregar() {
        model.setRowCount(0);
        try {
            List<RhFuncionarioModel> funcs = funcDAO.listar(false);
            Map<String, String> nomes = new HashMap<>();
            for (RhFuncionarioModel f : funcs) nomes.put(f.getId(), f.getNome());

            List<RhComissaoModel> lista = dao.listar(null, null);
            for (RhComissaoModel c : lista) {
                model.addRow(new Object[]{c.getId(), nomes.getOrDefault(c.getFuncionarioId(), c.getFuncionarioId()), c.getVendaId(), c.getPercentual(), c.getValor(), c.getData()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar comissoes: " + ex.getMessage());
        }
    }

    private void gerarPeriodo() {
        JFormattedTextField tfIni = FormatterFactory.getFormattedDateField();
        JFormattedTextField tfFim = FormatterFactory.getFormattedDateField();
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Data inicio:"));
        p.add(tfIni);
        p.add(new JLabel("Data fim:"));
        p.add(tfFim);
        int ok = JOptionPane.showConfirmDialog(this, p, "Gerar comissoes", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        String ini = toIso(tfIni.getText());
        String fim = toIso(tfFim.getText());
        if (ini == null || fim == null) return;
        try {
            int n = service.gerarComissoesPeriodo(ini, fim);
            JOptionPane.showMessageDialog(this, "Comissoes geradas: " + n);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar comissoes: " + ex.getMessage());
        }
    }

    private void excluir() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        int ok = JOptionPane.showConfirmDialog(this, "Excluir comissao?", "Confirmar", JOptionPane.YES_NO_OPTION);
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
        table.getColumnModel().getColumn(3).setCellRenderer(percentRenderer(zebra));
        table.getColumnModel().getColumn(4).setCellRenderer(moneyRenderer(zebra));
        table.getColumnModel().getColumn(5).setCellRenderer(dateRenderer(zebra));
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

    private DefaultTableCellRenderer percentRenderer(DefaultTableCellRenderer zebraBase) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double v = (value instanceof Number n) ? n.doubleValue() : 0.0;
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setText(nf.format(v) + "%");
                return l;
            }
        };
    }

    private DefaultTableCellRenderer dateRenderer(DefaultTableCellRenderer zebraBase) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setText(toBr(value != null ? value.toString() : ""));
                return l;
            }
        };
    }

    private String toIso(String br) {
        if (br == null) return null;
        String s = br.trim();
        if (s.isEmpty() || s.contains("_")) return null;
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return java.time.LocalDate.parse(s, f).toString();
        } catch (Exception e) {
            return s;
        }
    }

    private String toBr(String iso) {
        if (iso == null || iso.isBlank())
            return "";
        String s = iso.trim();
        if (s.length() >= 10) s = s.substring(0, 10);
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(s);
            return d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return s;
        }
    }
}
