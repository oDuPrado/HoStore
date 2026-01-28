package ui.estoque.dialog;

import dao.JogoDAO;
import dao.ProdutoDAO;
import dao.ConfigFiscalDefaultDAO;
import dao.FiscalCatalogDAO;
import model.DeckModel;
import model.JogoModel;
import model.ConfigFiscalModel;
import model.CodigoDescricaoModel;
import model.FornecedorModel;
import model.NcmModel;
import service.NcmService;
import service.ProdutoEstoqueService;
import ui.ajustes.dialog.FornecedorDialog;
import util.MaskUtils;
import util.FormatterFactory;
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

    private final JComboBox<String> cbNcm = new JComboBox<>();
    private final JComboBox<String> cbCfop = new JComboBox<>();
    private final JComboBox<String> cbCsosn = new JComboBox<>();
    private final JComboBox<String> cbOrigem = new JComboBox<>();
    private final JComboBox<String> cbUnidade = new JComboBox<>();

    private final JFormattedTextField tfQtd = MaskUtils.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getMoneyField(0.0);

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
        carregarNcms();
        carregarCombosFiscais();
        aplicarDefaultsFiscais();

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
        addField(form, g, r++, "NCM:", cbNcm);
        addField(form, g, r++, "CFOP:", cbCfop);
        addField(form, g, r++, "CSOSN:", cbCsosn);
        addField(form, g, r++, "Origem:", cbOrigem);
        addField(form, g, r++, "Unidade:", cbUnidade);

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
        
        JPanel fornButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        fornButtons.setOpaque(false);
        fornButtons.add(btnSelectFornec);
        
        JButton btnNovoFornec = new JButton("➕ Criar");
        btnNovoFornec.addActionListener(e -> criarNovoFornecedor((JFrame) getOwner()));
        fornButtons.add(btnNovoFornec);
        
        fornRow.add(fornButtons, BorderLayout.EAST);
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
        selecionarPorCodigoPrefix(cbNcm, deckOrig.getNcm());
        selecionarPorCodigoPrefix(cbCfop, deckOrig.getCfop());
        selecionarPorCodigoPrefix(cbCsosn, deckOrig.getCsosn());
        selecionarPorCodigoPrefix(cbOrigem, deckOrig.getOrigem());
        selecionarPorCodigoPrefix(cbUnidade, deckOrig.getUnidade());
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
            String ncm = firstToken((String) cbNcm.getSelectedItem());
            String cfop = firstToken((String) cbCfop.getSelectedItem());
            String csosn = firstToken((String) cbCsosn.getSelectedItem());
            String origem = firstToken((String) cbOrigem.getSelectedItem());
            String unidade = firstToken((String) cbUnidade.getSelectedItem());

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
                    UiKit.getIntValue(tfQtd, 0),
                    UiKit.getDoubleValue(tfCusto, 0.0),
                    UiKit.getDoubleValue(tfPreco, 0.0),
                    fornecedorSel.getNome(),
                    tfColecao.getText().trim(),
                    (String) cbTipoDeck.getSelectedItem(),
                    (String) cbCategoria.getSelectedItem(),
                    jogoSel.getId());

            d.setCodigoBarras(codigoBarras);
            d.setFornecedorId(fornecedorSel.getId());
            d.setNcm(ncm);
            d.setCfop(cfop);
            d.setCsosn(csosn);
            d.setOrigem(origem);
            d.setUnidade(unidade);

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

    private void carregarNcms() {
        try {
            cbNcm.removeAllItems();
            List<NcmModel> ncms = NcmService.getInstance().findAll();
            for (NcmModel n : ncms) {
                cbNcm.addItem(n.getCodigo() + " - " + n.getDescricao());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar NCMs:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarCombosFiscais() {
        try {
            cbCfop.removeAllItems();
            cbCsosn.removeAllItems();
            cbOrigem.removeAllItems();
            cbUnidade.removeAllItems();

            FiscalCatalogDAO dao = new FiscalCatalogDAO();
            for (CodigoDescricaoModel it : dao.findAll("cfop")) {
                cbCfop.addItem(it.getCodigo() + " - " + it.getDescricao());
            }
            for (CodigoDescricaoModel it : dao.findAll("csosn")) {
                cbCsosn.addItem(it.getCodigo() + " - " + it.getDescricao());
            }
            for (CodigoDescricaoModel it : dao.findAll("origem")) {
                cbOrigem.addItem(it.getCodigo() + " - " + it.getDescricao());
            }
            for (CodigoDescricaoModel it : dao.findAll("unidades")) {
                cbUnidade.addItem(it.getCodigo() + " - " + it.getDescricao());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar dados fiscais:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarDefaultsFiscais() {
        try {
            ConfigFiscalModel cfg = new ConfigFiscalDefaultDAO().getDefault();
            if (cfg == null)
                return;
            selecionarPorCodigoPrefix(cbCfop, cfg.getCfopPadrao());
            selecionarPorCodigoPrefix(cbCsosn, cfg.getCsosnPadrao());
            selecionarPorCodigoPrefix(cbOrigem, cfg.getOrigemPadrao());
            selecionarPorCodigoPrefix(cbNcm, cfg.getNcmPadrao());
            selecionarPorCodigoPrefix(cbUnidade, cfg.getUnidadePadrao());
        } catch (Exception ignored) {
        }
    }

    private void selecionarPorCodigoPrefix(JComboBox<String> combo, String codigo) {
        if (codigo == null || codigo.isBlank())
            return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String it = combo.getItemAt(i);
            if (it != null && it.startsWith(codigo + " ")) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String firstToken(String s) {
        if (s == null)
            return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private void criarNovoFornecedor(JFrame owner) {
        FornecedorDialog dlg = new FornecedorDialog(owner, null);
        dlg.setVisible(true);
    }
}
