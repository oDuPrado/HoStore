package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import model.BoosterModel;
import model.ColecaoModel;
import model.FornecedorModel;
import service.ProdutoEstoqueService;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CadastroBoosterDialog extends JDialog {

    private final boolean isEdicao;
    private final BoosterModel boosterOrig;

    private final JTextField tfNome           = new JTextField(20);
    private final JComboBox<String> cbSerie   = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    private final JComboBox<String> cbTipo    = new JComboBox<>(new String[]{
        "Unitário", "Quadri-pack", "Triple-pack", "Especial", "Blister"
    });
    private final JComboBox<String> cbIdioma  = new JComboBox<>();
    private final JTextField tfDataLanc       = new JTextField();
    private final JTextField tfCodigoBarras   = new JTextField();
    private final JFormattedTextField tfQtd   = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getFormattedDoubleField(0.0);

    private final JLabel lblFornecedor      = new JLabel("Nenhum");
    private final JButton btnSelectFornec   = new JButton("Escolher Fornecedor");
    private FornecedorModel fornecedorSel;

    private static final DateTimeFormatter DISPLAY_DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CadastroBoosterDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroBoosterDialog(JFrame owner, BoosterModel booster) {
        super(owner, booster == null ? "Novo Booster" : "Editar Booster", true);
        this.isEdicao    = booster != null;
        this.boosterOrig = booster;
        buildUI(owner);
        if (isEdicao) preencherCampos();
    }

    private void buildUI(JFrame owner) {
        setLayout(new GridLayout(0, 2, 8, 8));

        // séries e coleções
        carregarSeries();
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());
        carregarColecoesPorSerie();

        // data não editável
        tfDataLanc.setEditable(false);
        cbColecao.addActionListener(e -> {
            ColecaoModel c = (ColecaoModel) cbColecao.getSelectedItem();
            if (c != null && c.getReleaseDate() != null && !c.getReleaseDate().isBlank()) {
                LocalDate d = LocalDate.parse(c.getReleaseDate(),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                tfDataLanc.setText(d.format(DISPLAY_DATE_FMT));
            } else {
                tfDataLanc.setText("");
            }
        });

        // idiomas
        carregarIdiomas();

        // fornecedor
        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });

        // montar formulário
        add(new JLabel("Nome:"));               add(tfNome);
        add(new JLabel("Série:"));              add(cbSerie);
        add(new JLabel("Coleção:"));            add(cbColecao);
        add(new JLabel("Tipo:"));               add(cbTipo);
        add(new JLabel("Idioma:"));             add(cbIdioma);
        add(new JLabel("Data de Lançamento:")); add(tfDataLanc);
        add(new JLabel("Código de Barras:"));   add(tfCodigoBarras);
        add(new JLabel("Quantidade:"));         add(tfQtd);
        add(new JLabel("Custo (R$):"));         add(tfCusto);
        add(new JLabel("Preço Venda (R$):"));   add(tfPreco);
        add(new JLabel("Fornecedor:"));         add(lblFornecedor);
        add(new JLabel());                      add(btnSelectFornec);

        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel());
        add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

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
            if (serie == null) return;
            for (ColecaoModel c : new dao.ColecaoDAO().listarPorSerie(serie))
                cbColecao.addItem(c);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void carregarIdiomas() {
        try {
            for (Map<String,String> m : new dao.CadastroGenericoDAO("linguagens","id","nome").listar())
                cbIdioma.addItem(m.get("nome"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar idiomas.");
        }
    }

    private void preencherCampos() {
        tfNome.setText(boosterOrig.getNome());
        tfQtd.setValue(boosterOrig.getQuantidade());
        tfCusto.setValue(boosterOrig.getPrecoCompra());
        tfPreco.setValue(boosterOrig.getPrecoVenda());

        cbSerie.removeAllItems();
        cbSerie.addItem(boosterOrig.getSet());
        cbSerie.setSelectedIndex(0);
        carregarColecoesPorSerie();
        cbColecao.removeAllItems();
        ColecaoModel col = new ColecaoModel();
        col.setId(""); // ou mantenha col.getId() real
        col.setName(boosterOrig.getColecao());
        cbColecao.addItem(col);
        cbColecao.setSelectedIndex(0);

        cbTipo.setSelectedItem(boosterOrig.getTipoBooster());
        cbIdioma.setSelectedItem(boosterOrig.getIdioma());
        tfDataLanc.setText(boosterOrig.getValidade());
        tfCodigoBarras.setText(boosterOrig.getCodigoBarras());

        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(boosterOrig.getFornecedor());
        fornecedorSel.setNome(boosterOrig.getFornecedorNome());
        lblFornecedor.setText(boosterOrig.getFornecedorNome());
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

        try {
            String id = isEdicao
                ? boosterOrig.getId()
                : UUID.randomUUID().toString();

            String nome    = tfNome.getText().trim();
            String serie   = (String) cbSerie.getSelectedItem();
            String colecao = cbColecao.getSelectedItem() != null
                ? ((ColecaoModel)cbColecao.getSelectedItem()).getName()
                : "";
            String tipo    = (String) cbTipo.getSelectedItem();
            String idioma  = (String) cbIdioma.getSelectedItem();
            String validade= tfDataLanc.getText().trim();
            String codigo  = tfCodigoBarras.getText().trim();
            int    qtd     = ((Number)tfQtd.getValue()).intValue();
            double custo   = ((Number)tfCusto.getValue()).doubleValue();
            double preco   = ((Number)tfPreco.getValue()).doubleValue();
            String fornId  = fornecedorSel.getId();
            String fornNom = fornecedorSel.getNome();

            // 1) Salva ou atualiza na tabela boosters
            BoosterModel b = new BoosterModel(
                id, nome, qtd, custo, preco,
                fornId,
                colecao, serie, tipo,
                idioma, validade, codigo
            );
            b.setFornecedorNome(fornNom);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) service.atualizarBooster(b);
            else           service.salvarNovoBooster(b);

            // 2) Mantém a mesma lógica das cartas para resumo em produtos
            model.ProdutoModel p = new model.ProdutoModel(
                id, nome, "Booster", qtd, custo, preco
            );
            new ProdutoEstoqueController().salvar(p);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar Booster:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
