package ui.venda.dialog;

import dao.ProdutoDAO;
import model.VendaItemModel;
import util.AlertUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import util.FormatterFactory;

/**
 * Dialog para permitir ao lojista editar o preço unitário de cada item
 * antes de finalizar a venda. Recebe a lista de VendaItemModel e altera
 * diretamente o campo preco de cada item.
 */
public class EditarPrecoDialog extends JDialog {

    private final List<VendaItemModel> itens;
    private final DefaultTableModel tableModel;
    private boolean ok = false;

    // mantém referência pra conseguir "commit" da edição antes de salvar
    private final JTable table;

    public EditarPrecoDialog(Window owner, List<VendaItemModel> itens) {
        super(owner, "Editar Preço Base", ModalityType.APPLICATION_MODAL);
        this.itens = itens;

        UiKit.applyDialogBase(this);

        setLayout(new BorderLayout(10, 10));

        /* ===================== CARD PRINCIPAL ===================== */
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));
        add(card, BorderLayout.CENTER);

        // Header
        JPanel header = new JPanel(new BorderLayout(8, 6));
        header.setOpaque(false);

        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Editar preços"));
        left.add(UiKit.hint("Edite apenas o preço unitário. Quantidade e produto são fixos."));
        header.add(left, BorderLayout.WEST);

        card.add(header, BorderLayout.NORTH);

        /* ===================== TABELA ===================== */
        String[] cols = { "Produto", "Quantidade", "Preço Unitário (R$)" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2; // só preço
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return (col == 1) ? Integer.class : (col == 2) ? Double.class : String.class;
            }
        };

        table = new JTable(tableModel);
        UiKit.tableDefaults(table);

        // Zebra base em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Alinhamentos
        DefaultTableCellRenderer centerZebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r,
                    int c) {
                JLabel l = (JLabel) zebra.getTableCellRendererComponent(t, v, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        };

        // Quantidade centralizada
        table.getColumnModel().getColumn(1).setCellRenderer(centerZebra);

        // Editor e renderer de preço (mantém zebra)
        NumberFormat moneyFmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        JFormattedTextField ft = FormatterFactory.getMoneyField(0.0);
        ft.setBorder(new EmptyBorder(0, 6, 0, 6));

        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(ft));
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r,
                    int c) {
                JLabel l = (JLabel) zebra.getTableCellRendererComponent(t, v, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                double val = (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
                l.setText(moneyFmt.format(val));
                return l;
            }
        });

        // Ajustes de largura
        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);

        // Preenche modelo com itens atuais
        ProdutoDAO pdao = new ProdutoDAO();
        for (VendaItemModel item : itens) {
            String nome;
            try {
                var pm = pdao.findById(item.getProdutoId());
                nome = (pm != null && pm.getNome() != null) ? pm.getNome() : item.getProdutoId();
            } catch (Exception ex) {
                nome = item.getProdutoId();
            }

            tableModel.addRow(new Object[] { nome, item.getQtd(), item.getPreco() });
        }

        card.add(UiKit.scroll(table), BorderLayout.CENTER);

        /* ===================== FOOTER (BOTÕES) ===================== */
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel hint2 = UiKit.hint("Dica: TAB muda de célula, ENTER confirma edição.");
        footer.add(hint2, BorderLayout.WEST);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBtns.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        JButton btnSalvar = UiKit.primary("Salvar");

        pnlBtns.add(btnCancelar);
        pnlBtns.add(btnSalvar);

        footer.add(pnlBtns, BorderLayout.EAST);

        card.add(footer, BorderLayout.SOUTH);

        /* ===================== AÇÕES ===================== */
        btnSalvar.addActionListener(e -> salvar());
        btnCancelar.addActionListener(e -> dispose());

        // Atalhos
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");
        am.put("cancelar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                btnCancelar.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("ENTER"), "salvar");
        am.put("salvar", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // ENTER também é usado na edição da tabela, então só salva se não estiver
                // editando
                if (!table.isEditing())
                    btnSalvar.doClick();
            }
        });

        setMinimumSize(new Dimension(720, 360));
        pack();
        setLocationRelativeTo(owner);
    }

    private void salvar() {
        try {
            // garante commit do editor atual
            if (table.isEditing()) {
                try {
                    TableCellEditor ed = table.getCellEditor();
                    if (ed != null)
                        ed.stopCellEditing();
                } catch (Exception ignored) {
                }
            }

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object v = tableModel.getValueAt(i, 2);
                double novoPreco = (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
                if (Double.isNaN(novoPreco) || Double.isInfinite(novoPreco) || novoPreco < 0) {
                    novoPreco = 0.0;
                }
                itens.get(i).setPreco(novoPreco);
            }

            ok = true;
            dispose();

        } catch (Exception ex) {
            AlertUtils.error("Erro ao salvar preços:\n" + ex.getMessage());
        }
    }

    /** Retorna true se o usuário clicou Salvar */
    public boolean isOk() {
        return ok;
    }
}
