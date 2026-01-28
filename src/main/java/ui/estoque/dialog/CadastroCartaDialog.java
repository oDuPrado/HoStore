package ui.estoque.dialog;

import service.EstoqueService;
import service.CartaService;
import dao.SetDAO;
import dao.ColecaoDAO;
import dao.CadastroGenericoDAO;
import dao.ConfigFiscalDefaultDAO;
import dao.FiscalCatalogDAO;
import model.ColecaoModel;
import model.ConfigFiscalModel;
import model.CodigoDescricaoModel;
import model.FornecedorModel;
import model.NcmModel;
import service.NcmService;
import ui.ajustes.dialog.FornecedorDialog;
import util.FormatterFactory;
import util.UiKit;
import java.awt.event.KeyEvent;
import ui.estoque.dialog.BuscarCartaDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CadastroCartaDialog extends JDialog {

    private final EstoqueService estoqueService = new EstoqueService();
    private final boolean isEdicao;
    private final model.Carta cartaOrig;

    /* Campos básicos */
    private final JTextField tfNome = new JTextField(24);
    private final JButton btnBuscarCarta = new JButton("Buscar Carta…");
    private final JLabel lblPrecoRef = new JLabel("Ref. Preço: R$ 0,00");
    private final JComboBox<String> cbSet = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    private final JTextField tfNumero = new JTextField(8);
    private final JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JComboBox<ComboItem> cbCondicao = new JComboBox<>();
    private final JFormattedTextField tfCusto = FormatterFactory.getMoneyField(0.0);
    private final JComboBox<ComboItem> cbIdioma = new JComboBox<>();
    private final JComboBox<String> cbNcm = new JComboBox<>();
    private final JComboBox<String> cbCfop = new JComboBox<>();
    private final JComboBox<String> cbCsosn = new JComboBox<>();
    private final JComboBox<String> cbOrigem = new JComboBox<>();
    private final JComboBox<String> cbUnidade = new JComboBox<>();

    /* Venda / consignado */
    private final JComboBox<String> cbTipoVenda = new JComboBox<>(new String[] { "Loja", "Consignado" });
    private final JComboBox<ComboItem> cbDono = new JComboBox<>();
    private final JFormattedTextField tfPrecoConsignado = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfPercentualLoja = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfValorLoja = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfPrecoVenda = FormatterFactory.getMoneyField(0.0);

    /* Especificações */
    private final JComboBox<ComboItem> cbTipoCarta = new JComboBox<>();
    private final JComboBox<ComboItem> cbSubtipo = new JComboBox<>();
    private final JComboBox<ComboItem> cbRaridade = new JComboBox<>();
    private final JComboBox<ComboItem> cbSubraridade = new JComboBox<>();
    private final JComboBox<ComboItem> cbIlustracao = new JComboBox<>();
    private final Map<String, List<ComboItem>> subtiposPorTipo = new HashMap<>();

    /* Fornecedor */
    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Selecionar…");
    private FornecedorModel fornecedorSelecionado;

    /** Painéis que alternam */
    private JPanel pnlConsignado;
    private JPanel pnlSubraridade;
    private JPanel bodyGrid; // pra revalidate limpo

    /** Helper para combobox id/label */
    private static class ComboItem {
        final String id, label;
        ComboItem(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
        String getId() { return id; }
    }

    public CadastroCartaDialog(Frame owner, model.Carta cartaExistente) {
        super(owner, cartaExistente == null ? "🃏 Nova Carta" : "🃏 Editar Carta", true);
        UiKit.applyDialogBase(this);

        this.isEdicao = cartaExistente != null;
        this.cartaOrig = cartaExistente;

        buildUI();
        bindDataAndLogic();

        // tamanho fixo “bonito” (evita quebrar ao alternar Consignado)
        setMinimumSize(new Dimension(980, 640));
        setSize(980, 640);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        // largura dos campos numéricos
        tfCusto.setColumns(8);
        tfPrecoConsignado.setColumns(8);
        tfPercentualLoja.setColumns(6);
        tfValorLoja.setColumns(8);
        tfPrecoVenda.setColumns(8);

        setLayout(new BorderLayout(12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title(isEdicao ? "Editar Carta" : "Cadastrar Nova Carta"));
        left.add(UiKit.hint("Cadastro completo com integração de busca. Campos mudam conforme tipo de venda."));
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btnImportarLiga = UiKit.ghost("Importar Liga");
        btnImportarLiga.addActionListener(e -> {
            Window win = SwingUtilities.getWindowAncestor(this);
            new ImportLigaDialog(win).setVisible(true);
        });

        // deixa os botões com aparência consistente com tema
        right.add(btnImportarLiga);
        right.add(UiKit.primary("🔎 Buscar Carta"));
        header.add(right, BorderLayout.EAST);

        // ligar o botão bonito ao seu botão real
        // (sem mexer na lógica, só roteando clique)
        ((JButton) right.getComponent(1)).addActionListener(e -> btnBuscarCarta.doClick());

        return header;
    }

    private JComponent buildBody() {
        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setOpaque(false);

        bodyGrid = new JPanel(new GridBagLayout());
        bodyGrid.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 0;
        g.gridy = 0;

        // Linha 0: Básico (maior) + Venda
        g.gridx = 0; g.weightx = 0.58;
        bodyGrid.add(buildCardBasico(), g);
        g.gridx = 1; g.weightx = 0.42;
        bodyGrid.add(buildCardVenda(), g);

        // Linha 1: Specs + Fornecedor
        g.gridy = 1;
        g.gridx = 0; g.weightx = 0.58;
        bodyGrid.add(buildCardSpecs(), g);
        g.gridx = 1; g.weightx = 0.42;
        bodyGrid.add(buildCardFornecedor(), g);

        // empurra pra cima e não deixa “vazio feio”
        g.gridy = 2;
        g.gridx = 0;
        g.gridwidth = 2;
        g.weighty = 1;
        bodyGrid.add(Box.createVerticalGlue(), g);

        wrap.add(bodyGrid, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildCardBasico() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Informações Básicas"), BorderLayout.WEST);
        head.add(UiKit.hint("Nome, set/coleção, numeração, custo e idioma"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(6, 6, 6, 6);
        f.anchor = GridBagConstraints.WEST;
        f.fill = GridBagConstraints.HORIZONTAL;

        // linha 0: Nome + preço ref
        f.gridy = 0;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Nome:"), f);
        f.gridx = 1; f.weightx = 1;
        JPanel nomeBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nomeBox.setOpaque(false);
        nomeBox.add(tfNome);
        nomeBox.add(btnBuscarCarta);
        form.add(nomeBox, f);

        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Ref.:"), f);
        f.gridx = 3; f.weightx = 0.4; form.add(lblPrecoRef, f);

        // linha 1: Set + Coleção
        f.gridy = 1;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Set:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbSet, f);
        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Coleção:"), f);
        f.gridx = 3; f.weightx = 0.4; form.add(cbColecao, f);

        // linha 2: Número + Quantidade + Condição
        f.gridy = 2;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Número:"), f);
        f.gridx = 1; f.weightx = 1; form.add(tfNumero, f);
        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Qtd:"), f);
        f.gridx = 3; f.weightx = 0.4; form.add(spQtd, f);

        // linha 3: Condição + Idioma + Custo
        f.gridy = 3;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Condição:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbCondicao, f);
        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Idioma:"), f);
        f.gridx = 3; f.weightx = 0.4; form.add(cbIdioma, f);

        // linha 4: custo
        f.gridy = 4;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Custo (R$):"), f);
        f.gridx = 1; f.weightx = 1; form.add(tfCusto, f);

        // linha 5: NCM + CFOP
        f.gridy = 5;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("NCM:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbNcm, f);
        f.gridx = 2; f.weightx = 0; form.add(new JLabel("CFOP:"), f);
        f.gridx = 3; f.weightx = 1; form.add(cbCfop, f);

        // linha 6: CSOSN + Origem
        f.gridy = 6;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("CSOSN:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbCsosn, f);
        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Origem:"), f);
        f.gridx = 3; f.weightx = 1; form.add(cbOrigem, f);

        // linha 7: Unidade
        f.gridy = 7;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Unidade:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbUnidade, f);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardVenda() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Venda / Consignado"), BorderLayout.WEST);
        head.add(UiKit.hint("Alterna campos automaticamente"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(6, 6, 6, 6);
        f.anchor = GridBagConstraints.WEST;
        f.fill = GridBagConstraints.HORIZONTAL;

        // tipo venda
        f.gridy = 0;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Tipo:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbTipoVenda, f);

        // painel consignado
        pnlConsignado = new JPanel(new GridBagLayout());
        pnlConsignado.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.gridx = 0; c.weightx = 0; pnlConsignado.add(new JLabel("Dono:"), c);
        c.gridx = 1; c.weightx = 1; pnlConsignado.add(cbDono, c);

        c.gridy = 1;
        c.gridx = 0; c.weightx = 0; pnlConsignado.add(new JLabel("Preço Cons.:"), c);
        c.gridx = 1; c.weightx = 1; pnlConsignado.add(tfPrecoConsignado, c);

        c.gridy = 2;
        c.gridx = 0; c.weightx = 0; pnlConsignado.add(new JLabel("% Loja:"), c);
        c.gridx = 1; c.weightx = 1; pnlConsignado.add(tfPercentualLoja, c);

        c.gridy = 3;
        c.gridx = 0; c.weightx = 0; pnlConsignado.add(new JLabel("Valor Loja:"), c);
        c.gridx = 1; c.weightx = 1; pnlConsignado.add(tfValorLoja, c);

        // adiciona consignado abaixo do tipo
        f.gridy = 1;
        f.gridx = 0;
        f.gridwidth = 2;
        form.add(pnlConsignado, f);
        f.gridwidth = 1;

        // preço venda sempre visível
        f.gridy = 2;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Preço Venda:"), f);
        f.gridx = 1; f.weightx = 1; form.add(tfPrecoVenda, f);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardSpecs() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Especificações"), BorderLayout.WEST);
        head.add(UiKit.hint("Tipo, subtipo, raridade e ilustração"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(6, 6, 6, 6);
        f.anchor = GridBagConstraints.WEST;
        f.fill = GridBagConstraints.HORIZONTAL;

        // tipo + subtipo
        f.gridy = 0;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Tipo:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbTipoCarta, f);

        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Subtipo:"), f);
        f.gridx = 3; f.weightx = 1; form.add(cbSubtipo, f);

        // raridade + subraridade
        f.gridy = 1;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Raridade:"), f);
        f.gridx = 1; f.weightx = 1; form.add(cbRaridade, f);

        f.gridx = 2; f.weightx = 0; form.add(new JLabel("Sub-raridade:"), f);
        pnlSubraridade = new JPanel(new BorderLayout());
        pnlSubraridade.setOpaque(false);
        pnlSubraridade.add(cbSubraridade, BorderLayout.CENTER);
        f.gridx = 3; f.weightx = 1; form.add(pnlSubraridade, f);

        // ilustração
        f.gridy = 2;
        f.gridx = 0; f.weightx = 0; form.add(new JLabel("Ilustração:"), f);
        f.gridx = 1; f.gridwidth = 3; f.weightx = 1; form.add(cbIlustracao, f);
        f.gridwidth = 1;

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardFornecedor() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Fornecedor"), BorderLayout.WEST);
        head.add(UiKit.hint("Opcional"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        line.setOpaque(false);
        lblFornecedor.setBorder(new EmptyBorder(2, 6, 2, 6));
        line.add(new JLabel("Selecionado:"));
        line.add(lblFornecedor);
        
        JPanel fornButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        fornButtons.setOpaque(false);
        fornButtons.add(btnSelectFornec);
        
        JButton btnNovoFornec = new JButton("➕ Criar");
        btnNovoFornec.addActionListener(e -> criarNovoFornecedor((JFrame) getOwner()));
        fornButtons.add(btnNovoFornec);
        
        line.add(fornButtons);

        card.add(line, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Revise os campos antes de salvar. O modo Consignado exibe campos adicionais."), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btCancelar = UiKit.ghost("Cancelar");
        JButton btSalvar = UiKit.primary(isEdicao ? "Atualizar" : "Cadastrar");

        btCancelar.addActionListener(e -> dispose());
        btSalvar.addActionListener(e -> onSalvar());

        actions.add(btCancelar);
        actions.add(btSalvar);
        footer.add(actions, BorderLayout.EAST);

        // tecla ESC fecha
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        return footer;
    }

    private void bindDataAndLogic() {
        // lookups e listeners: sua lógica original, só rearrumada
        try {
            new SetDAO().listarSeriesUnicas().forEach(cbSet::addItem);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries: " + e.getMessage());
        }
        cbSet.addActionListener(e -> carregarColecoes());
        carregarColecoes();

        carregarLookup("condicoes", cbCondicao);
        carregarLookup("linguagens", cbIdioma);
        carregarLookup("tipo_cartas", cbTipoCarta);
        carregarSubtiposAgrupados();

        cbTipoCarta.addActionListener(e -> {
            atualizarSubtiposPorTipo();
            toggleSubRaridade();
        });

        carregarLookup("raridades", cbRaridade);
        cbRaridade.addActionListener(e -> toggleSubRaridade());

        carregarLookup("sub_raridades", cbSubraridade);
        carregarLookup("ilustracoes", cbIlustracao);
        carregarClientes();
        carregarNcms();
        carregarCombosFiscais();
        aplicarDefaultsFiscais();

        cbTipoVenda.addActionListener(e -> toggleConsignado());

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { atualizarValorLoja(); }
            public void removeUpdate(DocumentEvent e) { atualizarValorLoja(); }
            public void changedUpdate(DocumentEvent e) { atualizarValorLoja(); }
        };
        tfPrecoConsignado.getDocument().addDocumentListener(dl);
        tfPercentualLoja.getDocument().addDocumentListener(dl);

        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog((Frame) getOwner());
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSelecionado = f;
                lblFornecedor.setText(f.getNome());
            }
        });

        btnBuscarCarta.addActionListener(e -> {
            BuscarCartaDialog dlg = new BuscarCartaDialog((Frame) getOwner());
            dlg.setVisible(true);
            BuscarCartaDialog.Carta api = dlg.getCartaSelecionada();
            if (api != null) {
                tfNome.setText(api.getName());
                double usd = api.getPrecoUSD();
                double brl = usd * BuscarCartaDialog.COTACAO_DOLAR;
                lblPrecoRef.setText(String.format("Ref. Preço: $%.2f / R$ %.2f", usd, brl));
                tfPrecoVenda.setValue(brl);

                cbSet.removeAllItems();
                cbSet.addItem(api.getSetSeries());
                cbSet.setSelectedIndex(0);

                carregarColecoes();

                try {
                    ColecaoModel m = new ColecaoDAO().buscarPorId(api.getSetId());
                    cbColecao.removeAllItems();
                    cbColecao.addItem(m);
                    cbColecao.setSelectedIndex(0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Falha ao buscar coleção no banco:\n" + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }

                tfNumero.setText(api.getNumber());
            }
        });

        toggleConsignado();
        if (isEdicao) preencherCampos();
    }

    private void carregarColecoes() {
        try {
            cbColecao.removeAllItems();
            String serie = (String) cbSet.getSelectedItem();
            if (serie == null) return;
            for (ColecaoModel cm : new ColecaoDAO().listarPorSerie(serie)) cbColecao.addItem(cm);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void carregarLookup(String tabela, JComboBox<ComboItem> combo) {
        try {
            combo.removeAllItems();
            for (Map<String, String> m : new CadastroGenericoDAO(tabela, "id", "nome").listar()) {
                combo.addItem(new ComboItem(m.get("id"), m.get("nome")));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar " + tabela + ".");
        }
    }

    private void carregarClientes() {
        try {
            cbDono.removeAllItems();
            for (Map<String, String> m : new CadastroGenericoDAO("clientes", "id", "nome").listar()) {
                cbDono.addItem(new ComboItem(m.get("id"), m.get("nome")));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar clientes.");
        }
    }

    private void carregarSubtiposAgrupados() {
        subtiposPorTipo.clear();
        subtiposPorTipo.put("T1", List.of(
                new ComboItem("S1", "Básico"),
                new ComboItem("S2", "Estágio 1"),
                new ComboItem("S3", "Estágio 2")));
        subtiposPorTipo.put("T2", List.of(
                new ComboItem("S4", "Item"),
                new ComboItem("S5", "Suporte"),
                new ComboItem("S6", "Estádio"),
                new ComboItem("S7", "Ferramenta")));
        subtiposPorTipo.put("T3", List.of(
                new ComboItem("S8", "Água"),
                new ComboItem("S9", "Fogo"),
                new ComboItem("S10", "Grama"),
                new ComboItem("S11", "Elétrico"),
                new ComboItem("S12", "Lutador"),
                new ComboItem("S13", "Noturno"),
                new ComboItem("S14", "Psíquico"),
                new ComboItem("S15", "Metálico"),
                new ComboItem("S16", "Dragão"),
                new ComboItem("S17", "Incolor")));
    }

    private void atualizarSubtiposPorTipo() {
        ComboItem tipoSel = (ComboItem) cbTipoCarta.getSelectedItem();
        if (tipoSel == null) return;

        List<ComboItem> lista = subtiposPorTipo.get(tipoSel.getId());
        cbSubtipo.removeAllItems();
        if (lista != null) for (ComboItem item : lista) cbSubtipo.addItem(item);
    }

    private void toggleSubRaridade() {
        ComboItem tipo = (ComboItem) cbTipoCarta.getSelectedItem();
        ComboItem raridade = (ComboItem) cbRaridade.getSelectedItem();
        if (tipo == null || raridade == null) return;

        String rarId = raridade.getId();
        boolean mostrar = !(rarId.equals("R1") || rarId.equals("R2") || rarId.equals("R3")
                || rarId.equals("R5") || rarId.equals("R6"));

        pnlSubraridade.setVisible(mostrar);

        // UI-only: revalida sem pack (pack é o que quebra e encolhe)
        pnlSubraridade.revalidate();
        pnlSubraridade.repaint();
        bodyGrid.revalidate();
        bodyGrid.repaint();
    }

    private void toggleConsignado() {
        boolean cons = "Consignado".equals(cbTipoVenda.getSelectedItem());
        pnlConsignado.setVisible(cons);

        // UI-only: não usa pack, porque pack no GridBag “dança”
        pnlConsignado.revalidate();
        pnlConsignado.repaint();
        bodyGrid.revalidate();
        bodyGrid.repaint();
    }

    private void atualizarValorLoja() {
        if (!pnlConsignado.isVisible()) return;
        try {
            double pc = UiKit.getDoubleValue(tfPrecoConsignado, 0.0);
            double pct = UiKit.getDoubleValue(tfPercentualLoja, 0.0);
            tfValorLoja.setValue(pc * pct / 100.0);
        } catch (Exception ignored) {}
    }

    // ======= ABAIXO: sua lógica original intacta =======

    private void preencherCampos() {
        tfNome.setText(cartaOrig.getNome());
        tfNumero.setText(cartaOrig.getNumero());
        spQtd.setValue(cartaOrig.getQtd());
        tfCusto.setValue(cartaOrig.getCusto());
        tfPrecoConsignado.setValue(cartaOrig.getPrecoConsignado());
        tfPercentualLoja.setValue(cartaOrig.getPercentualLoja());
        tfValorLoja.setValue(cartaOrig.getValorLoja());
        tfPrecoVenda.setValue(cartaOrig.getPrecoLoja());

        cbSet.removeAllItems();
        cbSet.addItem(cartaOrig.getSetId());
        cbSet.setSelectedIndex(0);
        carregarColecoes();

        cbColecao.removeAllItems();
        ColecaoModel colecao = new ColecaoModel();
        colecao.setName(cartaOrig.getColecao());
        colecao.setSigla(null);
        cbColecao.addItem(colecao);
        cbColecao.setSelectedIndex(0);

        selecionarComboBox(cbCondicao, cartaOrig.getCondicaoId());
        selecionarComboBox(cbIdioma, cartaOrig.getLinguagemId());

        selecionarComboBox(cbTipoCarta, cartaOrig.getTipoId());
        atualizarSubtiposPorTipo();
        toggleSubRaridade();

        selecionarComboBox(cbSubtipo, cartaOrig.getSubtipoId());
        selecionarComboBox(cbRaridade, cartaOrig.getRaridadeId());
        selecionarComboBox(cbSubraridade, cartaOrig.getSubRaridadeId());
        selecionarComboBox(cbIlustracao, cartaOrig.getIlustracaoId());

        cbTipoVenda.setSelectedItem(cartaOrig.isConsignado() ? "Consignado" : "Loja");
        toggleConsignado();

        if (cartaOrig.isConsignado()) selecionarComboBox(cbDono, cartaOrig.getDono());
    }

    private void selecionarComboBox(JComboBox<ComboItem> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            ComboItem item = combo.getItemAt(i);
            if (item.getId().equals(id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void onSalvar() {
        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome obrigatório");
            return;
        }

        try {
            // 1) Nome da carta
            String nome = tfNome.getText().trim();
            System.out.println("[LOG] Nome da carta: " + nome);

            // 2) Set (série) selecionada no combo cbSet
            String setId = (String) cbSet.getSelectedItem();
            System.out.println("[LOG] setId (série) selecionada: " + setId);

            // 3) Coleção selecionada no combo cbColecao
            ColecaoModel colecaoSelecionada = (ColecaoModel) cbColecao.getSelectedItem();
            if (colecaoSelecionada == null) {
                System.out.println("[LOG] AVISO: não há coleção selecionada (colecaoSelecionada == null)");
            } else {
                System.out.println("[LOG] ColecaoModel selecionada -> id: "
                        + colecaoSelecionada.getId()
                        + " | nome: " + colecaoSelecionada.getName()
                        + " | sigla: " + colecaoSelecionada.getSigla()
                        + " | series: " + colecaoSelecionada.getSeries()
                        + " | data_lancamento: " + colecaoSelecionada.getReleaseDate());
            }

            // 4) Extrai o nome da coleção (para salvar no campo “colecao” da tabela cartas)
            String colecao = colecaoSelecionada != null
                    ? colecaoSelecionada.getName()
                    : "";
            System.out.println("[LOG] Valor usado em 'colecao' (nome): " + colecao);

            // 5) Extrai a sigla da coleção (para usar na montagem do ID)
            String sigla = "SEMID";
            if (colecaoSelecionada != null && colecaoSelecionada.getSigla() != null) {
                sigla = colecaoSelecionada.getSigla();
            }
            System.out.println("[LOG] Sigla usada para gerar ID: " + sigla);

            // 6) Número da carta (campo tfNumero)
            String num = tfNumero.getText().trim();
            System.out.println("[LOG] Número (numero): " + num);

            // 7) Sub-raridade selecionada
            ComboItem subraridadeItem = (ComboItem) cbSubraridade.getSelectedItem();
            String srar = subraridadeItem != null
                    ? subraridadeItem.getId()
                    : "";
            System.out.println("[LOG] sub_raridade selecionada (srar): " + srar);

            // 8) Montagem do ID completo:
            String id;
            if (isEdicao) {
                id = cartaOrig.getId();
                System.out.println("[LOG] Modo EDIÇÃO -> reaproveitando ID original: " + id);
            } else {
                String sufixoR = (srar != null && !srar.isEmpty()) ? "-R" : "";
                id = sigla + "-" + num + sufixoR;
                System.out.println("[LOG] Modo NOVO -> ID gerado: " + id);
            }

            // 9) Quantidade
            int qtd = (Integer) spQtd.getValue();
            System.out.println("[LOG] Quantidade (qtd): " + qtd);

            // 10) Condição
            ComboItem condItem = (ComboItem) cbCondicao.getSelectedItem();
            String cond = condItem != null
                    ? condItem.getId()
                    : "";
            System.out.println("[LOG] condicao_id selecionada (cond): " + cond);

            // 11) Verifica se o custo foi preenchido
            if (tfCusto.getValue() == null) {
                JOptionPane.showMessageDialog(this, "⚠️ Custo é obrigatório!");
                tfCusto.requestFocus();
                System.out.println("[LOG] Custo NULO -> abortando gravação");
                return;
            }
            double custo = UiKit.getDoubleValue(tfCusto, 0.0);
            System.out.println("[LOG] custo: " + custo);

            // 12) Idioma
            ComboItem langItem = (ComboItem) cbIdioma.getSelectedItem();
            String lang = langItem != null
                    ? langItem.getId()
                    : "";
            System.out.println("[LOG] linguagem_id selecionada (lang): " + lang);

            // 13) Verifica se é consignado ou loja
            boolean cons = "Consignado".equals(cbTipoVenda.getSelectedItem());
            String donoId;
            if (cons) {
                ComboItem donoItem = (ComboItem) cbDono.getSelectedItem();
                donoId = donoItem != null
                        ? donoItem.getId()
                        : "";
            } else {
                donoId = "Loja";
            }
            System.out.println("[LOG] consignado? " + cons + " | donoId: " + donoId);

            // 14) Tipo da carta
            ComboItem tipoItem = (ComboItem) cbTipoCarta.getSelectedItem();
            String tipo = tipoItem != null
                    ? tipoItem.getId()
                    : "";
            System.out.println("[LOG] tipo_id selecionado (tipo): " + tipo);

            // 15) Subtipo da carta
            ComboItem subItem = (ComboItem) cbSubtipo.getSelectedItem();
            String sub = subItem != null
                    ? subItem.getId()
                    : "";
            System.out.println("[LOG] subtipo_id selecionado (sub): " + sub);

            // 16) Raridade da carta
            ComboItem raridadeItem = (ComboItem) cbRaridade.getSelectedItem();
            String rar = raridadeItem != null
                    ? raridadeItem.getId()
                    : "";
            System.out.println("[LOG] raridade_id selecionada (rar): " + rar);

            // 17) Ilustração da carta
            ComboItem iluItem = (ComboItem) cbIlustracao.getSelectedItem();
            String ilu = iluItem != null
                    ? iluItem.getId()
                    : "";
            System.out.println("[LOG] ilustracao_id selecionada (ilu): " + ilu);

            // 18) Preço consignado, percentual e valor Loja
            double precoCons = cons
                    ? UiKit.getDoubleValue(tfPrecoConsignado, 0.0)
                    : 0.0;
            double percLoja = cons
                    ? UiKit.getDoubleValue(tfPercentualLoja, 0.0)
                    : 0.0;
            double valorLoja = cons
                    ? UiKit.getDoubleValue(tfValorLoja, 0.0)
                    : 0.0;
            System.out.println("[LOG] precoCons: " + precoCons
                    + " | percLoja: " + percLoja
                    + " | valorLoja: " + valorLoja);

            // 19) Preço de venda
            double precoVenda = UiKit.getDoubleValue(tfPrecoVenda, 0.0);
            System.out.println("[LOG] precoVenda: " + precoVenda);

            // 20) Monta o objeto Carta para salvar
            model.Carta c = new model.Carta(
                    id, nome, setId, colecao, num, qtd,
                    precoVenda, precoCons, percLoja, valorLoja,
                    custo, cond, lang, cons, donoId,
                    tipo, sub, rar, srar, ilu,
                    fornecedorSelecionado != null
                            ? fornecedorSelecionado.getId()
                            : null
            );
            System.out.println("[LOG] Objeto Carta preparado para salvar: " + c);

            // 21) Chama o serviço para salvar ou atualizar
            new CartaService().salvarOuAtualizarCarta(c);
            System.out.println("[LOG] Carta enviada para CartaService.salvarOuAtualizarCarta()");

            // 22) Fecha o diálogo
            dispose();
            System.out.println("[LOG] CadastroCartaDialog fechado com sucesso.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.out);
            System.out.println("[LOG] Exceção ao salvar carta: " + ex.getMessage());
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
