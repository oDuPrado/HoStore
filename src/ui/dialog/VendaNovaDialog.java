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
import java.text.*;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo de criação da venda – agora com edição inline
 * e botão “Salvar” por linha para não travar a UI.
 */
public class VendaNovaDialog extends JDialog {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final CartaDAO cartaDAO = new CartaDAO();
    private final VendaController controller = new VendaController();
    private final PainelVendas painelPai;

    private final JComboBox<String> clienteCombo;
    private final DefaultTableModel carrinhoModel;
    private final JTable carrinhoTable;
    private final JLabel totalCarrinhoLbl = new JLabel("Total: R$ 0,00");

    public VendaNovaDialog(JFrame owner, PainelVendas painelPai) {
        super(owner, "Nova Venda", true);
        this.painelPai = painelPai;

        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane())
                .setBorder(new EmptyBorder(10, 10, 10, 10));

        // —— Topo: Cliente + Botão “Adicionar Cartas” ——
        clienteCombo = new JComboBox<>(
                clienteDAO.listarTodosNomes().toArray(new String[0]));
        clienteCombo.setEditable(true);

        // 🛠️ Inicializa o carrinhoModel AQUI antes do botão usar
        carrinhoModel = new DefaultTableModel(
                new String[] {
                        "Carta", "Qtd", "R$ Unit.", "Desc (%)", "R$ Total", "Ações"
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return (col >= 1 && col <= 3) || col == 5;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 1:
                        return Integer.class;
                    case 2:
                    case 3:
                    case 4:
                        return Double.class;
                    default:
                        return String.class;
                }
            }
        };

        JButton btnAddCartas = new JButton("Adicionar Cartas");
        btnAddCartas.addActionListener(e -> {
            SelectCartaDialog dlg = new SelectCartaDialog(owner);
            dlg.setVisible(true);
            List<Carta> sel = dlg.getSelecionadas();
            for (Carta c : sel) {
                controller.adicionarItem(
                        new VendaItemModel(c.getId(), 0, c.getPreco(), 0));
                carrinhoModel.addRow(new Object[] {
                        c.getNome(), 0, c.getPreco(), 0, 0, "Salvar"
                });
            }
            atualizarTotalCarrinho();
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topo.add(new JLabel("Cliente:"));
        topo.add(clienteCombo);
        topo.add(btnAddCartas);
        add(topo, BorderLayout.NORTH);

        // —— Tabela de Carrinho com 6 colunas ——
        carrinhoTable = new JTable(carrinhoModel);
        JScrollPane scroll = new JScrollPane(carrinhoTable);
        add(scroll, BorderLayout.CENTER);

        // — Cell Editors —
        TableColumnModel tcm = carrinhoTable.getColumnModel();

        // Quantidade (inteiro)
        NumberFormatter intFmt = new NumberFormatter(
                new DecimalFormat("#0"));
        intFmt.setValueClass(Integer.class);
        intFmt.setAllowsInvalid(false);
        JFormattedTextField intField = new JFormattedTextField(intFmt);
        intField.setHorizontalAlignment(JTextField.RIGHT);
        tcm.getColumn(1).setCellEditor(new DefaultCellEditor(intField));

        // Preço unitário (moeda)
        NumberFormat br = NumberFormat.getNumberInstance(
                new Locale("pt", "BR"));
        br.setMinimumFractionDigits(2);
        br.setMaximumFractionDigits(2);
        NumberFormatter curFmt = new NumberFormatter(br);
        curFmt.setValueClass(Double.class);
        curFmt.setAllowsInvalid(false);
        JFormattedTextField priceField = new JFormattedTextField(curFmt);
        priceField.setHorizontalAlignment(JTextField.RIGHT);
        tcm.getColumn(2).setCellEditor(new DefaultCellEditor(priceField));

        // Desconto (%)
        NumberFormatter pctFmt = new NumberFormatter(
                new DecimalFormat("#0.##"));
        pctFmt.setValueClass(Double.class);
        pctFmt.setAllowsInvalid(false);
        JFormattedTextField pctField = new JFormattedTextField(pctFmt);
        pctField.setHorizontalAlignment(JTextField.RIGHT);
        tcm.getColumn(3).setCellEditor(new DefaultCellEditor(pctField));

        // Coluna “Ações”: botão Salvar
        tcm.getColumn(5).setCellRenderer(new ButtonRenderer());
        tcm.getColumn(5).setCellEditor(new ButtonEditor(carrinhoTable));

        // —— Rodapé: Total + Finalizar ——
        JPanel rodape = new JPanel(new BorderLayout());
        totalCarrinhoLbl.setFont(
                totalCarrinhoLbl.getFont().deriveFont(Font.BOLD, 14f));
        rodape.add(totalCarrinhoLbl, BorderLayout.WEST);

        JButton btnFinalizar = new JButton("Finalizar");
        btnFinalizar.addActionListener(e -> {
            if (controller.getCarrinho().isEmpty()) {
                AlertUtils.error("Carrinho vazio!");
                return;
            }
            String nome = ((String) clienteCombo.getEditor()
                    .getItem()).trim();
            String id = clienteDAO.obterIdPorNome(nome);
            if (id == null) {
                AlertUtils.error("Cliente inválido.");
                return;
            }
            new VendaFinalizarDialog(
                    this, controller, id, painelPai).setVisible(true);
        });
        rodape.add(btnFinalizar, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    /** Recalcula total geral sem travar UI */
    private void atualizarTotalCarrinho() {
        double soma = 0;
        for (int r = 0; r < carrinhoModel.getRowCount(); r++) {
            int qtd = (Integer) carrinhoModel.getValueAt(r, 1);
            double unit = (Double) carrinhoModel.getValueAt(r, 2);
            double desc = (Double) carrinhoModel.getValueAt(r, 3);
            double tot = qtd * unit * (1 - desc / 100);
            carrinhoModel.setValueAt(tot, r, 4);
            soma += tot;
        }
        totalCarrinhoLbl.setText(
                String.format("Total: R$ %.2f", soma));
    }

    /** Renderiza botão na célula */
    private static class ButtonRenderer extends JButton
            implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText("Salvar");
            return this;
        }
    }

    /** Editor de botão que salva edição ao clicar */
    private class ButtonEditor extends AbstractCellEditor
            implements TableCellEditor, ActionListener {

        private final JButton button = new JButton("Salvar");
        private final JTable table;

        public ButtonEditor(JTable table) {
            this.table = table;
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value,
                boolean isSelected, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Salvar";
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            try {
                String qtdStr = carrinhoModel.getValueAt(row, 1).toString().replaceAll("[^0-9]", "");
                String unitStr = carrinhoModel.getValueAt(row, 2).toString().replace(",", ".").replaceAll("[^0-9.]",
                        "");
                String descStr = carrinhoModel.getValueAt(row, 3).toString().replace(",", ".").replaceAll("[^0-9.]",
                        "");

                int qtd = qtdStr.isEmpty() ? 0 : Integer.parseInt(qtdStr);
                double unit = unitStr.isEmpty() ? 0.0 : Double.parseDouble(unitStr);
                double desc = descStr.isEmpty() ? 0.0 : Double.parseDouble(descStr);

                double tot = qtd * unit * (1 - desc / 100);
                carrinhoModel.setValueAt(tot, row, 4);

                VendaItemModel item = controller.getCarrinho().get(row);
                item.setQtd(qtd);
                item.setPreco(unit);
                item.setDesconto(desc);

                atualizarTotalCarrinho();

            } catch (Exception ex) {
                AlertUtils.error("Erro ao salvar linha: " + ex.getMessage());
            }
            fireEditingStopped();
        }
    }

}
