// src/main/java/ui/ajustes/dialog/CondicaoDialog.java
package ui.ajustes.dialog;

import dao.NcmDAO;
import model.NcmModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ncmDialog extends JDialog {

    private final NcmDAO dao = new NcmDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JFormattedTextField txtCodigo;
    private JTextField txtDescricao;

    public ncmDialog(Frame owner) {
        super(owner, "Configurar NCM", true);
        initComponents();
        loadNcms();
        setSize(600, 400);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- topo: entrada de código e descrição ---
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        try {
            MaskFormatter mask = new MaskFormatter("########");
            mask.setPlaceholderCharacter('_');
            txtCodigo = new JFormattedTextField(mask);
            txtCodigo.setColumns(8);
        } catch (Exception e) {
            txtCodigo = new JFormattedTextField();
            txtCodigo.setColumns(8);
        }
        txtDescricao = new JTextField(20);
        JButton btnAdd = new JButton("Adicionar/Atualizar");
        btnAdd.addActionListener(e -> addOrUpdate());
        inputPanel.add(new JLabel("Código NCM:"));
        inputPanel.add(txtCodigo);
        inputPanel.add(new JLabel("Descrição:"));
        inputPanel.add(txtDescricao);
        inputPanel.add(btnAdd);

        panel.add(inputPanel, BorderLayout.NORTH);

        // --- centro: tabela de NCMs ---
        tableModel = new DefaultTableModel(
            new Object[]{"Código", "Descrição"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- rodapé: botões ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnRemove = new JButton("Remover");
        btnRemove.addActionListener(e -> removeSelected());
        JButton btnSave = new JButton("Salvar");
        btnSave.addActionListener(e -> saveAll());
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        buttons.add(btnRemove);
        buttons.add(btnSave);
        buttons.add(btnCancel);

        panel.add(buttons, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    private void loadNcms() {
        tableModel.setRowCount(0);
        try {
            List<NcmModel> list = dao.findAll();
            for (NcmModel n : list) {
                tableModel.addRow(new Object[]{
                    n.getCodigo(),
                    n.getDescricao()
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar NCMs:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void addOrUpdate() {
        String codigo = txtCodigo.getText().replace("_", "").trim();
        String desc   = txtDescricao.getText().trim();
        if (codigo.isEmpty() || desc.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Preencha código e descrição.",
                "Atenção",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        // se já existe na tabela, atualiza descrição
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(codigo)) {
                tableModel.setValueAt(desc, i, 1);
                clearInputs();
                return;
            }
        }
        // senão adiciona nova linha
        tableModel.addRow(new Object[]{codigo, desc});
        clearInputs();
    }

    private void clearInputs() {
        txtCodigo.setValue(null);
        txtDescricao.setText("");
        txtCodigo.requestFocus();
    }

    private void removeSelected() {
        int sel = table.getSelectedRow();
        if (sel != -1) {
            tableModel.removeRow(sel);
        }
    }

    private void saveAll() {
    List<NcmModel> list = new ArrayList<>();
    for (int i = 0; i < tableModel.getRowCount(); i++) {
        String codigo = (String) tableModel.getValueAt(i, 0);
        String desc   = (String) tableModel.getValueAt(i, 1);
        list.add(new NcmModel(codigo, desc));
    }
    try {
        dao.deleteAll();
        for (NcmModel n : list) {
            dao.insert(n);
        }
        JOptionPane.showMessageDialog(
            this,
            "NCMs salvos com sucesso!",
            "Sucesso",
            JOptionPane.INFORMATION_MESSAGE
        );
        dispose();
    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(
            this,
            "Erro ao salvar NCMs:\n" + ex.getMessage(),
            "Erro",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
}