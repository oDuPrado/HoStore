package ui.dialog;

import util.UiKit;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/* Retorna a categoria escolhida ou null se cancelar */
public class SelecionarCategoriaDialog extends JDialog {

    private String categoriaSelecionada;

    public SelecionarCategoriaDialog(Frame owner) {
        super(owner, "Selecionar categoria", true);
        UiKit.applyDialogBase(this);

        String[] categorias = {
            "Carta", "Booster", "Deck", "Acessório", "ETB",
            "Promo", "Comida/Bebida", "Outro"
        };

        JComboBox<String> combo = new JComboBox<>(categorias);
        JButton ok = new JButton("OK");
        ok.addActionListener((ActionEvent e) -> {
            categoriaSelecionada = (String) combo.getSelectedItem();
            dispose();
        });

        setLayout(new BorderLayout(10,10));
        add(new JLabel("Escolha a categoria:"), BorderLayout.NORTH);
        add(combo, BorderLayout.CENTER);
        add(ok, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public String getCategoriaSelecionada() { return categoriaSelecionada; }
}
