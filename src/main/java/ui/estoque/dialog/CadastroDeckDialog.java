// Caminho no projeto: src/ui/estoque/dialog/CadastroDeckDialog.java
package ui.estoque.dialog;

import dao.JogoDAO;
import model.DeckModel;
import model.JogoModel;
import service.ProdutoEstoqueService;
import util.MaskUtils;
import util.ScannerUtils; // <-- import necessário para o leitor de código de barras

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.JFormattedTextField;
import java.awt.*;
import java.util.List;
import java.util.UUID;

/**
 * Dialog para cadastro/edição de Decks, agora com seleção de Jogo (TCG) e leitor de código de barras.
 */
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
    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();

    // *** NOVO: Label que exibirá o código de barras lido (via scanner ou manual) ***
    private final JLabel lblCodigoLido = new JLabel("");

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
        // Painel principal com padding, 2 colunas
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        // Nome
        content.add(new JLabel("Nome:"));
        content.add(tfNome);

        // Jogo (novo campo)
        content.add(new JLabel("Jogo:"));
        content.add(cbJogo);
        carregarJogos(); // carrega lista de jogos no combo

        // Coleção
        content.add(new JLabel("Coleção:"));
        content.add(tfColecao);

        // Tipo de Deck
        content.add(new JLabel("Tipo Deck:"));
        content.add(cbTipoDeck);

        // Categoria
        content.add(new JLabel("Categoria:"));
        content.add(cbCategoria);

        // Quantidade
        content.add(new JLabel("Quantidade:"));
        content.add(tfQtd);

        // Custo
        content.add(new JLabel("Custo (R$):"));
        content.add(tfCusto);

        // Preço de Venda
        content.add(new JLabel("Preço Venda (R$):"));
        content.add(tfPreco);

        // Fornecedor
        content.add(new JLabel("Fornecedor:"));
        content.add(tfFornec);

        // *** NOVO: Seção Código de Barras ***
        content.add(new JLabel("Código de Barras:"));
        JPanel painelCodBarras = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnScanner = new JButton("Ler com Scanner");
        JButton btnManual = new JButton("Inserir Manualmente");

        painelCodBarras.add(btnScanner);
        painelCodBarras.add(btnManual);
        painelCodBarras.add(lblCodigoLido); // exibe o código lido
        content.add(painelCodBarras);

        // Ação para chamar o util de leitura
        btnScanner.addActionListener(e -> {
            ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", codigo -> {
                lblCodigoLido.setText(codigo);
                lblCodigoLido.setToolTipText(codigo);
                lblCodigoLido.putClientProperty("codigoBarras", codigo);
            });
        });

        // Ação para inserir manualmente via diálogo
        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty()) {
                String c = input.trim();
                lblCodigoLido.setText(c);
                lblCodigoLido.setToolTipText(c);
                lblCodigoLido.putClientProperty("codigoBarras", c);
            }
        });
        // *** Fim da seção Código de Barras ***

        // Botão Salvar/Atualizar
        content.add(new JLabel());
        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        content.add(btnSalvar);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void carregarJogos() {
        try {
            cbJogo.removeAllItems();
            cbJogo.addItem(new JogoModel(null, "Selecione..."));
            List<JogoModel> jogos = new JogoDAO().listarTodos();
            for (JogoModel jogo : jogos) {
                cbJogo.addItem(jogo);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar jogos.");
        }
    }

    private void preencherCampos() {
        tfNome.setText(deckOrig.getNome());
        tfColecao.setText(deckOrig.getColecao());
        cbTipoDeck.setSelectedItem(deckOrig.getTipoDeck());
        cbCategoria.setSelectedItem(deckOrig.getCategoria());
        tfQtd.setValue(deckOrig.getQuantidade());
        tfCusto.setValue(deckOrig.getPrecoCompra());
        tfPreco.setValue(deckOrig.getPrecoVenda());
        tfFornec.setText(deckOrig.getFornecedor());

        // Selecionar jogo no combo
        String jogoId = deckOrig.getJogoId();
        if (jogoId != null) {
            for (int i = 0; i < cbJogo.getItemCount(); i++) {
                JogoModel jm = cbJogo.getItemAt(i);
                if (jm.getId() != null && jm.getId().equals(jogoId)) {
                    cbJogo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Se o modelo DeckModel já possuísse campo “codigoBarras”, poderíamos preencher:
        // String codigoExistente = deckOrig.getCodigoBarras();
        // lblCodigoLido.setText(codigoExistente);
        // lblCodigoLido.putClientProperty("codigoBarras", codigoExistente);
    }

    private void salvar() {
        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório.");
            return;
        }
        if (tfFornec.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fornecedor é obrigatório.");
            return;
        }
        JogoModel jogoSel = (JogoModel) cbJogo.getSelectedItem();
        if (jogoSel == null || jogoSel.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecione um jogo.");
            return;
        }

        try {
            String id = isEdicao
                ? deckOrig.getId()
                : UUID.randomUUID().toString();

            // Recupera o código de barras lido (se houver); caso contrário, deixa em branco.
            String codigoBarras = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigoBarras == null) {
                codigoBarras = "";
            }
            // (No momento, não estamos passando esse código para o model. 
            // Se quiser persistir, inclua no construtor de DeckModel e no DAO.)

            DeckModel d = new DeckModel(
                id,
                tfNome.getText().trim(),
                ((Number) tfQtd.getValue()).intValue(),
                ((Number) tfCusto.getValue()).doubleValue(),
                ((Number) tfPreco.getValue()).doubleValue(),
                tfFornec.getText().trim(),
                tfColecao.getText().trim(),
                (String) cbTipoDeck.getSelectedItem(),
                (String) cbCategoria.getSelectedItem(),
                jogoSel.getId()             // passa jogoId
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
