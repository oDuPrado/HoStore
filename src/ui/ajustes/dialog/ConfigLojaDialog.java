package ui.ajustes.dialog;

import javax.swing.*;
import java.awt.*;

public class ConfigLojaDialog extends JDialog {

    private final JTextField tfNome = new JTextField();
    private final JTextField tfCnpj = new JTextField();
    private final JTextField tfFone = new JTextField();

    public ConfigLojaDialog(JFrame owner) {
        super(owner, "Dados da Loja", true);
        setLayout(new GridLayout(0, 2, 8, 8));

        add(new JLabel("Nome da Loja:")); add(tfNome);
        add(new JLabel("CNPJ:"));         add(tfCnpj);
        add(new JLabel("Telefone:"));     add(tfFone);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> {
            System.out.println("[ConfigLoja] Salvo: "
                + tfNome.getText() + " / " + tfCnpj.getText() + " / " + tfFone.getText());
            dispose();
        });
        add(new JLabel()); add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }
}
