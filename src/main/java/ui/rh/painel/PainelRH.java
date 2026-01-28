package ui.rh.painel;

import util.UiKit;

import javax.swing.*;
import java.awt.*;

public class PainelRH extends JPanel {

    public PainelRH() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(8, 4));
        topCard.add(UiKit.title("Gestao de RH"), BorderLayout.WEST);
        topCard.add(UiKit.hint("Funcionarios, ponto, escalas, folha, ferias e comissoes"), BorderLayout.EAST);
        add(topCard, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Funcionarios", new RhFuncionariosPanel());
        tabs.addTab("Cargos", new RhCargosPanel());
        tabs.addTab("Ponto", new RhPontoPanel());
        tabs.addTab("Escala", new RhEscalaPanel());
        tabs.addTab("Ferias/Abonos", new RhFeriasPanel());
        tabs.addTab("Comissoes", new RhComissoesPanel());
        tabs.addTab("Folha", new RhFolhaPanel());

        add(tabs, BorderLayout.CENTER);
    }
}
