package ui.comandas.dialog;

import util.UiKit;
import service.ComandaService;
import service.SessaoService;

import javax.swing.*;
import java.awt.*;

public class ComandaPagamentoDialog extends JDialog {

    private final int comandaId;

    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"PIX","DINHEIRO","CARTAO","OUTRO"});
    private final JSpinner spValor = new JSpinner(new SpinnerNumberModel(0.0, 0.01, 999999.0, 1.00));

    public ComandaPagamentoDialog(Window owner, int comandaId, double sugestao) {
        super(owner, "Registrar Pagamento - Comanda #" + comandaId, ModalityType.APPLICATION_MODAL);
        UiKit.applyDialogBase(this);
        this.comandaId = comandaId;

        setSize(420, 220);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        spValor.setValue(Math.max(0.01, sugestao));

        JButton btnOk = new JButton("Registrar");
        btnOk.addActionListener(e -> registrar());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        g.gridx=0; g.gridy=0; form.add(new JLabel("Tipo:"), g);
        g.gridx=1; form.add(cbTipo, g);

        g.gridx=0; g.gridy=1; form.add(new JLabel("Valor:"), g);
        g.gridx=1; form.add(spValor, g);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnCancel);
        bottom.add(btnOk);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void registrar() {
        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";

            String tipo = (String) cbTipo.getSelectedItem();
            double valor = (double) spValor.getValue();

            new ComandaService().registrarPagamento(comandaId, tipo, valor, usuario);

            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
