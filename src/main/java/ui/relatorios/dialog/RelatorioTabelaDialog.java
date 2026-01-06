package ui.relatorios.dialog;

import util.CsvExportUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

public class RelatorioTabelaDialog extends JDialog {

    private final JTable table;
    private final DefaultTableModel model;

    public RelatorioTabelaDialog(Window owner, String titulo, Object[] colunas) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10,10));
        setSize(900, 520);
        setLocationRelativeTo(owner);

        model = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(22);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btExport = new JButton("Exportar CSV");
        JButton btClose = new JButton("Fechar");

        btExport.addActionListener(e -> exportCsv());
        btClose.addActionListener(e -> dispose());

        top.add(btExport);
        top.add(btClose);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void addRow(Object[] row) {
        model.addRow(row);
    }

    public void setRows(java.util.List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] r : rows) model.addRow(r);
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Salvar CSV");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                CsvExportUtil.exportarTabelaParaCsv(table, f);
                JOptionPane.showMessageDialog(this, "CSV salvo: " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(),
                        "Exportar CSV", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
