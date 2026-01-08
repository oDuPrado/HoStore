// src/ui/relatorios/dialog/InfoFonteDialog.java
package ui.relatorios.dialog;

import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class InfoFonteDialog extends JDialog {

    public InfoFonteDialog(Window owner, String titulo, String texto) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);

        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(560, 360));

        // Card principal
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.title(titulo), BorderLayout.WEST);
        header.add(UiKit.hint("Informações e fonte do indicador"), BorderLayout.SOUTH);

        JTextArea ta = new JTextArea(texto);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(false);
        ta.setBorder(new EmptyBorder(6, 6, 6, 6));

        card.add(header, BorderLayout.NORTH);
        card.add(UiKit.scroll(ta), BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton bt = UiKit.primary("Fechar");
        bt.addActionListener(e -> dispose());
        footer.add(bt);

        add(card, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
