package ui.dialog;

import controller.VendaController;
import factory.VendaFactory;
import ui.PainelVendas;
import util.AlertUtils;

import javax.swing.*;
import java.awt.*;

/** Diálogo de confirmação final da venda */
public class VendaFinalizarDialog extends JDialog {

    private final JComboBox<String> formaPG;
    private final JTextField parcelasField = new JTextField("1", 3);

    public VendaFinalizarDialog(JDialog owner, VendaController controller,
                                String clienteId, PainelVendas painelPai) {
        super(owner, "Finalizar Venda", true);
        setSize(400, 250);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        double total = controller.getCarrinho().stream()
                .mapToDouble(i -> i.getPreco() * i.getQtd()).sum();

        // Resumo
        JTextArea resumo = new JTextArea(
                String.format("Cliente: %s%nItens: %d%nTotal: R$ %.2f",
                        clienteId, controller.getCarrinho().size(), total));
        resumo.setEditable(false);
        add(resumo, BorderLayout.CENTER);

        // Pagamento
        JPanel pgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formaPG = new JComboBox<>(new String[]{"DINHEIRO", "CARTAO", "PIX"});
        pgPanel.add(new JLabel("Forma PG:"));
        pgPanel.add(formaPG);
        pgPanel.add(new JLabel("Parcelas:"));
        pgPanel.add(parcelasField);

        add(pgPanel, BorderLayout.NORTH);

        // Botões
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmar = criarBotao("Confirmar");
        confirmar.addActionListener(e -> {
            try {
                int parcelas = Integer.parseInt(parcelasField.getText().trim());
                int idVenda = controller.finalizar(
                        clienteId,
                        formaPG.getSelectedItem().toString(),
                        parcelas
                );
                AlertUtils.info("Venda #" + idVenda + " concluída com sucesso!");
                painelPai.carregarVendas(null);
                dispose();
                owner.dispose();
            } catch (Exception ex) {
                AlertUtils.error("Erro ao finalizar venda:\n" + ex.getMessage());
            }
        });
        btns.add(confirmar);

        JButton cancelar = criarBotao("Cancelar");
        cancelar.addActionListener(e -> dispose());
        btns.add(cancelar);

        add(btns, BorderLayout.SOUTH);
    }

    /** Estilo dark padronizado */
    private JButton criarBotao(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}
