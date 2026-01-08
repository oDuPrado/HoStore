package ui.ajustes.dialog;

import dao.UsuarioDAO;
import model.UsuarioModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class LoginDialog extends JDialog {
    private final JTextField tfUsuario = new JTextField(18);
    private final JPasswordField pfSenha = new JPasswordField(18);
    private final UsuarioDAO dao = new UsuarioDAO();
    private UsuarioModel usuarioLogado;

    public LoginDialog(Frame owner) {
        super(owner, "Login - HoStore", true);

        UiKit.applyDialogBase(this);

        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        // ===================== CARD =====================
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(12, 12));
        add(card, BorderLayout.CENTER);

        // ===================== HEADER =====================
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Bem-vindo ao HoStore"));
        header.add(UiKit.hint("Faça login para continuar"));
        card.add(header, BorderLayout.NORTH);

        // ===================== FORM =====================
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        card.add(form, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Labels alinhados (largura fixa)
        Dimension labelSize = new Dimension(90, 26);
        JLabel lblUsuario = new JLabel("Usuário:");
        JLabel lblSenha = new JLabel("Senha:");
        lblUsuario.setPreferredSize(labelSize);
        lblSenha.setPreferredSize(labelSize);

        // Campos consistentes
        Dimension fieldSize = new Dimension(280, 30);
        tfUsuario.setPreferredSize(fieldSize);
        tfUsuario.setMinimumSize(fieldSize);
        pfSenha.setPreferredSize(fieldSize);
        pfSenha.setMinimumSize(fieldSize);

        // Linha 0 - Usuário
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        form.add(lblUsuario, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(tfUsuario, gbc);

        // Linha 1 - Senha
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(lblSenha, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(pfSenha, gbc);

        // Hint
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 6, 2, 6);
        form.add(UiKit.hint("ENTER para entrar • ESC para cancelar"), gbc);

        // ===================== FOOTER BUTTONS =====================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        card.add(footer, BorderLayout.SOUTH);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        JButton btnEntrar = UiKit.primary("Entrar");

        btnCancelar.setPreferredSize(new Dimension(110, 32));
        btnEntrar.setPreferredSize(new Dimension(110, 32));

        // Default button (ENTER)
        getRootPane().setDefaultButton(btnEntrar);

        // ESC para fechar
        bindEscapeToClose();

        btnCancelar.addActionListener(e -> dispose());
        btnEntrar.addActionListener(e -> autenticar());

        footer.add(btnCancelar);
        footer.add(btnEntrar);

        // UX: foco inicial
        SwingUtilities.invokeLater(() -> {
            tfUsuario.requestFocusInWindow();
            tfUsuario.selectAll();
        });

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void bindEscapeToClose() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void autenticar() {
        String usr = tfUsuario.getText().trim();
        char[] pwdChars = pfSenha.getPassword();

        try {
            if (usr.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Informe o usuário.",
                        "Login", JOptionPane.WARNING_MESSAGE);
                tfUsuario.requestFocusInWindow();
                return;
            }
            if (pwdChars.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "Informe a senha.",
                        "Login", JOptionPane.WARNING_MESSAGE);
                pfSenha.requestFocusInWindow();
                return;
            }

            String pwd = new String(pwdChars); // DAO ainda exige String (vida triste)
            usuarioLogado = dao.buscarPorUsuarioESenha(usr, pwd);

            if (usuarioLogado != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Usuário ou senha inválidos.",
                        "Login", JOptionPane.ERROR_MESSAGE);
                pfSenha.requestFocusInWindow();
                pfSenha.selectAll();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao autenticar:\n" + ex.getMessage(),
                    "Login", JOptionPane.ERROR_MESSAGE);
        } finally {
            // limpa senha da memória (não é magia, mas é melhor que nada)
            Arrays.fill(pwdChars, '\0');
        }
    }

    public UsuarioModel getUsuarioLogado() {
        return usuarioLogado;
    }
}
