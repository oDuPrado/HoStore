package ui.clientes.dialog;

import model.ClienteModel;
import service.ClienteService;
import util.UiKit;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

/**
 * ClienteCadastroDialog (visual alinhado ao UiKit)
 * - Remove null layout/undecorated/fade
 * - Mantém a mesma lógica de salvar/validar
 */
public class ClienteCadastroDialog extends JDialog {

    private JTextField txtNome;
    private JFormattedTextField txtTelefone;
    private JFormattedTextField txtCPF;
    private JFormattedTextField txtDataNasc;
    private JComboBox<String> comboTipo;
    private JTextField txtEndereco;
    private JTextField txtCidade;
    private JComboBox<String> comboEstado;
    private JTextArea txtObservacoes;

    private boolean salvou;
    private ClienteModel clienteModel;

    public ClienteCadastroDialog(Window parent, ClienteModel existente) {
        super(parent, "Cadastro de Cliente", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);

        setSize(760, 560);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        clienteModel = (existente == null) ? novoModelo() : existente;

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        preencherCamposDoModel();

        bindKeys();
        SwingUtilities.invokeLater(() -> txtNome.requestFocusInWindow());
    }

    private JComponent buildHeader() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);

        String titulo = (clienteModel != null && clienteModel.getId() != null)
                ? "👤 Cliente"
                : "👤 Cadastro de Cliente";

        left.add(UiKit.title(titulo));
        left.add(UiKit.hint("Preencha os dados e salve. CPF não pode duplicar."));
        card.add(left, BorderLayout.WEST);

        JLabel right = UiKit.hint("Enter salva | Esc cancela");
        card.add(right, BorderLayout.EAST);

        return card;
    }

    private JComponent buildForm() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        txtNome = new JTextField();
        txtTelefone = campoMascara("(##) #####-####", "");
        txtCPF = campoMascara("###.###.###-##", "");
        txtDataNasc = campoMascara("##/##/####", "");
        comboTipo = new JComboBox<>(new String[] { "Colecionador", "Jogador", "Ambos" });
        txtEndereco = new JTextField();
        txtCidade = new JTextField();
        comboEstado = new JComboBox<>(UF);

        txtObservacoes = new JTextArea(5, 34);
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        JScrollPane spObs = UiKit.scroll(txtObservacoes);
        spObs.setPreferredSize(new Dimension(520, 140));

        // placeholders (FlatLaf)
        txtNome.putClientProperty("JTextField.placeholderText", "Nome completo");
        txtTelefone.putClientProperty("JTextField.placeholderText", "(##) #####-####");
        txtCPF.putClientProperty("JTextField.placeholderText", "###.###.###-##");
        txtDataNasc.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa");
        txtEndereco.putClientProperty("JTextField.placeholderText", "Rua, número, bairro");
        txtCidade.putClientProperty("JTextField.placeholderText", "Cidade");

        // Linha 1: Nome (full)
        addRow(form, g, 0, "Nome:", txtNome, true);

        // Linha 2: Telefone | CPF
        addRow2(form, g, 1, "Telefone:", txtTelefone, "CPF:", txtCPF);

        // Linha 3: Data nasc | Tipo
        addRow2(form, g, 2, "Data Nasc.:", txtDataNasc, "Tipo:", comboTipo);

        // Linha 4: Endereço (full)
        addRow(form, g, 3, "Endereço:", txtEndereco, true);

        // Linha 5: Cidade | Estado
        addRow2(form, g, 4, "Cidade:", txtCidade, "Estado:", comboEstado);

        // Linha 6: Observações
        g.gridy = 5;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Observações:"), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 3;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        form.add(spObs, g);

        card.add(form, BorderLayout.CENTER);
        card.add(UiKit.hint("Dica: cliente “Ambos” é o padrão bom para quem joga e coleciona."), BorderLayout.SOUTH);

        return card;
    }

    private JComponent buildFooter() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar (ESC)");
        btnCancelar.addActionListener(e -> {
            salvou = false;
            dispose();
        });

        JButton btnSalvarNovo = UiKit.ghost("Salvar + Novo");
        btnSalvarNovo.addActionListener(e -> salvar(true));

        JButton btnSalvar = UiKit.primary("Salvar (ENTER)");
        btnSalvar.addActionListener(e -> salvar(false));

        actions.add(btnCancelar);
        actions.add(btnSalvarNovo);
        actions.add(btnSalvar);

        card.add(actions, BorderLayout.EAST);

        // default button
        getRootPane().setDefaultButton(btnSalvar);

        return card;
    }

    private void bindKeys() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        am.put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                salvou = false;
                dispose();
            }
        });
    }

    private void preencherCamposDoModel() {
        txtNome.setText(nvl(clienteModel.getNome()));
        txtTelefone.setText(nvl(clienteModel.getTelefone()));
        txtCPF.setText(nvl(clienteModel.getCpf()));
        txtDataNasc.setText(nvl(clienteModel.getDataNasc()));
        comboTipo.setSelectedItem(clienteModel.getTipo() != null ? clienteModel.getTipo() : "Ambos");
        txtEndereco.setText(nvl(clienteModel.getEndereco()));
        txtCidade.setText(nvl(clienteModel.getCidade()));
        comboEstado.setSelectedItem(clienteModel.getEstado() != null ? clienteModel.getEstado() : "MS");
        txtObservacoes.setText(nvl(clienteModel.getObservacoes()));
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /* ===================== SALVAR (LÓGICA ORIGINAL) ===================== */

    private void salvar(boolean novoApos) {
        String nome = txtNome.getText().trim();
        String cpf = txtCPF.getText().replaceAll("\\D", "");

        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório!");
            return;
        }
        if (cpf.length() != 11) {
            JOptionPane.showMessageDialog(this, "CPF inválido.");
            return;
        }

        List<ClienteModel> todos = ClienteService.loadAll();
        boolean duplicado = todos.stream()
                .anyMatch(c -> c.getCpf().replaceAll("\\D", "").equals(cpf)
                        && !c.getId().equals(clienteModel.getId()));
        if (duplicado) {
            JOptionPane.showMessageDialog(this, "CPF já cadastrado para outro cliente.");
            return;
        }

        clienteModel.setNome(nome);
        clienteModel.setCpf(txtCPF.getText());
        clienteModel.setTelefone(txtTelefone.getText());
        clienteModel.setDataNasc(txtDataNasc.getText());
        clienteModel.setTipo((String) comboTipo.getSelectedItem());
        clienteModel.setEndereco(txtEndereco.getText());
        clienteModel.setCidade(txtCidade.getText());
        clienteModel.setEstado((String) comboEstado.getSelectedItem());
        clienteModel.setObservacoes(txtObservacoes.getText());

        clienteModel.setAlteradoEm("2025-04-16 10:10");
        clienteModel.setAlteradoPor("admin");

        ClienteService.upsert(clienteModel);
        salvou = true;

        if (novoApos) {
            dispose();
            new ClienteCadastroDialog(
                    SwingUtilities.getWindowAncestor(getParent()),
                    null).setVisible(true);
        } else {
            dispose();
        }
    }

    /* ===================== HELPERS UI ===================== */

    private JFormattedTextField campoMascara(String mask, String valorInicial) {
        try {
            MaskFormatter mf = new MaskFormatter(mask);
            mf.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mf);
            f.setText(valorInicial == null ? "" : valorInicial);
            return f;
        } catch (ParseException e) {
            return new JFormattedTextField(valorInicial);
        }
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field, boolean full) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = full ? 3 : 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, g);
    }

    private void addRow2(JPanel p, GridBagConstraints g, int row,
            String l1, JComponent f1,
            String l2, JComponent f2) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l1), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f1, g);

        g.gridx = 2;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l2), g);

        g.gridx = 3;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f2, g);
    }

    private ClienteModel novoModelo() {
        ClienteModel m = new ClienteModel();
        m.setId("C-" + UUID.randomUUID().toString().substring(0, 5));
        m.setCriadoEm("2025-04-16 10:00");
        m.setCriadoPor("admin");
        return m;
    }

    public boolean isSalvou() {
        return salvou;
    }

    public ClienteModel getClienteModel() {
        return clienteModel;
    }

    private static final String[] UF = {
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC",
            "SE", "SP", "TO"
    };
}
