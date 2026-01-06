package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import dao.JogoDAO;
import model.BoosterModel;
import model.ColecaoModel;
import model.FornecedorModel;
import model.JogoModel;
import service.ProdutoEstoqueService;
import util.FormatterFactory;
import util.ScannerUtils;
import model.NcmModel;
import service.NcmService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.ArrayList;

/**
 * Dialog para cadastro/edição de Boosters, agora com seleção de Jogo (TCG).
 */
public class CadastroBoosterDialog extends JDialog {

    private final boolean isEdicao;
    private final BoosterModel boosterOrig;

    private final JTextField tfNome = new JTextField(20);
    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();
    private final JComboBox<String> cbSerie = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    // Para jogos que não são Pokémon
    private final JComboBox<String> cbSetJogo = new JComboBox<>();
    private final JTextField tfSetManual = new JTextField();
    // Painel com CardLayout para alternar entre ComboBox e campo manual
    private final JPanel panelSetSwitcher = new JPanel(new CardLayout());
    private final static String CARD_COMBO = "combo";
    private final static String CARD_MANUAL = "manual";

    private List<String> setsFiltrados = new ArrayList<>();

    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] {
            "Unitário", "Quadri-pack", "Triple-pack", "Especial", "Blister"
    });
    private final JComboBox<String> cbIdioma = new JComboBox<>();
    private final JTextField tfDataLanc = new JTextField();
    private final JLabel lblCodigoLido = new JLabel("");
    private final JTextField tfCodigoBarras = new JTextField();
    private final JFormattedTextField tfQtd = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getFormattedDoubleField(0.0);

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Escolher Fornecedor");
    private FornecedorModel fornecedorSel;

    private final JComboBox<String> cbNcm = new JComboBox<>();

    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CadastroBoosterDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroBoosterDialog(JFrame owner, BoosterModel booster) {
        super(owner, booster == null ? "Novo Booster" : "Editar Booster", true);
        this.isEdicao = booster != null;
        this.boosterOrig = booster;
        buildUI(owner);
        if (isEdicao) {
            preencherCampos();
        }
    }

    private void buildUI(JFrame owner) {
        setLayout(new GridLayout(0, 2, 8, 8));

        // Carregamento inicial de séries, coleções, idiomas e jogos
        carregarSeries();
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());
        carregarColecoesPorSerie();
        carregarIdiomas();
        carregarJogos(); // NOVO: popula cbJogo

        // Data não editável
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

        // Fornecedor
        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });

        // Montar formulário
        add(new JLabel("Nome:"));
        add(tfNome);

        // NOVO: Campo Jogo
        add(new JLabel("Jogo:"));
        add(cbJogo);

        add(new JLabel("Série:"));
        add(cbSerie);
        add(new JLabel("Coleção:"));
        add(cbColecao);
        add(new JLabel("Set:"));

        // Adiciona os dois componentes ao painel com "cartões"
        panelSetSwitcher.add(cbSetJogo, CARD_COMBO);
        panelSetSwitcher.add(tfSetManual, CARD_MANUAL);

        add(panelSetSwitcher); // adiciona o painel no layout

        add(new JLabel("Tipo:"));
        add(cbTipo);
        add(new JLabel("Idioma:"));
        add(cbIdioma);
        add(new JLabel("Data de Lançamento:"));
        add(tfDataLanc);
        add(new JLabel("Código de Barras:"));
        // @USAR_SCANNER_UTIL
        JPanel painelCodBarras = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnScanner = new JButton("Ler com Scanner");
        JButton btnManual = new JButton("Inserir Manualmente");

        // deixa o label "visível" mesmo vazio
        lblCodigoLido.setText(" ");
        lblCodigoLido.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblCodigoLido.setPreferredSize(new Dimension(160, 22));

        painelCodBarras.add(btnScanner);
        painelCodBarras.add(btnManual);
        painelCodBarras.add(lblCodigoLido);
        add(painelCodBarras);

        // NCM: combo com todos os NCMs cadastrados
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
        add(new JLabel("NCM:"));
        add(cbNcm);

        // Ação para chamar o util
        btnScanner.addActionListener(e -> {
            ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", codigo -> {
                lblCodigoLido.setText(codigo);
                lblCodigoLido.setToolTipText(codigo);
                lblCodigoLido.putClientProperty("codigoBarras", codigo);

                lblCodigoLido.revalidate();
                lblCodigoLido.repaint();
                pack();
            });
        });

        // Ação para inserir manualmente via diálogo
        btnManual.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Digite o código de barras:");
            if (input != null && !input.trim().isEmpty()) {
                String c = input.trim();
                lblCodigoLido.setText(c);
                lblCodigoLido.setToolTipText(c);
                lblCodigoLido.putClientProperty("codigoBarras", c);

                lblCodigoLido.revalidate();
                lblCodigoLido.repaint();
                pack();
            }
        });

        add(new JLabel("Quantidade:"));
        add(tfQtd);
        add(new JLabel("Custo (R$):"));
        add(tfCusto);
        add(new JLabel("Preço Venda (R$):"));
        add(tfPreco);
        add(new JLabel("Fornecedor:"));
        add(lblFornecedor);
        add(new JLabel());
        add(btnSelectFornec);

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
            for (JogoModel jogo : jogos) {
                cbJogo.addItem(jogo);
            }

            // 🔧 ESSENCIAL: atualizar visuais ao selecionar jogo
            cbJogo.addActionListener(e -> atualizarCamposPorJogo());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar jogos.");
        }
    }

    private void atualizarCamposPorJogo() {
        JogoModel jogo = (JogoModel) cbJogo.getSelectedItem();
        if (jogo == null || jogo.getId() == null)
            return;

        boolean isPokemon = jogo.getId().equalsIgnoreCase("POKEMON");
        boolean isOnePiece = jogo.getId().equalsIgnoreCase("ONEPIECE");
        boolean isDragonBall = jogo.getId().equalsIgnoreCase("DRAGONBALL");

        // Mostrar apenas os campos relevantes
        cbSerie.setVisible(isPokemon);
        cbColecao.setVisible(isPokemon);
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
    }

    private void carregarSetsJogo(String jogoId) {
        try {
            cbSetJogo.removeAllItems();

            var sets = new dao.SetJogoDAO().listarPorJogo(jogoId);
            // Ordenar por nome (ignorando maiúsculas/minúsculas)
            sets.sort(Comparator.comparing(s -> s.getNome().toLowerCase()));

            // Guardar os nomes para uso no filtro
            setsFiltrados = sets.stream()
                    .map(s -> s.getNome())
                    .toList();

            // Atualizar combo com a lista inicial completa
            atualizarComboBoxSet(setsFiltrados);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar sets do jogo.");
            ex.printStackTrace();
        }
    }

    private void atualizarComboBoxSet(List<String> lista) {
        cbSetJogo.removeAllItems();
        for (String nome : lista) {
            cbSetJogo.addItem(nome);
        }
    }

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

        // Série (set)
        try {
            cbSerie.removeAllItems();
            List<String> series = new dao.SetDAO().listarSeriesUnicas();
            for (String s : series) {
                cbSerie.addItem(s);
            }
            cbSerie.setSelectedItem(boosterOrig.getSet());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
            ex.printStackTrace();
        }

        // Coleções + Data de Lançamento
        cbColecao.removeAllItems();
        try {
            List<ColecaoModel> todas = new dao.ColecaoDAO().listarPorSerie(boosterOrig.getSet());
            for (ColecaoModel c : todas) {
                cbColecao.addItem(c);
                if (c.getName().equalsIgnoreCase(boosterOrig.getColecao())) {
                    cbColecao.setSelectedItem(c);
                    if (c.getReleaseDate() != null && !c.getReleaseDate().isBlank()) {
                        LocalDate d = LocalDate.parse(
                                c.getReleaseDate(),
                                DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                        tfDataLanc.setText(d.format(DISPLAY_DATE_FMT));
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
            e.printStackTrace();
        }

        // Tipo + Idioma + Código de Barras
        cbTipo.setSelectedItem(boosterOrig.getTipoBooster());
        cbIdioma.setSelectedItem(boosterOrig.getIdioma());
        String cod = boosterOrig.getCodigoBarras();
        if (cod != null && !cod.isBlank()) {
            lblCodigoLido.setText(cod);
            lblCodigoLido.setToolTipText(cod);
            lblCodigoLido.putClientProperty("codigoBarras", cod);
        }

        // Fornecedor
        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(boosterOrig.getFornecedor());
        fornecedorSel.setNome(boosterOrig.getFornecedorNome());
        lblFornecedor.setText(boosterOrig.getFornecedorNome());

        // Seleciona o NCM correspondente se existir
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
        // Verifica seleção de jogo
        JogoModel jogoSel = (JogoModel) cbJogo.getSelectedItem();
        if (jogoSel == null || jogoSel.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selecione um jogo.");
            return;
        }

        try {
            // Recupera o código do NCM selecionado
            String ncmCombo = (String) cbNcm.getSelectedItem();
            String ncm = "";
            if (ncmCombo != null && ncmCombo.contains("-")) {
                ncm = ncmCombo.split("-")[0].trim();
            }

            String id = isEdicao
                    ? boosterOrig.getId()
                    : UUID.randomUUID().toString();

            String nome = tfNome.getText().trim();
            String serie = (String) cbSerie.getSelectedItem();
            String colecao = cbColecao.getSelectedItem() != null
                    ? ((ColecaoModel) cbColecao.getSelectedItem()).getName()
                    : "";
            // Se for jogo com set manual, pegar do campo de texto
            if (cbSetJogo.isVisible()) {
                serie = (String) cbSetJogo.getSelectedItem();
            } else if (tfSetManual.isVisible()) {
                serie = tfSetManual.getText().trim();
            }

            String tipo = (String) cbTipo.getSelectedItem();
            String idioma = (String) cbIdioma.getSelectedItem();
            String validade = tfDataLanc.getText().trim();
            String codigo = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigo == null) {
                codigo = ""; // ou trate como “não informado”
            }
            int qtd = ((Number) tfQtd.getValue()).intValue();
            double custo = ((Number) tfCusto.getValue()).doubleValue();
            double preco = ((Number) tfPreco.getValue()).doubleValue();
            String fornId = fornecedorSel.getId();
            String fornNom = fornecedorSel.getNome();
            String jogoId = jogoSel.getId();

            // Cria BoosterModel com jogoId
            BoosterModel b = new BoosterModel(
                    id, nome, qtd, custo, preco,
                    fornId,
                    colecao, serie, tipo,
                    idioma, validade, codigo,
                    jogoId // NOVO
            );
            // Assegura que o fornecedorId seja preenchido corretamente
            b.setFornecedorId(fornId);
            b.setFornecedorNome(fornNom);
            b.setNcm(ncm);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) {
                service.atualizarBooster(b); // já salva em ambas as tabelas
            } else {
                service.salvarNovoBooster(b);
            }

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar Booster:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
