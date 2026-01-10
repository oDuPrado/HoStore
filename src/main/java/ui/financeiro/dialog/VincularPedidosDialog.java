package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import dao.PedidoCompraDAO;
import model.PedidoCompraModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * Diálogo para vincular um ou mais pedidos de compra a uma conta a pagar.
 */
public class VincularPedidosDialog extends JDialog {

    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();

    private final Set<String> pedidosSelecionados = new HashSet<>();
    private final PedidosTableModel tableModel = new PedidosTableModel();
    private final JTable tblPedidos = new JTable(tableModel);

    private final JComboBox<String> cbStatus = new JComboBox<>(
            new String[] { "todos", "rascunho", "enviado", "recebido" });

    // padrão: hoje -> 1 ano pra frente
    private final JDateChooser dcDataIni = new JDateChooser(dateFrom(LocalDate.now()));
    private final JDateChooser dcDataFim = new JDateChooser(dateFrom(LocalDate.now().plusDays(365)));

    private final SimpleDateFormat dfISO = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dfBR = new SimpleDateFormat("dd/MM/yyyy");

    private boolean confirmado = false;

    public VincularPedidosDialog(Frame owner) {
        super(owner, "Vincular Pedidos", true);
        UiKit.applyDialogBase(this);
        setContentPane(buildUI());
        pack();
        setMinimumSize(new Dimension(860, 520));
        setLocationRelativeTo(owner);

        // ESC fecha
        getRootPane().registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        carregarPedidos();
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Vincular Pedidos de Compra"));
        header.add(UiKit.hint("Marque um ou mais pedidos e clique em Salvar. Duplo clique alterna a seleção."));
        root.add(header, BorderLayout.NORTH);

        // Card filtros
        JPanel filtrosCard = UiKit.card();
        filtrosCard.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        cbStatus.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        prepararDateChooser(dcDataIni);
        prepararDateChooser(dcDataFim);

        JButton btnFiltrar = UiKit.primary("Filtrar");
        JButton btnHoje = UiKit.ghost("Hoje → +1 ano");

        btnFiltrar.addActionListener(e -> carregarPedidos());
        btnHoje.addActionListener(e -> {
            dcDataIni.setDate(dateFrom(LocalDate.now()));
            dcDataFim.setDate(dateFrom(LocalDate.now().plusDays(365)));
            carregarPedidos();
        });

        // linha 0
        addField(filtrosCard, gc, 0, "Status", cbStatus);
        addField(filtrosCard, gc, 1, "Data Início", dcDataIni);
        addField(filtrosCard, gc, 2, "Data Fim", dcDataFim);

        // botões filtros (coluna 3)
        GridBagConstraints gb = (GridBagConstraints) gc.clone();
        gb.gridx = 3;
        gb.gridy = 0;
        gb.gridheight = 2;
        gb.weightx = 0;
        gb.fill = GridBagConstraints.NONE;

        JPanel box = new JPanel(new GridLayout(2, 1, 6, 6));
        box.setOpaque(false);
        box.add(btnFiltrar);
        box.add(btnHoje);

        filtrosCard.add(box, gb);

        root.add(filtrosCard, BorderLayout.BEFORE_FIRST_LINE);

        // Card tabela
        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(10, 10));

        setupTable();
        tableCard.add(UiKit.scroll(tblPedidos), BorderLayout.CENTER);

        // ações rápidas de seleção
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.setOpaque(false);

        JButton btnAll = UiKit.ghost("Selecionar tudo");
        JButton btnNone = UiKit.ghost("Limpar seleção");

        btnAll.addActionListener(e -> tableModel.setAllSelected(true));
        btnNone.addActionListener(e -> tableModel.setAllSelected(false));

        quick.add(btnAll);
        quick.add(btnNone);
        tableCard.add(quick, BorderLayout.SOUTH);

        root.add(tableCard, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        JButton btnSalvar = UiKit.primary("Salvar");

        btnSalvar.addActionListener(e -> onSave());
        btnCancelar.addActionListener(e -> onCancel());

        footer.add(btnCancelar);
        footer.add(btnSalvar);

        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private void setupTable() {
        UiKit.tableDefaults(tblPedidos);
        tblPedidos.setAutoCreateRowSorter(true);
        tblPedidos.setFillsViewportHeight(true);

        // largura checkbox
        TableColumn colCheck = tblPedidos.getColumnModel().getColumn(0);
        colCheck.setMaxWidth(46);
        colCheck.setPreferredWidth(46);

        // zebra base em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int c = 0; c < tblPedidos.getColumnCount(); c++) {
            tblPedidos.getColumnModel().getColumn(c).setCellRenderer(zebra);
        }

        // alinhamento data central
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tblPedidos.getColumnModel().getColumn(2).setCellRenderer(new DelegatingRenderer(zebra, center));

        // status com badge
        tblPedidos.getColumnModel().getColumn(3).setCellRenderer(UiKit.badgeStatusRenderer());

        // duplo clique: alterna checkbox
        tblPedidos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = tblPedidos.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = tblPedidos.convertRowIndexToModel(viewRow);
                        tableModel.toggleSelected(modelRow);
                    }
                }
            }
        });
    }

    private void carregarPedidos() {
        String status = (String) cbStatus.getSelectedItem();
        Date dataIni = dcDataIni.getDate();
        Date dataFim = dcDataFim.getDate();

        try {
            List<PedidoCompraModel> pedidos = pedidoDAO.listarPorDataEStatus(
                    dataIni,
                    dataFim,
                    "todos".equalsIgnoreCase(status) ? null : status);

            tableModel.setPedidos(pedidos);

            // ordena por data desc (mais recente primeiro)
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) tblPedidos.getRowSorter();
            sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao carregar pedidos:\n" + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        confirmado = true;
        pedidosSelecionados.clear();

        // pega do MODEL (não do view), então não sofre com sorting
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0))) {
                PedidoCompraModel p = tableModel.getPedidos().get(i);
                if (p != null && p.getId() != null) {
                    pedidosSelecionados.add(p.getId());
                }
            }
        }

        dispose();
    }

    private void onCancel() {
        confirmado = false;
        dispose();
    }

    /**
     * Abre o diálogo e retorna o conjunto de IDs de pedidos selecionados.
     */
    public Set<String> showDialog() {
        setVisible(true);
        return confirmado ? pedidosSelecionados : Collections.emptySet();
    }

    /* ───────────────────────── helpers UI ───────────────────────── */

    private void prepararDateChooser(JDateChooser dc) {
        dc.setDateFormatString("dd/MM/yyyy");
        dc.setPreferredSize(new Dimension(140, 30));

        if (dc.getDateEditor() != null && dc.getDateEditor().getUiComponent() instanceof JComponent editor) {
            editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        }

        JButton calBtn = dc.getCalendarButton();
        if (calBtn != null) {
            calBtn.setText("📅");
            calBtn.setFocusPainted(false);
            calBtn.setMargin(new Insets(2, 8, 2, 8));
            calBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0; font: +1;");
            calBtn.setToolTipText("Selecionar data");
        }
    }

    private void addField(JPanel parent, GridBagConstraints base, int col, String label, Component field) {
        int row = (col >= 2) ? 1 : 0;
        int x = (col % 2) * 2;

        GridBagConstraints gl = (GridBagConstraints) base.clone();
        gl.gridx = x;
        gl.gridy = row;
        gl.weightx = 0;
        gl.fill = GridBagConstraints.NONE;
        parent.add(new JLabel(label + ":"), gl);

        GridBagConstraints gf = (GridBagConstraints) base.clone();
        gf.gridx = x + 1;
        gf.gridy = row;
        gf.weightx = 1;
        parent.add(field, gf);
    }

    private static Date dateFrom(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /* ───────────────────────── TableModel ───────────────────────── */

    private class PedidosTableModel extends AbstractTableModel {
        private final String[] colNames = { "", "Nome", "Data", "Status" };
        private List<PedidoCompraModel> pedidos = new ArrayList<>();
        private final List<Boolean> selected = new ArrayList<>();

        public void setPedidos(List<PedidoCompraModel> lista) {
            this.pedidos = (lista == null) ? new ArrayList<>() : lista;

            selected.clear();
            for (int i = 0; i < this.pedidos.size(); i++)
                selected.add(false);

            fireTableDataChanged();
        }

        public List<PedidoCompraModel> getPedidos() {
            return pedidos;
        }

        public void toggleSelected(int row) {
            if (row < 0 || row >= selected.size())
                return;
            selected.set(row, !selected.get(row));
            fireTableCellUpdated(row, 0);
        }

        public void setAllSelected(boolean value) {
            for (int i = 0; i < selected.size(); i++)
                selected.set(i, value);
            fireTableRowsUpdated(0, Math.max(0, selected.size() - 1));
        }

        @Override
        public int getRowCount() {
            return pedidos.size();
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return colNames[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return col == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public Object getValueAt(int row, int col) {
            PedidoCompraModel p = pedidos.get(row);
            switch (col) {
                case 0:
                    return selected.get(row);
                case 1:
                    return p.getNome();
                case 2:
                    try {
                        Date d = dfISO.parse(p.getData());
                        return dfBR.format(d);
                    } catch (Exception ex) {
                        return p.getData();
                    }
                case 3:
                    return p.getStatus();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                selected.set(row, Boolean.TRUE.equals(value));
                fireTableCellUpdated(row, col);
            }
        }
    }

    /* ───────────────────────── Renderer helper ───────────────────────── */

    static class DelegatingRenderer extends DefaultTableCellRenderer {
        private final DefaultTableCellRenderer base;
        private final DefaultTableCellRenderer aligner;

        DelegatingRenderer(DefaultTableCellRenderer base, DefaultTableCellRenderer aligner) {
            this.base = base;
            this.aligner = aligner;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel l) {
                l.setHorizontalAlignment(aligner.getHorizontalAlignment());
            }
            return c;
        }
    }
}
