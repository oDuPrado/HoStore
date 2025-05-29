// src/ui/venda/dialog/VendaNovaDialog.java
package ui.venda.dialog;

import controller.VendaController;
import dao.ClienteDAO;
import dao.ProdutoDAO;
import model.ProdutoModel;
import model.VendaItemModel;
import ui.venda.painel.PainelVendas;
import util.AlertUtils;

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
import ui.dialog.SelectCartaDialog;

/**
 * Carrinho de venda genérico (qualquer produto).
 */
public class VendaNovaDialog extends JDialog {

    /* ---------- DAOs & Controller ---------- */
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaController controller = new VendaController();
    private final PainelVendas painelPai;

    /* ---------- Combos e tabela ---------- */
    private final JComboBox<String> clienteCombo;
    private final DefaultTableModel carrinhoModel = new DefaultTableModel(
            new String[] { "Produto", "Qtd", "R$ Unit.", "% Desc", "R$ Total", "R$ Desc", "" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c >= 1 && c <= 3; // apenas qtd, unit e desconto são editáveis
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

    // ⚠️ Adiciona o listener de atualização automática
    {
        carrinhoModel.addTableModelListener(e -> {
            int col = e.getColumn();
            // Só recalcula se for uma das colunas editáveis
            if (col >= 1 && col <= 3) {
                atualizarTodosTotais();
            }
        });
    }

    /* ---------- Resumo/rodapé ---------- */
    private final JLabel resumoLbl = new JLabel();

    public VendaNovaDialog(JFrame owner, PainelVendas painelPai) {
        super(owner, "Nova Venda", true);
        this.painelPai = painelPai;

        setSize(900, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        /* ===== TOP: Cliente + botões ===== */
        clienteCombo = new JComboBox<>(clienteDAO.listarTodosNomes().toArray(new String[0]));
        clienteCombo.setEditable(true);

        JButton btnAddProd = new JButton("➕ Adicionar Produto");
        btnAddProd.addActionListener(e -> abrirSelectProduto());

        JButton btnAddCarta = new JButton("🎴 Adicionar Cartas");
        btnAddCarta.addActionListener(e -> abrirSelectCarta());

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topo.add(new JLabel("Cliente:"));
        topo.add(clienteCombo);
        topo.add(btnAddProd);
        topo.add(btnAddCarta);
        add(topo, BorderLayout.NORTH);

        /* ===== Tabela ===== */
        personalizarTabela();
        add(new JScrollPane(carrinhoTable), BorderLayout.CENTER);

        /* ===== Rodapé ===== */
        JPanel rodape = new JPanel(new BorderLayout());

        resumoLbl.setFont(resumoLbl.getFont().deriveFont(Font.BOLD, 14f));
        rodape.add(resumoLbl, BorderLayout.WEST);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnExcluir = new JButton("🗑️ Excluir Linha");
        btnExcluir.addActionListener(e -> excluirLinhaSelecionada());
        JButton btnFinalizar = new JButton("✅ Finalizar");
        btnFinalizar.addActionListener(e -> finalizarVenda());

        botoes.add(btnExcluir);
        botoes.add(btnFinalizar);
        rodape.add(botoes, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
        atualizarResumo();

        // Atalhos de teclado globais
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // F2 - Adicionar Produto
        im.put(KeyStroke.getKeyStroke("F2"), "addProduto");
        am.put("addProduto", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnAddProd.doClick();
            }
        });

        // F3 - Adicionar Carta
        im.put(KeyStroke.getKeyStroke("F3"), "addCarta");
        am.put("addCarta", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnAddCarta.doClick();
            }
        });

        // DELETE - Excluir linha
        im.put(KeyStroke.getKeyStroke("DELETE"), "excluirLinha");
        am.put("excluirLinha", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnExcluir.doClick();
            }
        });

        // ENTER - Finalizar
        im.put(KeyStroke.getKeyStroke("ENTER"), "finalizarVenda");
        am.put("finalizarVenda", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                btnFinalizar.doClick();
            }
        });

    }

    /* ==================================================================== */
    /* ================ MÉTODOS AUXILIARES ================================= */
    /* ==================================================================== */

    private void personalizarTabela() {
        TableColumnModel tcm = carrinhoTable.getColumnModel();

        // --------- Qtd (int) ---------
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

        // --------- Preço unitário ---------
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

        // --------- Desconto % ---------
        NumberFormatter pctEditor = new NumberFormatter(NumberFormat.getNumberInstance(new Locale("pt", "BR")));
        pctEditor.setValueClass(Double.class);
        pctEditor.setMinimum(0.0);
        pctEditor.setMaximum(100.0);
        pctEditor.setAllowsInvalid(false);
        pctEditor.setOverwriteMode(false); // permite digitação parcial

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

        tcm.getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        double desc = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
        if (desc > 0) {
            c.setBackground(new Color(255, 240, 200)); // amarelo claro
        } else {
            c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
        }
        return c;
    }
});

        /* Atualiza totais sempre que uma edição é concluída */
        carrinhoTable.getDefaultEditor(Integer.class)
                .addCellEditorListener(new CellEditorListener() {
                    public void editingStopped(ChangeEvent e) {
                        atualizarTodosTotais();
                    }

                    public void editingCanceled(ChangeEvent e) {
                    }
                });

        carrinhoTable.getDefaultEditor(Double.class)
                .addCellEditorListener(new CellEditorListener() {
                    public void editingStopped(ChangeEvent e) {
                        atualizarTodosTotais();
                    }

                    public void editingCanceled(ChangeEvent e) {
                    }
                });

        // --------- Formatação de moeda nas colunas 4/5 ---------
        tcm.getColumn(4).setCellRenderer(moedaRenderer());
        tcm.getColumn(5).setCellRenderer(moedaRenderer());

        // --------- Coluna vazia (expansão) ----------
        tcm.getColumn(6).setPreferredWidth(50);
    }

    private static DefaultTableCellRenderer moedaRenderer() {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object v) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                super.setText(cf.format((Double) v));
            }
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

    /* ---------- Selecionar Produtos ---------- */
    private void abrirSelectProduto() {
        SelectProdutoDialog dlg = new SelectProdutoDialog((JFrame) getOwner());
        dlg.setVisible(true);
        List<ProdutoModel> prod = dlg.getSelecionados();
        for (ProdutoModel p : prod) {
            controller.adicionarItem(new VendaItemModel(
                    p.getId(), 1, p.getPrecoVenda(), 0));
            carrinhoModel.addRow(new Object[] {
                    p.getNome(), 1, p.getPrecoVenda(), 0.0,
                    p.getPrecoVenda(), 0.0, ""
            });
        }
        atualizarTodosTotais();
    }

    /* ---------- Selecionar Cartas (dialog antigo) ---------- */
    private void abrirSelectCarta() {
        SelectCartaDialog dlg = new SelectCartaDialog((JFrame) getOwner());
        dlg.setVisible(true);
        dlg.getSelecionadas().forEach(c -> {
            controller.adicionarItem(new VendaItemModel(
                    c.getId(), 1, c.getPrecoLoja(), 0));
            carrinhoModel.addRow(new Object[] {
                    c.getNome(), 1, c.getPrecoLoja(), 0.0,
                    c.getPrecoLoja(), 0.0, ""
            });
        });
        atualizarTodosTotais();
    }

    /* ---------- Recalcula totais linha a linha ---------- */
    private void atualizarTodosTotais() {
        double totalVenda = 0, totalDesc = 0;
        for (int r = 0; r < carrinhoModel.getRowCount(); r++) {
            int qtd = (Integer) carrinhoModel.getValueAt(r, 1);
            double unit = (Double) carrinhoModel.getValueAt(r, 2);
            double pct = (Double) carrinhoModel.getValueAt(r, 3);
            double bruto = qtd * unit;
            double descV = bruto * pct / 100.0;
            double tot = bruto - descV;
            carrinhoModel.setValueAt(tot, r, 4);
            carrinhoModel.setValueAt(descV, r, 5);
            totalVenda += tot;
            totalDesc += descV;
            // Atualiza model na lista do controller
            VendaItemModel m = controller.getCarrinho().get(r);
            m.setQtd(qtd);
            m.setPreco(unit);
            m.setDesconto(pct);
        }
        resumoLbl.setText(String.format("Total: R$ %.2f | Desconto: R$ %.2f",
                totalVenda, totalDesc));
    }

    private void atualizarResumo() {
        atualizarTodosTotais();
    }

    /* ---------- Excluir linha ---------- */
    private void excluirLinhaSelecionada() {
        int r = carrinhoTable.getSelectedRow();
        if (r < 0) {
            AlertUtils.info("Selecione uma linha.");
            return;
        }
        controller.getCarrinho().remove(r);
        carrinhoModel.removeRow(r);
        atualizarResumo();
    }

    /* ---------- Finalizar ---------- */
    private void finalizarVenda() {
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
        new VendaFinalizarDialog(this, controller, clienteId, painelPai)
                .setVisible(true);
    }
}
