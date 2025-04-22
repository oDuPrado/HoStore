// src/ui/ajustes/dialog/FornecedorDialog.java
package ui.ajustes.dialog;

import dao.FornecedorDAO;
import model.FornecedorModel;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FornecedorDialog extends JDialog {

    private final JTextField tfNome      = new JTextField();
    private final JFormattedTextField ftfTelefone;
    private final JTextField tfEmail     = new JTextField();
    private final JFormattedTextField ftfCnpj;
    private final JTextField tfContato   = new JTextField();
    private final JTextField tfEndereco  = new JTextField();
    private final JTextField tfCidade    = new JTextField();
    private final JTextField tfEstado    = new JTextField();
    private final JTextArea  taObs       = new JTextArea(3, 20);
    private final JComboBox<String> cbTipo   = new JComboBox<>(new String[]{"À Vista", "A Prazo"});
    private final JComboBox<Integer> cbPrazo = new JComboBox<>(new Integer[]{7,15,30,45,60,90,180});

    private final boolean isEdit;
    private final String idOriginal;
    private final FornecedorDAO dao = new FornecedorDAO();

    public FornecedorDialog(Frame owner, FornecedorModel f) throws ParseException {
        super(owner, f == null ? "Novo Fornecedor" : "Editar Fornecedor", true);

        // máscaras
        ftfCnpj = new JFormattedTextField(new MaskFormatter("##.###.###/####-##"));
        ftfTelefone = new JFormattedTextField(new MaskFormatter("(##) #####-####"));

        this.isEdit = f != null;
        this.idOriginal = isEdit ? f.getId() : UUID.randomUUID().toString();

        JPanel form = new JPanel(new GridLayout(0,2,8,8));
        form.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
        form.add(new JLabel("Nome:"));   form.add(tfNome);
        form.add(new JLabel("Telefone:")); form.add(ftfTelefone);
        form.add(new JLabel("Email:"));    form.add(tfEmail);
        form.add(new JLabel("CNPJ:"));     form.add(ftfCnpj);
        form.add(new JLabel("Contato:"));  form.add(tfContato);
        form.add(new JLabel("Endereço:")); form.add(tfEndereco);
        form.add(new JLabel("Cidade:"));   form.add(tfCidade);
        form.add(new JLabel("Estado:"));   form.add(tfEstado);
        form.add(new JLabel("Tipo Pagamento:")); form.add(cbTipo);
        form.add(new JLabel("Prazo (dias):"));   form.add(cbPrazo);
        form.add(new JLabel("Observações:"));    form.add(new JScrollPane(taObs));

        // habilitar prazo só quando "A Prazo"
        cbTipo.addActionListener(e ->
            cbPrazo.setEnabled("A Prazo".equals(cbTipo.getSelectedItem()))
        );
        cbPrazo.setEnabled(false);

        if (isEdit) {
            tfNome.setText(f.getNome());
            ftfTelefone.setText(f.getTelefone());
            tfEmail.setText(f.getEmail());
            ftfCnpj.setText(f.getCnpj());
            tfContato.setText(f.getContato());
            tfEndereco.setText(f.getEndereco());
            tfCidade.setText(f.getCidade());
            tfEstado.setText(f.getEstado());
            taObs.setText(f.getObservacoes());
            cbTipo.setSelectedItem(f.getPagamentoTipo());
            if (f.getPrazo() != null) cbPrazo.setSelectedItem(f.getPrazo());
        }

        JButton btnSalvar = new JButton("💾 Salvar");
        btnSalvar.addActionListener(e -> onSave());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnSalvar);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void onSave() {
        try {
            FornecedorModel f = new FornecedorModel(
                idOriginal,
                tfNome.getText().trim(),
                ftfTelefone.getText().trim(),
                tfEmail.getText().trim(),
                ftfCnpj.getText().trim(),
                tfContato.getText().trim(),
                tfEndereco.getText().trim(),
                tfCidade.getText().trim(),
                tfEstado.getText().trim(),
                taObs.getText().trim(),
                (String) cbTipo.getSelectedItem(),
                cbPrazo.isEnabled() ? (Integer) cbPrazo.getSelectedItem() : null,
                null,
                null
            );
            if (isEdit) dao.atualizar(f);
            else dao.inserir(f);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar fornecedor:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
