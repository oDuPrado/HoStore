package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import model.AcessorioModel;
import util.MaskUtils;

import javax.swing.*;
import java.awt.*;

public class CadastroAcessorioDialog extends JDialog {

    private JTextField tfId   = new JTextField();
    private JTextField tfNome = new JTextField();
    private JComboBox<String> cbTipo = new JComboBox<>(
        new String[]{"Chaveiro","Dados","Playmat","Outro"});
    private JTextField tfQtd  = new JTextField("0");
    private JFormattedTextField tfCusto = MaskUtils.moneyField(0.00);
    private JFormattedTextField tfPreco = MaskUtils.moneyField(0.00);
    private JTextField tfFornec = new JTextField();

    private ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public CadastroAcessorioDialog(JFrame owner) {
        super(owner, "Novo Acessório", true);
        setLayout(new GridLayout(0,2,8,8));

        add(new JLabel("ID:"));   add(tfId);
        add(new JLabel("Nome:")); add(tfNome);
        add(new JLabel("Tipo:")); add(cbTipo);
        add(new JLabel("Quantidade:")); add(tfQtd);
        add(new JLabel("Custo:"));      add(tfCusto);
        add(new JLabel("Preço Venda:"));add(tfPreco);
        add(new JLabel("Fornecedor:")); add(tfFornec);

        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvar());
        add(new JLabel()); add(salvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void salvar() {
        try {
            AcessorioModel a = new AcessorioModel(
                tfId.getText().trim(),
                tfNome.getText().trim(),
                Integer.parseInt(tfQtd.getText().trim()),
                ((Number) tfCusto.getValue()).doubleValue(),
                ((Number) tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                (String) cbTipo.getSelectedItem()
            );
            new dao.AcessorioDAO().insert(a);
            ctrl.listar("");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }
}
