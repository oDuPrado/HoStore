package ui.comandas.dialog;

import dao.ClienteDAO;
import service.ComandaService;
import service.SessaoService;
import ui.clientes.dialog.ClienteCadastroDialog;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ComandaNovaDialog extends JDialog {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ComandaService comandaService = new ComandaService();

    // UI
    private final JCheckBox cbAvulso = new JCheckBox("Sem cadastro (Avulso)");
    private final JComboBox<String> cbClientes = new JComboBox<>();
    private final JButton btnAddCliente = new JButton("➕");

    private final JTextField tfNomeAvulso = new JTextField(25);
    private final JTextField tfMesa = new JTextField(12);
    private final JTextArea taObs = new JTextArea(5, 34);

    private Integer comandaIdCriada;

    public ComandaNovaDialog(Window owner) {
        super(owner, "Nova Comanda", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(owner);

        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        carregarClientesNoCombo();
        configurarComportamento();

        // estado inicial
        atualizarModoCliente();

        SwingUtilities.invokeLater(() -> {
            if (cbClientes.isEnabled()) cbClientes.requestFocusInWindow();
            else tfNomeAvulso.requestFocusInWindow();
        });
    }

    private JComponent buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("🧾 Abrir Comanda");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel sub = new JLabel("Vincule um cliente cadastrado ou abra como avulso.");
        sub.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        p.add(left, BorderLayout.WEST);
        return p;
    }

    private JComponent buildForm() {
        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        int y = 0;

        // Linha: modo (cadastrado/avulso)
        g.gridx = 0; g.gridy = y; g.gridwidth = 2;
        form.add(cbAvulso, g);
        y++;

        // Linha: cliente cadastrado (combo + botão)
        g.gridwidth = 1;
        g.gridx = 0; g.gridy = y;
        form.add(new JLabel("Cliente:"), g);

        JPanel clienteRow = new JPanel(new BorderLayout(6, 0));
        clienteRow.setOpaque(false);

        cbClientes.setEditable(true);
        cbClientes.setPrototypeDisplayValue("Nome bem grande só pra manter o layout estável");
        clienteRow.add(cbClientes, BorderLayout.CENTER);

        btnAddCliente.setToolTipText("Cadastrar novo cliente");
        btnAddCliente.setMargin(new Insets(2, 8, 2, 8));
        clienteRow.add(btnAddCliente, BorderLayout.EAST);

        g.gridx = 1;
        form.add(clienteRow, g);
        y++;

        // Linha: nome avulso (só aparece quando avulso marcado)
        g.gridx = 0; g.gridy = y;
        form.add(new JLabel("Nome (avulso):"), g);

        g.gridx = 1;
        form.add(tfNomeAvulso, g);
        y++;

        // Linha: mesa
        g.gridx = 0; g.gridy = y;
        form.add(new JLabel("Mesa:"), g);

        g.gridx = 1;
        form.add(tfMesa, g);
        y++;

        // Linha: observações
        g.gridx = 0; g.gridy = y;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Observações:"), g);

        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(taObs);
        sp.setPreferredSize(new Dimension(420, 140));

        g.gridx = 1;
        form.add(sp, g);

        wrap.add(form, BorderLayout.NORTH);

        // dica visual
        JPanel tip = new JPanel(new BorderLayout());
        tip.setOpaque(false);
        JLabel hint = new JLabel("Dica: você pode digitar no campo de cliente pra buscar rápido.");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        tip.add(hint, BorderLayout.WEST);

        wrap.add(tip, BorderLayout.SOUTH);

        return wrap;
    }

    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JLabel info = new JLabel("Enter cria | Esc cancela");
        info.setForeground(UIManager.getColor("Label.disabledForeground"));
        bottom.add(info, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnCriar = new JButton("✅ Criar Comanda");
        btnCriar.addActionListener(e -> criar());

        actions.add(btnCancelar);
        actions.add(btnCriar);

        bottom.add(actions, BorderLayout.EAST);

        // atalhos
        getRootPane().setDefaultButton(btnCriar);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");
        am.put("cancelar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                btnCancelar.doClick();
            }
        });

        return bottom;
    }

    private void configurarComportamento() {
        cbAvulso.addActionListener(e -> atualizarModoCliente());

        btnAddCliente.addActionListener(e -> abrirCadastroCliente());

        // Enter no campo mesa cria (porque humano gosta de ser rápido)
        tfMesa.addActionListener(e -> criar());
    }

    private void atualizarModoCliente() {
        boolean avulso = cbAvulso.isSelected();

        cbClientes.setEnabled(!avulso);
        btnAddCliente.setEnabled(!avulso);

        tfNomeAvulso.setEnabled(avulso);

        // “limpeza” de UX
        if (avulso) {
            cbClientes.setSelectedItem(null);
            tfNomeAvulso.requestFocusInWindow();
        } else {
            tfNomeAvulso.setText("");
            cbClientes.requestFocusInWindow();
        }

        // se quiser esconder visualmente, dá pra usar setVisible,
        // mas aqui só desabilitar já resolve sem bagunçar layout.
    }

    private void carregarClientesNoCombo() {
        try {
            List<String> nomes = clienteDAO.listarTodosNomes();
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();

            // opcional: deixa “Avulso” explícito pra quem não quer cadastrar
            m.addElement("AVULSO");

            for (String n : nomes) m.addElement(n);

            cbClientes.setModel(m);
            cbClientes.setSelectedItem(nomes.isEmpty() ? "AVULSO" : nomes.get(0));
        } catch (Exception e) {
            // sem drama, mas avisa
            cbClientes.setModel(new DefaultComboBoxModel<>(new String[]{"AVULSO"}));
        }
    }

    private void abrirCadastroCliente() {
        try {
            Window w = SwingUtilities.getWindowAncestor(this);
            ClienteCadastroDialog dlg = new ClienteCadastroDialog(w, null);
            dlg.setVisible(true);

            // atualiza lista depois do cadastro
            List<String> nomes = clienteDAO.listarTodosNomes();
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement("AVULSO");
            for (String n : nomes) m.addElement(n);

            cbClientes.setModel(m);

            // seleciona o último cadastrado por padrão
            if (!nomes.isEmpty()) {
                cbClientes.setSelectedItem(nomes.get(nomes.size() - 1));
            } else {
                cbClientes.setSelectedItem("AVULSO");
            }

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
                clienteId = "AVULSO"; // pra não estourar FK
                nomeCliente = trimOrNull(tfNomeAvulso.getText());
            } else {
                String nomeSelecionado = null;
                Object item = cbClientes.getEditor().getItem();
                if (item != null) nomeSelecionado = item.toString();

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
                    nomeCliente = nomeSelecionado; // opcional, mas ajuda no histórico
                }
            }

            String mesa = trimOrNull(tfMesa.getText());
            String obs = trimOrNull(taObs.getText());

            int id = comandaService.abrirComanda(
                    clienteId,
                    nomeCliente,
                    mesa,
                    obs,
                    usuario
            );

            this.comandaIdCriada = id;
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    public Integer getComandaIdCriada() {
        return comandaIdCriada;
    }
}
