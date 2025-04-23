package ui.ajustes.dialog;

import dao.FornecedorDAO;
import model.FornecedorModel;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.util.UUID;
import java.util.regex.Pattern;

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

    public FornecedorDialog(Frame owner, FornecedorModel f) {
        super(owner, f == null ? "Novo Fornecedor" : "Editar Fornecedor", true);

        // máscara
        ftfCnpj     = createMasked("##.###.###/####-##");
        ftfTelefone = createMasked("(##) #####-####");

        this.isEdit     = f != null;
        this.idOriginal = isEdit ? f.getId() : UUID.randomUUID().toString();

        // tamanho das colunas
        tfNome.setColumns(20);
        ftfTelefone.setColumns(14);
        tfEmail.setColumns(20);
        ftfCnpj.setColumns(14);
        tfContato.setColumns(20);
        tfEndereco.setColumns(20);
        tfCidade.setColumns(15);
        tfEstado.setColumns(2);

        // validador de email (não obrigatório, mas se preenchido deve bater regex)
        tfEmail.setInputVerifier(new InputVerifier() {
            Pattern p = Pattern.compile("^[\\w.%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,6}$");
            @Override
            public boolean verify(JComponent input) {
                String txt = tfEmail.getText().trim();
                return txt.isEmpty() || p.matcher(txt).matches();
            }
        });

        // só habilita prazo quando “A Prazo”
        cbPrazo.setEnabled(false);
        cbTipo.addActionListener(e ->
            cbPrazo.setEnabled("A Prazo".equals(cbTipo.getSelectedItem()))
        );

        // pré-preenche se for edição
        if (isEdit) fillFields(f);

        // monta form compacto
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        addRow(form, gbc, y++, "Nome:", tfNome);
        addRow(form, gbc, y++, "Telefone:", ftfTelefone);
        addRow(form, gbc, y++, "Email:", tfEmail);
        addRow(form, gbc, y++, "CNPJ:", ftfCnpj);
        addRow(form, gbc, y++, "Contato:", tfContato);
        addRow(form, gbc, y++, "Endereço:", tfEndereco);
        addRow(form, gbc, y++, "Cidade:", tfCidade);
        addRow(form, gbc, y++, "Estado:", tfEstado);
        addRow(form, gbc, y++, "Pagamento:", cbTipo);
        addRow(form, gbc, y++, "Prazo (dias):", cbPrazo);

        // Observações
        gbc.gridy = y; gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Observações:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(taObs), gbc);

        // botão salvar
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> onSave());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        rodape.add(btnSalvar);

        setLayout(new BorderLayout());
        add(form,   BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JFormattedTextField createMasked(String mask) {
        try {
            MaskFormatter mf = new MaskFormatter(mask);
            mf.setPlaceholderCharacter('_');
            return new JFormattedTextField(mf);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, gbc);
    }

    private void fillFields(FornecedorModel f) {
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
        if (f.getPrazo() != null) {
            cbPrazo.setSelectedItem(f.getPrazo());
            cbPrazo.setEnabled("A Prazo".equals(f.getPagamentoTipo()));
        }
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
            else       dao.inserir(f);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar fornecedor:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
