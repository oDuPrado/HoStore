package ui.ajustes.painel;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * Painel que contém abas para gerenciar idiomas, condições e tipos/subtipos de carta.
 * Reusa os painéis já existentes para CRUD.
 */
public class CartaAtributosPanel extends JPanel {
    private JTabbedPane abas;

    public CartaAtributosPanel() {
        super(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        abas = new JTabbedPane();

        // Aba 1: Idiomas
        abas.addTab("Idiomas", new IdiomaPainel());
        // Aba 2: Condições (NM, SP, LP, etc)
        abas.addTab("Condições", new CondicaoPainel());
        // Aba 3: Tipos/Subtipos de Carta
        abas.addTab("Tipos/Subtipos", new TipoCartaPainel());

        // adiciona o JTabbedPane ao centro do painel
        add(abas, BorderLayout.CENTER);
    }

    // Ctrl+F: // @TODO: CARTA_ATRIBUTOS_PANEL
    // cole este arquivo em src/ui/ajustes/painel/CartaAtributosPanel.java
}
