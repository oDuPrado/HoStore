package ui.dialog;

import controller.VendaController;
import dao.ClienteDAO;
import dao.CartaDAO;
import model.Carta;
import model.VendaItemModel;
import ui.PainelVendas;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo de criação da venda – edição inline com Salvar e Excluir por linha.
 */
public class VendaNovaDialog extends JDialog {

    private final ClienteDAO clienteDAO   = new ClienteDAO();
    private final CartaDAO   cartaDAO     = new CartaDAO();
    private final VendaController controller = new VendaController();
    private final PainelVendas painelPai;

    private final JComboBox<String> clienteCombo;
    private final DefaultTableModel carrinhoModel = new DefaultTableModel(
        new String[]{ "Carta", "Qtd", "R$ Unit.", "Desc (%)", "R$ Total", "Salvar", "Excluir" }, 0
    ) {
        @Override public boolean isCellEditable(int row, int col) {
            return (col >= 1 && col <= 3) || col == 5 || col == 6;
        }

        @Override public Class<?> getColumnClass(int col) {
            switch (col) {
                case 1: return Integer.class;
                case 2:
                case 3:
                case 4: return Double.class;
                default: return String.class;
            }
        }
    };

    private final JTable carrinhoTable;
    private final JLabel totalCarrinhoLbl = new JLabel("Total: R$ 0,00");

    public VendaNovaDialog(JFrame owner, PainelVendas painelPai) {
        super(owner, "Nova Venda", true);
        this.painelPai = painelPai;

        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        // --- Topo: Cliente + "Adicionar Cartas" ---
        clienteCombo = new JComboBox<>(
            clienteDAO.listarTodosNomes().toArray(new String[0])
        );
        clienteCombo.setEditable(true);
        JButton btnAddCartas = new JButton("Adicionar Cartas");
        btnAddCartas.addActionListener(e -> {
            SelectCartaDialog dlg = new SelectCartaDialog(owner);
            dlg.setVisible(true);
            List<Carta> sel = dlg.getSelecionadas();
            for (Carta c : sel) {
                controller.adicionarItem(new VendaItemModel(c.getId(), 0, c.getPreco(), 0));
                carrinhoModel.addRow(new Object[]{ c.getNome(), 0, c.getPreco(), 0, 0, "", "" });
            }
            atualizarTotalCarrinho();
        });
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        topo.add(new JLabel("Cliente:"));
        topo.add(clienteCombo);
        topo.add(btnAddCartas);
        add(topo, BorderLayout.NORTH);

        // --- Tabela de carrinho ---
        carrinhoTable = new JTable(carrinhoModel);
        add(new JScrollPane(carrinhoTable), BorderLayout.CENTER);

        // --- Cell Editors para Qtd / Preço / Desconto ---
        TableColumnModel tcm = carrinhoTable.getColumnModel();

        // --- Quantidade (Integer) ---
        NumberFormatter intFmt = new NumberFormatter(new DecimalFormat("#0"));
        intFmt.setValueClass(Integer.class);
        intFmt.setAllowsInvalid(true); // mais permissivo pra edição direta
        JFormattedTextField intField = new JFormattedTextField(intFmt);
        intField.setHorizontalAlignment(JTextField.RIGHT);
        intField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        intField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(intField::selectAll);
            }
        });
        tcm.getColumn(1).setCellEditor(new DefaultCellEditor(intField) {
            @Override public Object getCellEditorValue() {
                return intField.getValue();
            }
        });

        // --- Preço unitário ---
        NumberFormat moneyFmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);

        NumberFormatter priceFmt = new NumberFormatter(moneyFmt);
        priceFmt.setValueClass(Double.class);
        priceFmt.setAllowsInvalid(true);
        JFormattedTextField priceField = new JFormattedTextField(priceFmt);
        priceField.setHorizontalAlignment(JTextField.RIGHT);
        priceField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        priceField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(priceField::selectAll);
            }
        });
        tcm.getColumn(2).setCellEditor(new DefaultCellEditor(priceField) {
            @Override public Object getCellEditorValue() {
                return priceField.getValue();
            }
        });

        // --- Desconto (%) ---
        NumberFormatter discFmt = new NumberFormatter(moneyFmt);
        discFmt.setValueClass(Double.class);
        discFmt.setAllowsInvalid(true);
        JFormattedTextField discField = new JFormattedTextField(discFmt);
        discField.setHorizontalAlignment(JTextField.RIGHT);
        discField.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        discField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(discField::selectAll);
            }
        });
        tcm.getColumn(3).setCellEditor(new DefaultCellEditor(discField) {
            @Override public Object getCellEditorValue() {
                return discField.getValue();
            }
        });


        // --- Coluna Salvar ---
        tcm.getColumn(5).setCellRenderer(new SalvarCellRenderer());
        tcm.getColumn(5).setCellEditor(new SalvarCellEditor());

        // --- Coluna Excluir ---
        tcm.getColumn(6).setCellRenderer(new ExcluirCellRenderer());
        tcm.getColumn(6).setCellEditor(new ExcluirCellEditor());

        // --- Rodapé: Total + Finalizar ---
        JPanel rodape = new JPanel(new BorderLayout());
        totalCarrinhoLbl.setFont(totalCarrinhoLbl.getFont().deriveFont(Font.BOLD,14f));
        rodape.add(totalCarrinhoLbl, BorderLayout.WEST);
        JButton btnFinalizar = new JButton("Finalizar");
        btnFinalizar.addActionListener(e -> {
            if (controller.getCarrinho().isEmpty()) {
                AlertUtils.error("Carrinho vazio!");
                return;
            }
            String nome = ((String)clienteCombo.getEditor().getItem()).trim();
            String id = clienteDAO.obterIdPorNome(nome);
            if (id == null) {
                AlertUtils.error("Cliente inválido.");
                return;
            }
            new VendaFinalizarDialog(this, controller, id, painelPai).setVisible(true);
        });
        rodape.add(btnFinalizar, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    // Recalcula total
    private void atualizarTotalCarrinho() {
        double soma = 0;
        for (int r = 0; r < carrinhoModel.getRowCount(); r++) {
            Object qObj = carrinhoModel.getValueAt(r,1);
            Object uObj = carrinhoModel.getValueAt(r,2);
            Object dObj = carrinhoModel.getValueAt(r,3);
            String qs = qObj==null?"0":qObj.toString().replaceAll("[^0-9]","");
            String us = uObj==null?"0":uObj.toString().replace(",",".").replaceAll("[^0-9.]","");
            String ds = dObj==null?"0":dObj.toString().replace(",",".").replaceAll("[^0-9.]","");
            int    qtd   = qs.isEmpty()?0:Integer.parseInt(qs);
            double unit  = us.isEmpty()?0:Double.parseDouble(us);
            double desc  = ds.isEmpty()?0:Double.parseDouble(ds);
            double tot = qtd * unit * (1 - desc/100);
            carrinhoModel.setValueAt(tot, r, 4);
            soma += tot;
        }
        totalCarrinhoLbl.setText(String.format("Total: R$ %.2f", soma));
    }

    // Renderer e Editor do Salvar
    private class SalvarCellRenderer extends JButton implements TableCellRenderer {
        SalvarCellRenderer() {
            setText("Salvar");
            setFocusable(false);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                                                                  boolean isSelected, boolean hasFocus,
                                                                  int row, int column) {
            return this;
        }
    }
    private class SalvarCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton btn = new JButton("Salvar");
        private int row;
        SalvarCellEditor() {
            btn.setFocusable(false);
            btn.addActionListener(e -> {
                try {
                    Object qO = carrinhoModel.getValueAt(row,1);
                    Object uO = carrinhoModel.getValueAt(row,2);
                    Object dO = carrinhoModel.getValueAt(row,3);
                    String qs = qO==null?"0":qO.toString().replaceAll("[^0-9]","");
                    String us = uO==null?"0":uO.toString().replace(",",".").replaceAll("[^0-9.]","");
                    String ds = dO==null?"0":dO.toString().replace(",",".").replaceAll("[^0-9.]","");
                    int    qtd = qs.isEmpty()?0:Integer.parseInt(qs);
                    double unit= us.isEmpty()?0:Double.parseDouble(us);
                    double desc= ds.isEmpty()?0:Double.parseDouble(ds);
                    double tot = qtd*unit*(1-desc/100);
                    carrinhoModel.setValueAt(tot, row, 4);
                    VendaItemModel item = controller.getCarrinho().get(row);
                    item.setQtd(qtd);
                    item.setPreco(unit);
                    item.setDesconto(desc);
                    atualizarTotalCarrinho();
                } catch(Exception ex){
                    AlertUtils.error("Erro ao salvar: " + ex.getMessage());
                }
                fireEditingStopped();
            });
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value,
                                                               boolean isSelected, int row, int column) {
            this.row = row;
            return btn;
        }
        @Override public Object getCellEditorValue() { return ""; }
    }

    // Renderer e Editor do Excluir
    private class ExcluirCellRenderer extends JButton implements TableCellRenderer {
        ExcluirCellRenderer() {
            setText("Excluir");
            setFocusable(false);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                                                                  boolean isSelected, boolean hasFocus,
                                                                  int row, int column) {
            return this;
        }
    }
    private class ExcluirCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton btn = new JButton("Excluir");
        private int row;
        ExcluirCellEditor() {
            btn.setFocusable(false);
            btn.addActionListener(e -> {
                controller.getCarrinho().remove(row);
                carrinhoModel.removeRow(row);
                atualizarTotalCarrinho();
                fireEditingStopped();
            });
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value,
                                                               boolean isSelected, int row, int column) {
            this.row = row;
            return btn;
        }
        @Override public Object getCellEditorValue() { return ""; }
    }
}
