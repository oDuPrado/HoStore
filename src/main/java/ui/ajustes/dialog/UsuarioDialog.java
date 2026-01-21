// ui/ajustes/dialog/UsuarioDialog.java
package ui.ajustes.dialog;

import util.UiKit;
import dao.UsuarioDAO;
import model.UsuarioModel;
import util.SenhaUtils; // ⚠️ IMPORTANTE: adicionar isso

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class UsuarioDialog extends JDialog {
    private final JTextField tfNome    = new JTextField(20);
    private final JTextField tfUsuario = new JTextField(20);
    private final JPasswordField pfSenha  = new JPasswordField(20);
    private final JComboBox<String> cbTipo  = new JComboBox<>(
        new String[]{"Admin","Gerente","Vendedor","Estoquista","Financeiro"}
    );
    private final JCheckBox chkAtivo   = new JCheckBox("Ativo");
    private final UsuarioDAO dao       = new UsuarioDAO();
    private final boolean isEdit;
    private final String id;

    public UsuarioDialog(Frame owner, UsuarioModel u) {
        super(owner, u==null?"Novo Usuário":"Editar Usuário", true);
        UiKit.applyDialogBase(this);
        isEdit = u != null;
        id     = isEdit ? u.getId() : UUID.randomUUID().toString();

        if (isEdit) {
            tfNome.setText(u.getNome());
            tfUsuario.setText(u.getUsuario());
            cbTipo.setSelectedItem(u.getTipo());
            chkAtivo.setSelected(u.isAtivo());
        } else {
            chkAtivo.setSelected(true);
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        int y=0;
        addRow(form,gbc,y++,"Nome:",    tfNome);
        addRow(form,gbc,y++,"Usuário:", tfUsuario);
        addRow(form,gbc,y++,"Senha:",   pfSenha);
        addRow(form,gbc,y++,"Tipo:",    cbTipo);
        gbc.gridy=y; gbc.gridx=0; gbc.gridwidth=2;
        form.add(chkAtivo, gbc);

        JButton btnSalvar   = new JButton("Salvar");
        JButton btnCancelar = new JButton("Cancelar");
        btnSalvar.addActionListener(e -> onSave());
        btnCancelar.addActionListener(e -> dispose());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT,4,4));
        rodape.add(btnCancelar);
        rodape.add(btnSalvar);

        setLayout(new BorderLayout());
        add(form,   BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int y,
                        String label, JComponent comp) {
        gbc.gridy = y; gbc.gridx = 0; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(comp, gbc);
    }

    private void onSave() {
        String nome    = tfNome.getText().trim();
        String usuario = tfUsuario.getText().trim();
        String senhaOriginal = new String(pfSenha.getPassword()).trim();
        String tipo    = (String) cbTipo.getSelectedItem();
        boolean ativo  = chkAtivo.isSelected();

        if (nome.isEmpty() || usuario.isEmpty() || senhaOriginal.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Preencha todos os campos", "Atenção",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            // Aplica o hash na senha ANTES de salvar
            String senhaHash = SenhaUtils.hashSenha(senhaOriginal);

            UsuarioModel usr = new UsuarioModel(
                id, nome, usuario, senhaHash, tipo, ativo
            );

            if (isEdit) dao.atualizar(usr);
            else        dao.inserir(usr);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
