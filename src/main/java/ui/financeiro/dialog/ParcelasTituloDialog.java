package ui.financeiro.dialog;

import dao.ParcelaContaPagarDAO;
import model.ParcelaContaPagarModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Dialog – exibe parcelas de um título a pagar e permite registrar pagamento.
 * Inclui coluna "Em Aberto" (nominal + juros + acréscimo - desconto - pago).
 */
public class ParcelasTituloDialog extends JDialog {

    private final String tituloId;

    private final ParcelaContaPagarDAO parcelaDAO = new ParcelaContaPagarDAO();
    private List<ParcelaContaPagarModel> parcelas;

    private final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "ID", "Parcela", "Vencimento", "Nominal", "Juros", "Acréscimo", "Desconto", "Pago",
                    "Em Aberto", "Status" },
            0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };

    private JTable table;

    public ParcelasTituloDialog(Window owner, String tituloId) {
        super(owner, "Parcelas do Título", ModalityType.APPLICATION_MODAL);
        this.tituloId = tituloId;

        UiKit.applyDialogBase(this);
        setContentPane(buildUI());

        pack();
        setMinimumSize(new Dimension(1040, 520));
        setLocationRelativeTo(owner);

        loadParcelas();
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Parcelas do Título"));
        header.add(UiKit.hint("Duplo clique ou botão “Registrar Pagamento” para baixar a parcela."));
        root.add(header, BorderLayout.NORTH);

        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        table = new JTable(tableModel);
        UiKit.tableDefaults(table);
        table.setAutoCreateRowSorter(true);

        esconderColunaID(table);
        aplicarRenderers(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    registrarPagamento();
                }
            }
        });

        card.add(UiKit.scroll(table), BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton btnFechar = UiKit.ghost("Fechar");
        JButton btnRegistrar = UiKit.primary("Registrar Pagamento");

        btnFechar.addActionListener(e -> dispose());
        btnRegistrar.addActionListener(e -> registrarPagamento());

        buttons.add(btnFechar);
        buttons.add(btnRegistrar);

        root.add(buttons, BorderLayout.SOUTH);

        return root;
    }

    private void loadParcelas() {
        tableModel.setRowCount(0);

        try {
            parcelas = parcelaDAO.listarPorTitulo(tituloId);

            for (ParcelaContaPagarModel p : parcelas) {
                String dtVenc = "";
                try {
                    if (p.getVencimento() != null) {
                        dtVenc = visFmt.format(isoFmt.parse(p.getVencimento()));
                    }
                } catch (Exception ignored) {
                }

                double nominal = nz(p.getValorNominal());
                double juros = nz(p.getValorJuros());
                double acres = nz(p.getValorAcrescimo());
                double desc = nz(p.getValorDesconto());
                double pago = nz(p.getValorPago());

                double aberto = nominal + juros + acres - desc - pago;
                if (aberto < 0)
                    aberto = 0; // clamp

                tableModel.addRow(new Object[] {
                        p.getId(),
                        p.getNumeroParcela(),
                        dtVenc,
                        moneyFmt.format(nominal),
                        moneyFmt.format(juros),
                        moneyFmt.format(acres),
                        moneyFmt.format(desc),
                        moneyFmt.format(pago),
                        moneyFmt.format(aberto),
                        safe(p.getStatus())
                });
            }

            // Ordena por número da parcela
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao carregar parcelas:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarPagamento() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma parcela.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        Object idObj = tableModel.getValueAt(modelRow, 0);
        String id = (idObj == null) ? null : String.valueOf(idObj);

        if (id == null || id.isBlank()) {
            JOptionPane.showMessageDialog(this, "Parcela inválida (ID vazio).");
            return;
        }

        ParcelaContaPagarModel parcela = findParcelaById(id);
        if (parcela == null) {
            JOptionPane.showMessageDialog(this, "Não foi possível localizar a parcela selecionada.");
            return;
        }

        new PagamentoContaPagarDialog(this, parcela).setVisible(true);
        loadParcelas();
    }

    private ParcelaContaPagarModel findParcelaById(String id) {
        if (parcelas == null)
            return null;
        for (ParcelaContaPagarModel p : parcelas) {
            if (id.equals(String.valueOf(p.getId())))
                return p;
        }
        return null;
    }

    /* ─────────────────────────── UI helpers ─────────────────────────── */

    private void esconderColunaID(JTable t) {
        TableColumn col = t.getColumnModel().getColumn(0);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }

    private void aplicarRenderers(JTable t) {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();

        for (int c = 0; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(zebra);
        }

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        // Parcela e vencimento centralizados
        t.getColumnModel().getColumn(1).setCellRenderer(new DelegatingRenderer(zebra, center));
        t.getColumnModel().getColumn(2).setCellRenderer(new DelegatingRenderer(zebra, center));

        // Valores à direita: nominal, juros, acréscimo, desconto, pago, em aberto
        for (int col : new int[] { 3, 4, 5, 6, 7, 8 }) {
            t.getColumnModel().getColumn(col).setCellRenderer(new DelegatingRenderer(zebra, right));
        }

        // Status badge
        t.getColumnModel().getColumn(9).setCellRenderer(new StatusBadgePagarRenderer());
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    /* ─────────────────────────── Renderers ─────────────────────────── */

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

    static class StatusBadgePagarRenderer extends DefaultTableCellRenderer {
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
                case "pago", "quitado" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
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
