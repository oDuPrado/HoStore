// src/ui/estoque/dialog/CadastroEtbDialog.java
package ui.estoque.dialog;

import dao.ColecaoDAO;
import dao.JogoDAO;
import dao.SetDAO;
import model.ColecaoModel;
import model.EtbModel;
import model.FornecedorModel;
import model.JogoModel;
import model.NcmModel;
import service.NcmService;
import service.ProdutoEstoqueService;
import util.MaskUtils;
import util.ScannerUtils;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;

public class CadastroEtbDialog extends JDialog {

    private final boolean isEdicao;
    private final EtbModel etbOrig;

    private final JTextField tfNome = new JTextField(24);

    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();

    // Pokémon
    private final JComboBox<String> cbSerie = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();

    // Não-Pokémon
    private final JComboBox<String> cbSetJogo = new JComboBox<>();
    private final JTextField tfSetManual = new JTextField(24);

    private final JPanel panelSetSwitcher = new JPanel(new CardLayout());
    private static final String CARD_COMBO = "combo";
    private static final String CARD_MANUAL = "manual";

    private List<String> setsFiltrados = new ArrayList<>();

    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] {
            "Booster Box", "Pokémon Center", "ETB", "Mini ETB", "Collection Box",
            "Special Collection", "Latas", "Box colecionáveis", "Trainer Kit", "Mini Booster Box"
    });

    private final JComboBox<String> cbVersao = new JComboBox<>(new String[] { "Nacional", "Americana" });
    private final JComboBox<String> cbNcm = new JComboBox<>();

    private final JFormattedTextField tfQtd = MaskUtils.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = MaskUtils.moneyField(0.0);
    private final JFormattedTextField tfPreco = MaskUtils.moneyField(0.0);

    private final JLabel lblCodigoLido = new JLabel("—");

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private FornecedorModel fornecedorSel;

    // ===== Linhas para esconder label+campo juntos =====
    private JPanel rowSerie;
    private JPanel rowColecao;
    private JPanel rowSet;
    private JPanel rowColecaoPokemon; // só pra ficar explícito
    private JPanel rowTipo;
    private JPanel rowVersao;

    public CadastroEtbDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroEtbDialog(JFrame owner, EtbModel etb) {
        super(owner, etb == null ? "Novo ETB" : "Editar ETB", true);
        UiKit.applyDialogBase(this);

        this.isEdicao = etb != null;
        this.etbOrig = etb;

        buildUI(owner);
        wireEvents(owner);

        // carga inicial
        carregarJogos();
        carregarSeries();
        carregarColecoesPorSerie();
        carregarNcms();

        if (isEdicao)
            preencherCampos();

        // aplica a UI correta conforme jogo/tipo
        atualizarCamposPorJogo();
        adjustFieldsByTipo();

        setMinimumSize(new Dimension(860, 620));
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
        left.add(UiKit.title(isEdicao ? "Editar ETB" : "Novo ETB"));
        left.add(UiKit.hint("Cadastro de selados • visual consistente • dark/light OK"));
        header.add(left, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // ===== Form Card =====
        JPanel form = UiKit.card();
        form.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;

        addField(form, g, r++, "Nome:", tfNome);
        addField(form, g, r++, "Jogo:", cbJogo);

        // Série e Coleção (Pokémon)
        rowSerie = makeRow("Série:", cbSerie);
        addRowPanel(form, g, r++, rowSerie);

        rowColecao = makeRow("Coleção:", cbColecao);
        addRowPanel(form, g, r++, rowColecao);

        // Set (outros jogos)
        panelSetSwitcher.add(cbSetJogo, CARD_COMBO);
        panelSetSwitcher.add(tfSetManual, CARD_MANUAL);
        rowSet = makeRow("Set:", panelSetSwitcher);
        addRowPanel(form, g, r++, rowSet);

        rowTipo = makeRow("Tipo:", cbTipo);
        addRowPanel(form, g, r++, rowTipo);

        addField(form, g, r++, "NCM:", cbNcm);

        rowVersao = makeRow("Versão:", cbVersao);
        addRowPanel(form, g, r++, rowVersao);

        // Valores
        JPanel valores = new JPanel(new GridLayout(1, 3, 10, 0));
        valores.setOpaque(false);
        valores.add(labeledInline("Qtd:", tfQtd));
        valores.add(labeledInline("Custo (R$):", tfCusto));
        valores.add(labeledInline("Venda (R$):", tfPreco));
        addField(form, g, r++, "Valores:", valores);

        // Código de barras (sem hardcode feio)
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

        // Fornecedor
        JPanel fornRow = new JPanel(new BorderLayout(8, 0));
        fornRow.setOpaque(false);
        fornRow.add(lblFornecedor, BorderLayout.CENTER);

        JButton btnFornecedor = UiKit.ghost("Selecionar…");
        btnFornecedor.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });
        fornRow.add(btnFornecedor, BorderLayout.EAST);

        addField(form, g, r++, "Fornecedor:", fornRow);

        JScrollPane sp = UiKit.scroll(form);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // ===== Footer =====
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());
        footer.add(UiKit.hint("Dica: o jogo define Série/Coleção ou Set. O tipo pode esconder set."),
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

        // ações barcode
        btnScanner.addActionListener(
                e -> ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", this::setCodigoBarras));

        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty())
                setCodigoBarras(input.trim());
        });
    }

    private void wireEvents(JFrame owner) {
        cbJogo.addActionListener(e -> {
            atualizarCamposPorJogo();
            adjustFieldsByTipo();
        });

        cbSerie.addActionListener(e -> carregarColecoesPorSerie());

        cbTipo.addActionListener(e -> adjustFieldsByTipo());
    }

    private void setCodigoBarras(String codigo) {
        lblCodigoLido.setText(codigo);
        lblCodigoLido.setToolTipText(codigo);
        lblCodigoLido.putClientProperty("codigoBarras", codigo);
        lblCodigoLido.revalidate();
        lblCodigoLido.repaint();
        pack();
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

    private void carregarSeries() {
        try {
            cbSerie.removeAllItems();
            for (String s : new SetDAO().listarSeriesUnicas())
                cbSerie.addItem(s);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
        }
    }

    private void carregarColecoesPorSerie() {
        try {
            cbColecao.removeAllItems();
            String serie = (String) cbSerie.getSelectedItem();
            if (serie != null) {
                for (ColecaoModel c : new ColecaoDAO().listarPorSerie(serie))
                    cbColecao.addItem(c);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void carregarNcms() {
        try {
            cbNcm.removeAllItems();
            List<NcmModel> ncms = NcmService.getInstance().findAll();
            for (NcmModel n : ncms)
                cbNcm.addItem(n.getCodigo() + " - " + n.getDescricao());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar NCMs:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Jogo define:
     * - Pokémon: Série/Coleção visíveis, Set escondido
     * - OnePiece/DragonBall: Set manual
     * - Outros: Set dropdown (carrega sets do jogo)
     */
    private void atualizarCamposPorJogo() {
        JogoModel jogo = (JogoModel) cbJogo.getSelectedItem();

        if (jogo == null || jogo.getId() == null) {
            rowSerie.setVisible(false);
            rowColecao.setVisible(false);
            rowSet.setVisible(false);
            revalidate();
            repaint();
            return;
        }

        String jogoId = jogo.getId();
        boolean isPokemon = jogoId.equalsIgnoreCase("POKEMON");
        boolean isOnePiece = jogoId.equalsIgnoreCase("ONEPIECE");
        boolean isDragonBall = jogoId.equalsIgnoreCase("DRAGONBALL");

        rowSerie.setVisible(isPokemon);
        rowColecao.setVisible(isPokemon);

        rowSet.setVisible(!isPokemon);

        CardLayout cl = (CardLayout) panelSetSwitcher.getLayout();
        if (isOnePiece || isDragonBall) {
            cl.show(panelSetSwitcher, CARD_MANUAL);
        } else if (!isPokemon) {
            cl.show(panelSetSwitcher, CARD_COMBO);
            carregarSetsJogo(jogoId);
        } else {
            // Pokémon: não usa set genérico
            cl.show(panelSetSwitcher, CARD_COMBO);
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

    private void preencherCampos() {
        tfNome.setText(etbOrig.getNome());

        // jogo
        String jogoId = etbOrig.getJogoId();
        if (jogoId != null) {
            for (int i = 0; i < cbJogo.getItemCount(); i++) {
                JogoModel jm = cbJogo.getItemAt(i);
                if (jm.getId() != null && jm.getId().equals(jogoId)) {
                    cbJogo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // barcode
        String cod = etbOrig.getCodigoBarras();
        if (cod != null && !cod.isBlank()) {
            setCodigoBarras(cod);
        }

        // aplica campos por jogo
        atualizarCamposPorJogo();

        // Pokémon: série/coleção
        if (etbOrig.getJogoId() != null && etbOrig.getJogoId().equalsIgnoreCase("POKEMON")) {
            cbSerie.setSelectedItem(etbOrig.getSerie());
            carregarColecoesPorSerie();
            for (int i = 0; i < cbColecao.getItemCount(); i++) {
                ColecaoModel c = cbColecao.getItemAt(i);
                if (c.getName().equalsIgnoreCase(etbOrig.getColecao())) {
                    cbColecao.setSelectedIndex(i);
                    break;
                }
            }
        }

        // dropdown (não pokemon, não manual)
        if (etbOrig.getJogoId() != null
                && !etbOrig.getJogoId().equalsIgnoreCase("POKEMON")
                && !etbOrig.getJogoId().equalsIgnoreCase("ONEPIECE")
                && !etbOrig.getJogoId().equalsIgnoreCase("DRAGONBALL")) {

            for (int i = 0; i < cbSetJogo.getItemCount(); i++) {
                String nomeSet = cbSetJogo.getItemAt(i);
                if (nomeSet != null && nomeSet.equalsIgnoreCase(etbOrig.getSerie())) {
                    cbSetJogo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // manual
        if (etbOrig.getJogoId() != null
                && (etbOrig.getJogoId().equalsIgnoreCase("ONEPIECE")
                        || etbOrig.getJogoId().equalsIgnoreCase("DRAGONBALL"))) {
            tfSetManual.setText(etbOrig.getSerie());
        }

        cbTipo.setSelectedItem(etbOrig.getTipo());
        cbVersao.setSelectedItem(etbOrig.getVersao());
        tfQtd.setValue(etbOrig.getQuantidade());
        tfCusto.setValue(etbOrig.getPrecoCompra());
        tfPreco.setValue(etbOrig.getPrecoVenda());

        // NCM
        if (etbOrig.getNcm() != null) {
            for (int i = 0; i < cbNcm.getItemCount(); i++) {
                if (cbNcm.getItemAt(i).startsWith(etbOrig.getNcm())) {
                    cbNcm.setSelectedIndex(i);
                    break;
                }
            }
        }

        // fornecedor (mantém tua busca, mas sem jogar NPE na cara)
        try {
            fornecedorSel = new dao.FornecedorDAO().buscarPorId(etbOrig.getFornecedor());
            lblFornecedor.setText(fornecedorSel != null ? fornecedorSel.getNome() : "Fornecedor não cadastrado");
        } catch (Exception ex) {
            lblFornecedor.setText("Erro ao carregar fornecedor");
            ex.printStackTrace();
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
            String ncm = null;
            if (ncmCombo != null && ncmCombo.contains("-")) {
                ncm = ncmCombo.split("-")[0].trim();
            }

            String id = isEdicao ? etbOrig.getId() : UUID.randomUUID().toString();

            String jogoId = jogoSel.getId();
            boolean isPokemon = jogoId.equalsIgnoreCase("POKEMON");
            boolean isOnePiece = jogoId.equalsIgnoreCase("ONEPIECE");
            boolean isDragonBall = jogoId.equalsIgnoreCase("DRAGONBALL");

            String setSelecionado;
            if (isPokemon) {
                setSelecionado = (String) cbSerie.getSelectedItem();
            } else if (isOnePiece || isDragonBall) {
                setSelecionado = tfSetManual.getText().trim();
            } else {
                setSelecionado = (String) cbSetJogo.getSelectedItem();
            }

            String codigoBarras = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigoBarras == null)
                codigoBarras = "";

            EtbModel e = new EtbModel(
                    id,
                    tfNome.getText().trim(),
                    ((Number) tfQtd.getValue()).intValue(),
                    ((Number) tfCusto.getValue()).doubleValue(),
                    ((Number) tfPreco.getValue()).doubleValue(),
                    fornecedorSel != null ? fornecedorSel.getId() : null,
                    setSelecionado,
                    (cbColecao.getSelectedItem() != null)
                            ? ((ColecaoModel) cbColecao.getSelectedItem()).getName()
                            : "",
                    (String) cbTipo.getSelectedItem(),
                    (String) cbVersao.getSelectedItem(),
                    jogoSel.getId());

            e.setNcm(ncm);
            e.setFornecedorId(fornecedorSel.getId());
            e.setCodigoBarras(codigoBarras);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao)
                service.atualizarEtb(e);
            else
                service.salvarNovoEtb(e);

            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar ETB:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Tipo pode esconder set/coleção:
     * - Box colecionáveis: esconde tudo de set/coleção
     * - Trainer Kit: mantém set (painel), mas pode esconder coleção dependendo do
     * jogo
     */
    private void adjustFieldsByTipo() {
        String tipo = (String) cbTipo.getSelectedItem();
        boolean isBox = "Box colecionáveis".equals(tipo);
        boolean isTrainer = "Trainer Kit".equals(tipo);

        if (isBox) {
            rowSerie.setVisible(false);
            rowColecao.setVisible(false);
            rowSet.setVisible(false);
        } else {
            // volta ao modo por jogo
            atualizarCamposPorJogo();

            // Trainer Kit: não precisa coleção em geral, mas você decide:
            // aqui eu deixo coleção seguir o jogo (pokémon mostra, outros não mostram
            // mesmo).
            if (isTrainer) {
                // nada extra, só deixa o jogo mandar.
            }
        }

        revalidate();
        repaint();
        pack();
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
}
