package ui.ajustes.dialog;

import model.TaxaCartaoModel;
import service.TaxaCartaoService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Dialog para cadastrar/editar/excluir taxas de cartão.
 */
public class TaxaCartaoDialog extends JDialog {
    private final TaxaCartaoService service = new TaxaCartaoService();

    private final DefaultTableModel tableModel = new DefaultTableModel(new String[] {
            "ID", "Bandeira", "Tipo", "Min", "Max", "Mês", "Taxa (%)", "Observações"
    }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0 -> Integer.class;
                case 3, 4 -> Integer.class;
                case 6 -> Double.class;
                default -> String.class;
            };
        }
    };

    private final JTable table = new JTable(tableModel);

    // Form
    private final JComboBox<String> cbBandeira = new JComboBox<>(
            new String[] { "Cielo", "Stone", "Rede", "Getnet", "PagSeguro" });
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] { "CREDITO", "DEBITO" });
    private final JSpinner spMin = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
    private final JSpinner spMax = new JSpinner(new SpinnerNumberModel(1, 1, 36, 1));
    private final JComboBox<Integer> cbMes = new JComboBox<>();
    private final JFormattedTextField ftTaxa;
    private final JTextArea taObservacoes = new JTextArea();

    private Integer editingId = null;

    public TaxaCartaoDialog(Frame owner) {
        super(owner, "Taxas de Cartão", true);

        UiKit.applyDialogBase(this);

        // Formatter Taxa (%)
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

        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        initComponents();
        loadTaxas();
        clearForm();

        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // ===================== CARD =====================
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(12, 12));
        add(card, BorderLayout.CENTER);

        // ===================== HEADER =====================
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Taxas da Maquininha"));
        header.add(UiKit.hint("Selecione uma linha para editar • ENTER salva • ESC fecha"));
        card.add(header, BorderLayout.NORTH);

        // ===================== LEFT FORM (CARD) =====================
        JPanel formCard = UiKit.card();
        formCard.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        formCard.add(form, BorderLayout.CENTER);

        // Combo Mês (1–12) com renderer por nome
        cbMes.removeAllItems();
        for (int m = 1; m <= 12; m++)
            cbMes.addItem(m);
        cbMes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel,
                    boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Integer mm) {
                    setText(Month.of(mm).getDisplayName(TextStyle.FULL, new Locale("pt", "BR")));
                }
                return this;
            }
        });

        Dimension labelSize = new Dimension(110, 26);
        Dimension fieldSize = new Dimension(260, 32);

        pad(cbBandeira, fieldSize);
        pad(cbTipo, fieldSize);
        pad(cbMes, fieldSize);
        ftTaxa.setPreferredSize(new Dimension(120, 32));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        addRow(form, gc, labelSize, "Bandeira:", cbBandeira);
        addRow(form, gc, labelSize, "Tipo:", cbTipo);
        addRow(form, gc, labelSize, "Min parcelas:", spMin);
        addRow(form, gc, labelSize, "Max parcelas:", spMax);
        addRow(form, gc, labelSize, "Mês:", cbMes);

        // Taxa com “%”
        gc.gridx = 0;
        gc.weightx = 0;
        JLabel lTaxa = new JLabel("Taxa:");
        lTaxa.setPreferredSize(labelSize);
        form.add(lTaxa, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        JPanel pTaxa = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pTaxa.setOpaque(false);
        pTaxa.add(ftTaxa);
        pTaxa.add(new JLabel("%"));
        form.add(pTaxa, gc);
        gc.gridy++;

        // Observações
        gc.gridx = 0;
        gc.weightx = 0;
        JLabel lObs = new JLabel("Obs.:");
        lObs.setPreferredSize(labelSize);
        form.add(lObs, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        JScrollPane spObs = UiKit.scroll(taObservacoes);
        spObs.setPreferredSize(new Dimension(260, 120));
        spObs.getVerticalScrollBar().setUnitIncrement(16);
        form.add(spObs, gc);
        gc.gridy++;

        // Buttons do form
        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        formButtons.setOpaque(false);

        JButton btnLimpar = UiKit.ghost("Limpar");
        JButton btnSalvar = UiKit.primary("Salvar");

        btnLimpar.setPreferredSize(new Dimension(110, 32));
        btnSalvar.setPreferredSize(new Dimension(110, 32));

        btnLimpar.addActionListener(e -> clearForm());
        btnSalvar.addActionListener(e -> onSalvar());

        formButtons.add(btnLimpar);
        formButtons.add(btnSalvar);
        formCard.add(formButtons, BorderLayout.SOUTH);

        // ===================== TABLE (CARD) =====================
        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));
        tableCard.add(UiKit.title("Taxas cadastradas"), BorderLayout.NORTH);

        UiKit.tableDefaults(table);

        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        // Zebra
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Renderer % na coluna Taxa
        table.getColumnModel().getColumn(6).setCellRenderer(percentRendererZebra(zebra));

        // Larguras
        table.getColumnModel().getColumn(0).setPreferredWidth(60); // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(90); // Bandeira
        table.getColumnModel().getColumn(2).setPreferredWidth(80); // Tipo
        table.getColumnModel().getColumn(3).setPreferredWidth(60); // Min
        table.getColumnModel().getColumn(4).setPreferredWidth(60); // Max
        table.getColumnModel().getColumn(5).setPreferredWidth(120); // Mês
        table.getColumnModel().getColumn(6).setPreferredWidth(80); // Taxa
        // Obs deixa flexível

        JScrollPane scroll = UiKit.scroll(table);
        tableCard.add(scroll, BorderLayout.CENTER);

        // Seleção preenche form
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                preencherFormDaSelecao();
            }
        });

        // ===================== SPLIT =====================
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formCard, tableCard);
        split.setResizeWeight(0.33);
        split.setContinuousLayout(true);
        card.add(split, BorderLayout.CENTER);

        // ===================== FOOTER =====================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);

        JButton btnExcluir = UiKit.ghost("Excluir");
        JButton btnFechar = UiKit.ghost("Fechar");

        btnExcluir.setPreferredSize(new Dimension(110, 32));
        btnFechar.setPreferredSize(new Dimension(110, 32));

        btnExcluir.addActionListener(e -> onExcluir());
        btnFechar.addActionListener(e -> dispose());

        footer.add(btnExcluir);
        footer.add(btnFechar);
        card.add(footer, BorderLayout.SOUTH);

        // Enter salva
        getRootPane().setDefaultButton(btnSalvar);

        // ESC fecha
        bindEscapeToClose();
    }

    private void addRow(JPanel form, GridBagConstraints gc, Dimension labelSize, String label, JComponent field) {
        gc.gridx = 0;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        JLabel l = new JLabel(label);
        l.setPreferredSize(labelSize);
        form.add(l, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JSpinner sp) {
            ((JComponent) sp.getEditor()).setPreferredSize(new Dimension(120, 32));
        }
        form.add(field, gc);

        gc.gridy++;
    }

    private void pad(JComponent c, Dimension size) {
        c.setPreferredSize(size);
        c.setMinimumSize(size);
    }

    private void bindEscapeToClose() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private static DefaultTableCellRenderer percentRendererZebra(DefaultTableCellRenderer zebraBase) {
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);

        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r,
                    int c) {
                Component comp = zebraBase.getTableCellRendererComponent(t, v, sel, focus, r, c);
                JLabel l = (JLabel) comp;
                l.setHorizontalAlignment(SwingConstants.RIGHT);

                double val = (v instanceof Number n) ? n.doubleValue() : 0.0;
                l.setText(fmt.format(val) + " %");
                return l;
            }
        };
    }

    private void loadTaxas() {
        try {
            tableModel.setRowCount(0);
            List<TaxaCartaoModel> lista = service.listar();

            for (TaxaCartaoModel m : lista) {
                int mes = parseMes(m.getMesVigencia());
                String nomeMes = Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));

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

    private int parseMes(String mesVigencia) {
        // aceita "YYYY-MM" (recomendado) ou "MM"
        try {
            if (mesVigencia == null || mesVigencia.isBlank())
                return 1;
            if (mesVigencia.contains("-")) {
                String[] partes = mesVigencia.split("-");
                if (partes.length == 2)
                    return Integer.parseInt(partes[1]);
            }
            return Integer.parseInt(mesVigencia.trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private void clearForm() {
        editingId = null;
        table.clearSelection();
        cbBandeira.setSelectedIndex(0);
        cbTipo.setSelectedIndex(0);
        spMin.setValue(1);
        spMax.setValue(1);
        cbMes.setSelectedIndex(0);
        ftTaxa.setValue(null);
        taObservacoes.setText("");
    }

    private void preencherFormDaSelecao() {
        int sel = table.getSelectedRow();
        if (sel < 0)
            return;

        editingId = (Integer) tableModel.getValueAt(sel, 0);

        cbBandeira.setSelectedItem(String.valueOf(tableModel.getValueAt(sel, 1)));
        cbTipo.setSelectedItem(String.valueOf(tableModel.getValueAt(sel, 2)));
        spMin.setValue((Integer) tableModel.getValueAt(sel, 3));
        spMax.setValue((Integer) tableModel.getValueAt(sel, 4));

        // Coluna Mês é texto (nome), então converte de volta pro número pelo índice
        String mesNome = String.valueOf(tableModel.getValueAt(sel, 5)).toLowerCase(new Locale("pt", "BR"));
        int mesNum = 1;
        for (int i = 1; i <= 12; i++) {
            String n = Month.of(i).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
                    .toLowerCase(new Locale("pt", "BR"));
            if (n.equals(mesNome)) {
                mesNum = i;
                break;
            }
        }
        cbMes.setSelectedItem(mesNum);

        ftTaxa.setValue((Double) tableModel.getValueAt(sel, 6));
        taObservacoes.setText(String.valueOf(tableModel.getValueAt(sel, 7)));
    }

    private void onSalvar() {
        try {
            int min = (Integer) spMin.getValue();
            int max = (Integer) spMax.getValue();

            if (min > max) {
                JOptionPane.showMessageDialog(this,
                        "Min parcelas não pode ser maior que Max parcelas.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                spMin.requestFocusInWindow();
                return;
            }

            Number v = (Number) ftTaxa.getValue();
            if (v == null) {
                JOptionPane.showMessageDialog(this,
                        "Informe uma taxa válida!",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                ftTaxa.requestFocusInWindow();
                return;
            }

            TaxaCartaoModel m = new TaxaCartaoModel();
            if (editingId != null)
                m.setId(editingId);

            m.setBandeira(cbBandeira.getSelectedItem().toString());
            m.setTipo(cbTipo.getSelectedItem().toString());
            m.setMinParcelas(min);
            m.setMaxParcelas(max);

            // ✅ padrão bom: "YYYY-MM" (bate com YearMonth.now().toString())
            int mes = (Integer) cbMes.getSelectedItem();
            String mesVigencia = YearMonth.now().getYear() + "-" + String.format("%02d", mes);
            m.setMesVigencia(mesVigencia);

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
