package ui.ajustes.dialog;

import util.UiKit;
import javax.swing.*;
import java.awt.*;

public class ConfigFinanceiroDialog extends JDialog {
    public ConfigFinanceiroDialog(JFrame owner) {
        super(owner, "Configurações Financeiras", true);
        UiKit.applyDialogBase(this);
        setLayout(new BorderLayout());
        add(new JLabel("Defina taxas, juros e parâmetros financeiros aqui.",
                SwingConstants.CENTER), BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        add(ok, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }
}
