package ui.dialog;

import controller.ProdutoEstoqueController;
import dao.BoosterDAO;
import dao.ColecaoDAO;
import dao.SetDAO;
import model.BoosterModel;
import model.ColecaoModel;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class CadastroBoosterDialog extends JDialog {

    private final JTextField tfNome = new JTextField();
    private final JComboBox<String> cbSerie = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] {
            "Unitário", "Quadri‑pack", "Triple‑pack", "Especial", "Blister"
    });
    private final JTextField tfIdioma = new JTextField();
    private final JTextField tfDataLancamento = new JTextField();
    private final JTextField tfCodigoBarras = new JTextField();

    private final JFormattedTextField tfQtd = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getFormattedDoubleField(0.0);
    private final JTextField tfFornec = new JTextField();

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    // formatter pra exibir data no padrão dd/MM/yyyy
    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CadastroBoosterDialog(JFrame owner) {
        super(owner, "Novo Booster", true);
        setLayout(new GridLayout(0, 2, 8, 8));

        // carregar séries e setar listener pra filtrar coleções
        carregarSeries();
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());
        // carregar coleções iniciais para a primeira série
        carregarColecoesPorSerie();

        // listener pra preencher data de lançamento ao escolher coleção
        tfDataLancamento.setEditable(false);
        cbColecao.addActionListener(e -> atualizarDataLancamentoDaColecao());

        // Monta o formulário em ordem lógica
        add(new JLabel("Nome:"));
        add(tfNome);
        add(new JLabel("Série:"));
        add(cbSerie);
        add(new JLabel("Coleção:"));
        add(cbColecao);
        add(new JLabel("Tipo:"));
        add(cbTipo);
        add(new JLabel("Idioma:"));
        add(tfIdioma);
        add(new JLabel("Data de Lançamento:"));
        add(tfDataLancamento);
        add(new JLabel("Código de Barras:"));
        add(tfCodigoBarras);
        add(new JLabel("Quantidade:"));
        add(tfQtd);
        add(new JLabel("Custo (R$):"));
        add(tfCusto);
        add(new JLabel("Preço Venda (R$):"));
        add(tfPreco);
        add(new JLabel("Fornecedor:"));
        add(tfFornec);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel());
        add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void carregarSeries() {
        try {
            SetDAO dao = new SetDAO();
            List<String> seriesList = dao.listarSeriesUnicas();
            cbSerie.removeAllItems();
            for (String s : seriesList) {
                cbSerie.addItem(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
        }
    }

    private void carregarColecoesPorSerie() {
        try {
            String serie = (String) cbSerie.getSelectedItem();
            cbColecao.removeAllItems();
            if (serie == null)
                return;
            ColecaoDAO dao = new ColecaoDAO();
            List<ColecaoModel> colecoes = dao.listarPorSerie(serie);
            for (ColecaoModel c : colecoes) {
                cbColecao.addItem(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções da série.");
        }
    }

    private void atualizarDataLancamentoDaColecao() {
        ColecaoModel c = (ColecaoModel) cbColecao.getSelectedItem();
        if (c != null && c.getReleaseDate() != null && !c.getReleaseDate().isBlank()) {
            DateTimeFormatter apiFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            LocalDate d = LocalDate.parse(c.getReleaseDate(), apiFormatter);

            tfDataLancamento.setText(d.format(DISPLAY_DATE_FMT));
        } else {
            tfDataLancamento.setText("");
        }
    }

    private void salvar() {
        try {
            String id = UUID.randomUUID().toString();
            String serie = cbSerie.getSelectedItem() != null
                    ? cbSerie.getSelectedItem().toString()
                    : "";
            String colecao = cbColecao.getSelectedItem() != null
                    ? ((ColecaoModel) cbColecao.getSelectedItem()).getName()
                    : "";

            BoosterModel b = new BoosterModel(
                    id,
                    tfNome.getText().trim(),
                    Integer.parseInt(tfQtd.getValue().toString()),
                    ((Number) tfCusto.getValue()).doubleValue(),
                    ((Number) tfPreco.getValue()).doubleValue(),
                    tfFornec.getText().trim(),
                    colecao, // nome da coleção
                    serie, // nome da série (set)
                    (String) cbTipo.getSelectedItem(),
                    tfIdioma.getText().trim(),
                    tfDataLancamento.getText().trim(),
                    tfCodigoBarras.getText().trim());

            new BoosterDAO().insert(b);
            ctrl.listar(""); // refresh da tabela
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao salvar Booster:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
