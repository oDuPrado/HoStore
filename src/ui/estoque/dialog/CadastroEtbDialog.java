package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import dao.EtbDAO;
import model.EtbModel;
import util.MaskUtils;

import javax.swing.*;
import javax.swing.JFormattedTextField;
import java.awt.*;

public class CadastroEtbDialog extends JDialog {

    private JTextField tfId        = new JTextField();
    private JTextField tfNome      = new JTextField();
    private JTextField tfColecao   = new JTextField();
    private JTextField tfSet       = new JTextField();
    private JComboBox<String> cbTipo = new JComboBox<>(new String[]{
        "Booster Box","Pokémon Center","ETB"});
    private JComboBox<String> cbVersao = new JComboBox<>(new String[]{
        "Nacional","Americana"});
    private JTextField tfQtd       = new JTextField("0");
    private JFormattedTextField tfCusto  = MaskUtils.moneyField(0.00);
    private JFormattedTextField tfPreco  = MaskUtils.moneyField(0.00);
    private JTextField tfFornec    = new JTextField();

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public CadastroEtbDialog(JFrame owner) {
        super(owner, "Novo ETB", true);
        setLayout(new GridLayout(0,2,8,8));

        add(new JLabel("ID:"));           add(tfId);
        add(new JLabel("Nome:"));         add(tfNome);
        add(new JLabel("Coleção:"));      add(tfColecao);
        add(new JLabel("Set:"));          add(tfSet);
        add(new JLabel("Tipo:"));         add(cbTipo);
        add(new JLabel("Versão:"));       add(cbVersao);
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
            EtbModel e = new EtbModel(
                tfId.getText().trim(),
                tfNome.getText().trim(),
                Integer.parseInt(tfQtd.getText().trim()),
                ((Number) tfCusto.getValue()).doubleValue(),
                ((Number) tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                tfColecao.getText().trim(),
                tfSet.getText().trim(),
                (String) cbTipo.getSelectedItem(),
                (String) cbVersao.getSelectedItem()
            );
            new EtbDAO().insert(e);
            ctrl.listar("");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }
}
