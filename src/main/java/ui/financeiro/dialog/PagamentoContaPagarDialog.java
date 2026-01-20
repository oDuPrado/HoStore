package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import model.ParcelaContaPagarModel;
import service.ContaPagarService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog – registra (ou complementa) pagamento de uma parcela.
 */
public class PagamentoContaPagarDialog extends JDialog {

    private final ParcelaContaPagarModel parcela;

    private final JFormattedTextField ftValor = new JFormattedTextField(
            NumberFormat.getNumberInstance(Locale.getDefault()));

    private final JComboBox<String> cbForma = new JComboBox<>(new String[] {
            "Dinheiro", "Pix", "Cheque",
            "Cartão de Débito", "Cartão de Crédito",
            "Boleto", "Promissória", "Outros"
    });

    private final JTextField tfOutro = new JTextField();

    private final JDateChooser dtPag = new JDateChooser(hoje());

    private final ContaPagarService service = new ContaPagarService();

    public PagamentoContaPagarDialog(Window owner, ParcelaContaPagarModel p) {
        super(owner, "Registrar Pagamento - Parcela " + p.getNumeroParcela(), ModalityType.APPLICATION_MODAL);
        this.parcela = p;

        UiKit.applyDialogBase(this);
        setContentPane(buildUI(p));

        pack();
        setMinimumSize(new Dimension(640, 360));
        setLocationRelativeTo(owner);
    }

    private JComponent buildUI(ParcelaContaPagarModel p) {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Registrar Pagamento"));
        header.add(UiKit.hint("Parcela " + p.getNumeroParcela() + " • Valor, forma e data do pagamento."));
        root.add(header, BorderLayout.NORTH);

        // Card com formulário
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Valor a pagar (aberto)
        double aberto = (p.getValorNominal() - p.getValorPago());
        if (aberto < 0)
            aberto = 0;

        ftValor.setValue(aberto);
        ftValor.setColumns(12);
        ftValor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        addField(card, gc, 0, "Valor a Pagar (R$)", ftValor);

        // Forma
        cbForma.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        addField(card, gc, 1, "Forma de Pagamento", cbForma);

        // Campo "Outros"
        tfOutro.setEnabled(false);
        tfOutro.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        addField(card, gc, 2, "Pagamento (se Outros)", tfOutro);

        cbForma.addActionListener(e -> {
            boolean outros = "Outros".equals(cbForma.getSelectedItem());
            tfOutro.setEnabled(outros);
            if (!outros)
                tfOutro.setText("");
        });

        // Data pagamento
        dtPag.setDateFormatString("dd/MM/yyyy");
        prepararDateChooser(dtPag);
        addField(card, gc, 3, "Data do Pagamento", dtPag);

        root.add(card, BorderLayout.CENTER);

        // Footer (botões)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        JButton btnRegistrar = UiKit.primary("Registrar");

        btnCancelar.addActionListener(e -> dispose());
        btnRegistrar.addActionListener(this::registrar);

        footer.add(btnCancelar);
        footer.add(btnRegistrar);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private void registrar(ActionEvent e) {
        try {
            double aberto = (parcela.getValorNominal() - parcela.getValorPago());
            if (aberto < 0)
                aberto = 0;

            Number nv = (Number) ftValor.getValue();
            double valor = (nv == null) ? 0.0 : nv.doubleValue();

            if (valor <= 0) {
                JOptionPane.showMessageDialog(this, "Informe um valor maior que zero.");
                return;
            }

            // ✅ Clamp / confirmação se pagar acima do aberto
            if (valor > aberto) {
                int op = JOptionPane.showConfirmDialog(
                        this,
                        "O valor informado (R$ " + formatMoney(valor) + ") é MAIOR que o valor em aberto (R$ "
                                + formatMoney(aberto) + ").\n" +
                                "Deseja registrar assim mesmo?",
                        "Valor acima do aberto",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (op != JOptionPane.YES_OPTION)
                    return;
            }

            String forma = (String) cbForma.getSelectedItem();
            if ("Outros".equals(forma)) {
                forma = tfOutro.getText().trim();
                if (forma.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Informe a forma de pagamento em 'Outros'.");
                    return;
                }
            }

            Date data = dtPag.getDate();
            if (data == null) {
                JOptionPane.showMessageDialog(this, "Informe a data do pagamento.");
                return;
            }

            service.registrarPagamento(parcela.getId(), valor, data, forma);

            JOptionPane.showMessageDialog(this, "Pagamento registrado com sucesso!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao registrar pagamento:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ─────────────────────────── Helpers UI ─────────────────────────── */

    private void addField(JPanel parent, GridBagConstraints base, int row, String label, Component field) {
        GridBagConstraints gl = (GridBagConstraints) base.clone();
        gl.gridx = 0;
        gl.gridy = row;
        gl.weightx = 0;
        gl.anchor = GridBagConstraints.WEST;
        gl.fill = GridBagConstraints.NONE;
        parent.add(new JLabel(label), gl);

        GridBagConstraints gf = (GridBagConstraints) base.clone();
        gf.gridx = 1;
        gf.gridy = row;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.HORIZONTAL;
        parent.add(field, gf);
    }

    private void prepararDateChooser(JDateChooser dc) {
        dc.setPreferredSize(new Dimension(170, 30));

        if (dc.getDateEditor() != null) {
            JComponent editor = dc.getDateEditor().getUiComponent();
            if (editor instanceof JComponent) {
                editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
            }
        }

        JButton calBtn = dc.getCalendarButton();
        if (calBtn != null) {
            calBtn.setText("📅");
            calBtn.setFocusPainted(false);
            calBtn.setMargin(new Insets(2, 8, 2, 8));
            calBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0; font: +1;");
            calBtn.setToolTipText("Selecionar data");
        }
    }

    private static Date hoje() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static String formatMoney(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }
}
