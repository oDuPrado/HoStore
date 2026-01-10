package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import dao.ParcelaContaReceberDAO;
import dao.PagamentoContaReceberDAO;
import model.ParcelaContaReceberModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * @CR Dialog: exibe e permite baixar parcelas de um título.
 */
public class ParcelasContaReceberDialog extends JDialog {

    private final ParcelaContaReceberDAO parcelaDAO = new ParcelaContaReceberDAO();
    @SuppressWarnings("unused")
    private final PagamentoContaReceberDAO pgDAO = new PagamentoContaReceberDAO();

    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd");

    private DefaultTableModel model;
    private JTable tabela;

    private final String tituloId;

    public ParcelasContaReceberDialog(Window owner, String tituloId) {
        super(owner, "Parcelas do Título", ModalityType.APPLICATION_MODAL);
        this.tituloId = tituloId;

        UiKit.applyDialogBase(this);
        setContentPane(buildUI());

        pack();
        setMinimumSize(new Dimension(820, 480));
        setLocationRelativeTo(owner);

        carregarTabela();
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Parcelas do Título"));
        header.add(UiKit.hint("Selecione uma parcela e clique em Baixar. Duplo clique também abre."));
        root.add(header, BorderLayout.NORTH);

        // Card com tabela
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));
        card.add(criarTabela(), BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btFechar = UiKit.ghost("Fechar");
        JButton btBaixar = UiKit.primary("Baixar");

        btFechar.addActionListener(e -> dispose());
        btBaixar.addActionListener(e -> baixarParcela());

        footer.add(btFechar);
        footer.add(btBaixar);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private JScrollPane criarTabela() {
        String[] cols = { "ID", "Parcela", "Vencimento", "Valor", "Pago", "Status" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tabela = new JTable(model);
        UiKit.tableDefaults(tabela);
        tabela.setAutoCreateRowSorter(true);

        esconderColunaID(tabela);
        aplicarRenderers(tabela);

        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2)
                    baixarParcela();
            }
        });

        return UiKit.scroll(tabela);
    }

    private void carregarTabela() {
        model.setRowCount(0);

        try {
            List<ParcelaContaReceberModel> list = parcelaDAO.listarPorTitulo(tituloId);

            for (ParcelaContaReceberModel p : list) {
                String vencVis = "";
                try {
                    vencVis = visFmt.format(isoFmt.parse(p.getVencimento()));
                } catch (Exception ignored) {
                }

                model.addRow(new Object[] {
                        p.getId(),
                        p.getNumeroParcela(),
                        vencVis,
                        moneyFmt.format(p.getValorNominal()),
                        moneyFmt.format(p.getValorPago()),
                        safe(p.getStatus())
                });
            }

            // Ordena por número da parcela
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) tabela.getRowSorter();
            sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void baixarParcela() {
        int viewRow = tabela.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma parcela");
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(viewRow);
        Object idObj = model.getValueAt(modelRow, 0);

        int parcelaId;
        if (idObj instanceof Integer i) {
            parcelaId = i;
        } else {
            parcelaId = Integer.parseInt(String.valueOf(idObj));
        }

        new PagamentoReceberDialog(this, parcelaId).setVisible(true);
        carregarTabela();
    }

    /* ─────────────────────────── Helpers ─────────────────────────── */

    private void esconderColunaID(JTable t) {
        TableColumn col = t.getColumnModel().getColumn(0);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }

    private void aplicarRenderers(JTable t) {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();

        // Zebra em tudo
        for (int c = 0; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(zebra);
        }

        // Alinhamentos
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        // Parcela e vencimento central
        t.getColumnModel().getColumn(1).setCellRenderer(new DelegatingRenderer(zebra, center));
        t.getColumnModel().getColumn(2).setCellRenderer(new DelegatingRenderer(zebra, center));

        // Valores à direita
        t.getColumnModel().getColumn(3).setCellRenderer(new DelegatingRenderer(zebra, right));
        t.getColumnModel().getColumn(4).setCellRenderer(new DelegatingRenderer(zebra, right));

        // Status como badge
        t.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeReceberRenderer());
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    /*
     * ─────────────────────────── Renderers auxiliares ───────────────────────────
     */

    static class DelegatingRenderer extends DefaultTableCellRenderer {
        private final DefaultTableCellRenderer base;
        private final DefaultTableCellRenderer aligner;

        DelegatingRenderer(DefaultTableCellRenderer base, DefaultTableCellRenderer aligner) {
            this.base = base;
            this.aligner = aligner;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel l) {
                l.setHorizontalAlignment(aligner.getHorizontalAlignment());
            }
            return c;
        }
    }

    static class StatusBadgeReceberRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String st = (value == null) ? "" : value.toString().toLowerCase(Locale.ROOT).trim();
            l.setText(" " + st + " ");
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
            l.setBorder(new EmptyBorder(4, 10, 4, 10));

            if (isSelected) {
                l.setOpaque(true);
                l.setBackground(table.getSelectionBackground());
                l.setForeground(table.getSelectionForeground());
                return l;
            }

            boolean dark = Boolean.TRUE.equals(UIManager.get("laf.dark"));
            Color fg = UIManager.getColor("Label.foreground");
            if (fg == null)
                fg = dark ? new Color(0xE6E8EB) : new Color(0x111827);

            Color bg;
            switch (st) {
                case "aberto", "pendente" -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
                case "quitado", "pago" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
                case "vencido" -> bg = dark ? new Color(0x4A1D1D) : new Color(0xFEE2E2);
                case "cancelado" -> bg = dark ? new Color(0x3A2A2A) : new Color(0xE5E7EB);
                default -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
            }

            l.setOpaque(true);
            l.setBackground(bg);
            l.setForeground(fg);

            Color border = UIManager.getColor("Component.borderColor");
            if (border == null)
                border = dark ? new Color(0x313844) : new Color(0xE5E7EB);

            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    new EmptyBorder(4, 10, 4, 10)));

            return l;
        }
    }
}
