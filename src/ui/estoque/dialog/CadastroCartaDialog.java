package ui.estoque.dialog;

import service.EstoqueService;
import dao.SetDAO;
import dao.ColecaoDAO;
import dao.CadastroGenericoDAO;
import model.ColecaoModel;
import model.FornecedorModel;
import util.FormatterFactory;
import ui.estoque.dialog.BuscarCartaDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dialog completo e definitivo de cadastro / edição de Carta
 * • Integração com BuscarCartaDialog para pré-povoar campos a partir da API
 * • Campos de consignado ficam ocultos quando o tipo de venda é “Loja”
 * • Valor Loja é calculado automaticamente a partir de Preço Consignado × %
 * Loja
 */
public class CadastroCartaDialog extends JDialog {

    private final EstoqueService estoqueService = new EstoqueService();
    private final boolean isEdicao;
    private final model.Carta cartaOrig;

    /* Campos básicos */
    private final JTextField tfNome = new JTextField(20);
    private final JButton btnBuscarCarta = new JButton("Buscar Carta…");
    private final JLabel lblPrecoRef = new JLabel("Ref. Preço: R$ 0.00");
    private final JComboBox<String> cbSet = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    private final JTextField tfNumero = new JTextField(6);
    private final JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JComboBox<ComboItem> cbCondicao = new JComboBox<>();
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JComboBox<ComboItem> cbIdioma = new JComboBox<>();

    /* Venda / consignado */
    private final JComboBox<String> cbTipoVenda = new JComboBox<>(new String[] { "Loja", "Consignado" });
    private final JComboBox<ComboItem> cbDono = new JComboBox<>();
    private final JFormattedTextField tfPrecoConsignado = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPercentualLoja = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfValorLoja = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPrecoVenda = FormatterFactory.getFormattedDoubleField(0.0);

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

    /** Painel que agrupa os campos de consignado */
    private JPanel pnlConsignado;
    private JPanel pnlSubraridade;

    /** Helper para combobox id/label */
    private static class ComboItem {
        final String id, label;

        ComboItem(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        String getId() {
            return id;
        }
    }

    public CadastroCartaDialog(Frame owner, model.Carta cartaExistente) {
        super(owner, cartaExistente == null ? "Nova Carta" : "Editar Carta", true);
        this.isEdicao = cartaExistente != null;
        this.cartaOrig = cartaExistente;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        // largura dos JFormattedTextField
        tfCusto.setColumns(8);
        tfPrecoConsignado.setColumns(8);
        tfPercentualLoja.setColumns(5);
        tfValorLoja.setColumns(8);
        tfPrecoVenda.setColumns(8);

        setLayout(new BorderLayout(8, 8));
        JPanel pnlMain = new JPanel(new GridBagLayout());
        pnlMain.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints root = new GridBagConstraints();
        root.insets = new Insets(4, 4, 4, 4);
        root.gridx = 0;
        root.gridy = 0;
        root.gridwidth = 2;
        root.fill = GridBagConstraints.HORIZONTAL;

        /* Informações Básicas */
        JPanel pnlBasic = new JPanel(new GridBagLayout());
        pnlBasic.setBorder(BorderFactory.createTitledBorder("Informações Básicas"));
        GridBagConstraints b = new GridBagConstraints();
        b.insets = new Insets(4, 4, 4, 4);
        b.anchor = GridBagConstraints.EAST;

        // nome + botão de busca
        JPanel pnlNome = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlNome.add(tfNome);
        pnlNome.add(btnBuscarCarta);
        addRow(pnlBasic, b, 0, "Nome:", pnlNome);

        addRow(pnlBasic, b, 1, "Preço Ref.:", lblPrecoRef);
        addRow(pnlBasic, b, 2, "Set:", cbSet);
        addRow(pnlBasic, b, 3, "Coleção:", cbColecao);
        addRow(pnlBasic, b, 4, "Número:", tfNumero);
        addRow(pnlBasic, b, 5, "Quantidade:", spQtd);
        addRow(pnlBasic, b, 6, "Condição:", cbCondicao);
        addRow(pnlBasic, b, 7, "Custo (R$):", tfCusto);
        addRow(pnlBasic, b, 8, "Idioma:", cbIdioma);

        /* Venda / Consignado */
        JPanel pnlVenda = new JPanel(new GridBagLayout());
        pnlVenda.setBorder(BorderFactory.createTitledBorder("Venda / Consignado"));
        GridBagConstraints v = new GridBagConstraints();
        v.insets = new Insets(4, 4, 4, 4);
        v.anchor = GridBagConstraints.EAST;
        addRow(pnlVenda, v, 0, "Tipo Venda:", cbTipoVenda);

        pnlConsignado = new JPanel(new GridBagLayout());
        pnlConsignado.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.EAST;
        addRow(pnlConsignado, c, 0, "Dono:", cbDono);
        addRow(pnlConsignado, c, 1, "Preço Consignado:", tfPrecoConsignado);
        addRow(pnlConsignado, c, 2, "% Loja:", tfPercentualLoja);
        addRow(pnlConsignado, c, 3, "Valor Loja:", tfValorLoja);
        v.gridy = 1;
        v.gridx = 0;
        v.gridwidth = 2;
        pnlVenda.add(pnlConsignado, v);
        v.gridwidth = 1;
        v.gridy = 2;
        addRow(pnlVenda, v, 2, "Preço Venda:", tfPrecoVenda);

        /* Especificações */
        JPanel pnlSpecs = new JPanel(new GridBagLayout());
        pnlSpecs.setBorder(BorderFactory.createTitledBorder("Especificações da Carta"));
        GridBagConstraints s = new GridBagConstraints();
        s.insets = new Insets(4, 4, 4, 4);
        s.anchor = GridBagConstraints.EAST;
        addRow(pnlSpecs, s, 0, "Tipo:", cbTipoCarta);
        addRow(pnlSpecs, s, 1, "Subtipo:", cbSubtipo);
        addRow(pnlSpecs, s, 2, "Raridade:", cbRaridade);
        pnlSubraridade = new JPanel(new BorderLayout());
        pnlSubraridade.add(cbSubraridade, BorderLayout.CENTER);

        addRow(pnlSpecs, s, 3, "Sub-raridade:", pnlSubraridade);

        addRow(pnlSpecs, s, 4, "Ilustração:", cbIlustracao);

        /* Fornecedor */
        JPanel pnlFornec = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlFornec.setBorder(BorderFactory.createTitledBorder("Fornecedor"));
        pnlFornec.add(lblFornecedor);
        pnlFornec.add(btnSelectFornec);

        /* agrega tudo */
        root.gridy = 0;
        pnlMain.add(pnlBasic, root);
        root.gridy = 1;
        pnlMain.add(pnlVenda, root);
        root.gridy = 2;
        pnlMain.add(pnlSpecs, root);
        root.gridy = 3;
        pnlMain.add(pnlFornec, root);
        add(pnlMain, BorderLayout.CENTER);

        /* botões Salvar */
        JPanel pnlBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btSalvar = new JButton(isEdicao ? "Atualizar" : "Cadastrar");
        pnlBtn.add(btSalvar);
        add(pnlBtn, BorderLayout.SOUTH);

        /* lookups */
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
        carregarSubtiposAgrupados(); // Carrega os subtipos por tipo manualmente
        cbTipoCarta.addActionListener(e -> atualizarSubtiposPorTipo());
        cbTipoCarta.addActionListener(e -> {
            atualizarSubtiposPorTipo();
            toggleSubRaridade();
        });
        carregarLookup("raridades", cbRaridade);
        carregarLookup("sub_raridades", cbSubraridade);
        carregarLookup("ilustracoes", cbIlustracao);
        carregarClientes();

        /* listeners */
        cbTipoVenda.addActionListener(e -> toggleConsignado());
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                atualizarValorLoja();
            }

            public void removeUpdate(DocumentEvent e) {
                atualizarValorLoja();
            }

            public void changedUpdate(DocumentEvent e) {
                atualizarValorLoja();
            }
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

        // integração com API
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

                // preenche Set com series e Coleção com name
                cbSet.removeAllItems();
                cbSet.addItem(api.getSetSeries()); // ✅ Agora mostra a série correta no campo Set

                cbSet.setSelectedIndex(0);

                carregarColecoes();
                cbColecao.removeAllItems();
                ColecaoModel m = new ColecaoModel();
                m.setId(api.getSetId());
                m.setName(api.getSetName());
                cbColecao.addItem(m);

                cbColecao.setSelectedIndex(0);

                tfNumero.setText(api.getNumber());
            }
        });

        btSalvar.addActionListener(e -> onSalvar());
        toggleConsignado();
        if (isEdicao)
            preencherCampos();
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridy = row;
        g.gridx = 0;
        g.anchor = GridBagConstraints.EAST;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        g.anchor = GridBagConstraints.WEST;
        p.add(field, g);
    }

    private void carregarColecoes() {
        try {
            cbColecao.removeAllItems();
            String serie = (String) cbSet.getSelectedItem();
            if (serie == null)
                return;
            for (ColecaoModel cm : new ColecaoDAO().listarPorSerie(serie))
                cbColecao.addItem(cm);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void carregarLookup(String tabela, JComboBox<ComboItem> combo) {
        try {
            combo.removeAllItems();
            for (Map<String, String> m : new CadastroGenericoDAO(tabela, "id", "nome").listar())
                combo.addItem(new ComboItem(m.get("id"), m.get("nome")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar " + tabela + ".");
        }
    }

    private void carregarClientes() {
        try {
            cbDono.removeAllItems();
            for (Map<String, String> m : new CadastroGenericoDAO("clientes", "id", "nome").listar())
                cbDono.addItem(new ComboItem(m.get("id"), m.get("nome")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar clientes.");
        }
    }

    // Carrega os subtipos já agrupados por tipo
    private void carregarSubtiposAgrupados() {
        subtiposPorTipo.clear();

        // T1 = Pokémon
        subtiposPorTipo.put("T1", List.of(
                new ComboItem("S1", "Básico"),
                new ComboItem("S2", "Estágio 1"),
                new ComboItem("S3", "Estágio 2")));

        // T2 = Treinador
        subtiposPorTipo.put("T2", List.of(
                new ComboItem("S4", "Item"),
                new ComboItem("S5", "Suporte"),
                new ComboItem("S6", "Estádio"),
                new ComboItem("S7", "Ferramenta")));

        // T3 = Energia
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

    // Atualiza a lista do combo de subtipo baseado no tipo selecionado
    private void atualizarSubtiposPorTipo() {
        ComboItem tipoSel = (ComboItem) cbTipoCarta.getSelectedItem();
        if (tipoSel == null)
            return;

        List<ComboItem> lista = subtiposPorTipo.get(tipoSel.getId());
        cbSubtipo.removeAllItems();

        if (lista != null) {
            for (ComboItem item : lista) {
                cbSubtipo.addItem(item);
            }
        }
    }

    private void toggleSubRaridade() {
        ComboItem tipo = (ComboItem) cbTipoCarta.getSelectedItem();
        if (tipo == null)
            return;

        boolean mostrar = "T1".equals(tipo.getId()); // T1 = Pokémon
        pnlSubraridade.setVisible(mostrar);
        pnlSubraridade.getParent().revalidate();
        pnlSubraridade.getParent().repaint();
    }

    private void toggleConsignado() {
        boolean cons = "Consignado".equals(cbTipoVenda.getSelectedItem());
        pnlConsignado.setVisible(cons);
        // revalida o container que contém o pnlConsignado
        pnlConsignado.getParent().revalidate();
        pnlConsignado.getParent().revalidate();
        // ajusta o tamanho do diálogo ao novo layout
        pack();
    }

    private void atualizarValorLoja() {
        if (!pnlConsignado.isVisible())
            return;
        try {
            double pc = ((Number) tfPrecoConsignado.getValue()).doubleValue();
            double pct = ((Number) tfPercentualLoja.getValue()).doubleValue();
            tfValorLoja.setValue(pc * pct / 100.0);
        } catch (Exception ignored) {
        }
    }

    private void preencherCampos() {
        tfNome.setText(cartaOrig.getNome());
        tfNumero.setText(cartaOrig.getNumero());
        spQtd.setValue(cartaOrig.getQtd());
        tfCusto.setValue(cartaOrig.getCusto());
        tfPrecoConsignado.setValue(cartaOrig.getPrecoConsignado());
        tfPercentualLoja.setValue(cartaOrig.getPercentualLoja());
        tfValorLoja.setValue(cartaOrig.getValorLoja());
        tfPrecoVenda.setValue(cartaOrig.getPrecoLoja());

        // Set
        cbSet.removeAllItems();
        cbSet.addItem(cartaOrig.getSetId());
        cbSet.setSelectedIndex(0);
        carregarColecoes();

        // Coleção
        cbColecao.removeAllItems();
        ColecaoModel colecao = new ColecaoModel();
        colecao.setName(cartaOrig.getColecao());
        cbColecao.addItem(colecao);
        cbColecao.setSelectedIndex(0);

        // Condição
        selecionarComboBox(cbCondicao, cartaOrig.getCondicaoId());

        // Idioma
        selecionarComboBox(cbIdioma, cartaOrig.getLinguagemId());

        // Tipo/Subtipo/Raridade
        selecionarComboBox(cbTipoCarta, cartaOrig.getTipoId());
        atualizarSubtiposPorTipo(); // ← recarrega os subtipos corretos
        toggleSubRaridade();
        selecionarComboBox(cbSubtipo, cartaOrig.getSubtipoId());

        selecionarComboBox(cbRaridade, cartaOrig.getRaridadeId());
        selecionarComboBox(cbSubraridade, cartaOrig.getSubRaridadeId());
        selecionarComboBox(cbIlustracao, cartaOrig.getIlustracaoId());

        // Consignado
        cbTipoVenda.setSelectedItem(cartaOrig.isConsignado() ? "Consignado" : "Loja");
        toggleConsignado();

        // Dono
        if (cartaOrig.isConsignado()) {
            selecionarComboBox(cbDono, cartaOrig.getDono());
        }
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

        // Remova ou comente esta parte:
        // if (fornecedorSelecionado == null) {
        // JOptionPane.showMessageDialog(this, "Selecione um fornecedor");
        // return;
        // }
        try {
            String id = isEdicao ? cartaOrig.getId() : UUID.randomUUID().toString();
            String nome = tfNome.getText().trim();
            String setId = (String) cbSet.getSelectedItem();
            String colecao = cbColecao.getSelectedItem() != null
                    ? cbColecao.getSelectedItem().toString()
                    : "";
            String num = tfNumero.getText().trim();
            int qtd = (Integer) spQtd.getValue();
            String cond = ((ComboItem) cbCondicao.getSelectedItem()).getId();
            if (tfCusto.getValue() == null) {
                JOptionPane.showMessageDialog(this, "⚠️ Custo é obrigatório!");
                tfCusto.requestFocus();
                return;
            }
            double custo = ((Number) tfCusto.getValue()).doubleValue();

            String lang = ((ComboItem) cbIdioma.getSelectedItem()).getId();

            boolean cons = "Consignado".equals(cbTipoVenda.getSelectedItem());
            String donoId = cons
                    ? ((ComboItem) cbDono.getSelectedItem()).getId()
                    : "Loja";

            String tipo = ((ComboItem) cbTipoCarta.getSelectedItem()).getId();
            String sub = ((ComboItem) cbSubtipo.getSelectedItem()).getId();
            String rar = ((ComboItem) cbRaridade.getSelectedItem()).getId();
            String srar = ((ComboItem) cbSubraridade.getSelectedItem()).getId();
            String ilu = ((ComboItem) cbIlustracao.getSelectedItem()).getId();

            double precoCons = cons ? ((Number) tfPrecoConsignado.getValue()).doubleValue() : 0.0;
            double percLoja = cons ? ((Number) tfPercentualLoja.getValue()).doubleValue() : 0.0;
            double valorLoja = cons ? ((Number) tfValorLoja.getValue()).doubleValue() : 0.0;
            double precoVenda = ((Number) tfPrecoVenda.getValue()).doubleValue();

            model.Carta c = new model.Carta(
                    id, nome, setId, colecao, num, qtd,
                    precoVenda, precoCons, percLoja, valorLoja,
                    custo, cond, lang, cons, donoId,
                    tipo, sub, rar, srar, ilu,
                    fornecedorSelecionado != null ? fornecedorSelecionado.getId() : null);

            if (isEdicao) {
                estoqueService.atualizarCarta(c);

                model.ProdutoModel p = new model.ProdutoModel(
                        c.getId(),
                        c.getNome(),
                        "Carta",
                        c.getQtd(),
                        c.getCusto(),
                        c.getPrecoLoja());

                new controller.ProdutoEstoqueController().salvar(p);

            } else {
                estoqueService.salvarNovaCarta(c);

                // Também insere na tabela produtos
                model.ProdutoModel p = new model.ProdutoModel(
                        c.getId(),
                        c.getNome(),
                        "Carta",
                        c.getQtd(),
                        c.getCusto(),
                        precoVenda);
                new controller.ProdutoEstoqueController().salvar(p);
            }

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
