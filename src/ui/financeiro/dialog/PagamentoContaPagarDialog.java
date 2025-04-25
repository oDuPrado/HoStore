package ui.financeiro.dialog;

import com.toedter.calendar.JDateChooser;
import service.ContaPagarService;
import model.ParcelaContaPagarModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Date;

/**
 * Dialog – registra (ou complementa) pagamento de uma parcela.
 */
public class PagamentoContaPagarDialog extends JDialog {

    private final ParcelaContaPagarModel parcela;
    private final JFormattedTextField ftValor;
    private final JComboBox<String> cbForma;
    private final JTextField tfOutro;
    private final JDateChooser dtPag;
    private final ContaPagarService service = new ContaPagarService();

    public PagamentoContaPagarDialog(Window owner, ParcelaContaPagarModel p) {
        super(owner, "Registrar Pagamento - Parcela " + p.getNumeroParcela(), ModalityType.APPLICATION_MODAL);
        this.parcela = p;

        // Mesmo espaçamento e estilo do CadastroBoosterDialog
        setLayout(new GridLayout(0, 2, 8, 8));

        // Valor a Pagar
        add(new JLabel("Valor a Pagar:"));
        ftValor = new JFormattedTextField(NumberFormat.getNumberInstance());
        ftValor.setValue(p.getValorNominal() - p.getValorPago());
        add(ftValor);

        // Forma de Pagamento
        add(new JLabel("Forma de Pagamento:"));
        String[] formas = {
            "Dinheiro", "Pix", "Cheque",
            "Cartão de Débito", "Cartão de Crédito",
            "Boleto", "Promissória", "Outros"
        };
        cbForma = new JComboBox<>(formas);
        add(cbForma);

        // Se Outros, campo livre
        add(new JLabel("Pagamento:"));
        tfOutro = new JTextField();
        tfOutro.setEnabled(false);
        add(tfOutro);

        cbForma.addActionListener(e -> {
            boolean outros = "Outros".equals(cbForma.getSelectedItem());
            tfOutro.setEnabled(outros);
            if (!outros) tfOutro.setText("");
        });

        // Data do Pagamento
        add(new JLabel("Data do Pagamento:"));
        dtPag = new JDateChooser(new Date());
        dtPag.setDateFormatString("dd/MM/yyyy");
        add(dtPag);

        // Botões
        JButton btnRegistrar = new JButton("Registrar");
        btnRegistrar.addActionListener(this::registrar);
        add(btnRegistrar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        add(btnCancelar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void registrar(ActionEvent e) {
        try {
            double valor = ((Number) ftValor.getValue()).doubleValue();
            String forma = (String) cbForma.getSelectedItem();
            if ("Outros".equals(forma)) {
                forma = tfOutro.getText().trim();
            }
            Date data = dtPag.getDate();
            service.registrarPagamento(parcela.getId(), valor, data, forma);
            JOptionPane.showMessageDialog(this, "Pagamento registrado com sucesso!");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao registrar pagamento:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
