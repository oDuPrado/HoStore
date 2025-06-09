package ui.ajustes.dialog;

import model.TaxaCartaoModel;
import service.TaxaCartaoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Dialog para cadastrar/editar/excluir taxas de cartão da maquininha.
 * Campos: Bandeira, Tipo, Min/Max Parcelas, Mês, Taxa (%) e Observações.
 * Uso de JSplitPane para dimensionamento dinâmico.
 */
public class TaxaCartaoDialog extends JDialog {
    private final TaxaCartaoService service = new TaxaCartaoService();
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[] {
            "ID", "Bandeira", "Tipo", "Min", "Max", "Mês", "Taxa (%)", "Observações"
    }, 0);
    private final JTable table = new JTable(tableModel);

    // Formulário
    private final JComboBox<String> cbBandeira = new JComboBox<>(
            new String[] { "Cielo", "Stone", "Rede", "Getnet", "PagSeguro" });
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] { "CREDITO", "DEBITO" });
    private final JSpinner spMin = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
    private final JSpinner spMax = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
    private final JComboBox<Integer> cbMes = new JComboBox<>();
    private final JFormattedTextField ftTaxa;
    private final JTextArea taObservacoes = new JTextArea();

    public TaxaCartaoDialog(Frame owner) {
        super(owner, "Taxas de Cartão", true);
        // Formatter para Taxa (%)
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        NumberFormatter taxaFmt = new NumberFormatter(nf);
        taxaFmt.setValueClass(Double.class);
        taxaFmt.setMinimum(0.0);
        taxaFmt.setAllowsInvalid(false);
        ftTaxa = new JFormattedTextField(taxaFmt);
        ftTaxa.setColumns(8);

        taObservacoes.setLineWrap(true);
        taObservacoes.setWrapStyleWord(true);

        initComponents();
        loadTaxas();
        setSize(900, 600);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane())
                .setBorder(new EmptyBorder(10, 10, 10, 10));

        // popula combo Mês (1–12)
        for (int m = 1; m <= 12; m++)
            cbMes.addItem(m);
        cbMes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Integer) {
                    int mm = (Integer) value;
                    setText(Month.of(mm)
                            .getDisplayName(TextStyle.FULL, new Locale("pt", "BR")));
                }
                return this;
            }
        });

        // ─── Formulário ─────────────────────────────────────
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createTitledBorder("Nova / Editar Taxa"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Coluna 0 (labels), peso 0
        gc.weightx = 0;
        // Coluna 1 (campos), peso 1
        int row = 0;

        // Bandeira
        gc.gridy = row;
        gc.gridx = 0;
        panelForm.add(new JLabel("Bandeira:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(cbBandeira, gc);

        // Tipo
        row++;
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        panelForm.add(new JLabel("Tipo:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(cbTipo, gc);

        // Min Parcelas
        row++;
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        panelForm.add(new JLabel("Min Parcelas:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(spMin, gc);

        // Max Parcelas
        row++;
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        panelForm.add(new JLabel("Max Parcelas:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(spMax, gc);

        // Mês
        row++;
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        panelForm.add(new JLabel("Mês:"), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(cbMes, gc);

        // Taxa (%)
        row++;
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        panelForm.add(new JLabel("Taxa (%):"), gc);
        JPanel pTaxa = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pTaxa.add(ftTaxa);
        pTaxa.add(new JLabel(" %"));
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(pTaxa, gc);

        // Observações
        row++;
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0;
        panelForm.add(new JLabel("Observações:"), gc);
        JScrollPane spObs = new JScrollPane(taObservacoes);
        spObs.setPreferredSize(new Dimension(0, 100));
        gc.gridx = 1;
        gc.weightx = 1;
        panelForm.add(spObs, gc);

        // Botões Limpar / Salvar
        JButton btnLimpar = new JButton("Limpar");
        JButton btnSalvar = new JButton("Salvar");
        btnLimpar.addActionListener(e -> clearForm());
        btnSalvar.addActionListener(e -> onSalvar());
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pBtns.add(btnLimpar);
        pBtns.add(btnSalvar);

        JPanel west = new JPanel(new BorderLayout(8, 8));
        west.add(panelForm, BorderLayout.CENTER);
        west.add(pBtns, BorderLayout.SOUTH);
        west.setMinimumSize(new Dimension(350, 0));

        // ─── Tabela ─────────────────────────────────────────
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // formata coluna Taxa (%)
        table.getColumnModel().getColumn(6)
                .setCellRenderer(new DefaultTableCellRenderer() {
                    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
                    {
                        fmt.setMinimumFractionDigits(2);
                        fmt.setMaximumFractionDigits(2);
                    }

                    @Override
                    public Component getTableCellRendererComponent(JTable t, Object v,
                            boolean s, boolean f, int r, int c) {
                        super.getTableCellRendererComponent(t, v, s, f, r, c);
                        if (v instanceof Number) {
                            setText(fmt.format(((Number) v).doubleValue()) + " %");
                        }
                        return this;
                    }
                });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Taxas Cadastradas"));

        // usa JSplitPane para redimensionar
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                west,
                scroll);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);

        // ─── Rodapé ─────────────────────────────────────────
        JButton btnExcluir = new JButton("Excluir Selecionada");
        btnExcluir.addActionListener(e -> onExcluir());
        JButton btnFechar = new JButton("Fechar");
        btnFechar.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(btnExcluir);
        south.add(btnFechar);

        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void loadTaxas() {
        try {
            tableModel.setRowCount(0);
            List<TaxaCartaoModel> lista = service.listar();
            for (TaxaCartaoModel m : lista) {
                int mes = 1; // valor padrão
                try {
                    String[] partes = m.getMesVigencia().split("-");
                    if (partes.length == 2) {
                        mes = Integer.parseInt(partes[1]);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao parsear mes_vigencia: " + m.getMesVigencia());
                }

                String nomeMes = Month.of(mes)
                        .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                tableModel.addRow(new Object[] {
                        m.getId(),
                        m.getBandeira(),
                        m.getTipo(),
                        m.getMinParcelas(),
                        m.getMaxParcelas(),
                        nomeMes,
                        m.getTaxaPct(),
                        m.getObservacoes()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar taxas:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        table.clearSelection();
        cbBandeira.setSelectedIndex(0);
        cbTipo.setSelectedIndex(0);
        spMin.setValue(1);
        spMax.setValue(1);
        cbMes.setSelectedIndex(0);
        ftTaxa.setValue(null);
        taObservacoes.setText("");
    }

    private void onSalvar() {
        try {
            TaxaCartaoModel m = new TaxaCartaoModel();
            int sel = table.getSelectedRow();
            if (sel >= 0)
                m.setId((Integer) tableModel.getValueAt(sel, 0));

            m.setBandeira(cbBandeira.getSelectedItem().toString());
            m.setTipo(cbTipo.getSelectedItem().toString());
            m.setMinParcelas((Integer) spMin.getValue());
            m.setMaxParcelas((Integer) spMax.getValue());
            m.setMesVigencia(String.valueOf((Integer) cbMes.getSelectedItem()));

            Number v = (Number) ftTaxa.getValue();
            if (v == null) {
                JOptionPane.showMessageDialog(this,
                        "Informe uma taxa válida!",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            m.setTaxaPct(v.doubleValue());
            m.setObservacoes(taObservacoes.getText().trim());

            service.salvar(m);
            loadTaxas();
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExcluir() {
        int sel = table.getSelectedRow();
        if (sel < 0)
            return;
        int id = (Integer) tableModel.getValueAt(sel, 0);
        if (JOptionPane.showConfirmDialog(this,
                "Excluir taxa selecionada?",
                "Confirmação", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        try {
            service.excluir(id);
            loadTaxas();
            clearForm();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao excluir:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
