package ui.estoque.dialog;

import model.AlimentoModel;
import model.ConfigFiscalModel;
import model.CodigoDescricaoModel;
import dao.ProdutoDAO;
import dao.ConfigFiscalDefaultDAO;
import dao.FiscalCatalogDAO;
import model.FornecedorModel;
import model.NcmModel;
import service.NcmService;
import service.ProdutoEstoqueService;
import ui.ajustes.dialog.FornecedorDialog;
import util.FormatterFactory;
import util.ScannerUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class CadastroProdutoAlimenticioDialog extends JDialog {
    private final boolean isEdicao;
    private final AlimentoModel alimentoOrig;

    private final JTextField tfNome = new JTextField(24);
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[] { "Comida", "Bebida" });
    private final JComboBox<String> cbSubtipo = new JComboBox<>();
    private final JComboBox<String> cbMarca = new JComboBox<>();
    private final JTextField tfMarcaOutro = new JTextField(24);
    private final JComboBox<String> cbSabor = new JComboBox<>();
    private final JTextField tfLote = new JTextField(14);
    private final JFormattedTextField tfPeso = FormatterFactory.getFormattedDoubleField(0.0);
    private final JLabel lblPeso = new JLabel("Peso (g):");

    private final JFormattedTextField tfDataValidade;

    private final JFormattedTextField tfQtd = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getMoneyField(0.0);

    private final JLabel lblCodigoLido = new JLabel("—");

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Selecionar…");
    private FornecedorModel fornecedorSel;

    private final JComboBox<String> cbNcm = new JComboBox<>();
    private final JComboBox<String> cbCfop = new JComboBox<>();
    private final JComboBox<String> cbCsosn = new JComboBox<>();
    private final JComboBox<String> cbOrigem = new JComboBox<>();
    private final JComboBox<String> cbUnidade = new JComboBox<>();

    // ===== rows para esconder label + campo juntos =====
    private JPanel rowMarcaOutro;
    private JPanel rowSabor;

    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CadastroProdutoAlimenticioDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroProdutoAlimenticioDialog(JFrame owner, AlimentoModel a) {
        super(owner, a == null ? "Novo Produto Alimentício" : "Editar Produto Alimentício", true);
        UiKit.applyDialogBase(this);

        this.isEdicao = a != null;
        this.alimentoOrig = a;

        // máscara data
        JFormattedTextField temp;
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            temp = new JFormattedTextField(mask);
        } catch (ParseException e) {
            temp = new JFormattedTextField();
        }
        this.tfDataValidade = temp;

        buildUI(owner);
        wireEvents(owner);

        // combos iniciais
        carregarSubtipos();
        carregarMarcas();
        carregarSabores();
        atualizarVisibilidade();

        // NCMs
        carregarNcms();
        carregarCombosFiscais();
        aplicarDefaultsFiscais();

        if (isEdicao)
            preencherCampos();

        setMinimumSize(new Dimension(860, 640));
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
        left.add(UiKit.title(isEdicao ? "Editar Produto Alimentício" : "Novo Produto Alimentício"));
        left.add(UiKit.hint("Cadastro de lanches • consistente com os outros cadastros"));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // ===== Form card =====
        JPanel form = UiKit.card();
        form.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;

        addField(form, g, r++, "Nome:", tfNome);
        addField(form, g, r++, "Categoria:", cbCategoria);
        addField(form, g, r++, "Subtipo:", cbSubtipo);
        addField(form, g, r++, "Marca:", cbMarca);

        rowMarcaOutro = makeRow("Marca (Outros):", tfMarcaOutro);
        addRowPanel(form, g, r++, rowMarcaOutro);

        rowSabor = makeRow("Sabor (apenas Suco):", cbSabor);
        addRowPanel(form, g, r++, rowSabor);

        addField(form, g, r++, "Lote:", tfLote);

        JPanel pesoRow = new JPanel(new BorderLayout(6, 0));
        pesoRow.setOpaque(false);
        pesoRow.add(lblPeso, BorderLayout.WEST);
        pesoRow.add(tfPeso, BorderLayout.CENTER);
        addField(form, g, r++, "Peso/Volume:", pesoRow);

        addField(form, g, r++, "Data Validade:", tfDataValidade);

        // ===== Código de barras (bonito no tema) =====
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

        // ação scanner/manual
        btnScanner.addActionListener(
                e -> ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", this::setCodigoBarras));

        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty())
                setCodigoBarras(input.trim());
        });

        // NCM
        addField(form, g, r++, "NCM:", cbNcm);
        addField(form, g, r++, "CFOP:", cbCfop);
        addField(form, g, r++, "CSOSN:", cbCsosn);
        addField(form, g, r++, "Origem:", cbOrigem);
        addField(form, g, r++, "Unidade:", cbUnidade);

        // Valores em uma linha (fica mais profissional)
        JPanel valores = new JPanel(new GridLayout(1, 3, 10, 0));
        valores.setOpaque(false);
        valores.add(labeledInline("Qtd:", tfQtd));
        valores.add(labeledInline("Custo (R$):", tfCusto));
        valores.add(labeledInline("Venda (R$):", tfPreco));
        addField(form, g, r++, "Valores:", valores);

        // Fornecedor
        JPanel fornRow = new JPanel(new BorderLayout(8, 0));
        fornRow.setOpaque(false);
        fornRow.add(lblFornecedor, BorderLayout.CENTER);

        JPanel fornButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        fornButtons.setOpaque(false);
        
        btnSelectFornec.setText("Selecionar…");
        fornButtons.add(btnSelectFornec);
        
        JButton btnNovoFornec = new JButton("➕ Criar");
        btnNovoFornec.addActionListener(e -> criarNovoFornecedor(owner));
        fornButtons.add(btnNovoFornec);
        
        fornRow.add(fornButtons, BorderLayout.EAST);

        addField(form, g, r++, "Fornecedor:", fornRow);

        JScrollPane sp = UiKit.scroll(form);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // ===== Footer =====
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());
        footer.add(UiKit.hint("Marca e sabor aparecem só quando fizer sentido. Sem layout quebrado."),
                BorderLayout.WEST);

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

    private void wireEvents(JFrame owner) {
        cbCategoria.addActionListener(e -> {
            carregarSubtipos();
            carregarMarcas();
            carregarSabores();
            atualizarVisibilidade();
            pack();
        });

        cbSubtipo.addActionListener(e -> {
            carregarMarcas();
            carregarSabores();
            atualizarVisibilidade();
            pack();
        });

        cbMarca.addActionListener(e -> {
            atualizarVisibilidade();
            pack();
        });

        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });
    }

    private void criarNovoFornecedor(JFrame owner) {
        FornecedorDialog dlg = new FornecedorDialog(owner, null);
        dlg.setVisible(true);
        // Após criar, recarrega a lista e seleciona o novo fornecedor se houver
        // (FornecedorDialog salva automaticamente)
    }

    private void setCodigoBarras(String codigo) {
        lblCodigoLido.setText(codigo);
        lblCodigoLido.setToolTipText(codigo);
        lblCodigoLido.putClientProperty("codigoBarras", codigo);
        lblCodigoLido.revalidate();
        lblCodigoLido.repaint();
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

    private void carregarSubtipos() {
        cbSubtipo.removeAllItems();

        if ("Comida".equals(cbCategoria.getSelectedItem())) {
            for (String s : new String[] { "Salgadinho", "Doce" })
                cbSubtipo.addItem(s);
        } else {
            for (String s : new String[] { "Refrigerante", "Suco", "Achocolatado", "Água", "Bebida energética" })
                cbSubtipo.addItem(s);
        }

        if (cbSubtipo.getItemCount() > 0)
            cbSubtipo.setSelectedIndex(0);
    }

    private void carregarMarcas() {
        cbMarca.removeAllItems();
        String st = (String) cbSubtipo.getSelectedItem();
        if (st == null)
            return;

        String[] arr;
        switch (st) {
            case "Salgadinho":
                arr = new String[] { "Elma Chips", "Cheetos", "Fandangos", "Doritos", "Ruffles", "Lay's", "Torcida",
                        "Yoki", "Outros" };
                break;
            case "Doce":
                arr = new String[] { "Garoto", "Lacta", "Nestlé", "Ferrero Rocher", "Kopenhagen", "Hershey's",
                        "Outros" };
                break;
            case "Refrigerante":
                arr = new String[] { "Coca-Cola", "Pepsi", "Guaraná Antarctica", "Fanta", "Sprite", "Outros" };
                break;
            case "Achocolatado":
                arr = new String[] { "Nescau", "Toddy", "Ovomaltine", "Toddynho", "Nescau Pronto", "Outros" };
                break;
            case "Água":
                arr = new String[] { "Crystal", "Bonafont", "Minalba", "São Lourenço", "Pureza Vital", "Outros" };
                break;
            case "Bebida energética":
                arr = new String[] { "Red Bull", "Monster", "Burn", "TNT", "Fusion", "Outros" };
                break;
            default:
                arr = new String[] { "Outros" };
                break;
        }

        for (String m : arr)
            cbMarca.addItem(m);
    }

    private void carregarSabores() {
        cbSabor.removeAllItems();
        String st = (String) cbSubtipo.getSelectedItem();
        if (st == null)
            return;

        if ("Suco".equals(st)) {
            for (String s : new String[] {
                    "Laranja", "Uva", "Maçã", "Maracujá", "Manga", "Abacaxi", "Acerola", "Goiaba", "Pêssego", "Limão",
                    "Outros"
            })
                cbSabor.addItem(s);
        }
    }

    private void atualizarVisibilidade() {
        String st = (String) cbSubtipo.getSelectedItem();
        boolean marcaOutros = "Outros".equals(cbMarca.getSelectedItem());
        boolean isSuco = "Suco".equals(st);

        rowMarcaOutro.setVisible(marcaOutros);
        rowSabor.setVisible(isSuco);

        if ("Bebida".equals(cbCategoria.getSelectedItem())) {
            lblPeso.setText("Volume (ml):");
        } else {
            lblPeso.setText("Peso (g):");
        }

        revalidate();
        repaint();
    }

    private void preencherCampos() {
        tfNome.setText(alimentoOrig.getNome());
        cbCategoria.setSelectedItem(alimentoOrig.getCategoria());

        carregarSubtipos();
        cbSubtipo.setSelectedItem(alimentoOrig.getSubtipo());

        carregarMarcas();
        cbMarca.setSelectedItem(alimentoOrig.getMarca());

        tfMarcaOutro.setText(alimentoOrig.getMarca());

        carregarSabores();
        if ("Suco".equals(alimentoOrig.getSubtipo()))
            cbSabor.setSelectedItem(alimentoOrig.getSabor());

        tfLote.setText(alimentoOrig.getLote());
        tfPeso.setValue(alimentoOrig.getPeso());
        tfDataValidade.setText(alimentoOrig.getDataValidade());

        String codigoExistente = alimentoOrig.getCodigoBarras();
        if (codigoExistente != null && !codigoExistente.isBlank()) {
            setCodigoBarras(codigoExistente);
        }

        tfQtd.setValue(alimentoOrig.getQuantidade());
        tfCusto.setValue(alimentoOrig.getPrecoCompra());
        tfPreco.setValue(alimentoOrig.getPrecoVenda());

        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(alimentoOrig.getFornecedorId());
        fornecedorSel.setNome(alimentoOrig.getFornecedorNome());
        lblFornecedor.setText(alimentoOrig.getFornecedorNome());

        if (alimentoOrig.getNcm() != null) {
            for (int i = 0; i < cbNcm.getItemCount(); i++) {
                if (cbNcm.getItemAt(i).startsWith(alimentoOrig.getNcm())) {
                    cbNcm.setSelectedIndex(i);
                    break;
                }
            }
        }

        selecionarPorCodigoPrefix(cbCfop, alimentoOrig.getCfop());
        selecionarPorCodigoPrefix(cbCsosn, alimentoOrig.getCsosn());
        selecionarPorCodigoPrefix(cbOrigem, alimentoOrig.getOrigem());
        selecionarPorCodigoPrefix(cbUnidade, alimentoOrig.getUnidade());

        atualizarVisibilidade();
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
        // Lote agora é opcional - removido check
        if ("Outros".equals(cbMarca.getSelectedItem()) && tfMarcaOutro.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe a marca.");
            return;
        }

        try {
            String ncmCombo = (String) cbNcm.getSelectedItem();
            String ncm = null;
            if (ncmCombo != null && ncmCombo.contains("-")) {
                ncm = ncmCombo.split("-")[0].trim();
            }
            String cfop = firstToken((String) cbCfop.getSelectedItem());
            String csosn = firstToken((String) cbCsosn.getSelectedItem());
            String origem = firstToken((String) cbOrigem.getSelectedItem());
            String unidadeFiscal = firstToken((String) cbUnidade.getSelectedItem());

            String id = isEdicao ? alimentoOrig.getId() : UUID.randomUUID().toString();
            String nome = tfNome.getText().trim();
            String categoria = (String) cbCategoria.getSelectedItem();
            String subtipo = (String) cbSubtipo.getSelectedItem();
            String marca = "Outros".equals(cbMarca.getSelectedItem())
                    ? tfMarcaOutro.getText().trim()
                    : (String) cbMarca.getSelectedItem();
            String sabor = rowSabor.isVisible() && cbSabor.getSelectedItem() != null
                    ? (String) cbSabor.getSelectedItem()
                    : "";
            String lote = tfLote.getText().trim();
            double peso = UiKit.getDoubleValue(tfPeso, 0.0);
            String unidadePeso = "Bebida".equals(categoria) ? "ml" : "g";
            String dataVal = tfDataValidade.getText().trim();

            String codigo = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigo == null)
                codigo = "";

            int duplicados = new ProdutoDAO().contarPorCodigoBarrasAtivo(codigo, id);
            if (duplicados > 0) {
                JOptionPane.showMessageDialog(this,
                        "Este codigo ja existe em " + duplicados
                                + " produtos. Na venda, sera necessario selecionar qual produto.",
                        "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }

            int quantidade = UiKit.getIntValue(tfQtd, 0);
            double custo = UiKit.getDoubleValue(tfCusto, 0.0);
            double preco = UiKit.getDoubleValue(tfPreco, 0.0);
            String fornId = fornecedorSel.getId();

            AlimentoModel a = new AlimentoModel(
                    id, nome, quantidade, custo, preco,
                    fornId, categoria, subtipo, marca, sabor,
                    lote, peso, unidadePeso, codigo, dataVal);
            a.setFornecedorId(fornId);
            a.setNcm(ncm);
            a.setCfop(cfop);
            a.setCsosn(csosn);
            a.setOrigem(origem);
            a.setUnidade(unidadeFiscal);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao)
                service.atualizarAlimento(a);
            else
                service.salvarNovoAlimento(a);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao salvar Produto Alimentício:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== helpers layout =====

    private JPanel makeRow(String label, JComponent field) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 0, 0);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.weightx = 0;
        row.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        row.add(field, g);

        return row;
    }

    private void addRowPanel(JPanel parent, GridBagConstraints g, int row, JPanel rowPanel) {
        g.gridy = row;
        g.gridx = 0;
        g.gridwidth = 2;
        g.weightx = 1;
        parent.add(rowPanel, g);
        g.gridwidth = 1;
    }

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
}
