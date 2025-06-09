package ui.ajustes.dialog;

import javax.swing.JDialog;
import ui.ajustes.painel.CartaAtributosPanel;
import java.awt.Frame;

/**
 * @author Marco
 * Janela modal que agrupa todos os atributos de carta (idiomas, condições e tipos/subtipos)
 */
public class CartaAtributosDialog extends JDialog {
    // Painel principal com as abas
    private CartaAtributosPanel painel;

    /**
     * Construtor: recebe o frame pai para centralizar a janela
     */
    public CartaAtributosDialog(Frame owner) {
        super(owner, "Atributos da Carta", true);      // true = modal
        initComponents();
    }

    /**
     * Inicializa componentes da UI
     */
    private void initComponents() {
        // Instancia o painel unificado
        painel = new CartaAtributosPanel();
        setContentPane(painel);
        pack();                                        // ajusta tamanho ao conteúdo
        setLocationRelativeTo(getOwner());             // centra sobre o frame pai
    }

    // Ctrl+F: // @TODO: CARTA_ATRIBUTOS_DIALOG
    // cole este arquivo em src/ui/ajustes/dialog/CartaAtributosDialog.java
}
