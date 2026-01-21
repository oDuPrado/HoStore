package ui.estoque.dialog;

import util.UiKit;
import controller.ProdutoEstoqueController;
import model.ProdutoModel;
import util.FormatterFactory;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.util.UUID;

public class ProdutoCadastroDialog extends JDialog {
    private final boolean isEdicao;
    private final ProdutoModel produtoOrig;

    private final JTextField tfNome;
    private final JFormattedTextField tfQtd;
    private final JFormattedTextField tfCusto;
    private final JFormattedTextField tfPreco;

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public ProdutoCadastroDialog(JFrame owner, ProdutoModel produto) {
        super(owner, produto == null ? "Novo Produto" : "Editar Produto", true);
        UiKit.applyDialogBase(this);
        this.isEdicao     = produto != null;
        this.produtoOrig  = produto;

        // Cria e monta UI
        tfNome  = new JTextField(20);
        tfQtd   = FormatterFactory.getFormattedIntField(0);
        tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
        tfPreco = FormatterFactory.getFormattedDoubleField(0.0);

        initUI();
        if (isEdicao) preencherCampos(produto);

        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new GridLayout(0, 2, 8, 8));
        setResizable(false);

        // Nome
        add(new JLabel("Nome:"));
        add(tfNome);

        // Tipo fixo
        add(new JLabel("Tipo:"));
        add(new JLabel("Outro"));

        // Quantidade
        add(new JLabel("Quantidade:"));
        add(tfQtd);

        // Custo
        add(new JLabel("Custo (R$):"));
        add(tfCusto);

        // Preço de venda
        add(new JLabel("Preço Venda (R$):"));
        add(tfPreco);

        // Botão Salvar / Atualizar
        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel()); // filler
        add(btnSalvar);
    }

    private void preencherCampos(ProdutoModel p) {
        tfNome.setText(p.getNome());
        tfQtd.setValue(p.getQuantidade());
        tfCusto.setValue(p.getPrecoCompra());
        tfPreco.setValue(p.getPrecoVenda());
    }

    private void salvar() {
        try {
            String nome = tfNome.getText().trim();
            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O campo Nome é obrigatório.", "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String id = isEdicao
                ? produtoOrig.getId()
                : UUID.randomUUID().toString();

            int quantidade = ((Number) tfQtd.getValue()).intValue();
            double custo   = ((Number) tfCusto.getValue()).doubleValue();
            double venda   = ((Number) tfPreco.getValue()).doubleValue();

            ProdutoModel p = new ProdutoModel(id, nome, "Outro", quantidade, custo, venda);
            ctrl.salvar(p);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar produto:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
