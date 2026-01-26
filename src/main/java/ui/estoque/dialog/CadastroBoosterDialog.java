// src/ui/estoque/dialog/CadastroBoosterDialog.java
package ui.estoque.dialog;

import dao.JogoDAO;
import dao.ProdutoDAO;
import model.BoosterModel;
import model.ColecaoModel;
import model.FornecedorModel;
import model.JogoModel;
import model.NcmModel;
import service.NcmService;
import service.ProdutoEstoqueService;
import ui.ajustes.dialog.FornecedorDialog;
import util.FormatterFactory;
import util.ScannerUtils;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;

public class CadastroBoosterDialog extends JDialog {

    private final boolean isEdicao;
    private final BoosterModel boosterOrig;

    private final JTextField tfNome = new JTextField(24);

    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();

    // Pokémon
    private final JComboBox<String> cbSerie = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();

    // Outros jogos: Set por combo / manual
    private final JComboBox<String> cbSetJogo = new JComboBox<>();
    private final JTextField tfSetManual = new JTextField(24);

    private final JPanel panelSetSwitcher = new JPanel(new CardLayout());
    private static final String CARD_COMBO = "combo";
    private static final String CARD_MANUAL = "manual";

    private List<String> setsFiltrados = new ArrayList<>();

    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] {
            "Unitário", "Quadri-pack", "Triple-pack", "Especial", "Blister"
    });

    private final JComboBox<String> cbIdioma = new JComboBox<>();
    private final JTextField tfDataLanc = new JTextField(12);

    private final JLabel lblCodigoLido = new JLabel("—");

    private final JFormattedTextField tfQtd = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getFormattedDoubleField(0.0);

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private FornecedorModel fornecedorSel;

    private final JComboBox<String> cbNcm = new JComboBox<>();

    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ====== Linhas (pra esconder label+campo junto) ======
    private JPanel rowSerie;
    private JPanel rowColecao;
    private JPanel rowSet; // (cardlayout)

    public CadastroBoosterDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroBoosterDialog(JFrame owner, BoosterModel booster) {
        super(owner, booster == null ? "Novo Booster" : "Editar Booster", true);
        UiKit.applyDialogBase(this);

        this.isEdicao = booster != null;
        this.boosterOrig = booster;

        buildUI(owner);
        wireEvents(owner);

        // Carregamento inicial
        carregarJogos();
        carregarIdiomas();
        carregarSeries();
        carregarColecoesPorSerie();
        carregarNcms();

        tfDataLanc.setEditable(false);

        if (isEdicao)
            preencherCampos();

        // força aplicar o modo certo do jogo após preencher/selecionar
        atualizarCamposPorJogo();

        setMinimumSize(new Dimension(820, 560));
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
        left.add(UiKit.title(isEdicao ? "Editar Booster" : "Novo Booster"));
        left.add(UiKit.hint("Cadastro de booster • visual consistente • tema FlatLaf (dark/light)"));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // ===== Form Card =====
        JPanel formCard = UiKit.card();
        formCard.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;

        addField(formCard, g, r++, "Nome:", tfNome);
        addField(formCard, g, r++, "Jogo:", cbJogo);

        // Série (Pokémon)
        rowSerie = makeRow("Série:", cbSerie);
        addRowPanel(formCard, g, r++, rowSerie);

        // Coleção (Pokémon)
        rowColecao = makeRow("Coleção:", cbColecao);
        addRowPanel(formCard, g, r++, rowColecao);

        // Set (outros jogos)
        panelSetSwitcher.add(cbSetJogo, CARD_COMBO);
        panelSetSwitcher.add(tfSetManual, CARD_MANUAL);

        rowSet = makeRow("Set:", panelSetSwitcher);
        addRowPanel(formCard, g, r++, rowSet);

        addField(formCard, g, r++, "Tipo:", cbTipo);
        addField(formCard, g, r++, "Idioma:", cbIdioma);
        addField(formCard, g, r++, "Data de Lançamento:", tfDataLanc);

        // Código de barras: botões + label “campo”
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

        addField(formCard, g, r++, "Código de Barras:", barcodeRow);

        addField(formCard, g, r++, "NCM:", cbNcm);

        // Valores em linha (Qtd / Custo / Preço)
        JPanel linhaValores = new JPanel(new GridLayout(1, 3, 10, 0));
        linhaValores.setOpaque(false);
        linhaValores.add(labeledInline("Qtd:", tfQtd));
        linhaValores.add(labeledInline("Custo (R$):", tfCusto));
        linhaValores.add(labeledInline("Venda (R$):", tfPreco));
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

        JScrollPane sp = UiKit.scroll(formCard);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // ===== Footer =====
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());
        footer.add(UiKit.hint("Dica: troque o jogo para alternar Série/Coleção vs Set."), BorderLayout.WEST);

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

        // Barcode actions (mantém tua lógica)
        btnScanner.addActionListener(e -> {
            ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", this::setCodigoBarras);
        });

        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty()) {
                setCodigoBarras(input.trim());
            }
        });
    }

    private void wireEvents(JFrame owner) {
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());

        cbColecao.addActionListener(e -> {
            ColecaoModel c = (ColecaoModel) cbColecao.getSelectedItem();
            if (c != null && c.getReleaseDate() != null && !c.getReleaseDate().isBlank()) {
                try {
                    LocalDate d = LocalDate.parse(c.getReleaseDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    tfDataLanc.setText(d.format(DISPLAY_DATE_FMT));
                } catch (Exception ex) {
                    tfDataLanc.setText("");
                }
            } else {
                tfDataLanc.setText("");
            }
        });

        cbJogo.addActionListener(e -> atualizarCamposPorJogo());
    }

    private void setCodigoBarras(String codigo) {
        lblCodigoLido.setText(codigo);
        lblCodigoLido.setToolTipText(codigo);
        lblCodigoLido.putClientProperty("codigoBarras", codigo);
        lblCodigoLido.revalidate();
        lblCodigoLido.repaint();
        pack();
    }

    // ====== Carregamentos (mantém tua lógica) ======

    private void carregarSeries() {
        try {
            List<String> series = new dao.SetDAO().listarSeriesUnicas();
            cbSerie.removeAllItems();
            series.forEach(cbSerie::addItem);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
        }
    }

    private void carregarColecoesPorSerie() {
        try {
            String serie = (String) cbSerie.getSelectedItem();
            cbColecao.removeAllItems();
            if (serie == null)
                return;

            for (ColecaoModel c : new dao.ColecaoDAO().listarPorSerie(serie)) {
                cbColecao.addItem(c);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void carregarIdiomas() {
        try {
            cbIdioma.removeAllItems();
            for (Map<String, String> m : new dao.CadastroGenericoDAO("linguagens", "id", "nome").listar()) {
                cbIdioma.addItem(m.get("nome"));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar idiomas.");
        }
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

    // ====== Alternância de campos por Jogo (corrigida visualmente) ======

    private void atualizarCamposPorJogo() {
        JogoModel jogo = (JogoModel) cbJogo.getSelectedItem();
        if (jogo == null || jogo.getId() == null) {
            // Sem jogo: esconde tudo específico e não quebra layout
            rowSerie.setVisible(false);
            rowColecao.setVisible(false);
            rowSet.setVisible(false);
            revalidate();
            repaint();
            return;
        }

        boolean isPokemon = jogo.getId().equalsIgnoreCase("POKEMON");
        boolean isOnePiece = jogo.getId().equalsIgnoreCase("ONEPIECE");
        boolean isDragonBall = jogo.getId().equalsIgnoreCase("DRAGONBALL");

        // Série/Coleção só para Pokémon
        rowSerie.setVisible(isPokemon);
        rowColecao.setVisible(isPokemon);

        // Set para não-Pokémon (alguns manual)
        rowSet.setVisible(!isPokemon);

        CardLayout cl = (CardLayout) panelSetSwitcher.getLayout();

        if (isOnePiece || isDragonBall) {
            cl.show(panelSetSwitcher, CARD_MANUAL);
        } else {
            cl.show(panelSetSwitcher, CARD_COMBO);
        }

        if (isPokemon) {
            carregarSeries();
            carregarColecoesPorSerie();
        } else if (!isOnePiece && !isDragonBall) {
            carregarSetsJogo(jogo.getId());
        }

        revalidate();
        repaint();
        pack();
    }

    private void carregarSetsJogo(String jogoId) {
        try {
            cbSetJogo.removeAllItems();

            var sets = new dao.SetJogoDAO().listarPorJogo(jogoId);
            sets.sort(Comparator.comparing(s -> s.getNome().toLowerCase()));

            setsFiltrados = sets.stream().map(s -> s.getNome()).toList();

            atualizarComboBoxSet(setsFiltrados);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar sets do jogo.");
            ex.printStackTrace();
        }
    }

    private void atualizarComboBoxSet(List<String> lista) {
        cbSetJogo.removeAllItems();
        for (String nome : lista)
            cbSetJogo.addItem(nome);
    }

    // ====== Preencher / salvar (mantém regra) ======

    private void preencherCampos() {
        tfNome.setText(boosterOrig.getNome());
        tfQtd.setValue(boosterOrig.getQuantidade());
        tfCusto.setValue(boosterOrig.getPrecoCompra());
        tfPreco.setValue(boosterOrig.getPrecoVenda());

        // Jogo
        String jogoId = boosterOrig.getJogoId();
        if (jogoId != null) {
            for (int i = 0; i < cbJogo.getItemCount(); i++) {
                JogoModel jm = cbJogo.getItemAt(i);
                if (jm.getId() != null && jm.getId().equals(jogoId)) {
                    cbJogo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Série (Pokémon)
        try {
            cbSerie.removeAllItems();
            List<String> series = new dao.SetDAO().listarSeriesUnicas();
            for (String s : series)
                cbSerie.addItem(s);
            cbSerie.setSelectedItem(boosterOrig.getSet());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
            ex.printStackTrace();
        }

        // Coleções + Data
        cbColecao.removeAllItems();
        try {
            List<ColecaoModel> todas = new dao.ColecaoDAO().listarPorSerie(boosterOrig.getSet());
            for (ColecaoModel c : todas) {
                cbColecao.addItem(c);
                if (c.getName().equalsIgnoreCase(boosterOrig.getColecao())) {
                    cbColecao.setSelectedItem(c);
                    if (c.getReleaseDate() != null && !c.getReleaseDate().isBlank()) {
                        LocalDate d = LocalDate.parse(c.getReleaseDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                        tfDataLanc.setText(d.format(DISPLAY_DATE_FMT));
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
            e.printStackTrace();
        }

        cbTipo.setSelectedItem(boosterOrig.getTipoBooster());
        cbIdioma.setSelectedItem(boosterOrig.getIdioma());

        String cod = boosterOrig.getCodigoBarras();
        if (cod != null && !cod.isBlank())
            setCodigoBarras(cod);

        // Fornecedor
        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(boosterOrig.getFornecedor());
        fornecedorSel.setNome(boosterOrig.getFornecedorNome());
        lblFornecedor.setText(boosterOrig.getFornecedorNome());

        // NCM
        if (boosterOrig.getNcm() != null) {
            for (int i = 0; i < cbNcm.getItemCount(); i++) {
                if (cbNcm.getItemAt(i).startsWith(boosterOrig.getNcm())) {
                    cbNcm.setSelectedIndex(i);
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
            String ncmCombo = (String) cbNcm.getSelectedItem();
            String ncm = "";
            if (ncmCombo != null && ncmCombo.contains("-")) {
                ncm = ncmCombo.split("-")[0].trim();
            }

            String id = isEdicao ? boosterOrig.getId() : UUID.randomUUID().toString();

            String nome = tfNome.getText().trim();

            String serie = (String) cbSerie.getSelectedItem();
            String colecao = cbColecao.getSelectedItem() != null
                    ? ((ColecaoModel) cbColecao.getSelectedItem()).getName()
                    : "";

            // Se for jogo sem série/coleção, pega do Set switcher
            if (rowSet.isVisible()) {
                // combo
                Component showing = getShowingSetComponent();
                if (showing == cbSetJogo) {
                    serie = (String) cbSetJogo.getSelectedItem();
                } else {
                    serie = tfSetManual.getText().trim();
                }
            }

            String tipo = (String) cbTipo.getSelectedItem();
            String idioma = (String) cbIdioma.getSelectedItem();
            String validade = tfDataLanc.getText().trim();

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

            int qtd = ((Number) tfQtd.getValue()).intValue();
            double custo = ((Number) tfCusto.getValue()).doubleValue();
            double preco = ((Number) tfPreco.getValue()).doubleValue();

            String fornId = fornecedorSel.getId();
            String fornNom = fornecedorSel.getNome();
            String jogoId = jogoSel.getId();

            BoosterModel b = new BoosterModel(
                    id, nome, qtd, custo, preco,
                    fornId,
                    colecao, serie, tipo,
                    idioma, validade, codigo,
                    jogoId);

            b.setFornecedorId(fornId);
            b.setFornecedorNome(fornNom);
            b.setNcm(ncm);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao)
                service.atualizarBooster(b);
            else
                service.salvarNovoBooster(b);

            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar Booster:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Component getShowingSetComponent() {
        // CardLayout não expõe "qual está visível", então verificamos visibilidade real
        if (cbSetJogo.isShowing())
            return cbSetJogo;
        return tfSetManual;
    }

    // ====== Helpers de layout ======

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

    private void criarNovoFornecedor(JFrame owner) {
        FornecedorDialog dlg = new FornecedorDialog(owner, null);
        dlg.setVisible(true);
    }
}
