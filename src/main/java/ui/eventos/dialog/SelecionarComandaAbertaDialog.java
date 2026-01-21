package ui.eventos.dialog;

import dao.ComandaDAO;
import model.ComandaModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class SelecionarComandaAbertaDialog extends JDialog {

    private static final DateTimeFormatter UI_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Mesa", "Total", "Abertura" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private Integer comandaIdSelecionada;
    private boolean criarNova;

    public SelecionarComandaAbertaDialog(Window owner, String clienteId) {
        super(owner, "Selecionar Comanda Aberta", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);
        setSize(720, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildCenterCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        configurarTabela();
        carregar(clienteId);
    }

    public Integer getComandaIdSelecionada() {
        return comandaIdSelecionada;
    }

    public boolean isCriarNova() {
        return criarNova;
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Comandas Abertas"));
        left.add(UiKit.hint("Selecione uma comanda aberta do cliente"));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JPanel buildCenterCard() {
        JPanel center = UiKit.card();
        center.setLayout(new BorderLayout(8, 8));
        center.add(UiKit.scroll(table), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildBottomCard() {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnCriar = UiKit.ghost("Criar Nova Comanda");
        btnCriar.addActionListener(e -> {
            criarNova = true;
            dispose();
        });

        JButton btnSelecionar = UiKit.primary("Adicionar na Comanda");
        btnSelecionar.addActionListener(e -> selecionar());

        JButton btnCancelar = UiKit.ghost("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        right.add(btnCriar);
        right.add(btnCancelar);
        right.add(btnSelecionar);

        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void configurarTabela() {
        UiKit.tableDefaults(table);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(right);
    }

    private void carregar(String clienteId) {
        try {
            model.setRowCount(0);
            NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            List<ComandaModel> list = new ComandaDAO().listarAbertasPorCliente(clienteId);
            for (ComandaModel c : list) {
                String dt = (c.getCriadoEm() != null) ? c.getCriadoEm().format(UI_DTF) : "";
                model.addRow(new Object[] { c.getId(), c.getMesa(), moeda.format(c.getTotalLiquido()), dt });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selecionar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        comandaIdSelecionada = (Integer) model.getValueAt(modelRow, 0);
        dispose();
    }
}
