package ui.eventos.dialog;

import util.UiKit;

import javax.swing.*;
import java.awt.*;

public class EventoInscricaoOpcaoDialog extends JDialog {

    public enum Opcao {
        PAGAR_AGORA,
        ADICIONAR_COMANDA,
        CANCELAR
    }

    private Opcao opcao = Opcao.CANCELAR;

    public EventoInscricaoOpcaoDialog(Window owner) {
        super(owner, "Pagamento da Inscricao", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);
        setSize(520, 220);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildButtonsCard(), BorderLayout.CENTER);
    }

    public Opcao getOpcao() {
        return opcao;
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Como deseja pagar?"));
        left.add(UiKit.hint("A inscricao pode ser paga agora ou vinculada a uma comanda"));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JPanel buildButtonsCard() {
        JPanel card = UiKit.card();
        card.setLayout(new GridLayout(1, 2, 12, 12));

        JButton btnPagar = UiKit.primary("Pagar Agora");
        btnPagar.setPreferredSize(new Dimension(200, 80));
        btnPagar.addActionListener(e -> {
            opcao = Opcao.PAGAR_AGORA;
            dispose();
        });

        JButton btnComanda = UiKit.ghost("Adicionar na Comanda");
        btnComanda.setPreferredSize(new Dimension(200, 80));
        btnComanda.addActionListener(e -> {
            opcao = Opcao.ADICIONAR_COMANDA;
            dispose();
        });

        card.add(btnPagar);
        card.add(btnComanda);
        return card;
    }
}
