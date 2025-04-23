package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import dao.BoosterDAO;
import dao.CadastroGenericoDAO;
import dao.FornecedorDAO;
import model.BoosterModel;
import model.ColecaoModel;
import model.FornecedorModel;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CadastroBoosterDialog extends JDialog {

    private final JTextField      tfNome             = new JTextField();
    private final JComboBox<String> cbSerie           = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao   = new JComboBox<>();
    private final JComboBox<String> cbTipo            = new JComboBox<>(new String[] {
        "Unitário", "Quadri-pack", "Triple-pack", "Especial", "Blister"
    });
    private final JComboBox<String> cbIdioma          = new JComboBox<>();
    private final JTextField      tfDataLancamento   = new JTextField();
    private final JTextField      tfCodigoBarras     = new JTextField();
    private final JFormattedTextField tfQtd          = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto        = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco        = FormatterFactory.getFormattedDoubleField(0.0);

    private final JLabel          lblFornecedor      = new JLabel("Nenhum");
    private final JButton         btnSelectFornec    = new JButton("Escolher Fornecedor");
    private FornecedorModel       fornecedorSelecionado;

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();
    private static final DateTimeFormatter DISPLAY_DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CadastroBoosterDialog(JFrame owner) {
        super(owner, "Novo Booster", true);
        setLayout(new GridLayout(0, 2, 8, 8));

        // séries e coleções
        carregarSeries();
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());
        carregarColecoesPorSerie();

        // data não-editável (preenchida pela coleção)
        tfDataLancamento.setEditable(false);
        cbColecao.addActionListener(e -> atualizarDataLancamentoDaColecao());

        // idiomas
        carregarIdiomas();

        // fornecedor
        btnSelectFornec.addActionListener(e -> openFornecedorDialog());

        // monta o formulário
        add(new JLabel("Nome:"));               add(tfNome);
        add(new JLabel("Série:"));              add(cbSerie);
        add(new JLabel("Coleção:"));            add(cbColecao);
        add(new JLabel("Tipo:"));               add(cbTipo);
        add(new JLabel("Idioma:"));             add(cbIdioma);
        add(new JLabel("Data de Lançamento:")); add(tfDataLancamento);
        add(new JLabel("Código de Barras:"));   add(tfCodigoBarras);
        add(new JLabel("Quantidade:"));         add(tfQtd);
        add(new JLabel("Custo (R$):"));         add(tfCusto);
        add(new JLabel("Preço Venda (R$):"));   add(tfPreco);
        add(new JLabel("Fornecedor:"));         add(lblFornecedor);
        add(new JLabel());                      add(btnSelectFornec);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel());
        add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void carregarSeries() {
        try {
            dao.SetDAO dao = new dao.SetDAO();
            List<String> series = dao.listarSeriesUnicas();
            cbSerie.removeAllItems();
            series.forEach(cbSerie::addItem);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
        }
    }

    private void carregarColecoesPorSerie() {
        try {
            String serie = (String) cbSerie.getSelectedItem();
            cbColecao.removeAllItems();
            if (serie == null) return;
            dao.ColecaoDAO dao = new dao.ColecaoDAO();
            dao.listarPorSerie(serie).forEach(cbColecao::addItem);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void atualizarDataLancamentoDaColecao() {
        ColecaoModel c = (ColecaoModel) cbColecao.getSelectedItem();
        if (c != null && c.getReleaseDate() != null && !c.getReleaseDate().isBlank()) {
            LocalDate d = LocalDate.parse(c.getReleaseDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            tfDataLancamento.setText(d.format(DISPLAY_DATE_FMT));
        } else {
            tfDataLancamento.setText("");
        }
    }

    private void carregarIdiomas() {
        try {
            // uso de DAO genérico para tabela linguagens (id, nome)
            CadastroGenericoDAO dao = new CadastroGenericoDAO("linguagens", "id", "nome");
            List<Map<String, String>> itens = dao.listar();
            cbIdioma.removeAllItems();
            for (Map<String,String> m : itens) {
                cbIdioma.addItem(m.get("nome"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar idiomas.");
        }
    }

    private void openFornecedorDialog() {
        FornecedorSelectionDialog dlg = new FornecedorSelectionDialog((Frame)getOwner());
dlg.setVisible(true);
FornecedorModel f = dlg.getSelectedFornecedor();
if (f != null) {
    fornecedorSelecionado = f;
    lblFornecedor.setText(f.getNome());
}
    }

    private void salvar() {
        // validações mínimas
        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome é obrigatório.");
            return;
        }
        if (fornecedorSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor.");
            return;
        }

        try {
            String id        = UUID.randomUUID().toString();
            String nome      = tfNome.getText().trim();
            String serie     = (String) cbSerie.getSelectedItem();
            String colecao   = cbColecao.getSelectedItem() != null
                                ? ((ColecaoModel) cbColecao.getSelectedItem()).getName()
                                : "";
            String tipo      = (String) cbTipo.getSelectedItem();
            String idioma    = (String) cbIdioma.getSelectedItem();
            String dataLct   = tfDataLancamento.getText().trim();
            String codigo    = tfCodigoBarras.getText().trim();
            int    qtd       = Integer.parseInt(tfQtd.getValue().toString());
            double custo     = ((Number) tfCusto.getValue()).doubleValue();
            double preco     = ((Number) tfPreco.getValue()).doubleValue();
            String fornecId  = fornecedorSelecionado.getId();
            String fornecNom = fornecedorSelecionado.getNome();

            // 1) salva a base em produtos
            ctrl.salvarProdutoBase(id, nome, "Booster", qtd, custo, preco, fornecNom);

            // 2) salva detalhes específicos
            BoosterModel b = new BoosterModel(
                id, nome, qtd, custo, preco, fornecNom,
                colecao, serie, tipo, idioma, dataLct, codigo
            );
            new BoosterDAO().insert(b);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar Booster:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
