package ui.ajustes.dialog;

import javax.swing.*;
import java.awt.*;

public class ConfigImpressaoDialog extends JDialog {
    public ConfigImpressaoDialog(JFrame owner) {
        super(owner, "Configurações de Impressão", true);
        setLayout(new BorderLayout(8,8));
        add(new JLabel("Configurações de impressão/PDF ainda não definidas.",
                SwingConstants.CENTER), BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        add(ok, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }
}
