package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import model.ProdutoModel;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ProdutoCadastroDialog extends JDialog {

    private JTextField tfId, tfNome, tfQtd, tfPrecoCompra, tfPrecoVenda, tfFornecedor;
    private JComboBox<String> cbCategoria;
    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();
    private ProdutoModel atual; // null = novo

    public ProdutoCadastroDialog(Frame owner, ProdutoModel produto) {
        super(owner, true);
        this.atual = produto;
        setTitle(atual == null ? "Novo Produto" : "Editar Produto");

        initUI();
        if (atual != null) preencherCampos(atual);

        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new GridLayout(0,2,8,8));

        tfId           = new JTextField();
        tfNome         = new JTextField();
        cbCategoria    = new JComboBox<>(new String[]{
            "Carta","Booster","Deck","Acessório","ETB","Promo",
            "Comida/Bebida","Outro"});
        tfQtd          = new JTextField("0");
        tfPrecoCompra  = new JTextField("0.00");
        tfPrecoVenda   = new JTextField("0.00");
        tfFornecedor   = new JTextField();

        add(new JLabel("Código / SKU:")); add(tfId);
        add(new JLabel("Nome:"));         add(tfNome);
        add(new JLabel("Categoria:"));    add(cbCategoria);
        add(new JLabel("Quantidade:"));   add(tfQtd);
        add(new JLabel("Preço compra:")); add(tfPrecoCompra);
        add(new JLabel("Preço venda:"));  add(tfPrecoVenda);
        add(new JLabel("Fornecedor:"));   add(tfFornecedor);

        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvar());
        add(new JLabel()); // filler
        add(salvar);
    }

    private void preencherCampos(ProdutoModel p) {
        tfId.setText(p.getId()); tfId.setEditable(false);
        tfNome.setText(p.getNome());
        cbCategoria.setSelectedItem(p.getCategoria());
        tfQtd.setText(String.valueOf(p.getQuantidade()));
        tfPrecoCompra.setText(String.valueOf(p.getPrecoCompra()));
        tfPrecoVenda.setText(String.valueOf(p.getPrecoVenda()));
        tfFornecedor.setText(p.getFornecedor());
    }

    private void salvar() {
        try {
            String id   = tfId.getText().trim();
            String nome = tfNome.getText().trim();
            String cat  = (String) cbCategoria.getSelectedItem();
            int    qtd  = Integer.parseInt(tfQtd.getText().trim());
            double pc   = Double.parseDouble(tfPrecoCompra.getText().trim());
            double pv   = Double.parseDouble(tfPrecoVenda.getText().trim());
            String forn = tfFornecedor.getText().trim();

            if (id.isEmpty() || nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID e Nome são obrigatórios.");
                return;
            }

            ProdutoModel p = new ProdutoModel(id, nome, cat, qtd, pc, pv, forn);
            ctrl.salvar(p);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }
}
