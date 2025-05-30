package ui.financeiro.dialog;

import javax.swing.*;
import service.ContaReceberService;

import java.awt.*;

/**
 * @CR Dialog: registra um pagamento em uma parcela.
 */
public class PagamentoReceberDialog extends JDialog {

    private final ContaReceberService crSvc = new ContaReceberService();
    private final int parcelaId;

    private JTextField txtValor;
    private JComboBox<String> cbForma;

    public PagamentoReceberDialog(Window owner, int parcelaId) {
        super(owner, "Baixar Parcela", ModalityType.APPLICATION_MODAL);
        this.parcelaId = parcelaId;

        setSize(350,180);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(criarForm(), BorderLayout.CENTER);
        add(criarBotoes(), BorderLayout.SOUTH);
    }

    private JPanel criarForm() {
        JPanel p = new JPanel(new GridLayout(0,2,8,6));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        p.add(new JLabel("Valor pago:"));
        txtValor = new JTextField();
        p.add(txtValor);

        p.add(new JLabel("Forma pgto:"));
        cbForma = new JComboBox<>(new String[]{"dinheiro","pix","cartao"});
        p.add(cbForma);

        return p;
    }

    private JPanel criarBotoes() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btSalvar = new JButton("Salvar");
        JButton btCancelar = new JButton("Cancelar");
        p.add(btCancelar); p.add(btSalvar);

        btCancelar.addActionListener(e -> dispose());

        btSalvar.addActionListener(e -> {
            try {
                double valor = Double.parseDouble(txtValor.getText().replace(",",".")); 
                String forma = (String) cbForma.getSelectedItem();
                crSvc.registrarPagamento(parcelaId, valor, forma);
                JOptionPane.showMessageDialog(this,"Pagamento registrado!");
                dispose();
            } catch (Exception ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return p;
    }
}
