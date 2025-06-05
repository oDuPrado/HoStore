// Caminho no projeto: src/ui/estoque/dialog/CadastroEtbDialog.java
package ui.estoque.dialog;

import dao.ColecaoDAO;
import dao.EtbDAO;
import dao.JogoDAO;
import dao.SetDAO;
import model.ColecaoModel;
import model.EtbModel;
import model.FornecedorModel;
import model.JogoModel;
import service.ProdutoEstoqueService;
import util.MaskUtils;
import util.ScannerUtils; // <-- import necessário para o leitor de código de barras

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.ArrayList;

/**
 * Dialog para cadastro/edição de ETBs, agora com seleção de Jogo (TCG),
 * campo de “set” que muda dinamicamente conforme o jogo escolhido,
 * e leitor de código de barras.
 */
public class CadastroEtbDialog extends JDialog {

    private final boolean isEdicao;
    private final EtbModel etbOrig;

    private final JTextField tfNome = new JTextField(20);
    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();
    private final JComboBox<String> cbSerie = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    // → NOVO: combo de Set para jogos não-Pokémon
    private final JComboBox<String> cbSetJogo = new JComboBox<>();
    // → NOVO: campo de texto para One Piece / Dragon Ball
    private final JTextField tfSetManual = new JTextField(20);
    // → NOVO: painel com CardLayout para alternar combo ↔ textfield
    private final JPanel panelSetSwitcher = new JPanel(new CardLayout());
    private static final String CARD_COMBO = "combo";
    private static final String CARD_MANUAL = "manual";
    // → NOVO: lista de nomes de set (filtrados/ordenados) para dropdown
    private List<String> setsFiltrados = new ArrayList<>();

    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] {
            "Booster Box", "Pokémon Center", "ETB", "Mini ETB", "Collection Box",
            "Special Collection", "Latas", "Box colecionáveis", "Trainer Kit", "Mini Booster Box"
    });
    private final JComboBox<String> cbVersao = new JComboBox<>(new String[] {
            "Nacional", "Americana"
    });
    private final JFormattedTextField tfQtd = MaskUtils.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = MaskUtils.moneyField(0.0);
    private final JFormattedTextField tfPreco = MaskUtils.moneyField(0.0);

    // *** NOVO: Label que exibirá o código de barras lido (via scanner ou manual)
    // ***
    private final JLabel lblCodigoLido = new JLabel("");

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Escolher Fornecedor");
    private FornecedorModel fornecedorSel;

    public CadastroEtbDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroEtbDialog(JFrame owner, EtbModel etb) {
        super(owner, etb == null ? "Novo ETB" : "Editar ETB", true);
        this.isEdicao = etb != null;
        this.etbOrig = etb;
        buildUI(owner);
        if (isEdicao) {
            preencherCampos();
        }
    }

    private void buildUI(JFrame owner) {
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        // → Nome
        content.add(new JLabel("Nome:"));
        content.add(tfNome);

        // → Jogo (novo campo)
        content.add(new JLabel("Jogo:"));
        content.add(cbJogo);
        carregarJogos();
        // Quando o usuário selecionar um Jogo, a UI de “set” / “série/coleção” deve
        // mudar dinamicamente
        cbJogo.addActionListener(e -> atualizarCamposPorJogo());

        // → Série / Coleção (via API)
        carregarSeries();
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());

        content.add(new JLabel("Série:"));
        content.add(cbSerie);

        content.add(new JLabel("Coleção:"));
        content.add(cbColecao);

        // → NOVO: Label + painel com CardLayout para alternar entre combo-de-sets e
        // campo-manual
        content.add(new JLabel("Set (selecione ou digite):"));
        // Adiciona os dois “cards” no panelSetSwitcher:
        panelSetSwitcher.add(cbSetJogo, CARD_COMBO);
        panelSetSwitcher.add(tfSetManual, CARD_MANUAL);
        // Inicialmente, manter o painel mas sem escolher nenhum card
        cbSetJogo.setVisible(false);
        tfSetManual.setVisible(false);
        content.add(panelSetSwitcher);

        // → Tipo
        content.add(new JLabel("Tipo:"));
        content.add(cbTipo);

        // → Versão
        content.add(new JLabel("Versão:"));
        content.add(cbVersao);

        // → Quantidade
        content.add(new JLabel("Quantidade:"));
        content.add(tfQtd);

        // → Custo
        content.add(new JLabel("Custo (R$):"));
        content.add(tfCusto);

        // → Preço venda
        content.add(new JLabel("Preço Venda (R$):"));
        content.add(tfPreco);

        // *** NOVO: Seção Código de Barras ***
        content.add(new JLabel("Código de Barras:"));
        JPanel painelCodBarras = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnScanner = new JButton("Ler com Scanner");
        JButton btnManual = new JButton("Inserir Manualmente");

        painelCodBarras.add(btnScanner);
        painelCodBarras.add(btnManual);
        painelCodBarras.add(lblCodigoLido); // exibe o código lido
        content.add(painelCodBarras);

        // Ação para chamar o util de leitura
        btnScanner.addActionListener(e -> {
            ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", codigo -> {
                lblCodigoLido.setText(codigo);
                lblCodigoLido.setToolTipText(codigo);
                lblCodigoLido.putClientProperty("codigoBarras", codigo);
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
            }
        });
        // *** Fim da seção Código de Barras ***

        // → Fornecedor via diálogo de seleção
        content.add(new JLabel("Fornecedor:"));
        content.add(lblFornecedor);
        content.add(new JLabel());
        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });
        content.add(btnSelectFornec);

        // → Botão Salvar / Atualizar
        content.add(new JLabel());
        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        content.add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void carregarJogos() {
        try {
            cbJogo.removeAllItems();
            cbJogo.addItem(new JogoModel(null, "Selecione..."));
            List<JogoModel> jogos = new JogoDAO().listarTodos();
            for (JogoModel jogo : jogos) {
                cbJogo.addItem(jogo);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar jogos.");
        }
    }

    private void carregarSeries() {
        try {
            cbSerie.removeAllItems();
            for (String s : new SetDAO().listarSeriesUnicas()) {
                cbSerie.addItem(s);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
        }
    }

    private void carregarColecoesPorSerie() {
        try {
            cbColecao.removeAllItems();
            String serie = (String) cbSerie.getSelectedItem();
            if (serie != null) {
                for (ColecaoModel c : new ColecaoDAO().listarPorSerie(serie)) {
                    cbColecao.addItem(c);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    /**
     * Atualiza quais campos serão visíveis:
     * - Se for POKEMON → mostra cbSerie + cbColecao, esconde set genérico e campo
     * manual
     * - Se for ONEPIECE ou DRAGONBALL → mostra somente campo-manual (tfSetManual)
     * - Caso contrário (YUGIOH, MAGIC, DIGIMON, etc) → mostra dropdown de sets
     * (cbSetJogo)
     */
    private void atualizarCamposPorJogo() {
        JogoModel jogo = (JogoModel) cbJogo.getSelectedItem();
        if (jogo == null || jogo.getId() == null)
            return;

        String jogoId = jogo.getId();
        boolean isPokemon = jogoId.equalsIgnoreCase("POKEMON");
        boolean isOnePiece = jogoId.equalsIgnoreCase("ONEPIECE");
        boolean isDragonBall = jogoId.equalsIgnoreCase("DRAGONBALL");

        // Série/Coleção só para Pokémon
        cbSerie.setVisible(isPokemon);
        cbColecao.setVisible(isPokemon);

        // CardLayout: decide qual "card" exibir
        CardLayout cl = (CardLayout) panelSetSwitcher.getLayout();
        if (isOnePiece || isDragonBall) {
            // Usuário digita manualmente o nome do set
            cl.show(panelSetSwitcher, CARD_MANUAL);
        } else if (!isPokemon) {
            // Exibe combo de sets de acordo com API (Yu-Gi-Oh!, Magic, Digimon, etc)
            cl.show(panelSetSwitcher, CARD_COMBO);
            carregarSetsJogo(jogoId);
        } else {
            // Se for Pokémon, nem dropdown nem textfield de set devem aparecer
            // (força a exibição de um card vazio, apenas para manter o layout consistente)
            panelSetSwitcher.removeAll();
            panelSetSwitcher.add(new JPanel(), CARD_COMBO);
            panelSetSwitcher.add(new JPanel(), CARD_MANUAL);
            cl.show(panelSetSwitcher, CARD_COMBO);
        }

        revalidate();
        repaint();
    }

    /**
     * Carrega a lista de sets (filtrada + ordenada) para o combobox cbSetJogo.
     * Uso de Comparator para ordenar alfabeticamente (ignorando maiúsculas).
     */
    private void carregarSetsJogo(String jogoId) {
        try {
            cbSetJogo.removeAllItems();
            // Recupera todos os sets via DAO
            var sets = new dao.SetJogoDAO().listarPorJogo(jogoId);
            // Ordena por nome ignorando maiúsculas
            sets.sort(Comparator.comparing(s -> s.getNome().toLowerCase()));
            // Converte para lista de Strings
            setsFiltrados = sets.stream()
                    .map(s -> s.getNome())
                    .toList();
            // Atualiza o combobox
            atualizarComboBoxSet(setsFiltrados);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar sets do jogo.");
            ex.printStackTrace();
        }
    }

    /**
     * Atualiza todos os itens de cbSetJogo a partir da lista fornecida.
     */
    private void atualizarComboBoxSet(List<String> lista) {
        cbSetJogo.removeAllItems();
        for (String nome : lista) {
            cbSetJogo.addItem(nome);
        }
    }

    private void preencherCampos() {
        tfNome.setText(etbOrig.getNome());

        // Selecionar jogo no combo
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

        // Carrega automaticamente a UI de “set” / “série/coleção”
        atualizarCamposPorJogo();

        // Se for Pokémon, preenche o combo de série/coleção
        if (etbOrig.getJogoId().equalsIgnoreCase("POKEMON")) {
            cbSerie.setSelectedItem(etbOrig.getSerie());
            carregarColecoesPorSerie();
            for (int i = 0; i < cbColecao.getItemCount(); i++) {
                ColecaoModel c = cbColecao.getItemAt(i);
                if (c.getName().equalsIgnoreCase(etbOrig.getColecao())) {
                    cbColecao.setSelectedItem(c);
                    break;
                }
            }
        }

        // Se for jogo genérico com dropdown de sets, seleciona o set salvo (caso haja)
        if (!etbOrig.getJogoId().equalsIgnoreCase("POKEMON")
                && !etbOrig.getJogoId().equalsIgnoreCase("ONEPIECE")
                && !etbOrig.getJogoId().equalsIgnoreCase("DRAGONBALL")) {
            // Já executamos carregarSetsJogo() dentro de atualizarCamposPorJogo()
            // Agora basta encontrar o índice do set antigo
            for (int i = 0; i < cbSetJogo.getItemCount(); i++) {
                String nomeSet = cbSetJogo.getItemAt(i);
                if (nomeSet != null && nomeSet.equalsIgnoreCase(etbOrig.getSerie())) {
                    cbSetJogo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Se for One Piece ou Dragon Ball, preenche o campo manual
        if (etbOrig.getJogoId().equalsIgnoreCase("ONEPIECE")
                || etbOrig.getJogoId().equalsIgnoreCase("DRAGONBALL")) {
            tfSetManual.setText(etbOrig.getSerie());
        }

        // Preenche restante dos campos
        cbTipo.setSelectedItem(etbOrig.getTipo());
        cbVersao.setSelectedItem(etbOrig.getVersao());
        tfQtd.setValue(etbOrig.getQuantidade());
        tfCusto.setValue(etbOrig.getPrecoCompra());
        tfPreco.setValue(etbOrig.getPrecoVenda());

        // Preenche o código de barras existente, caso EtbModel possua esse campo:
        // String codigoExistente = etbOrig.getCodigoBarras();
        // lblCodigoLido.setText(codigoExistente);
        // lblCodigoLido.putClientProperty("codigoBarras", codigoExistente);

        // Fornecedor
        try {
            fornecedorSel = new dao.FornecedorDAO().buscarPorId(etbOrig.getFornecedor());
            if (fornecedorSel != null) {
                lblFornecedor.setText(fornecedorSel.getNome());
            } else {
                lblFornecedor.setText("Fornecedor não cadastrado");
            }
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
            String id = isEdicao
                    ? etbOrig.getId()
                    : UUID.randomUUID().toString();

            // Decide o "set" final (série → nome do set)
            String setSelecionado = "";
            String jogoId = jogoSel.getId();
            boolean isPokemon = jogoId.equalsIgnoreCase("POKEMON");
            boolean isOnePiece = jogoId.equalsIgnoreCase("ONEPIECE");
            boolean isDragonBall = jogoId.equalsIgnoreCase("DRAGONBALL");

            if (isPokemon) {
                // O campo “serie” já está em cbSerie (para Pokémon)
                setSelecionado = (String) cbSerie.getSelectedItem();
            } else if (isOnePiece || isDragonBall) {
                // Texto livre
                setSelecionado = tfSetManual.getText().trim();
            } else {
                // Dropdown de sets (Yu-Gi-Oh!, Magic, Digimon, etc)
                setSelecionado = (String) cbSetJogo.getSelectedItem();
            }

            // Recupera o código de barras lido (se houver); caso contrário, deixa em
            // branco.
            String codigoBarras = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigoBarras == null) {
                codigoBarras = "";
            }
            // (No momento, não estamos passando esse código para o model. Se quiser
            // persistir,
            // inclua no construtor de EtbModel e no DAO.)

            EtbModel e = new EtbModel(
                    id,
                    tfNome.getText().trim(),
                    ((Number) tfQtd.getValue()).intValue(),
                    ((Number) tfCusto.getValue()).doubleValue(),
                    ((Number) tfPreco.getValue()).doubleValue(),
                    fornecedorSel.getId(),
                    setSelecionado,
                    (String) ((cbColecao.getSelectedItem() != null)
                            ? ((ColecaoModel) cbColecao.getSelectedItem()).getName()
                            : ""),
                    (String) cbTipo.getSelectedItem(),
                    (String) cbVersao.getSelectedItem(),
                    jogoSel.getId());

            
            e.setCodigoBarras(codigoBarras);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) {
                service.atualizarEtb(e);
            } else {
                service.salvarNovoEtb(e);
            }
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar ETB:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
