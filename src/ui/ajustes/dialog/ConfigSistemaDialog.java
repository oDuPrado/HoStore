package ui.ajustes.dialog;

import javax.swing.*;
import java.awt.*;

public class ConfigSistemaDialog extends JDialog {
    public ConfigSistemaDialog(JFrame owner) {
        super(owner, "Backup / Sistema", true);
        setLayout(new BorderLayout());
        add(new JLabel("Ponto para configurar backup automático, sincronização etc.",
                SwingConstants.CENTER), BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        add(ok, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }
}
