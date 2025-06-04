package ui.financeiro.dialog;

import dao.PedidoCompraDAO;
import model.PedidoCompraModel;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.toedter.calendar.JDateChooser;

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
    private final JDateChooser dcDataIni = new JDateChooser();
    private final JDateChooser dcDataFim = new JDateChooser();
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dfBR = new SimpleDateFormat("dd/MM/yyyy");

    private boolean confirmado = false;

    public VincularPedidosDialog(Frame owner) {
        super(owner, "Vincular Pedidos", true);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Painel de filtros
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlFiltros.add(new JLabel("Status:"));
        pnlFiltros.add(cbStatus);
        pnlFiltros.add(new JLabel("Data Início:"));
        dcDataIni.setDateFormatString("dd/MM/yyyy");
        dcDataIni.setPreferredSize(new Dimension(120, dcDataIni.getPreferredSize().height));
        pnlFiltros.add(dcDataIni);

        pnlFiltros.add(new JLabel("Data Fim:"));
        dcDataFim.setDateFormatString("dd/MM/yyyy");
        dcDataFim.setPreferredSize(new Dimension(120, dcDataFim.getPreferredSize().height));
        pnlFiltros.add(dcDataFim);

        JButton btnFiltrar = new JButton("Filtrar");
        pnlFiltros.add(btnFiltrar);

        // Configura tabela: checkbox + Nome + Data + Status
        tblPedidos.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tblPedidos);
        // deixa só o checkbox pequenino
        TableColumn colCheck = tblPedidos.getColumnModel().getColumn(0);
        colCheck.setMaxWidth(30);

        // Botões de ação
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Salvar");
        JButton btnCancelar = new JButton("Cancelar");
        pnlBotoes.add(btnSalvar);
        pnlBotoes.add(btnCancelar);

        // Layout principal
        getContentPane().setLayout(new BorderLayout(5, 5));
        getContentPane().add(pnlFiltros, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(pnlBotoes, BorderLayout.SOUTH);

        // Listeners
        btnFiltrar.addActionListener(e -> carregarPedidos());
        btnSalvar.addActionListener(e -> onSave());
        btnCancelar.addActionListener(e -> onCancel());
        // ESC fecha
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Carrega pela primeira vez
        carregarPedidos();
    }

    private void carregarPedidos() {
        String status = (String) cbStatus.getSelectedItem();
        Date dataIni = dcDataIni.getDate();
        Date dataFim = dcDataFim.getDate();
        try {
            List<PedidoCompraModel> pedidos = pedidoDAO
                    .listarPorDataEStatus(dataIni, dataFim,
                            "todos".equals(status) ? null : status);
            tableModel.setPedidos(pedidos);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar pedidos:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Date parseDate(String text) {
        if (text == null || text.isBlank())
            return null;
        try {
            return df.parse(text);
        } catch (ParseException e) {
            return null;
        }
    }

    private void onSave() {
        confirmado = true;
        pedidosSelecionados.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                pedidosSelecionados.add(tableModel.getPedidos().get(i).getId());
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

    // ─────── TableModel ───────
    private class PedidosTableModel extends AbstractTableModel {
        // Colunas: [checkbox, Nome, Data, Status]
        private final String[] colNames = { "", "Nome", "Data", "Status" };
        private List<PedidoCompraModel> pedidos = new ArrayList<>();
        private final List<Boolean> selected = new ArrayList<>();

        public void setPedidos(List<PedidoCompraModel> lista) {
            this.pedidos = lista;
            selected.clear();
            for (int i = 0; i < lista.size(); i++)
                selected.add(false);
            fireTableDataChanged();
        }

        public List<PedidoCompraModel> getPedidos() {
            return pedidos;
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
                    // Formata de yyyy-MM-dd para dd/MM/yyyy
                    try {
                        Date d = df.parse(p.getData());
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
                selected.set(row, (Boolean) value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}
