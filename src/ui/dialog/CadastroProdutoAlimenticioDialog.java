package ui.dialog;

import controller.ProdutoEstoqueController;
import dao.ProdutoAlimenticioDAO;
import model.ProdutoAlimenticioModel;
import util.MaskUtils;

import javax.swing.*;
import javax.swing.JFormattedTextField;
import java.awt.*;

public class CadastroProdutoAlimenticioDialog extends JDialog {

    private JTextField tfId        = new JTextField();
    private JTextField tfNome      = new JTextField();
    private JTextField tfQtd       = new JTextField("0");
    private JFormattedTextField tfValidade = MaskUtils.dateField();
    private JFormattedTextField tfCusto    = MaskUtils.moneyField(0.00);
    private JFormattedTextField tfPreco    = MaskUtils.moneyField(0.00);
    private JTextField tfFornec    = new JTextField();

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public CadastroProdutoAlimenticioDialog(JFrame owner) {
        super(owner, "Novo Produto Alimentício", true);
        setLayout(new GridLayout(0,2,8,8));

        add(new JLabel("ID:"));           add(tfId);
        add(new JLabel("Nome:"));         add(tfNome);
        add(new JLabel("Quantidade:"));   add(tfQtd);
        add(new JLabel("Validade (DD/MM/AAAA):")); add(tfValidade);
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
            ProdutoAlimenticioModel p = new ProdutoAlimenticioModel(
                tfId.getText().trim(),
                tfNome.getText().trim(),
                Integer.parseInt(tfQtd.getText().trim()),
                ((Number) tfCusto.getValue()).doubleValue(),
                ((Number) tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                tfValidade.getText().trim()
            );
            new ProdutoAlimenticioDAO().insert(p);
            ctrl.listar("");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }
}
