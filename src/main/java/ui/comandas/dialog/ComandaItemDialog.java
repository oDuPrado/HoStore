package ui.comandas.dialog;

import dao.ProdutoDAO;
import model.ProdutoModel;
import service.ComandaService;
import service.SessaoService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ComandaItemDialog extends JDialog {

    private final int comandaId;

    private final JComboBox<ProdutoModel> cbProduto = new JComboBox<>();
    private final JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JSpinner spPreco = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.50));
    private final JSpinner spDesconto = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.50));
    private final JSpinner spAcrescimo = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 999999.0, 0.50));
    private final JTextArea taObs = new JTextArea(3, 30);

    public ComandaItemDialog(Window owner, int comandaId) {
        super(owner, "Adicionar Item - Comanda #" + comandaId, ModalityType.APPLICATION_MODAL);
        this.comandaId = comandaId;

        setSize(560, 340);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);

        carregarProdutos();

        cbProduto.addActionListener(e -> {
            ProdutoModel p = (ProdutoModel) cbProduto.getSelectedItem();
            if (p != null) spPreco.setValue(p.getPrecoVenda());
        });

        JButton btnAdd = new JButton("Adicionar");
        btnAdd.addActionListener(e -> adicionar());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        int y = 0;

        g.gridx=0; g.gridy=y; form.add(new JLabel("Produto:"), g);
        g.gridx=1; form.add(cbProduto, g); y++;

        g.gridx=0; g.gridy=y; form.add(new JLabel("Qtd:"), g);
        g.gridx=1; form.add(spQtd, g); y++;

        g.gridx=0; g.gridy=y; form.add(new JLabel("Preço Unit:"), g);
        g.gridx=1; form.add(spPreco, g); y++;

        g.gridx=0; g.gridy=y; form.add(new JLabel("Desconto:"), g);
        g.gridx=1; form.add(spDesconto, g); y++;

        g.gridx=0; g.gridy=y; form.add(new JLabel("Acréscimo:"), g);
        g.gridx=1; form.add(spAcrescimo, g); y++;

        g.gridx=0; g.gridy=y; g.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Obs:"), g);
        g.gridx=1; form.add(new JScrollPane(taObs), g);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnCancel);
        bottom.add(btnAdd);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void carregarProdutos() {
        try {
            List<ProdutoModel> produtos = new ProdutoDAO().listAll();
            DefaultComboBoxModel<ProdutoModel> m = new DefaultComboBoxModel<>();
            for (ProdutoModel p : produtos) m.addElement(p);

            cbProduto.setModel(m);
            cbProduto.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ProdutoModel p) {
                        setText(p.getNome() + "  |  " + String.format("R$ %.2f", p.getPrecoVenda()) + "  |  Estoque: " + p.getQuantidade());
                    }
                    return this;
                }
            });

            ProdutoModel first = (ProdutoModel) cbProduto.getSelectedItem();
            if (first != null) spPreco.setValue(first.getPrecoVenda());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adicionar() {
        try {
            ProdutoModel p = (ProdutoModel) cbProduto.getSelectedItem();
            if (p == null) throw new Exception("Selecione um produto.");

            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";

            int qtd = (int) spQtd.getValue();
            double preco = (double) spPreco.getValue();
            double desconto = (double) spDesconto.getValue();
            double acrescimo = (double) spAcrescimo.getValue();

            new ComandaService().adicionarItem(comandaId, p.getId(), qtd, preco, desconto, acrescimo, taObs.getText(), usuario);

            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
