package ui.ajustes.dialog;
import javax.swing.*;
import java.awt.*;

public abstract class AbstractCrudPainel extends JPanel {

    protected JTable tabela = new JTable();

    public AbstractCrudPainel() {
        setLayout(new BorderLayout(8,8));
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add   = new JButton("Adicionar");
        JButton edit  = new JButton("Editar");
        JButton del   = new JButton("Remover");

        add.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Adicionar em construção"));
        edit.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Editar em construção"));
        del.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Remover em construção"));

        botoes.add(add); botoes.add(edit); botoes.add(del);
        add(botoes, BorderLayout.NORTH);
    }

    /** Abre em diálogo próprio */
    public void abrir() {
        JDialog d = new JDialog((Frame) null, getTitulo(), true);
        d.setContentPane(this);
        d.setSize(600,400);
        d.setLocationRelativeTo(null);
        d.setVisible(true);
    }

    protected abstract String getTitulo();
}
