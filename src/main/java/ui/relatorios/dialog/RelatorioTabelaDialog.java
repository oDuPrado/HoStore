// src/ui/relatorios/dialog/RelatorioTabelaDialog.java
package ui.relatorios.dialog;

import util.CsvExportUtil;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;

public class RelatorioTabelaDialog extends JDialog {

    private final JTable table;
    private final DefaultTableModel model;

    public RelatorioTabelaDialog(Window owner, String titulo, Object[] colunas) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);

        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(980, 560));

        // Top Card (header + ações)
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(UiKit.title(titulo), BorderLayout.NORTH);
        left.add(UiKit.hint("Tabela gerada pelo relatório. Você pode exportar para CSV."), BorderLayout.SOUTH);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btExport = UiKit.ghost("⬇ Exportar CSV");
        JButton btClose = UiKit.primary("Fechar");

        btExport.addActionListener(e -> exportCsv());
        btClose.addActionListener(e -> dispose());

        right.add(btExport);
        right.add(btClose);

        topCard.add(left, BorderLayout.WEST);
        topCard.add(right, BorderLayout.EAST);

        add(topCard, BorderLayout.NORTH);

        // Center Card (tabela)
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));

        model = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        UiKit.tableDefaults(table);

        // Zebra em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        centerCard.add(UiKit.scroll(table), BorderLayout.CENTER);
        add(centerCard, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }

    public void addRow(Object[] row) {
        model.addRow(row);
    }

    public void setRows(List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] r : rows)
            model.addRow(r);
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
