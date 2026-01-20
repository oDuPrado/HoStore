package ui.estoque.dialog;

import dao.JogoDAO;
import dao.ProdutoDAO;
import model.DeckModel;
import model.JogoModel;
import model.FornecedorModel;
import service.ProdutoEstoqueService;
import util.MaskUtils;
import util.ScannerUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.UUID;

/**
 * Dialog para cadastro/edição de Decks, com seleção de Jogo (TCG)
 * e leitor de código de barras.
 *
 * Visual padronizado: Header + Card + Footer (sem GridLayout quebrado).
 */
public class CadastroDeckDialog extends JDialog {

    private final boolean isEdicao;
    private final DeckModel deckOrig;

    private final JTextField tfNome = new JTextField(24);
    private final JTextField tfColecao = new JTextField(24);

    private final JComboBox<String> cbTipoDeck = new JComboBox<>(new String[] {
            "Pré-montado", "Liga"
    });

    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[] {
            "Estrela", "2 Estrelas", "3 Estrelas", "Junior", "Master"
    });

    private final JFormattedTextField tfQtd = MaskUtils.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = MaskUtils.moneyField(0.0);
    private final JFormattedTextField tfPreco = MaskUtils.moneyField(0.0);

    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();

    private final JLabel lblCodigoLido = new JLabel("—");

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Selecionar…");
    private FornecedorModel fornecedorSel;

    public CadastroDeckDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroDeckDialog(JFrame owner, DeckModel deck) {
        super(owner, deck == null ? "Novo Deck" : "Editar Deck", true);
        UiKit.applyDialogBase(this);

        this.isEdicao = deck != null;
        this.deckOrig = deck;

        buildUI(owner);
        carregarJogos();

        if (isEdicao)
            preencherCampos();

        setMinimumSize(new Dimension(860, 560));
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI(JFrame owner) {
        setLayout(new BorderLayout(12, 12));

        // ===== Header =====
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title(isEdicao ? "Editar Deck" : "Novo Deck"));
        left.add(UiKit.hint("Decks • Jogo + fornecedor + código de barras"));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // ===== Form (Card) =====
        JPanel form = UiKit.card();
        form.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;

        addField(form, g, r++, "Nome:", tfNome);
        addField(form, g, r++, "Jogo:", cbJogo);
        addField(form, g, r++, "Coleção:", tfColecao);
        addField(form, g, r++, "Tipo Deck:", cbTipoDeck);
        addField(form, g, r++, "Categoria:", cbCategoria);

        // Valores em uma linha (mais “produto”, menos “planilha”)
        JPanel valores = new JPanel(new GridLayout(1, 3, 10, 0));
        valores.setOpaque(false);
        valores.add(labeledInline("Qtd:", tfQtd));
        valores.add(labeledInline("Custo (R$):", tfCusto));
        valores.add(labeledInline("Venda (R$):", tfPreco));
        addField(form, g, r++, "Valores:", valores);

        // Fornecedor (label + botão)
        JPanel fornRow = new JPanel(new BorderLayout(8, 0));
        fornRow.setOpaque(false);
        fornRow.add(lblFornecedor, BorderLayout.CENTER);
        fornRow.add(btnSelectFornec, BorderLayout.EAST);
        addField(form, g, r++, "Fornecedor:", fornRow);

        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog((JFrame) getOwner());
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });

        // Código de barras (tema-friendly, sem Color.GRAY)
        JPanel barcodeRow = new JPanel(new BorderLayout(8, 0));
        barcodeRow.setOpaque(false);

        JPanel barcodeActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barcodeActions.setOpaque(false);

        JButton btnScanner = UiKit.ghost("📷 Ler com Scanner");
        JButton btnManual = UiKit.ghost("⌨ Inserir Manualmente");

        barcodeActions.add(btnScanner);
        barcodeActions.add(btnManual);

        lblCodigoLido.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lblCodigoLido.setOpaque(true);
        lblCodigoLido.setBackground(UIManager.getColor("TextField.background"));
        lblCodigoLido.setForeground(UIManager.getColor("TextField.foreground"));

        JPanel codeBox = new JPanel(new BorderLayout());
        codeBox.setOpaque(false);
        codeBox.add(lblCodigoLido, BorderLayout.CENTER);
        codeBox.setPreferredSize(new Dimension(220, 34));

        barcodeRow.add(barcodeActions, BorderLayout.WEST);
        barcodeRow.add(codeBox, BorderLayout.EAST);

        addField(form, g, r++, "Código de Barras:", barcodeRow);

        btnScanner.addActionListener(
                e -> ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", this::setCodigoBarras));

        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty())
                setCodigoBarras(input.trim());
        });

        JScrollPane sp = UiKit.scroll(form);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // ===== Footer =====
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());
        footer.add(UiKit.hint("Dica: leia o código de barras pra evitar digitação e erro humano."), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btCancelar = UiKit.ghost("Cancelar");
        JButton btSalvar = UiKit.primary(isEdicao ? "Atualizar" : "Salvar");

        btCancelar.addActionListener(e -> dispose());
        btSalvar.addActionListener(e -> salvar());

        actions.add(btCancelar);
        actions.add(btSalvar);

        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private void setCodigoBarras(String codigo) {
        lblCodigoLido.setText(codigo);
        lblCodigoLido.setToolTipText(codigo);
        lblCodigoLido.putClientProperty("codigoBarras", codigo);
        lblCodigoLido.revalidate();
        lblCodigoLido.repaint();
    }

    private void carregarJogos() {
        try {
            cbJogo.removeAllItems();
            cbJogo.addItem(new JogoModel(null, "Selecione..."));
            List<JogoModel> jogos = new JogoDAO().listarTodos();
            for (JogoModel jogo : jogos)
                cbJogo.addItem(jogo);
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

        String cod = deckOrig.getCodigoBarras();
        if (cod != null && !cod.isBlank())
            setCodigoBarras(cod);

        // fornecedor
        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(deckOrig.getFornecedor());
        fornecedorSel.setNome(deckOrig.getFornecedor());
        lblFornecedor.setText(deckOrig.getFornecedor());

        // jogo
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
    }

    private void salvar() {
        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório.");
            return;
        }
        if (fornecedorSel == null) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor.");
            return;
        }
        JogoModel jogoSel = (JogoModel) cbJogo.getSelectedItem();
        if (jogoSel == null || jogoSel.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecione um jogo.");
            return;
        }

        try {
            String id = isEdicao ? deckOrig.getId() : UUID.randomUUID().toString();

            String codigoBarras = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigoBarras == null)
                codigoBarras = "";

            int duplicados = new ProdutoDAO().contarPorCodigoBarrasAtivo(codigoBarras, id);
            if (duplicados > 0) {
                JOptionPane.showMessageDialog(this,
                        "Este codigo ja existe em " + duplicados
                                + " produtos. Na venda, sera necessario selecionar qual produto.",
                        "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }

            DeckModel d = new DeckModel(
                    id,
                    tfNome.getText().trim(),
                    ((Number) tfQtd.getValue()).intValue(),
                    ((Number) tfCusto.getValue()).doubleValue(),
                    ((Number) tfPreco.getValue()).doubleValue(),
                    fornecedorSel.getNome(),
                    tfColecao.getText().trim(),
                    (String) cbTipoDeck.getSelectedItem(),
                    (String) cbCategoria.getSelectedItem(),
                    jogoSel.getId());

            d.setCodigoBarras(codigoBarras);
            d.setFornecedorId(fornecedorSel.getId());

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao)
                service.atualizarDeck(d);
            else
                service.salvarNovoDeck(d);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar Deck:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== helpers de layout =====

    private void addField(JPanel parent, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        parent.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        parent.add(field, g);
    }

    private JPanel labeledInline(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }
}
