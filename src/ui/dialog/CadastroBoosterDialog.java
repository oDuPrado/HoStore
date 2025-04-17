package ui.dialog;

import controller.ProdutoEstoqueController;
import dao.BoosterDAO;
import model.BoosterModel;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class CadastroBoosterDialog extends JDialog {

    // Campos específicos
    private final JTextField tfNome          = new JTextField();
    private final JTextField tfColecao       = new JTextField();
    private final JTextField tfSet           = new JTextField();
    private final JComboBox<String> cbTipo   = new JComboBox<>(
        new String[]{"Unitário","Quadri‑pack","Triple‑pack","Especial","Blister"}
    );
    private final JTextField tfIdioma        = new JTextField();
    private final JFormattedTextField tfValidade    = FormatterFactory.getFormattedDateField();
    private final JTextField tfCodigoBarras = new JTextField();

    // Campos genéricos padronizados
    private final JFormattedTextField tfQtd   = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getFormattedDoubleField(0.0);
    private final JTextField tfFornec        = new JTextField();

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public CadastroBoosterDialog(JFrame owner) {
        super(owner, "Novo Booster", true);
        setLayout(new GridLayout(0,2,8,8));

        // Monta o formulário
        add(new JLabel("Nome:"));              add(tfNome);
        add(new JLabel("Coleção:"));           add(tfColecao);
        add(new JLabel("Set:"));               add(tfSet);
        add(new JLabel("Tipo:"));              add(cbTipo);
        add(new JLabel("Idioma:"));            add(tfIdioma);
        add(new JLabel("Validade (DD/MM/AAAA):")); add(tfValidade);
        add(new JLabel("Código de Barras:"));  add(tfCodigoBarras);

        add(new JLabel("Quantidade:"));        add(tfQtd);
        add(new JLabel("Custo (R$):"));        add(tfCusto);
        add(new JLabel("Preço Venda (R$):"));  add(tfPreco);
        add(new JLabel("Fornecedor:"));        add(tfFornec);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel()); // espaço
        add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void salvar() {
        try {
            // ID gerado automaticamente
            String id = UUID.randomUUID().toString();

            BoosterModel b = new BoosterModel(
                id,
                tfNome.getText().trim(),
                Integer.parseInt(tfQtd.getValue().toString()),
                ((Number)tfCusto.getValue()).doubleValue(),
                ((Number)tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                tfColecao.getText().trim(),
                tfSet.getText().trim(),
                (String)cbTipo.getSelectedItem(),
                tfIdioma.getText().trim(),
                tfValidade.getText().trim(),
                tfCodigoBarras.getText().trim()
            );

            new BoosterDAO().insert(b);
            ctrl.listar("");  // força refresh
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar Booster:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
