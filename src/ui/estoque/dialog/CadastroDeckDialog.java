package ui.estoque.dialog;

import model.DeckModel;
import service.ProdutoEstoqueService;
import util.MaskUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.JFormattedTextField;
import java.awt.*;
import java.util.UUID;

public class CadastroDeckDialog extends JDialog {

    private final boolean isEdicao;
    private final DeckModel deckOrig;

    private final JTextField tfNome = new JTextField(20);
    private final JTextField tfColecao = new JTextField(20);
    private final JComboBox<String> cbTipoDeck = new JComboBox<>(new String[]{
        "Pré-montado", "Liga"
    });
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{
        "Estrela", "2 Estrelas", "3 Estrelas", "Junior", "Master"
    });
    private final JFormattedTextField tfQtd    = MaskUtils.getFormattedIntField(0);
    private final JFormattedTextField tfCusto  = MaskUtils.moneyField(0.0);
    private final JFormattedTextField tfPreco  = MaskUtils.moneyField(0.0);
    private final JTextField tfFornec = new JTextField(20);

    public CadastroDeckDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroDeckDialog(JFrame owner, DeckModel deck) {
        super(owner, deck == null ? "Novo Deck" : "Editar Deck", true);
        this.isEdicao  = deck != null;
        this.deckOrig  = deck;
        buildUI();
        if (isEdicao) preencherCampos();
    }

    private void buildUI() {
        // Painel principal com padding igual ao BoosterDialog
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        // Campos
        content.add(new JLabel("Nome:"));
        content.add(tfNome);

        content.add(new JLabel("Coleção:"));
        content.add(tfColecao);

        content.add(new JLabel("Tipo Deck:"));
        content.add(cbTipoDeck);

        content.add(new JLabel("Categoria:"));
        content.add(cbCategoria);

        content.add(new JLabel("Quantidade:"));
        content.add(tfQtd);

        content.add(new JLabel("Custo (R$):"));
        content.add(tfCusto);

        content.add(new JLabel("Preço Venda (R$):"));
        content.add(tfPreco);

        content.add(new JLabel("Fornecedor:"));
        content.add(tfFornec);

        // Botão Salvar/Atualizar
        content.add(new JLabel());
        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        content.add(btnSalvar);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void preencherCampos() {
        tfNome.setText(deckOrig.getNome());
        tfColecao.setText(deckOrig.getColecao());
        cbTipoDeck.setSelectedItem(deckOrig.getTipoDeck());
        cbCategoria.setSelectedItem(deckOrig.getCategoria());
        tfQtd.setValue(deckOrig.getQuantidade());
        tfCusto.setValue(deckOrig.getPrecoCompra());
        tfPreco.setValue(deckOrig.getPrecoVenda());
        // supondo que DeckModel tenha getFornecedor()
        tfFornec.setText(deckOrig.getFornecedor());
    }

    private void salvar() {
        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório.");
            return;
        }

        try {
            String id = isEdicao
                ? deckOrig.getId()
                : UUID.randomUUID().toString();

            DeckModel d = new DeckModel(
                id,
                tfNome.getText().trim(),
                ((Number) tfQtd.getValue()).intValue(),
                ((Number) tfCusto.getValue()).doubleValue(),
                ((Number) tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                tfColecao.getText().trim(),
                (String) cbTipoDeck.getSelectedItem(),
                (String) cbCategoria.getSelectedItem()
            );

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) {
                service.atualizarDeck(d);
            } else {
                service.salvarNovoDeck(d);
            }

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar Deck:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
