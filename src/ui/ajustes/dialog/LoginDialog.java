package ui.ajustes.dialog;

import dao.UsuarioDAO;
import model.UsuarioModel;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField tfUsuario = new JTextField(15);
    private final JPasswordField pfSenha = new JPasswordField(15);
    private final UsuarioDAO dao = new UsuarioDAO();
    private UsuarioModel usuarioLogado;

    public LoginDialog(Frame owner) {
        super(owner, "Login", true);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx=0; gbc.gridy=0;
        p.add(new JLabel("Usuário:"), gbc);
        gbc.gridx=1;
        p.add(tfUsuario, gbc);

        gbc.gridx=0; gbc.gridy=1;
        p.add(new JLabel("Senha:"), gbc);
        gbc.gridx=1;
        p.add(pfSenha, gbc);

        JButton btnEntrar = new JButton("Entrar");
        btnEntrar.addActionListener(e -> {
            try {
                String usr = tfUsuario.getText().trim();
                String pwd = new String(pfSenha.getPassword());
                usuarioLogado = dao.buscarPorUsuarioESenha(usr, pwd);
                if (usuarioLogado != null) {
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Usuário ou senha inválidos",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Erro ao conectar ao banco",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx=1; gbc.gridy=2; gbc.anchor=GridBagConstraints.EAST;
        p.add(btnEntrar, gbc);

        add(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public UsuarioModel getUsuarioLogado() {
        return usuarioLogado;
    }
}
