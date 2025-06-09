package ui.venda.dialog;

import dao.ProdutoDAO;
import model.VendaItemModel;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.Locale;

/**
 * Dialog para permitir ao lojista editar o preço unitário de cada item
 * antes de finalizar a venda. Recebe a lista de VendaItemModel e altera
 * diretamente o campo preco de cada item.
 */
public class EditarPrecoDialog extends JDialog {
    private final List<VendaItemModel> itens;
    private final DefaultTableModel tableModel;
    private boolean ok = false;

    public EditarPrecoDialog(Window owner, List<VendaItemModel> itens) {
        super(owner, "Editar Preço Base", ModalityType.APPLICATION_MODAL);
        this.itens = itens;

        setLayout(new BorderLayout(10,10));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        // Criar tabela
        String[] cols = {"Produto","Quantidade","Preço Unitário (R$)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 2; // só a coluna de preço é editável
            }
            @Override public Class<?> getColumnClass(int col) {
                return col == 1 ? Integer.class : col == 2 ? Double.class : String.class;
            }
        };
        JTable table = new JTable(tableModel);

        // Formatar editor de preço como moeda
        NumberFormat moneyFmt = NumberFormat.getNumberInstance(new Locale("pt","BR"));
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);
        NumberFormatter fmt = new NumberFormatter(moneyFmt);
        fmt.setValueClass(Double.class);
        fmt.setMinimum(0.0);
        fmt.setAllowsInvalid(false);
        JFormattedTextField ft = new JFormattedTextField(fmt);
        ft.setBorder(null);
        table.getColumnModel().getColumn(2)
             .setCellEditor(new DefaultCellEditor(ft));
        table.getColumnModel().getColumn(2)
             .setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, focus, r, c);
                if (v instanceof Number) {
                    setText(moneyFmt.format(((Number)v).doubleValue()));
                }
                return this;
            }
        });

        // Preencher modelo com os itens atuais
        ProdutoDAO pdao = new ProdutoDAO();
        for (VendaItemModel item : itens) {
            String nome;
            try {
                nome = pdao.findById(item.getProdutoId()).getNome();
            } catch (Exception ex) {
                nome = item.getProdutoId();
            }
            tableModel.addRow(new Object[]{
                nome,
                item.getQtd(),
                item.getPreco()
            });
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(500, 200));
        add(scroll, BorderLayout.CENTER);

        // Botões Salvar / Cancelar
        JButton btnSalvar = new JButton("Salvar");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBtns.add(btnCancelar);
        pnlBtns.add(btnSalvar);
        add(pnlBtns, BorderLayout.SOUTH);

        btnSalvar.addActionListener(e -> {
            try {
                // Salvar valores de volta nos modelos
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    double novoPreco = ((Number) tableModel.getValueAt(i, 2)).doubleValue();
                    itens.get(i).setPreco(novoPreco);
                }
                ok = true;
                dispose();
            } catch (Exception ex) {
                AlertUtils.error("Erro ao salvar preços:\n" + ex.getMessage());
            }
        });
        btnCancelar.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(owner);
    }

    /** Retorna true se o usuário clicou Salvar */
    public boolean isOk() {
        return ok;
    }
}
