package ui.relatorios.dialog;

import javax.swing.*;
import java.awt.*;

public class InfoFonteDialog extends JDialog {

    public InfoFonteDialog(Window owner, String titulo, String texto) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        setSize(520, 320);
        setLocationRelativeTo(owner);

        JTextArea ta = new JTextArea(texto);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);

        JButton bt = new JButton("Fechar");
        bt.addActionListener(e -> dispose());

        setLayout(new BorderLayout(10,10));
        add(new JScrollPane(ta), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(bt);
        add(south, BorderLayout.SOUTH);
    }
}
