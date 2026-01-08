package ui.comandas.dialog;

import dao.ClienteDAO;
import service.ComandaService;
import service.SessaoService;
import ui.clientes.dialog.ClienteCadastroDialog;
import util.AlertUtils;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ComandaNovaDialog extends JDialog {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ComandaService comandaService = new ComandaService();

    // UI
    private final JCheckBox cbAvulso = new JCheckBox("Sem cadastro (Avulso)");
    private final JComboBox<String> cbClientes = new JComboBox<>();
    private final JButton btnAddCliente = UiKit.ghost("➕");

    private final JTextField tfNomeAvulso = new JTextField(25);
    private final JTextField tfMesa = new JTextField(12);
    private final JTextArea taObs = new JTextArea(5, 34);

    private Integer comandaIdCriada;

    public ComandaNovaDialog(Window owner) {
        super(owner, "Nova Comanda", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        UiKit.applyDialogBase(this);

        setSize(740, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        add(buildHeaderCard(), BorderLayout.NORTH);
        add(buildFormCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        carregarClientesNoCombo();
        configurarComportamento();

        atualizarModoCliente();

        SwingUtilities.invokeLater(() -> {
            if (cbClientes.isEnabled())
                cbClientes.requestFocusInWindow();
            else
                tfNomeAvulso.requestFocusInWindow();
        });
    }

    private JComponent buildHeaderCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("🧾 Abrir Comanda"));
        left.add(UiKit.hint("Vincule um cliente cadastrado ou abra como avulso"));
        card.add(left, BorderLayout.WEST);

        JLabel right = UiKit.hint("Enter cria | Esc cancela");
        card.add(right, BorderLayout.EAST);

        return card;
    }

    private JComponent buildFormCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // consistência de altura
        enforceFieldSize(cbClientes, 420);
        enforceFieldSize(tfNomeAvulso, 420);
        enforceFieldSize(tfMesa, 200);
        btnAddCliente.setToolTipText("Cadastrar novo cliente");
        btnAddCliente.setMargin(new Insets(2, 8, 2, 8));

        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);

        int y = 0;

        // Linha: modo (cadastrado/avulso)
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 2;
        form.add(cbAvulso, g);
        y++;

        // Linha: cliente cadastrado (combo + botão)
        g.gridwidth = 1;
        g.gridx = 0;
        g.gridy = y;
        form.add(new JLabel("Cliente:"), g);

        JPanel clienteRow = new JPanel(new BorderLayout(6, 0));
        clienteRow.setOpaque(false);

        cbClientes.setEditable(true);
        cbClientes.setPrototypeDisplayValue("Nome bem grande só pra manter o layout estável");
        clienteRow.add(cbClientes, BorderLayout.CENTER);
        clienteRow.add(btnAddCliente, BorderLayout.EAST);

        g.gridx = 1;
        form.add(clienteRow, g);
        y++;

        // Linha: nome avulso
        g.gridx = 0;
        g.gridy = y;
        form.add(new JLabel("Nome (avulso):"), g);

        g.gridx = 1;
        form.add(tfNomeAvulso, g);
        y++;

        // Linha: mesa
        g.gridx = 0;
        g.gridy = y;
        form.add(new JLabel("Mesa:"), g);

        g.gridx = 1;
        form.add(tfMesa, g);
        y++;

        // Linha: observações
        g.gridx = 0;
        g.gridy = y;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Observações:"), g);

        g.gridx = 1;
        g.weightx = 1;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;

        JScrollPane spObs = UiKit.scroll(taObs);
        spObs.setPreferredSize(new Dimension(520, 160));
        form.add(spObs, g);

        card.add(form, BorderLayout.CENTER);

        JPanel tip = new JPanel(new BorderLayout());
        tip.setOpaque(false);
        tip.add(UiKit.hint("Dica: digite no campo de cliente para buscar rápido (combo editável)."), BorderLayout.WEST);
        card.add(tip, BorderLayout.SOUTH);

        return card;
    }

    private JComponent buildBottomCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar (ESC)");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnCriar = UiKit.primary("✅ Criar Comanda (ENTER)");
        btnCriar.addActionListener(e -> criar());

        actions.add(btnCancelar);
        actions.add(btnCriar);

        card.add(actions, BorderLayout.EAST);

        // atalhos
        getRootPane().setDefaultButton(btnCriar);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");
        am.put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                btnCancelar.doClick();
            }
        });

        return card;
    }

    private void configurarComportamento() {
        cbAvulso.addActionListener(e -> atualizarModoCliente());

        btnAddCliente.addActionListener(e -> abrirCadastroCliente());

        // Enter no campo mesa cria
        tfMesa.addActionListener(e -> criar());
    }

    private void atualizarModoCliente() {
        boolean avulso = cbAvulso.isSelected();

        cbClientes.setEnabled(!avulso);
        btnAddCliente.setEnabled(!avulso);

        tfNomeAvulso.setEnabled(avulso);

        if (avulso) {
            cbClientes.setSelectedItem(null);
            tfNomeAvulso.requestFocusInWindow();
        } else {
            tfNomeAvulso.setText("");
            cbClientes.requestFocusInWindow();
        }
    }

    private void carregarClientesNoCombo() {
        try {
            List<String> nomes = clienteDAO.listarTodosNomes();
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();

            m.addElement("AVULSO");
            for (String n : nomes)
                m.addElement(n);

            cbClientes.setModel(m);
            cbClientes.setSelectedItem(nomes.isEmpty() ? "AVULSO" : nomes.get(0));
        } catch (Exception e) {
            cbClientes.setModel(new DefaultComboBoxModel<>(new String[] { "AVULSO" }));
        }
    }

    private void abrirCadastroCliente() {
        try {
            Window w = SwingUtilities.getWindowAncestor(this);
            ClienteCadastroDialog dlg = new ClienteCadastroDialog(w, null);
            dlg.setVisible(true);

            List<String> nomes = clienteDAO.listarTodosNomes();
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement("AVULSO");
            for (String n : nomes)
                m.addElement(n);

            cbClientes.setModel(m);

            if (!nomes.isEmpty())
                cbClientes.setSelectedItem(nomes.get(nomes.size() - 1));
            else
                cbClientes.setSelectedItem("AVULSO");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void criar() {
        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";

            boolean avulso = cbAvulso.isSelected();

            String clienteId;
            String nomeCliente;

            if (avulso) {
                clienteId = "AVULSO";
                nomeCliente = trimOrNull(tfNomeAvulso.getText());
            } else {
                String nomeSelecionado = null;
                Object item = cbClientes.getEditor().getItem();
                if (item != null)
                    nomeSelecionado = item.toString();

                nomeSelecionado = trimOrNull(nomeSelecionado);

                if (nomeSelecionado == null) {
                    AlertUtils.error("Selecione um cliente ou marque como Avulso.");
                    return;
                }

                if ("AVULSO".equalsIgnoreCase(nomeSelecionado)) {
                    clienteId = "AVULSO";
                    nomeCliente = null;
                } else {
                    clienteId = clienteDAO.obterIdPorNome(nomeSelecionado);
                    if (clienteId == null) {
                        AlertUtils.error("Cliente inválido. Cadastre o cliente ou selecione outro.");
                        return;
                    }
                    nomeCliente = nomeSelecionado;
                }
            }

            String mesa = trimOrNull(tfMesa.getText());
            String obs = trimOrNull(taObs.getText());

            int id = comandaService.abrirComanda(
                    clienteId,
                    nomeCliente,
                    mesa,
                    obs,
                    usuario);

            this.comandaIdCriada = id;
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String trimOrNull(String s) {
        if (s == null)
            return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    public Integer getComandaIdCriada() {
        return comandaIdCriada;
    }

    private void enforceFieldSize(JComponent c, int prefWidth) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(Math.max(d.width, prefWidth), 30));
        c.setMinimumSize(new Dimension(140, 30));
    }
}
