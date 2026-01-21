package ui.ajustes.dialog;

import util.UiKit;
import model.BancoModel;
import service.BancoService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BancoDialog extends JDialog {

    private final BancoService service = new BancoService();
    private final BancoModel banco;  // null = novo

    private final JTextField tfNome       = new JTextField(20);
    private final JTextField tfAgencia    = new JTextField(10);
    private final JTextField tfConta      = new JTextField(15);
    private final JTextArea  taObservacoes= new JTextArea(3,20);

    public BancoDialog(Window owner, BancoModel banco) {
        super(owner, banco==null?"Nova Conta Bancária":"Editar Conta Bancária", ModalityType.APPLICATION_MODAL);
        UiKit.applyDialogBase(this);
        this.banco = banco;

        JPanel form = new JPanel(new GridLayout(0,2,6,6));
        form.add(new JLabel("Banco:"));       form.add(tfNome);
        form.add(new JLabel("Agência:"));     form.add(tfAgencia);
        form.add(new JLabel("Conta:"));       form.add(tfConta);
        form.add(new JLabel("Observações:")); form.add(UiKit.scroll(taObservacoes));

        JButton btnSalvar  = new JButton(banco==null?"Salvar":"Atualizar");
        JButton btnCancel  = new JButton("Cancelar");
        btnSalvar.addActionListener(this::onSalvar);
        btnCancel.addActionListener(e -> dispose());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnCancel);
        rodape.add(btnSalvar);

        getContentPane().setLayout(new BorderLayout(10,10));
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(rodape, BorderLayout.SOUTH);

        if (banco!=null) {
            tfNome.setText(banco.getNome());
            tfAgencia.setText(banco.getAgencia());
            tfConta.setText(banco.getConta());
            taObservacoes.setText(banco.getObservacoes());
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private void onSalvar(ActionEvent e) {
        try {
            String nome = tfNome.getText().trim();
            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this,"Banco é obrigatório");
                return;
            }
            String agencia = tfAgencia.getText().trim();
            String conta   = tfConta.getText().trim();
            String obs     = taObservacoes.getText().trim();

            if (banco==null) {
                service.criar(nome, agencia, conta, obs);
            } else {
                service.atualizar(banco.getId(), nome, agencia, conta, obs);
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage());
        }
    }
}
