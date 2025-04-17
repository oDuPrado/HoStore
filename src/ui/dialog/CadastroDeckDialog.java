package ui.dialog;

import controller.ProdutoEstoqueController;
import dao.DeckDAO;
import model.DeckModel;
import util.MaskUtils;

import javax.swing.*;
import javax.swing.JFormattedTextField;
import java.awt.*;

public class CadastroDeckDialog extends JDialog {

    private JTextField tfId         = new JTextField();
    private JTextField tfNome       = new JTextField();
    private JTextField tfColecao    = new JTextField();
    private JComboBox<String> cbTipoDeck = new JComboBox<>(new String[]{
        "Pré‑montado","Liga"});
    private JComboBox<String> cbCategoria = new JComboBox<>(new String[]{
        "Estrela","2 Estrelas","3 Estrelas","Junior","Master"});
    private JTextField tfQtd        = new JTextField("0");
    private JFormattedTextField tfCusto   = MaskUtils.moneyField(0.00);
    private JFormattedTextField tfPreco   = MaskUtils.moneyField(0.00);
    private JTextField tfFornec     = new JTextField();

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public CadastroDeckDialog(JFrame owner) {
        super(owner, "Novo Deck", true);
        setLayout(new GridLayout(0,2,8,8));

        add(new JLabel("ID:"));           add(tfId);
        add(new JLabel("Nome:"));         add(tfNome);
        add(new JLabel("Coleção:"));      add(tfColecao);
        add(new JLabel("Tipo Deck:"));    add(cbTipoDeck);
        add(new JLabel("Categoria:"));    add(cbCategoria);
        add(new JLabel("Quantidade:"));   add(tfQtd);
        add(new JLabel("Custo (R$):"));   add(tfCusto);
        add(new JLabel("Preço Venda:"));  add(tfPreco);
        add(new JLabel("Fornecedor:"));   add(tfFornec);

        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvar());
        add(new JLabel());
        add(salvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void salvar() {
        try {
            DeckModel d = new DeckModel(
                tfId.getText().trim(),
                tfNome.getText().trim(),
                Integer.parseInt(tfQtd.getText().trim()),
                ((Number) tfCusto.getValue()).doubleValue(),
                ((Number) tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                tfColecao.getText().trim(),
                (String) cbTipoDeck.getSelectedItem(),
                (String) cbCategoria.getSelectedItem()
            );
            new DeckDAO().insert(d);
            ctrl.listar("");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }
}
