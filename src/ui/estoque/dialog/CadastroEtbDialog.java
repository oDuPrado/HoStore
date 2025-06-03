package ui.estoque.dialog;

import controller.ProdutoEstoqueController;
import dao.ColecaoDAO;
import dao.EtbDAO;
import dao.SetDAO;
import model.ColecaoModel;
import model.EtbModel;
import model.FornecedorModel;
import service.ProdutoEstoqueService;
import util.MaskUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class CadastroEtbDialog extends JDialog {

    private final boolean isEdicao;
    private final EtbModel etbOrig;

    private final JTextField tfNome = new JTextField(20);
    private final JComboBox<String> cbSerie = new JComboBox<>();
    private final JComboBox<ColecaoModel> cbColecao = new JComboBox<>();
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[] {
    "Booster Box", "Pokémon Center", "ETB", "Mini ETB", "Collection Box", "Special Collection","Latas","Box colecionáveis", "Trainer Kit","Mini Booster Box"
    });
    private final JComboBox<String> cbVersao = new JComboBox<>(new String[] {
            "Nacional", "Americana"
    });
    private final JFormattedTextField tfQtd = MaskUtils.getFormattedIntField(0);
    private final JFormattedTextField tfCusto = MaskUtils.moneyField(0.0);
    private final JFormattedTextField tfPreco = MaskUtils.moneyField(0.0);

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
        if (isEdicao)
            preencherCampos();
    }

    private void buildUI(JFrame owner) {
        JPanel content = new JPanel(new GridLayout(0, 2, 8, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        // Série / Coleção (via API)
        carregarSeries();
        cbSerie.addActionListener(e -> carregarColecoesPorSerie());

        content.add(new JLabel("Nome:"));
        content.add(tfNome);

        content.add(new JLabel("Série:"));
        content.add(cbSerie);

        content.add(new JLabel("Coleção:"));
        content.add(cbColecao);

        content.add(new JLabel("Tipo:"));
        content.add(cbTipo);

        content.add(new JLabel("Versão:"));
        content.add(cbVersao);

        content.add(new JLabel("Quantidade:"));
        content.add(tfQtd);

        content.add(new JLabel("Custo (R$):"));
        content.add(tfCusto);

        content.add(new JLabel("Preço Venda (R$):"));
        content.add(tfPreco);

        // fornecedor via diálogo de seleção
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

        // botão Salvar / Atualizar
        content.add(new JLabel());
        JButton btnSalvar = new JButton(isEdicao ? "Atualizar" : "Salvar");
        btnSalvar.addActionListener(e -> salvar());
        content.add(btnSalvar);

        pack();
        setLocationRelativeTo(owner);
    }

    private void carregarSeries() {
        try {
            cbSerie.removeAllItems();
            for (String s : new SetDAO().listarSeriesUnicas())
                cbSerie.addItem(s);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar séries.");
        }
    }

    private void carregarColecoesPorSerie() {
        try {
            cbColecao.removeAllItems();
            String serie = (String) cbSerie.getSelectedItem();
            if (serie != null) {
                for (ColecaoModel c : new ColecaoDAO().listarPorSerie(serie))
                    cbColecao.addItem(c);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar coleções.");
        }
    }

    private void preencherCampos() {
        tfNome.setText(etbOrig.getNome());
        cbSerie.setSelectedItem(etbOrig.getSerie());
        carregarColecoesPorSerie();
        for (int i = 0; i < cbColecao.getItemCount(); i++) {
            ColecaoModel c = cbColecao.getItemAt(i);
            if (c.getName().equalsIgnoreCase(etbOrig.getColecao())) {
                cbColecao.setSelectedItem(c);
                break;
            }
        }
        cbTipo.setSelectedItem(etbOrig.getTipo());
        cbVersao.setSelectedItem(etbOrig.getVersao());
        tfQtd.setValue(etbOrig.getQuantidade());
        tfCusto.setValue(etbOrig.getPrecoCompra());
        tfPreco.setValue(etbOrig.getPrecoVenda());

        // fornecedor
        try {
            fornecedorSel = new dao.FornecedorDAO().buscarPorId(etbOrig.getFornecedor());
            if (fornecedorSel != null) {
                lblFornecedor.setText(fornecedorSel.getNome());
            } else {
                lblFornecedor.setText("Fornecedor não Cadastrado");
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
        try {
            String id = isEdicao
                    ? etbOrig.getId()
                    : UUID.randomUUID().toString();

            EtbModel e = new EtbModel(
                    id,
                    tfNome.getText().trim(),
                    ((Number) tfQtd.getValue()).intValue(),
                    ((Number) tfCusto.getValue()).doubleValue(),
                    ((Number) tfPreco.getValue()).doubleValue(),
                    fornecedorSel.getId(),
                    (String) cbSerie.getSelectedItem(),
                    ((ColecaoModel) cbColecao.getSelectedItem()).getName(),
                    (String) cbTipo.getSelectedItem(),
                    (String) cbVersao.getSelectedItem());

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
