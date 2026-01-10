package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import service.ContaReceberService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * @CR Dialog: registra um pagamento em uma parcela.
 */
public class PagamentoReceberDialog extends JDialog {

    private final ContaReceberService crSvc = new ContaReceberService();
    private final int parcelaId;

    private final JFormattedTextField ftValor = new JFormattedTextField(
            NumberFormat.getNumberInstance(new Locale("pt", "BR")));

    private final JComboBox<String> cbForma = new JComboBox<>(new String[] { "Dinheiro", "Pix", "Cartão", "Outros" });

    private final JTextField tfOutro = new JTextField();

    public PagamentoReceberDialog(Window owner, int parcelaId) {
        super(owner, "Baixar Parcela", ModalityType.APPLICATION_MODAL);
        this.parcelaId = parcelaId;

        UiKit.applyDialogBase(this);
        setContentPane(buildUI());

        pack();
        setMinimumSize(new Dimension(560, 300));
        setLocationRelativeTo(owner);
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Baixar Parcela"));
        header.add(UiKit.hint("Informe o valor pago e a forma de pagamento."));
        root.add(header, BorderLayout.NORTH);

        // Card
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Valor
        ftValor.setColumns(12);
        ftValor.setValue(0);
        ftValor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        addField(card, gc, 0, "Valor pago (R$)", ftValor);

        // Forma
        cbForma.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        addField(card, gc, 1, "Forma de pagamento", cbForma);

        // Outros
        tfOutro.setEnabled(false);
        tfOutro.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        addField(card, gc, 2, "Pagamento (se Outros)", tfOutro);

        cbForma.addActionListener(e -> {
            boolean outros = "Outros".equals(cbForma.getSelectedItem());
            tfOutro.setEnabled(outros);
            if (!outros)
                tfOutro.setText("");
        });

        root.add(card, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btCancelar = UiKit.ghost("Cancelar");
        JButton btSalvar = UiKit.primary("Salvar");

        btCancelar.addActionListener(e -> dispose());
        btSalvar.addActionListener(e -> registrar());

        footer.add(btCancelar);
        footer.add(btSalvar);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private void registrar() {
        try {
            Number nv = (Number) ftValor.getValue();
            double valor = (nv == null) ? 0.0 : nv.doubleValue();

            if (valor <= 0) {
                JOptionPane.showMessageDialog(this, "Informe um valor maior que zero.");
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

            crSvc.registrarPagamento(parcelaId, valor, forma);

            JOptionPane.showMessageDialog(this, "Pagamento registrado!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

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
}
