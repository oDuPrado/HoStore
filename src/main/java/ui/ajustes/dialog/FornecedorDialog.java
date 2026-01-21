package ui.ajustes.dialog;

import dao.FornecedorDAO;
import model.FornecedorModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;
import java.util.regex.Pattern;

public class FornecedorDialog extends JDialog {

    private final JTextField tfNome = new JTextField();
    private final JFormattedTextField ftfTelefone;
    private final JTextField tfEmail = new JTextField();
    private final JFormattedTextField ftfCnpj;
    private final JTextField tfContato = new JTextField();
    private final JTextField tfEndereco = new JTextField();
    private final JTextField tfCidade = new JTextField();
    private final JTextField tfEstado = new JTextField();
    private final JTextArea taObs = new JTextArea(4, 20);

    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] { "À Vista", "A Prazo" });
    private final JComboBox<Integer> cbPrazo = new JComboBox<>(new Integer[] { 7, 15, 30, 45, 60, 90, 180 });

    private final boolean isEdit;
    private final String idOriginal;
    private final FornecedorDAO dao = new FornecedorDAO();

    public FornecedorDialog(Frame owner, FornecedorModel f) {
        super(owner, f == null ? "Novo Fornecedor" : "Editar Fornecedor", true);

        UiKit.applyDialogBase(this);

        // Máscaras
        ftfCnpj = createMasked("##.###.###/####-##");
        ftfTelefone = createMasked("(##) #####-####");

        this.isEdit = f != null;
        this.idOriginal = isEdit ? f.getId() : UUID.randomUUID().toString();

        setSize(760, 540);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // ===================== TOP =====================
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topLeft.setOpaque(false);
        topLeft.add(UiKit.title(isEdit ? "Editar Fornecedor" : "Novo Fornecedor"));
        topLeft.add(UiKit.hint("Preencha os dados do fornecedor | ENTER salva | ESC fecha"));
        topCard.add(topLeft, BorderLayout.WEST);

        add(topCard, BorderLayout.NORTH);

        // ===================== CENTER (FORM) =====================
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tamanhos consistentes
        enforceFieldSize(tfNome);
        enforceFieldSize(ftfTelefone);
        enforceFieldSize(tfEmail);
        enforceFieldSize(ftfCnpj);
        enforceFieldSize(tfContato);
        enforceFieldSize(tfEndereco);
        enforceFieldSize(tfCidade);
        enforceFieldSize(tfEstado);
        enforceFieldSize(cbTipo);
        enforceFieldSize(cbPrazo);

        tfEstado.setColumns(2);
        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);

        // Validador de email (se preencher, tem que ser válido)
        tfEmail.setInputVerifier(new InputVerifier() {
            Pattern p = Pattern.compile("^[\\w.%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,}$");

            @Override
            public boolean verify(JComponent input) {
                String txt = tfEmail.getText().trim();
                return txt.isEmpty() || p.matcher(txt).matches();
            }
        });

        // Só habilita prazo quando “A Prazo”
        cbPrazo.setEnabled(false);
        cbTipo.addActionListener(e -> cbPrazo.setEnabled("A Prazo".equals(cbTipo.getSelectedItem())));

        int y = 0;

        // Você pode juntar em 2 colunas por linha para ficar mais “moderno”
        // Linha: Nome (full)
        addRow(form, gbc, y++, "Nome:", tfNome);

        // Linha: Telefone + CNPJ (duas colunas)
        addRow2(form, gbc, y++, "Telefone:", ftfTelefone, "CNPJ:", ftfCnpj);

        // Linha: Email (full)
        addRow(form, gbc, y++, "Email:", tfEmail);

        // Linha: Contato (full)
        addRow(form, gbc, y++, "Contato:", tfContato);

        // Linha: Endereço (full)
        addRow(form, gbc, y++, "Endereço:", tfEndereco);

        // Linha: Cidade + Estado
        addRow2(form, gbc, y++, "Cidade:", tfCidade, "Estado:", tfEstado);

        // Linha: Pagamento + Prazo
        addRow2(form, gbc, y++, "Pagamento:", cbTipo, "Prazo (dias):", cbPrazo);

        // Observações (ocupa altura)
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Observações:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = 3; // pega o resto das colunas
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane obsScroll = UiKit.scroll(taObs);
        form.add(obsScroll, gbc);

        centerCard.add(form, BorderLayout.CENTER);
        add(centerCard, BorderLayout.CENTER);

        // ===================== FOOTER =====================
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        botoes.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar (ESC)");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnSalvar = UiKit.primary("Salvar (ENTER)");
        btnSalvar.addActionListener(e -> onSave());

        botoes.add(btnCancelar);
        botoes.add(btnSalvar);

        bottomCard.add(botoes, BorderLayout.EAST);
        add(bottomCard, BorderLayout.SOUTH);

        // Pré-preenche se for edição
        if (isEdit)
            fillFields(f);

        // Atalhos (ENTER salva / ESC fecha)
        bindKeys(btnSalvar, btnCancelar);

        setResizable(false);
    }

    private void bindKeys(JButton btnSalvar, JButton btnCancelar) {
        JRootPane root = getRootPane();

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        am.put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnCancelar.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "salvar");
        am.put("salvar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Evita ENTER dentro do textarea salvar sem querer enquanto o cara digita
                if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextArea)
                    return;
                btnSalvar.doClick();
            }
        });
    }

    private void enforceFieldSize(JComponent c) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(Math.max(d.width, 260), 30));
        c.setMinimumSize(new Dimension(140, 30));
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

    // Linha simples: label + field ocupando tudo
    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, gbc);
    }

    // Linha dupla: label + field | label + field
    private void addRow2(JPanel p, GridBagConstraints gbc, int row,
            String l1, JComponent f1,
            String l2, JComponent f2) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l1), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(f1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l2), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6; // menor que o primeiro campo
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(f2, gbc);
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
        }
        cbPrazo.setEnabled("A Prazo".equals(cbTipo.getSelectedItem()));
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
                    null);

            if (isEdit)
                dao.atualizar(f);
            else
                dao.inserir(f);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar fornecedor:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
