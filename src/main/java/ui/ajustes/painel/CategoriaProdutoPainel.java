package ui.ajustes.painel;

import util.UiKit;
import com.formdev.flatlaf.*;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class CategoriaProdutoPainel extends JPanel {

    private final JComboBox<String> temaCombo;
    public static final Map<String, LookAndFeel> TEMAS = new LinkedHashMap<>();
    private static final Preferences PREFS = Preferences.userNodeForPackage(CategoriaProdutoPainel.class);

    static {
        TEMAS.put("Claro (FlatLight)", new FlatLightLaf());
        TEMAS.put("Escuro (FlatDark)", new FlatDarkLaf());
        TEMAS.put("IntelliJ", new FlatIntelliJLaf());
        TEMAS.put("Darcula", new FlatDarculaLaf());
    }

    public CategoriaProdutoPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(12, 12));
        temaCombo = new JComboBox<>(TEMAS.keySet().toArray(new String[0]));

        // tema salvo anteriormente
        String temaSalvo = PREFS.get("temaSelecionado", "Claro (FlatLight)");
        temaCombo.setSelectedItem(temaSalvo);

        temaCombo.addActionListener(e -> {
            String selecionado = (String) temaCombo.getSelectedItem();
            LookAndFeel laf = TEMAS.get(selecionado);
            try {
                UIManager.setLookAndFeel(laf);
                SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
                PREFS.put("temaSelecionado", selecionado);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao aplicar o tema.");
            }
        });

        JPanel conteudo = new JPanel(new BorderLayout(6, 6));
        conteudo.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        conteudo.add(new JLabel("Tema Visual:"), BorderLayout.WEST);
        conteudo.add(temaCombo, BorderLayout.CENTER);

        add(conteudo, BorderLayout.NORTH);
    }

    public void abrir() {
        JDialog d = new JDialog((Frame) null, "Aparência do Sistema", true);
        d.setContentPane(this);
        d.setSize(480, 150);
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
