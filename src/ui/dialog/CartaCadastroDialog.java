package ui.dialog;

import model.Carta;
import service.EstoqueService;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.UUID;

/** Modal para adicionar ou editar uma carta */
public class CartaCadastroDialog extends JDialog {

    private final EstoqueService estoqueService = new EstoqueService();
    private final boolean isEdicao;
    private Carta cartaOrig;

    // campos principais
    private final JTextField idField      = new JTextField(10);
    private final JTextField nomeField    = new JTextField(20);
    private final JTextField colecaoField = new JTextField(15);
    private final JTextField numField     = new JTextField(6);
    private final JSpinner   qtdSpin      = new JSpinner(new SpinnerNumberModel(0,0,9999,1));
    private final JTextField precoField   = new JTextField("0.00",8);
    private final JTextField custoField   = new JTextField("0.00",8);

    public CartaCadastroDialog(Frame owner, Carta cartaExistente) {
        super(owner, cartaExistente == null ? "Nova Carta" : "Editar Carta", true);
        this.isEdicao = cartaExistente != null;
        this.cartaOrig = cartaExistente;

        setSize(450, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8,8));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridLayout(0,2,4,4));

        form.add(new JLabel("ID:"));          form.add(idField);
        form.add(new JLabel("Nome:"));        form.add(nomeField);
        form.add(new JLabel("Coleção:"));     form.add(colecaoField);
        form.add(new JLabel("Número:"));      form.add(numField);
        form.add(new JLabel("Quantidade:"));  form.add(qtdSpin);
        form.add(new JLabel("Preço (R$):"));  form.add(precoField);
        form.add(new JLabel("Custo (R$):"));  form.add(custoField);

        add(form, BorderLayout.CENTER);

        JButton salvar = new JButton(isEdicao? "Atualizar":"Cadastrar");
        JButton cancelar = new JButton("Cancelar");
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(salvar); botoes.add(cancelar);
        add(botoes, BorderLayout.SOUTH);

        cancelar.addActionListener(e -> dispose());
        salvar.addActionListener(e -> onSalvar());

        if (isEdicao) preencherCampos();
        else idField.setText(UUID.randomUUID().toString());

        idField.setEditable(!isEdicao);
    }

    private void preencherCampos() {
        idField.setText(cartaOrig.getId());
        nomeField.setText(cartaOrig.getNome());
        colecaoField.setText(cartaOrig.getColecao());
        numField.setText(cartaOrig.getNumero());
        qtdSpin.setValue(cartaOrig.getQtd());
        precoField.setText(String.valueOf(cartaOrig.getPreco()));
        custoField.setText(String.valueOf(cartaOrig.getCusto()));
    }

    private void onSalvar() {
        try {
            Carta c = new Carta(
                idField.getText().trim(),
                nomeField.getText().trim(),
                colecaoField.getText().trim(),
                numField.getText().trim(),
                (Integer) qtdSpin.getValue(),
                Double.parseDouble(precoField.getText().replace(",",".")),
                Double.parseDouble(custoField.getText().replace(",",".")),
                "C1", "L1", false,"L-0001",
                "T1","ST1","R1","SR1","IL1"
            );

            if (isEdicao) estoqueService.atualizarCarta(c);
            else estoqueService.salvarNovaCarta(c);

            dispose();
        } catch (Exception ex) {
            AlertUtils.error("Erro ao salvar carta:\n"+ex.getMessage());
        }
    }
}
