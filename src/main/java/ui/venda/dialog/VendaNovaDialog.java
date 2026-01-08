// src/ui/venda/dialog/VendaNovaDialog.java
package ui.venda.dialog;

import controller.VendaController;
import dao.ClienteDAO;
import dao.ProdutoDAO;
import model.ProdutoModel;
import model.VendaItemModel;
import ui.venda.painel.PainelVendas;
import util.AlertUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import ui.clientes.dialog.ClienteCadastroDialog;
import ui.dialog.SelectCartaDialog;

public class VendaNovaDialog extends JDialog {

    /* ---------- DAOs & Controller ---------- */
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO(); // (mantido, mesmo que você não use aqui ainda)
    private final VendaController controller = new VendaController();
    private final PainelVendas painelPai;

    /* ---------- Combos e tabela ---------- */
    private final JComboBox<String> clienteCombo;

    private final DefaultTableModel carrinhoModel = new DefaultTableModel(
            new String[] { "Produto", "Qtd", "R$ Unit.", "% Desc", "R$ Total", "R$ Desc", "" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c >= 1 && c <= 3; // qtd, unit, desconto
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 1 -> Integer.class;
                case 2, 3, 4, 5 -> Double.class;
                default -> String.class;
            };
        }
    };

    private final JTable carrinhoTable = new JTable(carrinhoModel);

    /* ---------- Resumo ---------- */
    private final JLabel resumoLbl = new JLabel();

    // Listener automático de atualização
    {
        carrinhoModel.addTableModelListener(e -> {
            int col = e.getColumn();
            if (col >= 1 && col <= 3) {
                atualizarTodosTotais();
            }
        });
    }

    public VendaNovaDialog(JFrame owner, PainelVendas painelPai) {
        super(owner, "Nova Venda", true);
        this.painelPai = painelPai;

        UiKit.applyDialogBase(this);

        setSize(980, 680);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        /* ===================== TOP (CARD) ===================== */
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topLeft.setOpaque(false);

        topLeft.add(UiKit.title("Nova Venda"));
        topLeft.add(UiKit.hint("F2 Produto | F3 Cartas | DEL Remove | ENTER Finaliza"));

        topCard.add(topLeft, BorderLayout.WEST);

        // ===================== TOP ACTIONS (SEM BRIGA NO CABEÇALHO)
        // =====================
        JPanel topActions = new JPanel(new GridBagLayout());
        topActions.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.anchor = GridBagConstraints.EAST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Cliente combo + botão add cliente
        clienteCombo = new JComboBox<>(clienteDAO.listarTodosNomes().toArray(new String[0]));
        clienteCombo.setEditable(true);
        clienteCombo.setPreferredSize(new Dimension(340, 30)); // maior por padrão
        clienteCombo.setMinimumSize(new Dimension(220, 30)); // não deixa virar migalha

        JButton btnAddCliente = UiKit.ghost("➕");
        btnAddCliente.setMargin(new Insets(2, 8, 2, 8));
        btnAddCliente.setToolTipText("Cadastrar novo cliente");
        btnAddCliente.addActionListener(e -> abrirCadastroCliente());

        // Linha 0: "Cliente:" | Combo (expande) | + (fixo)
        gc.gridy = 0;

        gc.gridx = 0;
        gc.weightx = 0;
        topActions.add(new JLabel("Cliente:"), gc);

        gc.gridx = 1;
        gc.weightx = 1; // combo ganha o espaço todo
        topActions.add(clienteCombo, gc);

        gc.gridx = 2;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE; // botão não estica
        topActions.add(btnAddCliente, gc);

        // Botões
        JButton btnAddProd = UiKit.primary("➕ Produto (F2)");
        btnAddProd.addActionListener(e -> abrirSelectProduto());

        JButton btnAddCarta = UiKit.ghost("🎴 Cartas (F3)");
        btnAddCarta.addActionListener(e -> abrirSelectCarta());

        // Linha 1: (vazio) | botões alinhados à direita
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;

        gc.gridx = 0;
        gc.weightx = 1; // empurra pra direita
        gc.gridwidth = 1;
        topActions.add(Box.createHorizontalStrut(1), gc);

        gc.gridx = 1;
        gc.weightx = 0;
        topActions.add(btnAddProd, gc);

        gc.gridx = 2;
        topActions.add(btnAddCarta, gc);

        topCard.add(topActions, BorderLayout.EAST);

        add(topCard, BorderLayout.NORTH);

        /* ===================== CENTER (CARD + TABLE) ===================== */
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));

        JPanel centerHeader = new JPanel(new BorderLayout());
        centerHeader.setOpaque(false);
        centerHeader.add(UiKit.title("Itens"), BorderLayout.WEST);
        centerCard.add(centerHeader, BorderLayout.NORTH);

        personalizarTabela();
        UiKit.tableDefaults(carrinhoTable);

        // Zebra em tudo (inclusive quando não selecionado)
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < carrinhoTable.getColumnCount(); i++) {
            carrinhoTable.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        // Mas mantém moeda com alinhamento e formato nas colunas certas
        carrinhoTable.getColumnModel().getColumn(4).setCellRenderer(moedaRendererZebra(zebra));
        carrinhoTable.getColumnModel().getColumn(5).setCellRenderer(moedaRendererZebra(zebra));

        // Coluna % desc com destaque (sem “pintar o mundo de amarelo”)
        carrinhoTable.getColumnModel().getColumn(3).setCellRenderer(descRendererZebra(zebra));

        carrinhoTable.setRowHeight(30);
        carrinhoTable.setFillsViewportHeight(true);

        centerCard.add(UiKit.scroll(carrinhoTable), BorderLayout.CENTER);

        add(centerCard, BorderLayout.CENTER);

        /* ===================== FOOTER (CARD) ===================== */
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        resumoLbl.setFont(resumoLbl.getFont().deriveFont(Font.BOLD, 14f));
        bottomCard.add(resumoLbl, BorderLayout.WEST);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        botoes.setOpaque(false);

        JButton btnExcluir = UiKit.ghost("🗑️ Remover (DEL)");
        btnExcluir.addActionListener(e -> excluirLinhaSelecionada());

        JButton btnFinalizar = UiKit.primary("✅ Finalizar (ENTER)");
        btnFinalizar.addActionListener(e -> finalizarVenda());

        botoes.add(btnExcluir);
        botoes.add(btnFinalizar);

        bottomCard.add(botoes, BorderLayout.EAST);

        add(bottomCard, BorderLayout.SOUTH);

        atualizarResumo();

        // Atalhos
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke("F2"), "addProduto");
        am.put("addProduto", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnAddProd.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("F3"), "addCarta");
        am.put("addCarta", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnAddCarta.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("DELETE"), "excluirLinha");
        am.put("excluirLinha", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnExcluir.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("ENTER"), "finalizarVenda");
        am.put("finalizarVenda", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnFinalizar.doClick();
            }
        });
    }

    /* ==================================================================== */
    /* ========================== TABELA =================================== */
    /* ==================================================================== */

    private void personalizarTabela() {
        TableColumnModel tcm = carrinhoTable.getColumnModel();

        // Qtd (int)
        NumberFormatter intFmt = new NumberFormatter(new DecimalFormat("#0"));
        intFmt.setValueClass(Integer.class);
        intFmt.setAllowsInvalid(false);

        JFormattedTextField qtdField = new JFormattedTextField(intFmt);
        qtdField.setHorizontalAlignment(JTextField.RIGHT);
        qtdField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        qtdField.addFocusListener(selAll(qtdField));

        tcm.getColumn(1).setCellEditor(new DefaultCellEditor(qtdField) {
            @Override
            public Object getCellEditorValue() {
                return qtdField.getValue();
            }
        });

        // Preço unitário
        NumberFormat moneyFmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);

        NumberFormatter moneyEditor = new NumberFormatter(moneyFmt);
        moneyEditor.setValueClass(Double.class);
        moneyEditor.setAllowsInvalid(false);

        JFormattedTextField unitField = new JFormattedTextField(moneyEditor);
        unitField.setHorizontalAlignment(JTextField.RIGHT);
        unitField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        unitField.addFocusListener(selAll(unitField));

        tcm.getColumn(2).setCellEditor(new DefaultCellEditor(unitField) {
            @Override
            public Object getCellEditorValue() {
                return unitField.getValue();
            }
        });

        // Desconto %
        NumberFormatter pctEditor = new NumberFormatter(NumberFormat.getNumberInstance(new Locale("pt", "BR")));
        pctEditor.setValueClass(Double.class);
        pctEditor.setMinimum(0.0);
        pctEditor.setMaximum(100.0);
        pctEditor.setAllowsInvalid(false);
        pctEditor.setOverwriteMode(false);

        JFormattedTextField pctField = new JFormattedTextField(pctEditor);
        pctField.setHorizontalAlignment(JTextField.RIGHT);
        pctField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        pctField.addFocusListener(selAll(pctField));

        tcm.getColumn(3).setCellEditor(new DefaultCellEditor(pctField) {
            @Override
            public Object getCellEditorValue() {
                return pctField.getValue();
            }
        });

        // Moeda nas colunas 4/5
        tcm.getColumn(4).setCellRenderer(moedaRenderer());
        tcm.getColumn(5).setCellRenderer(moedaRenderer());

        // Coluna vazia (respiro)
        tcm.getColumn(6).setPreferredWidth(40);

        // Atualiza totais sempre que edição acaba
        addStopListener(Integer.class);
        addStopListener(Double.class);
    }

    private void addStopListener(Class<?> clazz) {
        TableCellEditor ed = carrinhoTable.getDefaultEditor(clazz);
        if (ed == null)
            return;

        ed.addCellEditorListener(new CellEditorListener() {
            public void editingStopped(ChangeEvent e) {
                atualizarTodosTotais();
            }

            public void editingCanceled(ChangeEvent e) {
            }
        });
    }

    private static DefaultTableCellRenderer moedaRenderer() {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object v) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                double val = (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
                super.setText(cf.format(val));
            }
        };
    }

    /**
     * Mantém zebra + formatação de moeda.
     */
    private static TableCellRenderer moedaRendererZebra(DefaultTableCellRenderer zebraBase) {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return (table, value, isSelected, hasFocus, row, column) -> {
            Component c = zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel l = (JLabel) c;
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            double val = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            l.setText(cf.format(val));
            return l;
        };
    }

    /**
     * Mantém zebra + destaca desconto > 0 sem destruir o tema.
     */
    private static TableCellRenderer descRendererZebra(DefaultTableCellRenderer zebraBase) {
        return (table, value, isSelected, hasFocus, row, column) -> {
            Component c = zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel l = (JLabel) c;
            l.setHorizontalAlignment(SwingConstants.RIGHT);

            double desc = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            if (!isSelected && desc > 0.0) {
                // sombra suave: reaproveita o fundo atual e dá um "shift"
                Color bg = l.getBackground();
                l.setBackground(new Color(
                        Math.min(255, bg.getRed() + 10),
                        Math.min(255, bg.getGreen() + 10),
                        Math.max(0, bg.getBlue() - 10)));
                l.setFont(l.getFont().deriveFont(Font.BOLD));
            } else if (!isSelected) {
                l.setFont(l.getFont().deriveFont(Font.PLAIN));
            }
            return l;
        };
    }

    private static FocusAdapter selAll(JFormattedTextField f) {
        return new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(f::selectAll);
            }
        };
    }

    /* ==================================================================== */
    /* ========================== AÇÕES ==================================== */
    /* ==================================================================== */

    private void abrirSelectProduto() {
        SelectProdutoDialog dlg = new SelectProdutoDialog((JFrame) getOwner());
        dlg.setVisible(true);

        List<ProdutoModel> prod = dlg.getSelecionados();
        for (ProdutoModel p : prod) {
            controller.adicionarItem(new VendaItemModel(p.getId(), 1, p.getPrecoVenda(), 0));
            carrinhoModel.addRow(new Object[] {
                    p.getNome(), 1, p.getPrecoVenda(), 0.0,
                    p.getPrecoVenda(), 0.0, ""
            });
        }
        atualizarTodosTotais();
    }

    private void abrirSelectCarta() {
        SelectCartaDialog dlg = new SelectCartaDialog((JFrame) getOwner());
        dlg.setVisible(true);

        dlg.getSelecionadas().forEach(c -> {
            controller.adicionarItem(new VendaItemModel(c.getId(), 1, c.getPrecoLoja(), 0));
            carrinhoModel.addRow(new Object[] {
                    c.getNome(), 1, c.getPrecoLoja(), 0.0,
                    c.getPrecoLoja(), 0.0, ""
            });
        });

        atualizarTodosTotais();
    }

    private void atualizarTodosTotais() {
        // Evita “metade commitada” quando ENTER finaliza durante edição
        if (carrinhoTable.isEditing()) {
            try {
                carrinhoTable.getCellEditor().stopCellEditing();
            } catch (Exception ignored) {
            }
        }

        double totalVenda = 0, totalDesc = 0;

        for (int r = 0; r < carrinhoModel.getRowCount(); r++) {
            int qtd = safeInt(carrinhoModel.getValueAt(r, 1));
            double unit = safeDouble(carrinhoModel.getValueAt(r, 2));
            double pct = safeDouble(carrinhoModel.getValueAt(r, 3));

            double bruto = qtd * unit;
            double descV = bruto * pct / 100.0;
            double tot = bruto - descV;

            carrinhoModel.setValueAt(tot, r, 4);
            carrinhoModel.setValueAt(descV, r, 5);

            totalVenda += tot;
            totalDesc += descV;

            if (r < controller.getCarrinho().size()) {
                VendaItemModel m = controller.getCarrinho().get(r);
                m.setQtd(qtd);
                m.setPreco(unit);
                m.setDesconto(pct);
            }
        }

        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        resumoLbl.setText("Total: " + cf.format(totalVenda) + " | Desconto: " + cf.format(totalDesc));
    }

    private int safeInt(Object o) {
        if (o instanceof Number n)
            return Math.max(0, n.intValue());
        return 0;
    }

    private double safeDouble(Object o) {
        if (o instanceof Number n) {
            double v = n.doubleValue();
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0)
                return 0.0;
            return v;
        }
        return 0.0;
    }

    private void atualizarResumo() {
        atualizarTodosTotais();
    }

    private void excluirLinhaSelecionada() {
        int r = carrinhoTable.getSelectedRow();
        if (r < 0) {
            AlertUtils.info("Selecione uma linha.");
            return;
        }
        if (r < controller.getCarrinho().size()) {
            controller.getCarrinho().remove(r);
        }
        carrinhoModel.removeRow(r);
        atualizarResumo();
    }

    private void finalizarVenda() {
        if (carrinhoTable.isEditing()) {
            try {
                carrinhoTable.getCellEditor().stopCellEditing();
            } catch (Exception ignored) {
            }
        }

        if (controller.getCarrinho().isEmpty()) {
            AlertUtils.error("Carrinho vazio!");
            return;
        }

        String nomeCliente = ((String) clienteCombo.getEditor().getItem()).trim();
        String clienteId = clienteDAO.obterIdPorNome(nomeCliente);
        if (clienteId == null) {
            AlertUtils.error("Cliente inválido.");
            return;
        }

        new VendaFinalizarDialog(this, controller, clienteId, painelPai).setVisible(true);
    }

    private void abrirCadastroCliente() {
        Window w = SwingUtilities.getWindowAncestor(this);
        ClienteCadastroDialog dlg = new ClienteCadastroDialog(w, null);
        dlg.setVisible(true);

        List<String> nomes = clienteDAO.listarTodosNomes();
        clienteCombo.setModel(new DefaultComboBoxModel<>(nomes.toArray(new String[0])));

        if (!nomes.isEmpty()) {
            clienteCombo.setSelectedItem(nomes.get(nomes.size() - 1));
        }
    }
}
