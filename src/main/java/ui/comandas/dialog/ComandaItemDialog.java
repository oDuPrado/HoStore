package ui.comandas.dialog;

import dao.ProdutoDAO;
import model.ProdutoModel;
import service.ComandaService;
import service.SessaoService;
import util.UiKit;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
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

        UiKit.applyDialogBase(this);

        setSize(720, 420);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);

        // Consistência visual dos campos
        enforceFieldSize(cbProduto);
        enforceSpinnerSize(spQtd);
        enforceSpinnerSize(spPreco);
        enforceSpinnerSize(spDesconto);
        enforceSpinnerSize(spAcrescimo);

        // Editor numérico alinhado
        alignSpinnerRight(spQtd);
        alignSpinnerRight(spPreco);
        alignSpinnerRight(spDesconto);
        alignSpinnerRight(spAcrescimo);

        carregarProdutos();

        cbProduto.addActionListener(e -> {
            ProdutoModel p = (ProdutoModel) cbProduto.getSelectedItem();
            if (p != null)
                spPreco.setValue(p.getPrecoVenda());
        });

        JButton btnAdd = UiKit.primary("Adicionar (ENTER)");
        btnAdd.addActionListener(e -> adicionar());

        JButton btnCancel = UiKit.ghost("Cancelar (ESC)");
        btnCancel.addActionListener(e -> dispose());

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildFormCard(), BorderLayout.CENTER);
        add(buildBottomCard(btnCancel, btnAdd), BorderLayout.SOUTH);

        bindKeys(btnAdd, btnCancel);
    }

    private JComponent buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("➕ Adicionar Item"));
        left.add(UiKit.hint("Selecione o produto e ajuste valores | Preço sugerido vem do produto"));
        top.add(left, BorderLayout.WEST);

        JLabel right = UiKit.hint("Comanda #" + comandaId);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JComponent buildFormCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // Produto (full)
        addRow(form, g, y++, "Produto:", cbProduto, true);

        // Linha dupla: Qtd | Preço
        addRow2(form, g, y++, "Qtd:", spQtd, "Preço Unit.:", spPreco);

        // Linha dupla: Desconto | Acréscimo
        addRow2(form, g, y++, "Desconto:", spDesconto, "Acréscimo:", spAcrescimo);

        // Observações (ocupa altura)
        g.gridy = y;
        g.gridx = 0;
        g.weightx = 0;
        g.weighty = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Obs:"), g);

        g.gridx = 1;
        g.weightx = 1;
        g.weighty = 1;
        g.gridwidth = 3;
        g.fill = GridBagConstraints.BOTH;
        form.add(UiKit.scroll(taObs), g);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildBottomCard(JButton btnCancel, JButton btnAdd) {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(btnCancel);
        right.add(btnAdd);

        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void bindKeys(JButton btnAdd, JButton btnCancel) {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        am.put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnCancel.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "adicionar");
        am.put("adicionar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // não sequestra ENTER enquanto o cara digita observação
                if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextArea)
                    return;
                btnAdd.doClick();
            }
        });
    }

    private void carregarProdutos() {
        try {
            List<ProdutoModel> produtos = new ProdutoDAO().listAll();
            DefaultComboBoxModel<ProdutoModel> m = new DefaultComboBoxModel<>();
            for (ProdutoModel p : produtos)
                m.addElement(p);

            cbProduto.setModel(m);
            cbProduto.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ProdutoModel p) {
                        setText(p.getNome()
                                + "  |  " + String.format("R$ %.2f", p.getPrecoVenda())
                                + "  |  Estoque: " + p.getQuantidade());
                    }
                    return this;
                }
            });

            ProdutoModel first = (ProdutoModel) cbProduto.getSelectedItem();
            if (first != null)
                spPreco.setValue(first.getPrecoVenda());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adicionar() {
        try {
            ProdutoModel p = (ProdutoModel) cbProduto.getSelectedItem();
            if (p == null)
                throw new Exception("Selecione um produto.");

            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";

            int qtd = (int) spQtd.getValue();
            double preco = (double) spPreco.getValue();
            double desconto = (double) spDesconto.getValue();
            double acrescimo = (double) spAcrescimo.getValue();

            new ComandaService().adicionarItem(
                    comandaId, p.getId(), qtd, preco, desconto, acrescimo, taObs.getText(), usuario);

            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
     * ===================== Helpers de layout (visual only) =====================
     */

    private void enforceFieldSize(JComponent c) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(Math.max(d.width, 420), 30));
        c.setMinimumSize(new Dimension(220, 30));
    }

    private void enforceSpinnerSize(JSpinner s) {
        Dimension d = s.getPreferredSize();
        s.setPreferredSize(new Dimension(Math.max(d.width, 180), 30));
        s.setMinimumSize(new Dimension(140, 30));
    }

    private void alignSpinnerRight(JSpinner s) {
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            JFormattedTextField tf = de.getTextField();
            tf.setHorizontalAlignment(SwingConstants.RIGHT);
            if (tf.getFormatter() instanceof NumberFormatter nf) {
                nf.setAllowsInvalid(true);
                nf.setOverwriteMode(false);
            }
            tf.setFocusLostBehavior(JFormattedTextField.COMMIT);
        }
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field, boolean full) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = full ? 3 : 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, g);
    }

    private void addRow2(JPanel p, GridBagConstraints g, int row,
            String l1, JComponent f1,
            String l2, JComponent f2) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l1), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f1, g);

        g.gridx = 2;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l2), g);

        g.gridx = 3;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f2, g);
    }
}
