// ui/ajustes/dialog/LoginDialog.java
package ui.ajustes.dialog;

import dao.UsuarioDAO;
import model.UsuarioModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField tfUsuario = new JTextField(15);
    private final JPasswordField pfSenha = new JPasswordField(15);
    private final UsuarioDAO dao = new UsuarioDAO();
    private UsuarioModel usuarioLogado;

    public LoginDialog(Frame owner) {
        super(owner, "Login - HoStore", true);
        // 👉 Define cor de fundo geral como branca
        getContentPane().setBackground(Color.WHITE);

        // ============ Painel principal (container) ============
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(Color.WHITE); // 👉 fundo branco
        container.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // 👉 padding externo

        GridBagConstraints gbcContainer = new GridBagConstraints();
        gbcContainer.insets = new Insets(5, 5, 5, 5);
        gbcContainer.anchor = GridBagConstraints.CENTER;
        gbcContainer.fill = GridBagConstraints.HORIZONTAL;
        gbcContainer.gridx = 0;
        gbcContainer.gridy = 0;

        // ============ Cabeçalho (título) ============
        JLabel lblTitle = new JLabel("Bem-vindo ao HoStore");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18)); // 👉 fonte maior e em negrito
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setForeground(new Color(50, 50, 50));

        gbcContainer.gridy = 0;
        gbcContainer.gridwidth = 2;
        container.add(lblTitle, gbcContainer);

        // ============ Painel interno com borda ============
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE); // 👉 fundo branco
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            "Faça login para continuar",
            TitledBorder.LEADING,
            TitledBorder.TOP,
            new Font("SansSerif", Font.PLAIN, 12),
            Color.DARK_GRAY
        )); // 👉 borda leve com título

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== Linha 1: Label "Usuário" =====
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblUsuario = new JLabel("Usuário:");
        lblUsuario.setFont(new Font("SansSerif", Font.BOLD, 12)); // 👉 negrito
        lblUsuario.setForeground(new Color(60, 60, 60));
        formPanel.add(lblUsuario, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfUsuario, gbc);

        // ===== Linha 2: Label "Senha" =====
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblSenha = new JLabel("Senha:");
        lblSenha.setFont(new Font("SansSerif", Font.BOLD, 12)); // 👉 negrito
        lblSenha.setForeground(new Color(60, 60, 60));
        formPanel.add(lblSenha, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(pfSenha, gbc);

        // --- Não insira mais o btnEntrar aqui! ---

        // ============ Adiciona formPanel dentro do container ============
        gbcContainer.gridy = 1;
        gbcContainer.gridwidth = 2;
        container.add(formPanel, gbcContainer);

        // ============ Painel de rodapé (Entrar + Cancelar) ============
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.setBackground(Color.WHITE); // mantém fundo branco

        // 👉 Botão “Cancelar”
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(100, 30));
        btnCancelar.addActionListener(e -> dispose());
        rodape.add(btnCancelar);

        // 👉 Botão “Entrar”
        JButton btnEntrar = new JButton("Entrar");
        btnEntrar.setPreferredSize(new Dimension(100, 30));
        // define “Entrar” como botão default (tecla Enter)
        getRootPane().setDefaultButton(btnEntrar);
        btnEntrar.addActionListener(e -> {
            try {
                String usr = tfUsuario.getText().trim();
                String pwd = new String(pfSenha.getPassword());
                // Atenção: verifique depois se DAO compara hash corretamente
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
        rodape.add(btnEntrar);

        // ============ Adiciona rodape ao container ============
        gbcContainer.gridy = 2;
        gbcContainer.gridwidth = 2;
        container.add(rodape, gbcContainer);

        // ============ Configurações finais do Dialog ============
        add(container);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public UsuarioModel getUsuarioLogado() {
        return usuarioLogado;
    }
}
