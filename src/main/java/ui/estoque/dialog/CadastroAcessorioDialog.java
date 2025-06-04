// Caminho no projeto: src/ui/estoque/dialog/CadastroAcessorioDialog.java
package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import model.AcessorioModel;
import model.FornecedorModel;
import service.ProdutoEstoqueService;
import util.FormatterFactory;
import util.ScannerUtils; // <-- import necessário para o leitor de código de barras

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class CadastroAcessorioDialog extends JDialog {

    private final boolean isEdicao;
    private final AcessorioModel acessoOrig;

    private final JTextField tfNome = new JTextField(20);
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{
        "Chaveiros", "Moedas", "Marcadores", "Kit (Moeda + Marcador)",
        "Sleeve", "Playmats", "Lancheiras", "Outros"
    });
    private final JLabel lblArte = new JLabel("Arte:");
    private final JComboBox<String> cbArte = new JComboBox<>();
    private final JLabel lblCor = new JLabel("Cor:");
    private final JTextField tfCor = new JTextField();

    // *** NOVO: Label que exibirá o código lido (via scanner ou manual) ***
    private final JLabel lblCodigoLido = new JLabel("");

    private final JFormattedTextField tfQtd = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco = FormatterFactory.getFormattedDoubleField(0.0);

    private final JLabel lblFornecedor = new JLabel("Nenhum");
    private final JButton btnSelectFornec = new JButton("Escolher Fornecedor");
    private FornecedorModel fornecedorSel;

    public CadastroAcessorioDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroAcessorioDialog(JFrame owner, AcessorioModel acesso) {
        super(owner, acesso == null ? "Novo Acessório" : "Editar Acessório", true);
        this.isEdicao = acesso != null;
        this.acessoOrig = acesso;
        buildUI(owner);
        if (isEdicao) preencherCampos();
    }

    private void buildUI(JFrame owner) {
        setLayout(new GridLayout(0, 2, 8, 8));

        // Categoria → define se mostra arte e cor
        cbCategoria.addActionListener(e -> atualizarCamposArteCor());
        atualizarCamposArteCor();

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

        // Montagem do formulário
        add(new JLabel("Nome:"));          add(tfNome);
        add(new JLabel("Categoria:"));     add(cbCategoria);
        add(lblArte);                      add(cbArte);
        add(lblCor);                       add(tfCor);

        // *** NOVO: Seção Código de Barras ***
        add(new JLabel("Código de Barras:"));
        JPanel painelCodBarras = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnScanner = new JButton("Ler com Scanner");
        JButton btnManual = new JButton("Inserir Manualmente");

        painelCodBarras.add(btnScanner);
        painelCodBarras.add(btnManual);
        painelCodBarras.add(lblCodigoLido); // exibe o código lido
        add(painelCodBarras);

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

        add(new JLabel("Quantidade:"));    add(tfQtd);
        add(new JLabel("Custo (R$):"));    add(tfCusto);
        add(new JLabel("Preço Venda (R$):")); add(tfPreco);
        add(new JLabel("Fornecedor:"));    add(lblFornecedor);
        add(new JLabel());                 add(btnSelectFornec);

        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel());                 add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Exibe ou oculta os campos Arte/Cor de acordo com a categoria */
    private void atualizarCamposArteCor() {
        String cat = (String) cbCategoria.getSelectedItem();
        lblArte.setVisible(false);
        cbArte.setVisible(false);
        lblCor.setVisible(false);
        tfCor.setVisible(false);

        if ("Playmats".equals(cat)) {
            lblArte.setVisible(true);
            cbArte.setModel(new DefaultComboBoxModel<>(new String[]{"Pokémon", "Treinador", "Outros"}));
            cbArte.setVisible(true);
        } else if ("Sleeve".equals(cat)) {
            lblArte.setVisible(true);
            cbArte.setModel(new DefaultComboBoxModel<>(new String[]{"Pokémon", "Treinador", "Outros", "Cor Única"}));
            cbArte.setVisible(true);
            cbArte.addActionListener(e -> {
                boolean corUnica = "Cor Única".equals(cbArte.getSelectedItem());
                lblCor.setVisible(corUnica);
                tfCor.setVisible(corUnica);
            });
        }
    }

    private void preencherCampos() {
        tfNome.setText(acessoOrig.getNome());
        tfQtd.setValue(acessoOrig.getQuantidade());
        tfCusto.setValue(acessoOrig.getPrecoCompra());
        tfPreco.setValue(acessoOrig.getPrecoVenda());

        cbCategoria.setSelectedItem(acessoOrig.getCategoria());
        atualizarCamposArteCor();
        if (cbArte.isVisible()) {
            cbArte.setSelectedItem(acessoOrig.getArte());
            if (tfCor.isVisible()) {
                tfCor.setText(acessoOrig.getCor());
            }
        }

        // Se o modelo AcessorioModel tivesse um campo “codigoBarras”, aqui preencheríamos:
        // String codigoExistente = acessoOrig.getCodigoBarras();
        // lblCodigoLido.setText(codigoExistente);
        // lblCodigoLido.putClientProperty("codigoBarras", codigoExistente);

        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(acessoOrig.getFornecedorId());
        fornecedorSel.setNome(acessoOrig.getFornecedorNome());
        lblFornecedor.setText(acessoOrig.getFornecedorNome());
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
                ? acessoOrig.getId()
                : UUID.randomUUID().toString();

            String nome = tfNome.getText().trim();
            String categoria = (String) cbCategoria.getSelectedItem();
            String arte = cbArte.isVisible()
                ? (String) cbArte.getSelectedItem()
                : "";
            String cor = tfCor.isVisible()
                ? tfCor.getText().trim()
                : "";

            // Recupera o código de barras lido (se houver); caso contrário, deixa em branco.
            String codigoBarras = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigoBarras == null) {
                codigoBarras = "";
            }

            int qtd = ((Number) tfQtd.getValue()).intValue();
            double custo = ((Number) tfCusto.getValue()).doubleValue();
            double preco = ((Number) tfPreco.getValue()).doubleValue();
            String fornId = fornecedorSel.getId();
            String fornNom = fornecedorSel.getNome();

            // Como o AcessorioModel original não possui campo “codigoBarras”, não
            // alteramos o construtor nem a lógica de persistência. Mantemos apenas
            // o valor em lblCodigoLido, caso mais tarde queira estender o model/DAO.

            AcessorioModel a = new AcessorioModel(
                id, nome, qtd, custo, preco,
                fornId, categoria, arte, cor
            );
            a.setFornecedorNome(fornNom);

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) {
                service.atualizarAcessorio(a);  // método existente no service
            } else {
                service.salvarNovoAcessorio(a);
            }
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar Acessório:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
