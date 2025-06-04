// Caminho no projeto: src/ui/estoque/dialog/CadastroProdutoAlimenticioDialog.java
package ui.estoque.dialog;

import model.AlimentoModel;
import model.FornecedorModel;
import service.ProdutoEstoqueService;
import util.FormatterFactory;
import util.ScannerUtils; // <-- import necessário para o leitor de código de barras

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CadastroProdutoAlimenticioDialog extends JDialog {
    private final boolean isEdicao;
    private final AlimentoModel alimentoOrig;

    private final JTextField tfNome         = new JTextField(20);
    private final JComboBox<String> cbCategoria = new JComboBox<>(new String[]{"Comida","Bebida"});
    private final JComboBox<String> cbSubtipo   = new JComboBox<>();
    private final JComboBox<String> cbMarca     = new JComboBox<>();
    private final JTextField tfMarcaOutro      = new JTextField(20);
    private final JComboBox<String> cbSabor     = new JComboBox<>();
    private final JTextField tfLote            = new JTextField(10);
    private final JFormattedTextField tfPeso   = FormatterFactory.getFormattedDoubleField(0.0);
    private final JLabel lblPeso               = new JLabel("Peso (g):");
    private final JFormattedTextField tfDataValidade;
    // private final JTextField tfCodigoBarras    = new JTextField(15); // removido para usar leitor
    private final JFormattedTextField tfQtd    = FormatterFactory.getFormattedIntField(0);
    private final JFormattedTextField tfCusto  = FormatterFactory.getFormattedDoubleField(0.0);
    private final JFormattedTextField tfPreco  = FormatterFactory.getFormattedDoubleField(0.0);

    // NOVO: Label que exibirá o código de barras lido (via scanner ou manual)
    private final JLabel lblCodigoLido         = new JLabel("");

    private final JLabel lblFornecedor        = new JLabel("Nenhum");
    private final JButton btnSelectFornec     = new JButton("Escolher Fornecedor");
    private FornecedorModel fornecedorSel;

    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CadastroProdutoAlimenticioDialog(JFrame owner) {
        this(owner, null);
    }

    public CadastroProdutoAlimenticioDialog(JFrame owner, AlimentoModel a) {
        super(owner, a == null ? "Novo Produto Alimentício" : "Editar Produto Alimentício", true);
        this.isEdicao     = a != null;
        this.alimentoOrig = a;
        // máscara data
        JFormattedTextField temp;
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            temp = new JFormattedTextField(mask);
        } catch (ParseException e) {
            temp = new JFormattedTextField();
        }
        this.tfDataValidade = temp;

        buildUI(owner);
        if (isEdicao) preencherCampos();
    }

    private void buildUI(JFrame owner) {
        setLayout(new GridLayout(0, 2, 8, 8));

        // popula combos iniciais
        carregarSubtipos();
        carregarMarcas();
        carregarSabores();
        tfMarcaOutro.setVisible(false);
        cbSabor.setVisible(false);
        lblPeso.setText("Peso (g):");

        // eventos
        cbCategoria.addActionListener(e -> {
            carregarSubtipos();
            atualizarVisibilidade();
        });
        cbSubtipo.addActionListener(e -> {
            carregarMarcas();
            carregarSabores();
            atualizarVisibilidade();
        });
        cbMarca.addActionListener(e -> tfMarcaOutro.setVisible("Outros".equals(cbMarca.getSelectedItem())));
        btnSelectFornec.addActionListener(e -> {
            FornecedorSelectionDialog dlg = new FornecedorSelectionDialog(owner);
            dlg.setVisible(true);
            FornecedorModel f = dlg.getSelectedFornecedor();
            if (f != null) {
                fornecedorSel = f;
                lblFornecedor.setText(f.getNome());
            }
        });

        // layout
        add(new JLabel("Nome:"));                 add(tfNome);
        add(new JLabel("Categoria:"));            add(cbCategoria);
        add(new JLabel("Subtipo:"));              add(cbSubtipo);
        add(new JLabel("Marca:"));                add(cbMarca);
        add(new JLabel("Marca (Outros):"));       add(tfMarcaOutro);
        add(new JLabel("Sabor (apenas Suco):"));  add(cbSabor);
        add(new JLabel("Lote:"));                 add(tfLote);
        add(lblPeso);                             add(tfPeso);
        add(new JLabel("Data Validade:"));        add(tfDataValidade);

        // ** Seção Código de Barras **
        add(new JLabel("Código de Barras:"));
        JPanel painelCodBarras = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnScanner = new JButton("Ler com Scanner");
        JButton btnManual = new JButton("Inserir Manualmente");

        painelCodBarras.add(btnScanner);
        painelCodBarras.add(btnManual);
        painelCodBarras.add(lblCodigoLido);
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
        // ** Fim da seção Código de Barras **

        add(new JLabel("Quantidade:"));           add(tfQtd);
        add(new JLabel("Custo (R$):"));           add(tfCusto);
        add(new JLabel("Preço Venda (R$):"));     add(tfPreco);
        add(new JLabel("Fornecedor:"));           add(lblFornecedor);
        add(new JLabel());                        add(btnSelectFornec);

        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel());                        add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void carregarSubtipos() {
        cbSubtipo.removeAllItems();

        if ("Comida".equals(cbCategoria.getSelectedItem())) {
            for (String s : new String[]{"Salgadinho", "Doce"})
                cbSubtipo.addItem(s);
        } else {
            for (String s : new String[]{"Refrigerante", "Suco",
                                         "Achocolatado", "Água", "Bebida energética"})
                cbSubtipo.addItem(s);
        }

        // ✅ Seleciona o primeiro item automaticamente
        if (cbSubtipo.getItemCount() > 0)
            cbSubtipo.setSelectedIndex(0);
    }

    private void carregarMarcas() {
        cbMarca.removeAllItems();
        String st = (String) cbSubtipo.getSelectedItem();

        if (st == null) return; // Protege o switch

        String[] arr;
        switch (st) {
            case "Salgadinho":
                arr = new String[]{"Elma Chips", "Cheetos", "Fandangos", "Doritos", "Ruffles", "Lay's", "Torcida", "Yoki", "Outros"};
                break;
            case "Doce":
                arr = new String[]{"Garoto", "Lacta", "Nestlé", "Ferrero Rocher", "Kopenhagen", "Hershey's", "Outros"};
                break;
            case "Refrigerante":
                arr = new String[]{"Coca-Cola", "Pepsi", "Guaraná Antarctica", "Fanta", "Sprite", "Outros"};
                break;
            case "Achocolatado":
                arr = new String[]{"Nescau", "Toddy", "Ovomaltine", "Toddynho", "Nescau Pronto", "Outros"};
                break;
            case "Água":
                arr = new String[]{"Crystal", "Bonafont", "Minalba", "São Lourenço", "Pureza Vital", "Outros"};
                break;
            case "Bebida energética":
                arr = new String[]{"Red Bull", "Monster", "Burn", "TNT", "Fusion", "Outros"};
                break;
            default:
                arr = new String[]{"Outros"};
                break;
        }

        for (String m : arr) cbMarca.addItem(m);
    }

    private void carregarSabores() {
        cbSabor.removeAllItems();

        String st = (String) cbSubtipo.getSelectedItem();
        if (st == null) return;

        if ("Suco".equals(st)) {
            for (String s : new String[]{
                    "Laranja", "Uva", "Maçã", "Maracujá", "Manga",
                    "Abacaxi", "Acerola", "Goiaba", "Pêssego", "Limão", "Outros"
            }) cbSabor.addItem(s);
        }
    }

    private void atualizarVisibilidade() {
        String st = (String) cbSubtipo.getSelectedItem();
        tfMarcaOutro.setVisible("Outros".equals(cbMarca.getSelectedItem()));
        cbSabor.setVisible("Suco".equals(st));

        if ("Bebida".equals(cbCategoria.getSelectedItem())) {
            lblPeso.setText("Volume (ml):");
        } else {
            lblPeso.setText("Peso (g):");
        }
        revalidate();
        repaint();
    }

    private void preencherCampos() {
        tfNome.setText(alimentoOrig.getNome());
        cbCategoria.setSelectedItem(alimentoOrig.getCategoria());
        carregarSubtipos();
        cbSubtipo.setSelectedItem(alimentoOrig.getSubtipo());
        carregarMarcas();
        cbMarca.setSelectedItem(alimentoOrig.getMarca());
        tfMarcaOutro.setText(alimentoOrig.getMarca());
        carregarSabores();
        if ("Suco".equals(alimentoOrig.getSubtipo()))
            cbSabor.setSelectedItem(alimentoOrig.getSabor());
        tfLote.setText(alimentoOrig.getLote());
        tfPeso.setValue(alimentoOrig.getPeso());
        tfDataValidade.setText(alimentoOrig.getDataValidade());

        // Preenche o código de barras existente no lblCodigoLido
        String codigoExistente = alimentoOrig.getCodigoBarras();
        if (codigoExistente != null) {
            lblCodigoLido.setText(codigoExistente);
            lblCodigoLido.putClientProperty("codigoBarras", codigoExistente);
        }

        tfQtd.setValue(alimentoOrig.getQuantidade());
        tfCusto.setValue(alimentoOrig.getPrecoCompra());
        tfPreco.setValue(alimentoOrig.getPrecoVenda());
        fornecedorSel = new FornecedorModel();
        fornecedorSel.setId(alimentoOrig.getFornecedorId());
        fornecedorSel.setNome(alimentoOrig.getFornecedorNome());
        lblFornecedor.setText(alimentoOrig.getFornecedorNome());
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
        if ("Outros".equals(cbMarca.getSelectedItem()) &&
            tfMarcaOutro.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe a marca.");
            return;
        }

        try {
            String id = isEdicao ? alimentoOrig.getId() : UUID.randomUUID().toString();
            String nome = tfNome.getText().trim();
            String categoria  = (String) cbCategoria.getSelectedItem();
            String subtipo    = (String) cbSubtipo.getSelectedItem();
            String marca      = "Outros".equals(cbMarca.getSelectedItem())
                                ? tfMarcaOutro.getText().trim()
                                : (String) cbMarca.getSelectedItem();
            String sabor      = cbSabor.isVisible()
                                ? (String) cbSabor.getSelectedItem()
                                : "";
            String lote       = tfLote.getText().trim();
            double peso       = ((Number) tfPeso.getValue()).doubleValue();
            String unidade    = "Bebida".equals(categoria) ? "ml" : "g";
            String dataVal    = tfDataValidade.getText().trim();

            // Recupera o código de barras lido (se houver), caso contrário, deixa em branco.
            String codigo     = (String) lblCodigoLido.getClientProperty("codigoBarras");
            if (codigo == null) {
                codigo = "";
            }

            int quantidade    = ((Number) tfQtd.getValue()).intValue();
            double custo      = ((Number) tfCusto.getValue()).doubleValue();
            double preco      = ((Number) tfPreco.getValue()).doubleValue();
            String fornId     = fornecedorSel.getId();

            AlimentoModel a = new AlimentoModel(
                id, nome, quantidade, custo, preco,
                fornId, categoria, subtipo, marca, sabor,
                lote, peso, unidade, codigo, dataVal
            );

            ProdutoEstoqueService service = new ProdutoEstoqueService();
            if (isEdicao) service.atualizarAlimento(a);
            else          service.salvarNovoAlimento(a);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar Produto Alimentício:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
