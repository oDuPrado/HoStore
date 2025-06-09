package ui.venda.dialog;

import model.TaxaCartaoModel;
import service.TaxaCartaoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo para configurar parcelamento de uma venda em CARTÃO,
 * unindo seleção de bandeira/taxas cadastradas e preview de parcelas.
 */
public class ParcelamentoDialog extends JDialog {
    /** Retorna config final ao chamar getConfig() após isOk()==true */
    public static class ParcelamentoConfig {
        public int parcelas;       // ex: 3
        public double juros;       // ex: 2.5 (%)
        public int intervaloDias;  // ex: 30
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

        // Spinner de parcelas
        int inic = Math.max(1, initialConfig.parcelas);
        spParcelas = new JSpinner(new SpinnerNumberModel(inic, 1, 36, 1));
        spParcelas.setPreferredSize(new Dimension(60, spParcelas.getPreferredSize().height));

        // Formatter para juros (%)
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt","BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        NumberFormatter fmtJ = new NumberFormatter(nf);
        fmtJ.setValueClass(Double.class);
        fmtJ.setMinimum(0.0);
        fmtJ.setAllowsInvalid(false);
        ftJuros = new JFormattedTextField(fmtJ);
        ftJuros.setColumns(6);
        ftJuros.setValue(initialConfig.juros);

        // Combo de intervalo
        cbIntervalo = new JComboBox<>(new String[]{ "15 dias", "30 dias" });

        // Modelo e tabela de taxas disponíveis
        taxasModel = new DefaultTableModel(new String[]{ "Tipo","Parcelas","Taxa (%)" }, 0);
        taxasTable = new JTable(taxasModel);

        // Modelo e tabela de preview de parcelas
        previewModel = new DefaultTableModel(new String[]{ "Parcela","Vencimento","Valor" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        previewTable = new JTable(previewModel);

        initComponents();
        loadBandeiras();
        refreshTaxasAndPreview();

        setSize(600, 500);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10,10));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        // ─── Controles ─────────────────────────────────────
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        form.add(new JLabel("Bandeira:"));  form.add(cbBandeira);
        form.add(new JLabel("Parcelas:"));  form.add(spParcelas);
        form.add(new JLabel("Taxa %:"));    form.add(ftJuros); form.add(new JLabel("%"));
        form.add(new JLabel("Intervalo:")); form.add(cbIntervalo);
        add(form, BorderLayout.NORTH);

        // listeners para autoatualizar
        cbBandeira.addActionListener(e -> refreshTaxasAndPreview());
        spParcelas.addChangeListener(e -> refreshTaxasAndPreview());
        cbIntervalo.addActionListener(e -> atualizarPreview());
        ftJuros.addPropertyChangeListener("value", evt -> atualizarPreview());

        // formata coluna Taxa (%) na tabela de taxas
        taxasTable.getColumnModel().getColumn(2)
            .setCellRenderer(new DefaultTableCellRenderer(){
                private final NumberFormat fmt = 
                    NumberFormat.getNumberInstance(new Locale("pt","BR"));
                { fmt.setMinimumFractionDigits(2); fmt.setMaximumFractionDigits(2); }
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean isSel, boolean hasFocus, int row, int col) {
                    super.getTableCellRendererComponent(t,v,isSel,hasFocus,row,col);
                    if (v instanceof Number) {
                        setText(fmt.format(((Number)v).doubleValue()) + " %");
                    }
                    return this;
                }
            });

        // Scroll de taxas
        JScrollPane spTaxas = new JScrollPane(taxasTable);
        spTaxas.setBorder(BorderFactory.createTitledBorder("Taxas Disponíveis"));

        // Scroll de preview
        JScrollPane spPreview = new JScrollPane(previewTable);
        spPreview.setBorder(BorderFactory.createTitledBorder("Preview Parcelamento"));

        // Split vertical
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTaxas, spPreview);
        split.setResizeWeight(0.4);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        // ─── Botões OK / Cancel ─────────────────────────────
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancelar");
        buttons.add(btnCancel); buttons.add(btnOK);
        add(buttons, BorderLayout.SOUTH);

        btnOK.addActionListener(e -> {
            config.parcelas      = (Integer) spParcelas.getValue();
            config.juros         = ((Number) ftJuros.getValue()).doubleValue();
            config.intervaloDias = Integer.parseInt(((String)cbIntervalo.getSelectedItem()).split(" ")[0]);
            ok = true;
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());
    }

    /** Carrega no combo todas as bandeiras que têm taxa para o mês atual */
    private void loadBandeiras() {
        YearMonth mesRef = YearMonth.now();
        try {
            for (TaxaCartaoModel t : taxaService.listar()) {
                if (t.getMesVigencia().equals(mesRef.toString()) &&
                    cbBandeira.getItemAt(0) == null   // primeira vez
                ) {
                    cbBandeira.addItem(t.getBandeira());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Busca e exibe as taxas válidas e atualiza o preview */
    private void refreshTaxasAndPreview() {
        String bandeira = (String) cbBandeira.getSelectedItem();
        int parcelas = (Integer) spParcelas.getValue();
        YearMonth mesRef = YearMonth.now();

        // 1) Busca taxa específica
        try {
            double taxaPct = taxaService.buscarTaxa(bandeira, "CREDITO", parcelas, mesRef)
                                 .orElse(0.0);
            ftJuros.setValue(taxaPct);
        } catch (Exception e) {
            ftJuros.setValue(0.0);
        }

        // 2) Preenche lista de taxas válidas
        taxasModel.setRowCount(0);
        try {
            for (TaxaCartaoModel t : taxaService.listar()) {
                if (t.getBandeira().equals(bandeira) &&
                    t.getMesVigencia().equals(mesRef.toString())) {
                    String faixa = t.getMinParcelas() + "x–" + t.getMaxParcelas() + "x";
                    taxasModel.addRow(new Object[]{
                        t.getTipo(), faixa, t.getTaxaPct()
                    });
                }
            }
            if (taxasModel.getRowCount() == 0) {
                taxasModel.addRow(new Object[]{"—","—",0.0});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 3) Atualiza preview de parcelas
        atualizarPreview();
    }

    /** Recalcula e exibe na tabela de preview */
    private void atualizarPreview() {
        previewModel.setRowCount(0);
        int parcelas = (Integer) spParcelas.getValue();
        double jurosPct = ((Number) ftJuros.getValue()).doubleValue();
        int dias = Integer.parseInt(((String)cbIntervalo.getSelectedItem()).split(" ")[0]);
        double base = totalVenda / parcelas;
        double valorParc = base * (1 + jurosPct / 100.0);

        LocalDate hoje = LocalDate.now();
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

        for (int i = 1; i <= parcelas; i++) {
            LocalDate d = hoje.plusDays((long) dias * i);
            previewModel.addRow(new Object[]{
                i + "/" + parcelas,
                dtFmt.format(d),
                cf.format(valorParc)
            });
        }
    }

    /** True se o usuário clicou OK */
    public boolean isOk() { return ok; }
    /** Retorna a configuração (válido se isOk()==true) */
    public ParcelamentoConfig getConfig() { return config; }
}
