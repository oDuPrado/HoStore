package ui.estoque.dialog;

import dao.FornecedorDAO;
import model.FornecedorModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class FornecedorSelectionDialog extends JDialog {

    private final JTextField tfBusca = new JTextField(20);
    private final DefaultTableModel tableModel;
    private final JTable           table;
    private final TableRowSorter<DefaultTableModel> rowSorter;
    private FornecedorModel        selecionado;

    public FornecedorSelectionDialog(Frame owner) {
        super(owner, "Selecionar Fornecedor", true);
        setLayout(new BorderLayout(8,8));
        setSize(600, 400);
        setLocationRelativeTo(owner);

        // ——— Top: campo de busca ———
        JPanel pnlBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        pnlBusca.add(new JLabel("Buscar:"));
        pnlBusca.add(tfBusca);
        add(pnlBusca, BorderLayout.NORTH);

        // ——— Centro: tabela ———
        String[] cols = {"Nome", "CNPJ", "Tipo", "Prazo (dias)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ——— Rodapé: botões ———
        JButton btnOk     = new JButton("OK");
        JButton btnCancel = new JButton("Cancelar");
        btnOk.addActionListener(e -> onOK());
        btnCancel.addActionListener(e -> onCancel());
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        pnlBtns.add(btnCancel);
        pnlBtns.add(btnOk);
        add(pnlBtns, BorderLayout.SOUTH);

        // ——— Listeners ———
        tfBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e){ filtrar(); }
        });

        // carrega dados
        carregarFornecedores();
    }

    private void carregarFornecedores() {
        try {
            List<FornecedorModel> lista = new FornecedorDAO()
                .listar("", "", "", null);
            tableModel.setRowCount(0);
            for (FornecedorModel f : lista) {
                tableModel.addRow(new Object[]{
                    f.getId(),
                    f.getNome(),
                    f.getCnpj(),
                    f.getPagamentoTipo(),
                    f.getPrazo()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar fornecedores:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filtrar() {
        String texto = tfBusca.getText().trim();
        if (texto.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            // filtra apenas por coluna Nome (índice 1), case-insensitive
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 1));
        }
    }

    private void onOK() {
        int sel = table.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor.");
            return;
        }
        // converte linha da view para model
        int modelRow = table.convertRowIndexToModel(sel);
        String id = (String) tableModel.getValueAt(modelRow, 0);
        try {
            selecionado = new FornecedorDAO().buscarPorId(id);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar fornecedor:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
        dispose();
    }

    private void onCancel() {
        selecionado = null;
        dispose();
    }

    /** Retorna o fornecedor escolhido (ou null se cancelou) */
    public FornecedorModel getSelectedFornecedor() {
        return selecionado;
    }
}
