package ui.estoque.dialog;

import util.UiKit;
import controller.ProdutoEstoqueController;
import dao.ConfigFiscalDefaultDAO;
import dao.FiscalCatalogDAO;
import model.ProdutoModel;
import model.ConfigFiscalModel;
import model.CodigoDescricaoModel;
import model.NcmModel;
import service.NcmService;
import util.FormatterFactory;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.util.UUID;

public class ProdutoCadastroDialog extends JDialog {
    private final boolean isEdicao;
    private final ProdutoModel produtoOrig;

    private final JTextField tfNome;
    private final JFormattedTextField tfQtd;
    private final JFormattedTextField tfCusto;
    private final JFormattedTextField tfPreco;
    private final JComboBox<String> cbNcm = new JComboBox<>();
    private final JComboBox<String> cbCfop = new JComboBox<>();
    private final JComboBox<String> cbCsosn = new JComboBox<>();
    private final JComboBox<String> cbOrigem = new JComboBox<>();
    private final JComboBox<String> cbUnidade = new JComboBox<>();

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();

    public ProdutoCadastroDialog(JFrame owner, ProdutoModel produto) {
        super(owner, produto == null ? "Novo Produto" : "Editar Produto", true);
        UiKit.applyDialogBase(this);
        this.isEdicao     = produto != null;
        this.produtoOrig  = produto;

        // Cria e monta UI
        tfNome  = new JTextField(20);
        tfQtd   = FormatterFactory.getFormattedIntField(0);
        tfCusto = FormatterFactory.getMoneyField(0.0);
        tfPreco = FormatterFactory.getMoneyField(0.0);

        initUI();
        carregarNcms();
        carregarCombosFiscais();
        aplicarDefaultsFiscais();
        if (isEdicao) preencherCampos(produto);

        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new GridLayout(0, 2, 8, 8));
        setResizable(true);

        // Nome
        add(new JLabel("Nome:"));
        add(tfNome);

        // Tipo fixo
        add(new JLabel("Tipo:"));
        add(new JLabel("Outro"));

        // Quantidade
        add(new JLabel("Quantidade:"));
        add(tfQtd);

        // Custo
        add(new JLabel("Custo (R$):"));
        add(tfCusto);

        // Preço de venda
        add(new JLabel("Preço Venda (R$):"));
        add(tfPreco);

        add(new JLabel("NCM:"));
        add(cbNcm);

        add(new JLabel("CFOP:"));
        add(cbCfop);

        add(new JLabel("CSOSN:"));
        add(cbCsosn);

        add(new JLabel("Origem:"));
        add(cbOrigem);

        add(new JLabel("Unidade:"));
        add(cbUnidade);

        // Botão Salvar / Atualizar
        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(new JLabel()); // filler
        add(btnSalvar);
    }

    private void preencherCampos(ProdutoModel p) {
        tfNome.setText(p.getNome());
        tfQtd.setValue(p.getQuantidade());
        tfCusto.setValue(p.getPrecoCompra());
        tfPreco.setValue(p.getPrecoVenda());
        selecionarPorCodigoPrefix(cbNcm, p.getNcm());
        selecionarPorCodigoPrefix(cbCfop, p.getCfop());
        selecionarPorCodigoPrefix(cbCsosn, p.getCsosn());
        selecionarPorCodigoPrefix(cbOrigem, p.getOrigem());
        selecionarPorCodigoPrefix(cbUnidade, p.getUnidade());
    }

    private void salvar() {
        try {
            String nome = tfNome.getText().trim();
            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O campo Nome é obrigatório.", "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String id = isEdicao
                ? produtoOrig.getId()
                : UUID.randomUUID().toString();

            int quantidade = UiKit.getIntValue(tfQtd, 0);
            double custo   = UiKit.getDoubleValue(tfCusto, 0.0);
            double venda   = UiKit.getDoubleValue(tfPreco, 0.0);

            String ncm = firstToken((String) cbNcm.getSelectedItem());
            String cfop = firstToken((String) cbCfop.getSelectedItem());
            String csosn = firstToken((String) cbCsosn.getSelectedItem());
            String origem = firstToken((String) cbOrigem.getSelectedItem());
            String unidade = firstToken((String) cbUnidade.getSelectedItem());

            ProdutoModel p = new ProdutoModel(id, nome, "Outro", quantidade, custo, venda);
            p.setNcm(ncm);
            p.setCfop(cfop);
            p.setCsosn(csosn);
            p.setOrigem(origem);
            p.setUnidade(unidade);
            ctrl.salvar(p);

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar produto:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void carregarNcms() {
        try {
            cbNcm.removeAllItems();
            java.util.List<NcmModel> ncms = NcmService.getInstance().findAll();
            for (NcmModel n : ncms) {
                cbNcm.addItem(n.getCodigo() + " - " + n.getDescricao());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar NCMs:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
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

    private void aplicarDefaultsFiscais() {
        try {
            ConfigFiscalModel cfg = new ConfigFiscalDefaultDAO().getDefault();
            if (cfg == null)
                return;
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
        if (s == null)
            return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}
