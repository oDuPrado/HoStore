// Plano de Contas com Selector Pai
// Procure com Ctrl+F por "// Plano de Contas com Selector Pai"
package ui.ajustes.dialog;

import model.PlanoContaModel;
import service.PlanoContaService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class SelecionarPlanoContaDialog extends JDialog {

    private final PlanoContaService service = new PlanoContaService();
    private final JTable table;
    private final TableRowSorter<TableModel> sorter;
    private PlanoContaModel selecionado;

    public SelecionarPlanoContaDialog(Window owner) {
        super(owner, "Selecionar Conta Pai", ModalityType.APPLICATION_MODAL);
        
        // modelo da tabela
        String[] colunas = {"Descrição", "Tipo", "Observações"};
        DefaultTableModel model = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        // popula
        try {
            for (PlanoContaModel p : service.listarTodos()) {
                model.addRow(new Object[]{
                    p.getDescricao(),
                    p.getTipo(),
                    p.getObservacoes()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(table);

        // filtro de busca
        JTextField tfFiltro = new JTextField(20);
        tfFiltro.getDocument().addDocumentListener(new DocumentListener() {
            public void update() {
                String txt = tfFiltro.getText().trim();
                if (txt.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txt));
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        // botões
        JButton btnSelecionar = new JButton("Selecionar");
        JButton btnCancelar   = new JButton("Cancelar");
        btnSelecionar.addActionListener(e -> onSelecionar());
        btnCancelar.addActionListener(e -> dispose());

        // duplo clique na tabela
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onSelecionar();
            }
        });

        // layout
        JPanel north = new JPanel(new BorderLayout(5,5));
        north.add(new JLabel("Filtrar:"), BorderLayout.WEST);
        north.add(tfFiltro, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
        south.add(btnCancelar);
        south.add(btnSelecionar);

        getContentPane().setLayout(new BorderLayout(10,10));
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(sp, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        setSize(500,400);
        setLocationRelativeTo(owner);
    }

    private void onSelecionar() {
        int idx = table.getSelectedRow();
        if (idx < 0) return;
        // pega o modelo por índice VISUAL → converte pro modelo real
        int modelIndex = table.getRowSorter().convertRowIndexToModel(idx);
        try {
            // listaTodos() retorna na mesma ordem do table model
            selecionado = service.listarTodos().get(modelIndex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        dispose();
    }

    /** @return a conta selecionada ou null se cancelou */
    public PlanoContaModel getSelecionado() {
        return selecionado;
    }
}
