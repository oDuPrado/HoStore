package ui.venda.dialog;

import model.TaxaCartaoModel;
import service.TaxaCartaoService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Diálogo para configurar parcelamento de uma venda em CARTÃO,
 * unindo seleção de bandeira/taxas cadastradas e preview de parcelas.
 */
public class ParcelamentoDialog extends JDialog {
    /** Retorna config final ao chamar getConfig() após isOk()==true */
    public static class ParcelamentoConfig {
        public int parcelas; // ex: 3
        public double juros; // taxa % do cartão
        public int intervaloDias; // ex: 30
        public String bandeira;
        public String tipo; // CREDITO/DEBITO
    }

    private final ParcelamentoConfig config;
    private final double totalVenda;
    private final TaxaCartaoService taxaService = new TaxaCartaoService();

    private final JComboBox<String> cbBandeira = new JComboBox<>();
    private final JSpinner spParcelas;
    private final JFormattedTextField ftJuros;
    private final JComboBox<String> cbIntervalo;

    private final DefaultTableModel taxasModel;
    private final JTable taxasTable;

    private final DefaultTableModel previewModel;
    private final JTable previewTable;

    private boolean ok = false;

    public ParcelamentoDialog(Window owner, ParcelamentoConfig initialConfig, double totalVenda) {
        super(owner, "Configurar Parcelamento", ModalityType.APPLICATION_MODAL);
        this.config = initialConfig;
        this.totalVenda = totalVenda;

        UiKit.applyDialogBase(this);

        // Spinner parcelas
        int inic = Math.max(1, initialConfig.parcelas);
        spParcelas = new JSpinner(new SpinnerNumberModel(inic, 1, 36, 1));
        spParcelas.setPreferredSize(new Dimension(72, spParcelas.getPreferredSize().height));

        // Juros %
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        NumberFormatter fmtJ = new NumberFormatter(nf);
        fmtJ.setValueClass(Double.class);
        fmtJ.setMinimum(0.0);
        fmtJ.setAllowsInvalid(false);

        ftJuros = new JFormattedTextField(fmtJ);
        ftJuros.setColumns(6);
        ftJuros.setValue(initialConfig.juros);

        // Intervalo
        cbIntervalo = new JComboBox<>(new String[] { "15 dias", "30 dias" });
        // tenta refletir config inicial
        cbIntervalo.setSelectedItem(initialConfig.intervaloDias == 15 ? "15 dias" : "30 dias");

        // Tabela de taxas
        taxasModel = new DefaultTableModel(new String[] { "Tipo", "Faixa", "Taxa" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        taxasTable = new JTable(taxasModel);

        // Preview
        previewModel = new DefaultTableModel(new String[] { "Parcela", "Vencimento", "Valor" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        previewTable = new JTable(previewModel);

        initComponents();
        loadBandeiras();
        refreshTaxasAndPreview();

        setMinimumSize(new Dimension(760, 540));
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        /* ===================== TOP (CARD) ===================== */
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));

        JPanel title = new JPanel(new GridLayout(0, 1, 0, 2));
        title.setOpaque(false);
        title.add(UiKit.title("Parcelamento no cartão"));
        title.add(UiKit.hint("Selecione a bandeira e parcelas. A taxa é puxada da tabela do mês atual."));
        top.add(title, BorderLayout.WEST);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        form.setOpaque(false);

        cbBandeira.setPreferredSize(new Dimension(160, 28));

        form.add(new JLabel("Bandeira:"));
        form.add(cbBandeira);

        form.add(new JLabel("Parcelas:"));
        form.add(spParcelas);

        form.add(new JLabel("Taxa:"));
        form.add(ftJuros);
        form.add(new JLabel("%"));

        form.add(new JLabel("Intervalo:"));
        form.add(cbIntervalo);

        top.add(form, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        /* ===================== CENTER (CARD + TABLES) ===================== */
        JPanel center = UiKit.card();
        center.setLayout(new BorderLayout(10, 10));

        JPanel labels = new JPanel(new GridLayout(1, 2, 10, 0));
        labels.setOpaque(false);
        labels.add(UiKit.title("Taxas disponíveis"));
        labels.add(UiKit.title("Preview parcelas"));
        center.add(labels, BorderLayout.NORTH);

        // defaults
        UiKit.tableDefaults(taxasTable);
        UiKit.tableDefaults(previewTable);

        // zebra
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        applyZebra(taxasTable, zebra);
        applyZebra(previewTable, zebra);

        // renderers específicos
        taxasTable.getColumnModel().getColumn(2).setCellRenderer(percentRendererZebra(zebra));
        previewTable.getColumnModel().getColumn(2).setCellRenderer(currencyRendererZebra(zebra));

        // alinhamentos
        centerAlignZebra(taxasTable, zebra, 1);
        centerAlignZebra(previewTable, zebra, 0);
        centerAlignZebra(previewTable, zebra, 1);

        // largura
        taxasTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        taxasTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        taxasTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        previewTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        JPanel grids = new JPanel(new GridLayout(1, 2, 10, 10));
        grids.setOpaque(false);
        grids.add(UiKit.scroll(taxasTable));
        grids.add(UiKit.scroll(previewTable));

        center.add(grids, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        /* ===================== FOOTER (CARD) ===================== */
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));

        bottom.add(UiKit.hint("OK aplica no finalizador. ESC fecha."), BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JButton btnCancel = UiKit.ghost("Cancelar");
        JButton btnOK = UiKit.primary("OK");

        buttons.add(btnCancel);
        buttons.add(btnOK);
        bottom.add(buttons, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        /* ===================== LISTENERS ===================== */
        cbBandeira.addActionListener(e -> refreshTaxasAndPreview());
        spParcelas.addChangeListener(e -> refreshTaxasAndPreview());
        cbIntervalo.addActionListener(e -> atualizarPreview());
        ftJuros.addPropertyChangeListener("value", evt -> atualizarPreview());

        btnOK.addActionListener(e -> {
            config.parcelas = (Integer) spParcelas.getValue();
            config.juros = safeDouble(ftJuros.getValue());
            config.intervaloDias = Integer.parseInt(((String) cbIntervalo.getSelectedItem()).split(" ")[0]);
            config.bandeira = (String) cbBandeira.getSelectedItem();
            config.tipo = "CREDITO";
            ok = true;
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        // atalhos
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");
        am.put("cancelar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                btnCancel.doClick();
            }
        });
    }

    /** Carrega no combo todas as bandeiras que têm taxa para o mês atual */
    private void loadBandeiras() {
        cbBandeira.removeAllItems();

        YearMonth mesRef = YearMonth.now();
        Set<String> bandeiras = new LinkedHashSet<>();

        try {
            List<TaxaCartaoModel> lista = taxaService.listar();
            for (TaxaCartaoModel t : lista) {
                if (t == null)
                    continue;
                if (t.getBandeira() == null)
                    continue;
                if (t.getMesVigencia() == null)
                    continue;

                if (t.getMesVigencia().equals(mesRef.toString())) {
                    bandeiras.add(t.getBandeira());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (bandeiras.isEmpty()) {
            cbBandeira.addItem("Sem taxas no mês");
            cbBandeira.setEnabled(false);
        } else {
            cbBandeira.setEnabled(true);
            for (String b : bandeiras)
                cbBandeira.addItem(b);
            cbBandeira.setSelectedIndex(0);
        }
    }

    /** Busca e exibe as taxas válidas e atualiza o preview */
    private void refreshTaxasAndPreview() {
        if (!cbBandeira.isEnabled()) {
            taxasModel.setRowCount(0);
            taxasModel.addRow(new Object[] { "—", "—", 0.0 });
            atualizarPreview();
            return;
        }

        String bandeira = (String) cbBandeira.getSelectedItem();
        int parcelas = (Integer) spParcelas.getValue();
        YearMonth mesRef = YearMonth.now();

        // 1) busca taxa específica (CREDITO) para parcelas
        try {
            double taxaPct = taxaService.buscarTaxa(bandeira, "CREDITO", parcelas, mesRef).orElse(0.0);
            ftJuros.setValue(taxaPct);
        } catch (Exception e) {
            ftJuros.setValue(0.0);
        }

        // 2) tabela de taxas válidas no mês pra bandeira
        taxasModel.setRowCount(0);
        try {
            for (TaxaCartaoModel t : taxaService.listar()) {
                if (t == null)
                    continue;
                if (t.getBandeira() == null || t.getMesVigencia() == null)
                    continue;

                if (t.getBandeira().equals(bandeira) && t.getMesVigencia().equals(mesRef.toString())) {
                    String faixa = t.getMinParcelas() + "x–" + t.getMaxParcelas() + "x";
                    taxasModel.addRow(new Object[] { t.getTipo(), faixa, t.getTaxaPct() });
                }
            }
            if (taxasModel.getRowCount() == 0) {
                taxasModel.addRow(new Object[] { "—", "—", 0.0 });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            taxasModel.addRow(new Object[] { "—", "—", 0.0 });
        }

        // 3) preview
        atualizarPreview();
    }

    /** Recalcula e exibe preview */
    private void atualizarPreview() {
        previewModel.setRowCount(0);

        int parcelas = (Integer) spParcelas.getValue();
        if (parcelas <= 0)
            parcelas = 1;

        double jurosPct = safeDouble(ftJuros.getValue());
        int dias = Integer.parseInt(((String) cbIntervalo.getSelectedItem()).split(" ")[0]);

        // fórmula atual: juros por parcela (simples). Mantive pra não mudar tua regra
        // do nada.
        double base = totalVenda / parcelas;
        double valorParc = base;

        LocalDate hoje = LocalDate.now();
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        for (int i = 1; i <= parcelas; i++) {
            LocalDate d = hoje.plusDays((long) dias * i);
            previewModel.addRow(new Object[] {
                    i + "/" + parcelas,
                    dtFmt.format(d),
                    valorParc
            });
        }
    }

    private static void applyZebra(JTable t, DefaultTableCellRenderer zebra) {
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
    }

    private static void centerAlignZebra(JTable t, DefaultTableCellRenderer zebra, int col) {
        t.getColumnModel().getColumn(col).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebra.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            return l;
        });
    }

    private static TableCellRenderer percentRendererZebra(DefaultTableCellRenderer zebra) {
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);

        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebra.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            l.setText(fmt.format(v) + " %");
            return l;
        };
    }

    private static TableCellRenderer currencyRendererZebra(DefaultTableCellRenderer zebra) {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebra.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            l.setText(cf.format(v));
            return l;
        };
    }

    private static double safeDouble(Object o) {
        if (o instanceof Number n) {
            double v = n.doubleValue();
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0)
                return 0.0;
            return v;
        }
        return 0.0;
    }

    /** True se o usuário clicou OK */
    public boolean isOk() {
        return ok;
    }

    /** Retorna a configuração (válido se isOk()==true) */
    public ParcelamentoConfig getConfig() {
        return config;
    }
}
