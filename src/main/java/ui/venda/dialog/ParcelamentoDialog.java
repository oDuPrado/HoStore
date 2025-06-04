package ui.venda.dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.event.ChangeListener;

/**
 * Diálogo para configurar parcelamento de uma venda em CARTÃO:
 * - Número de parcelas
 * - Juros (%)
 * - Intervalo entre parcelas (dias)
 * Exibe um preview em tabela: parcela, data de vencimento e valor.
 */
public class ParcelamentoDialog extends JDialog {
    /** Retorna config final ao chamar getConfig() após isOk()==true */
    public static class ParcelamentoConfig {
        public int parcelas; // ex: 3
        public double juros; // ex: 2.5 (% a.m.)
        public int intervaloDias; // ex: 30
    }

    private final ParcelamentoConfig config;
    private final double totalVenda;

    private final JSpinner spParcelas;
    private final JFormattedTextField ftJuros;
    private final JComboBox<String> cbIntervalo;
    private final DefaultTableModel previewModel;

    private boolean ok = false;

    /**
     * @param owner         janela-mãe
     * @param initialConfig valores iniciais (pode vir de
     *                      VendaFinalizarDialog.config)
     * @param totalVenda    valor total da venda (base para cálculo das parcelas)
     */
    public ParcelamentoDialog(Window owner, ParcelamentoConfig initialConfig, double totalVenda) {
        super(owner, "Configurar Parcelamento", ModalityType.APPLICATION_MODAL);
        this.config = initialConfig;
        this.totalVenda = totalVenda;

        setSize(500, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // ====== PAINEL DE CONTROLES ======
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        form.add(new JLabel("Parcelas:"));

        int parcelasIniciais = Math.max(1, initialConfig.parcelas);
        spParcelas = new JSpinner(new SpinnerNumberModel(parcelasIniciais, 1, 36, 1));

        spParcelas.setPreferredSize(new Dimension(60, spParcelas.getPreferredSize().height));
        form.add(spParcelas);

        form.add(new JLabel("Juros %:"));
        NumberFormatter pctFmt = new NumberFormatter(
                NumberFormat.getNumberInstance(new Locale("pt", "BR")));
        pctFmt.setValueClass(Double.class);
        pctFmt.setMinimum(0.0);
        pctFmt.setMaximum(100.0);
        pctFmt.setAllowsInvalid(false);
        ftJuros = new JFormattedTextField(pctFmt);
        ftJuros.setColumns(6);
        ftJuros.setValue(initialConfig.juros);
        form.add(ftJuros);

        form.add(new JLabel("Intervalo:"));
        cbIntervalo = new JComboBox<>(new String[] { "15 dias", "30 dias" });
        cbIntervalo.setSelectedItem(initialConfig.intervaloDias + " dias");
        form.add(cbIntervalo);

        add(form, BorderLayout.NORTH);

        // ====== TABELA DE PREVIEW ======
        previewModel = new DefaultTableModel(new String[] { "Parcela", "Vencimento", "Valor" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable previewTable = new JTable(previewModel);
        add(new JScrollPane(previewTable), BorderLayout.CENTER);

        // ====== LISTENERS PARA ATUALIZAR PREVIEW ======
        ChangeListener updater = e -> atualizarPreview();
        spParcelas.addChangeListener(updater);
        cbIntervalo.addActionListener(e -> atualizarPreview());
        ftJuros.addPropertyChangeListener("value", evt -> atualizarPreview());

        // Preenche pela primeira vez
        atualizarPreview();

        // ====== BOTÕES OK / CANCEL ======
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancelar");
        buttons.add(cancelBtn);
        buttons.add(okBtn);
        add(buttons, BorderLayout.SOUTH);

        okBtn.addActionListener(e -> {
            // Salva valores em config
            config.parcelas = (Integer) spParcelas.getValue();
            config.juros = ((Number) ftJuros.getValue()).doubleValue();
            String sel = (String) cbIntervalo.getSelectedItem();
            config.intervaloDias = Integer.parseInt(sel.split(" ")[0]);
            ok = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());
    }

    /** True se o usuário clicou OK */
    public boolean isOk() {
        return ok;
    }

    /** Retorna a configuração escolhida (apenas válido se isOk()==true) */
    public ParcelamentoConfig getConfig() {
        return config;
    }

    /** Recalcula e exibe na tabela as parcelas com datas e valores */
    private void atualizarPreview() {
        int parcelas = (Integer) spParcelas.getValue();
        double jurosPct = ((Number) ftJuros.getValue()).doubleValue();
        String sel = (String) cbIntervalo.getSelectedItem();
        int dias = Integer.parseInt(sel.split(" ")[0]);

        // limpa tabela
        previewModel.setRowCount(0);

        // cálculo simples: valor base / n * (1 + juros%)
        double base = totalVenda / parcelas;
        double fatorJ = 1 + jurosPct / 100.0;
        double valorParc = base * fatorJ;

        LocalDate hoje = LocalDate.now();
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 1; i <= parcelas; i++) {
            LocalDate d = hoje.plusDays((long) dias * i);
            previewModel.addRow(new Object[] {
                    i + "/" + parcelas,
                    dtFmt.format(d),
                    NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                            .format(valorParc)
            });
        }
    }
}
