// src/ui/estoque/dialog/CadastroAcessorioDialog.java
package ui.estoque.dialog;

import model.AcessorioModel;
import dao.ProdutoDAO;
import dao.ConfigFiscalDefaultDAO;
import dao.FiscalCatalogDAO;
import model.ConfigFiscalModel;
import model.CodigoDescricaoModel;
import model.FornecedorModel;
import model.NcmModel;
import service.NcmService;
import service.ProdutoEstoqueService;
import ui.ajustes.dialog.FornecedorDialog;
import util.FormatterFactory;
import util.ScannerUtils;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class CadastroAcessorioDialog extends JDialog {

    private final boolean isEdicao;
    private final AcessorioModel acessoOrig;

    private final JTextField tfNome = new JTextField(24);
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[] {
            "Chaveiros", "Moedas", "Marcadores", "Kit (Moeda + Marcador)",
            "Sleeve", "Playmats", "Lancheiras", "Outros"
    });

    private final JLabel lblArte = new JLabel("Arte:");
    private final JComboBox<String> cbArte = new JComboBox<>();
    private final JLabel lblCor = new JLabel("Cor:");
    private final JTextField tfCor = new JTextField(14);

    // Código de barras exibido (não muda lógica)
    private final JLabel lblCodigoLido = new JLabel("—");

    private final JFormattedTextField tfQtd = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getMoneyField(0.0);

    private final JComboBox<String> cbNcm = new JComboBox<>();
    private final JComboBox<String> cbCfop = new JComboBox<>();
    private final JComboBox<String> cbCsosn = new JComboBox<>();
    private final JComboBox<String> cbOrigem = new JComboBox<>();
    private final JComboBox<String> cbUnidade = new JComboBox<>();

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Escolher Fornecedor");
    private FornecedorModel fornecedorSel;

    // helpers visuais
    private JPanel rowArte;
    private JPanel rowCor;

    public CadastroAcessorioDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroAcessorioDialog(JFrame owner, AcessorioModel acesso) {
        super(owner, acesso == null ? "Novo Acessório" : "Editar Acessório", true);
        UiKit.applyDialogBase(this);

        this.isEdicao = acesso != null;
        this.acessoOrig = acesso;

        buildUI(owner);
        wireEvents(owner);

        if (isEdicao)
            preencherCampos();

        atualizarCamposArteCor(); // garante estado inicial

        setMinimumSize(new Dimension(760, 520));
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
        left.add(UiKit.title(isEdicao ? "Editar Acessório" : "Novo Acessório"));
        left.add(UiKit.hint("Cadastro de acessórios • consistente com tema (FlatLaf)"));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // ===== Form (card) =====
        JPanel formCard = UiKit.card();
        formCard.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;

        // Nome
        addField(formCard, g, r++, "Nome:", tfNome);

        // Categoria
        addField(formCard, g, r++, "Categoria:", cbCategoria);

        // Arte (linha escondível)
        rowArte = new JPanel(new BorderLayout(8, 0));
        rowArte.setOpaque(false);
        rowArte.add(cbArte, BorderLayout.CENTER);
        addField(formCard, g, r++, "Arte:", rowArte);

        // Cor (linha escondível)
        rowCor = new JPanel(new BorderLayout(8, 0));
        rowCor.setOpaque(false);
        rowCor.add(tfCor, BorderLayout.CENTER);
        addField(formCard, g, r++, "Cor:", rowCor);

        // Código de barras (card interno)
        JPanel barcodeRow = new JPanel(new BorderLayout(8, 0));
        barcodeRow.setOpaque(false);

        JPanel barcodeActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barcodeActions.setOpaque(false);

        JButton btnScanner = UiKit.ghost("📷 Ler com Scanner");
        JButton btnManual = UiKit.ghost("⌨ Inserir Manualmente");
        barcodeActions.add(btnScanner);
        barcodeActions.add(btnManual);

        // label do código com cara de "campo"
        lblCodigoLido.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        lblCodigoLido.setOpaque(true);
        lblCodigoLido.setBackground(UIManager.getColor("TextField.background"));
        lblCodigoLido.setForeground(UIManager.getColor("TextField.foreground"));

        // envolve label num panel pra não esticar estranho
        JPanel codeBox = new JPanel(new BorderLayout());
        codeBox.setOpaque(false);
        codeBox.add(lblCodigoLido, BorderLayout.CENTER);
        codeBox.setPreferredSize(new Dimension(220, 34));

        barcodeRow.add(barcodeActions, BorderLayout.WEST);
        barcodeRow.add(codeBox, BorderLayout.EAST);

        addField(formCard, g, r++, "Código de Barras:", barcodeRow);

        // NCM / Fiscal
        addField(formCard, g, r++, "NCM:", cbNcm);
        addField(formCard, g, r++, "CFOP:", cbCfop);
        addField(formCard, g, r++, "CSOSN:", cbCsosn);
        addField(formCard, g, r++, "Origem:", cbOrigem);
        addField(formCard, g, r++, "Unidade:", cbUnidade);

        // Quantidade / Custo / Preço (em uma linha)
        JPanel linhaValores = new JPanel(new GridLayout(1, 3, 10, 0));
        linhaValores.setOpaque(false);

        JPanel pQtd = labeledInline("Qtd:", tfQtd);
        JPanel pCusto = labeledInline("Custo (R$):", tfCusto);
        JPanel pPreco = labeledInline("Venda (R$):", tfPreco);

        linhaValores.add(pQtd);
        linhaValores.add(pCusto);
        linhaValores.add(pPreco);

        addField(formCard, g, r++, "Valores:", linhaValores);

        // Fornecedor
        JPanel fornRow = new JPanel(new BorderLayout(8, 0));
        fornRow.setOpaque(false);
        fornRow.add(lblFornecedor, BorderLayout.CENTER);

        JPanel fornButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        fornButtons.setOpaque(false);
        
        JButton btEscolher = UiKit.ghost("Selecionar…");
        btEscolher.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });
        fornButtons.add(btEscolher);
        
        JButton btnNovoFornec = new JButton("➕ Criar");
        btnNovoFornec.addActionListener(e -> criarNovoFornecedor(owner));
        fornButtons.add(btnNovoFornec);
        
        fornRow.add(fornButtons, BorderLayout.EAST);
        addField(formCard, g, r++, "Fornecedor:", fornRow);

        // Scroll pra não quebrar em telas menores
        JScrollPane sp = UiKit.scroll(formCard);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // ===== Footer =====
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Dica: Sleeve com “Cor Única” habilita o campo de cor."), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btCancelar = UiKit.ghost("Cancelar");
        JButton btSalvar = UiKit.primary(isEdicao ? "Atualizar" : "Salvar");

        actions.add(btCancelar);
        actions.add(btSalvar);

        btCancelar.addActionListener(e -> dispose());
        btSalvar.addActionListener(e -> salvar());

        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        // Guarda referência dos botões do barcode pra usar nos listeners
        btnScanner.putClientProperty("barcodeLabel", lblCodigoLido);
        btnManual.putClientProperty("barcodeLabel", lblCodigoLido);

        // actions barcode (mantendo tua lógica)
        btnScanner.addActionListener(e -> {
            ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", codigo -> {
                setCodigoBarras(codigo);
            });
        });

        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty()) {
                setCodigoBarras(input.trim());
            }
        });

        // NCM: carrega igual você fazia
        try {
            List<NcmModel> ncms = NcmService.getInstance().findAll();
            for (NcmModel n : ncms) {
                cbNcm.addItem(n.getCodigo() + " - " + n.getDescricao());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar NCMs:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }

        carregarCombosFiscais();
        aplicarDefaultsFiscais();
    }

    private void wireEvents(JFrame owner) {
        cbCategoria.addActionListener(e -> atualizarCamposArteCor());
    }

    private void setCodigoBarras(String codigo) {
        lblCodigoLido.setText(codigo);
        lblCodigoLido.setToolTipText(codigo);
        lblCodigoLido.putClientProperty("codigoBarras", codigo);
        lblCodigoLido.revalidate();
        lblCodigoLido.repaint();
        pack();
    }

    private JPanel labeledInline(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(UIManager.getColor("Label.foreground"));
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void addField(JPanel parent, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        JLabel l = new JLabel(label);
        parent.add(l, g);

        g.gridx = 1;
        g.weightx = 1;
        parent.add(field, g);
    }

    /** Exibe ou oculta os campos Arte/Cor de acordo com a categoria (mesma lógica, só mais estável visualmente) */
    private void atualizarCamposArteCor() {
        String cat = (String) cbCategoria.getSelectedItem();

        // default: some tudo
        rowArte.setVisible(false);
        rowCor.setVisible(false);

        if ("Playmats".equals(cat)) {
            cbArte.setModel(new DefaultComboBoxModel<>(new String[] { "Pokémon", "Treinador", "Outros" }));
            rowArte.setVisible(true);

        } else if ("Sleeve".equals(cat)) {
            cbArte.setModel(new DefaultComboBoxModel<>(new String[] { "Pokémon", "Treinador", "Outros", "Cor Única" }));
            rowArte.setVisible(true);

            // garante que não vai acumulando listeners toda vez que muda categoria
            for (var al : cbArte.getActionListeners())
                cbArte.removeActionListener(al);

            cbArte.addActionListener(e -> {
                boolean corUnica = "Cor Única".equals(cbArte.getSelectedItem());
                rowCor.setVisible(corUnica);
                revalidate();
                repaint();
                pack();
            });

            // aplica estado imediato
            boolean corUnica = "Cor Única".equals(cbArte.getSelectedItem());
            rowCor.setVisible(corUnica);
        }

        revalidate();
        repaint();
        pack();
    }

    private void preencherCampos() {
        tfNome.setText(acessoOrig.getNome());
        tfQtd.setValue(acessoOrig.getQuantidade());
        tfCusto.setValue(acessoOrig.getPrecoCompra());
        tfPreco.setValue(acessoOrig.getPrecoVenda());

        cbCategoria.setSelectedItem(acessoOrig.getCategoria());
        atualizarCamposArteCor();

        if (rowArte.isVisible()) {
            cbArte.setSelectedItem(acessoOrig.getArte());
            // cor só se estiver visível
            if (rowCor.isVisible()) {
                tfCor.setText(acessoOrig.getCor());
            }
        }

        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(acessoOrig.getFornecedorId());
        fornecedorSel.setNome(acessoOrig.getFornecedorNome());
        lblFornecedor.setText(acessoOrig.getFornecedorNome());

        // Seleciona NCM
        if (acessoOrig.getNcm() != null) {
            for (int i = 0; i < cbNcm.getItemCount(); i++) {
                if (cbNcm.getItemAt(i).startsWith(acessoOrig.getNcm())) {
                    cbNcm.setSelectedIndex(i);
                    break;
                }
            }
        }
        selecionarPorCodigoPrefix(cbCfop, acessoOrig.getCfop());
        selecionarPorCodigoPrefix(cbCsosn, acessoOrig.getCsosn());
        selecionarPorCodigoPrefix(cbOrigem, acessoOrig.getOrigem());
        selecionarPorCodigoPrefix(cbUnidade, acessoOrig.getUnidade());
    }

    private void salvar() {
        // === tua lógica original, intacta ===
        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório.");
            return;
        }
        if (fornecedorSel == null) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor.");
            return;
        }

        try {
            String id = isEdicao ? acessoOrig.getId() : UUID.randomUUID().toString();

            String nome = tfNome.getText().trim();
            String categoria = (String) cbCategoria.getSelectedItem();
            String arte = rowArte.isVisible() ? (String) cbArte.getSelectedItem() : "";
            String cor = rowCor.isVisible() ? tfCor.getText().trim() : "";

            String codigoBarras = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigoBarras == null) codigoBarras = "";

            int duplicados = new ProdutoDAO().contarPorCodigoBarrasAtivo(codigoBarras, id);
            if (duplicados > 0) {
                JOptionPane.showMessageDialog(this,
                        "Este codigo ja existe em " + duplicados
                                + " produtos. Na venda, sera necessario selecionar qual produto.",
                        "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }

            String ncmCombo = (String) cbNcm.getSelectedItem();
            String ncm = "";
            if (ncmCombo != null && ncmCombo.contains("-")) {
                ncm = ncmCombo.split("-")[0].trim();
            }
            String cfop = firstToken((String) cbCfop.getSelectedItem());
            String csosn = firstToken((String) cbCsosn.getSelectedItem());
            String origem = firstToken((String) cbOrigem.getSelectedItem());
            String unidade = firstToken((String) cbUnidade.getSelectedItem());

            int qtd = ((Number) tfQtd.getValue()).intValue();
            double custo = ((Number) tfCusto.getValue()).doubleValue();
            double preco = ((Number) tfPreco.getValue()).doubleValue();
            String fornId = fornecedorSel.getId();
            String fornNom = fornecedorSel.getNome();

            AcessorioModel a = new AcessorioModel(id, nome, qtd, custo, preco, fornId, categoria, arte, cor);
            a.setFornecedorId(fornId);
            a.setFornecedorNome(fornNom);
            a.setNcm(ncm);
            a.setCfop(cfop);
            a.setCsosn(csosn);
            a.setOrigem(origem);
            a.setUnidade(unidade);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) service.atualizarAcessorio(a);
            else service.salvarNovoAcessorio(a);

            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar Acessório:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void criarNovoFornecedor(JFrame owner) {
        FornecedorDialog dlg = new FornecedorDialog(owner, null);
        dlg.setVisible(true);
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
            if (cfg == null) return;
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
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}
