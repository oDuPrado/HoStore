// src/ui/venda/dialog/VendaDevolucaoDialog.java
package ui.venda.dialog;

import dao.ProdutoDAO;
import java.util.Objects;
import model.ProdutoModel;
import model.VendaDevolucaoModel;
import model.VendaItemModel;
import service.VendaDevolucaoService;
import util.AlertUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialog para registrar devoluções de venda:
 * - Nome do produto (não ID)
 * - Valor unitário e total devolvido por item
 * - Total geral da devolução
 * - Visual padrão UiKit (cards, zebra, botões)
 */
public class VendaDevolucaoDialog extends JDialog {

    private final int vendaId;
    private final List<VendaItemModel> itens;

    private final JTable tabela;
    private final DefaultTableModel model;

    private final Map<String, String> nomesProdutos = new HashMap<>();

    private final NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final JLabel lblTotalGeral = new JLabel();

    public VendaDevolucaoDialog(Window owner, int vendaId, List<VendaItemModel> itens) {
        super(owner, "Registrar Devolução", ModalityType.APPLICATION_MODAL);
        this.vendaId = vendaId;
        this.itens = itens;

        UiKit.applyDialogBase(this);

        setMinimumSize(new Dimension(900, 520));
        setLayout(new BorderLayout(10, 10));

        preloadNomeProdutos();

        /* ===================== TOP (CARD) ===================== */
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));

        JPanel title = new JPanel(new GridLayout(0, 1, 0, 2));
        title.setOpaque(false);
        title.add(UiKit.title("Registrar Devolução"));
        title.add(UiKit.hint("Digite a quantidade a devolver (0 até a vendida). O total é calculado automaticamente."));
        top.add(title, BorderLayout.WEST);

        JLabel vendaLbl = UiKit.hint("Venda #" + vendaId);
        top.add(vendaLbl, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        /* ===================== MODEL + TABLE ===================== */
        model = new DefaultTableModel(new String[] {
                "Produto", "Qtd Vendida", "V.Unit.", "Qtd Devolver", "Total Dev.", "Motivo"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // apenas Qtd Devolver e Motivo
                return column == 3 || column == 5;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1, 3 -> Integer.class;
                    case 2, 4 -> Double.class;
                    default -> String.class;
                };
            }
        };

        // popula linhas com tipos corretos (double/int), sem String formatada
        for (VendaItemModel it : itens) {
            String produtoId = it.getProdutoId();
            String nome = nomesProdutos.getOrDefault(produtoId, produtoId);

            int qtdVendida = Math.max(0, it.getQtd());
            double valorUnit = saneDouble(it.getPreco());

            model.addRow(new Object[] {
                    nome,
                    qtdVendida,
                    valorUnit,
                    0,
                    0.0,
                    ""
            });
        }

        tabela = new JTable(model);
        UiKit.tableDefaults(tabela);

        // zebra base
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // moeda zebra nas colunas 2 e 4
        tabela.getColumnModel().getColumn(2).setCellRenderer(currencyZebra(zebra));
        tabela.getColumnModel().getColumn(4).setCellRenderer(currencyZebra(zebra));

        // colunas: larguras amigáveis
        TableColumnModel cols = tabela.getColumnModel();
        cols.getColumn(0).setPreferredWidth(280); // Produto
        cols.getColumn(1).setPreferredWidth(90); // Vendida
        cols.getColumn(2).setPreferredWidth(110); // Unit
        cols.getColumn(3).setPreferredWidth(110); // Devolver
        cols.getColumn(4).setPreferredWidth(120); // Total
        cols.getColumn(5).setPreferredWidth(260); // Motivo

        // editor numérico para "Qtd Devolver"
        cols.getColumn(3).setCellEditor(qtdEditor());

        // listener: recalcula total por linha e total geral
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() != TableModelEvent.UPDATE)
                    return;

                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row < 0)
                    return;

                if (col == 3) {
                    ajustarQtdRow(row);
                    recalcularRow(row);
                    atualizarTotalGeral();
                } else if (col == 5) {
                    // motivo mudou: não precisa recalcular nada
                }
            }
        });

        // card da tabela
        JPanel center = UiKit.card();
        center.setLayout(new BorderLayout(8, 8));
        center.add(UiKit.title("Itens da Devolução"), BorderLayout.NORTH);
        center.add(UiKit.scroll(tabela), BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        /* ===================== BOTTOM (CARD) ===================== */
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));

        lblTotalGeral.setFont(lblTotalGeral.getFont().deriveFont(Font.BOLD, 14f));
        bottom.add(lblTotalGeral, BorderLayout.WEST);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnSalvar = UiKit.primary("Confirmar Devolução");
        btnSalvar.addActionListener(this::salvar);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnCancelar);
        actions.add(btnSalvar);

        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // inicializa totais
        for (int r = 0; r < model.getRowCount(); r++) {
            ajustarQtdRow(r);
            recalcularRow(r);
        }
        atualizarTotalGeral();

        pack();
        setLocationRelativeTo(owner);
    }

    private void preloadNomeProdutos() {
        ProdutoDAO produtoDAO = new ProdutoDAO();
        for (VendaItemModel it : itens) {
            String produtoId = it.getProdutoId();
            if (produtoId == null || nomesProdutos.containsKey(produtoId))
                continue;

            try {
                ProdutoModel pm = produtoDAO.findById(produtoId);
                nomesProdutos.put(produtoId, (pm != null && pm.getNome() != null) ? pm.getNome() : produtoId);
            } catch (Exception ex) {
                nomesProdutos.put(produtoId, produtoId);
            }
        }
    }

    private DefaultCellEditor qtdEditor() {
        NumberFormatter intFmt = new NumberFormatter(NumberFormat.getIntegerInstance(new Locale("pt", "BR")));
        intFmt.setValueClass(Integer.class);
        intFmt.setMinimum(0);
        intFmt.setAllowsInvalid(false);

        JFormattedTextField ft = new JFormattedTextField(intFmt);
        ft.setHorizontalAlignment(SwingConstants.RIGHT);
        ft.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        ft.setBorder(null);

        return new DefaultCellEditor(ft) {
            @Override
            public Object getCellEditorValue() {
                Object v = ft.getValue();
                if (v instanceof Number n)
                    return Math.max(0, n.intValue());
                return 0;
            }
        };
    }

    private void ajustarQtdRow(int row) {
        int vendida = safeInt(model.getValueAt(row, 1));
        int devolver = safeInt(model.getValueAt(row, 3));

        if (devolver < 0)
            devolver = 0;
        if (devolver > vendida)
            devolver = vendida;

        // se o usuário digitou lixo, normaliza
        if (!Objects.equals(model.getValueAt(row, 3), devolver)) {
            model.setValueAt(devolver, row, 3);
        }
    }

    private void recalcularRow(int row) {
        double unit = saneDouble(model.getValueAt(row, 2));
        int devolver = safeInt(model.getValueAt(row, 3));
        double total = devolver * unit;

        // evita loop infinito: só seta se mudou
        Object atual = model.getValueAt(row, 4);
        double atualD = (atual instanceof Number n) ? n.doubleValue() : -1;
        if (Double.compare(atualD, total) != 0) {
            model.setValueAt(total, row, 4);
        }
    }

    private void atualizarTotalGeral() {
        double soma = 0.0;
        for (int r = 0; r < model.getRowCount(); r++) {
            Object v = model.getValueAt(r, 4);
            if (v instanceof Number n)
                soma += n.doubleValue();
        }
        lblTotalGeral.setText("Total da Devolução: " + cf.format(soma));
    }

    private void salvar(ActionEvent evt) {
        // garante commit da célula em edição
        if (tabela.isEditing()) {
            try {
                tabela.getCellEditor().stopCellEditing();
            } catch (Exception ignored) {
            }
        }

        try {
            VendaDevolucaoService service = new VendaDevolucaoService();

            boolean fezAlgo = false;

            for (int i = 0; i < model.getRowCount(); i++) {
                int qtdDevolver = safeInt(model.getValueAt(i, 3));
                if (qtdDevolver <= 0)
                    continue;

                String motivo = (String) model.getValueAt(i, 5);
                if (motivo == null)
                    motivo = "";

                VendaItemModel itemOriginal = itens.get(i);
                String produtoId = itemOriginal.getProdutoId();
                int qtdVendida = Math.max(0, itemOriginal.getQtd());
                double valorUnit = saneDouble(itemOriginal.getPreco());

                if (qtdDevolver > qtdVendida) {
                    AlertUtils.error("A quantidade devolvida não pode ser maior que a vendida (linha " + (i + 1) + ")");
                    return;
                }

                VendaDevolucaoModel dev = new VendaDevolucaoModel();
                dev.setVendaId(vendaId);
                dev.setProdutoId(produtoId);
                dev.setQuantidade(qtdDevolver);
                dev.setMotivo(motivo.trim());
                dev.setData(LocalDate.now());
                dev.setValor(valorUnit);

                service.registrarDevolucao(dev);
                fezAlgo = true;
            }

            if (!fezAlgo) {
                AlertUtils.info("Nada para devolver (todas as quantidades estão 0).");
                return;
            }

            AlertUtils.info("Devoluções registradas com sucesso.");
            dispose();

        } catch (Exception e) {
            AlertUtils.error("Erro ao registrar devoluções:\n" + e.getMessage());
        }
    }

    private int safeInt(Object o) {
        if (o instanceof Number n)
            return Math.max(0, n.intValue());
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(o)));
        } catch (Exception e) {
            return 0;
        }
    }

    private double saneDouble(Object o) {
        if (o instanceof Number n) {
            double v = n.doubleValue();
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0)
                return 0.0;
            return v;
        }
        return 0.0;
    }

    private TableCellRenderer currencyZebra(DefaultTableCellRenderer zebraBase) {
        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            double v = (value instanceof Number n) ? n.doubleValue() : 0.0;
            l.setText(cf.format(v));
            return l;
        };
    }
}
