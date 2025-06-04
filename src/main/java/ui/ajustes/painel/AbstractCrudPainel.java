package ui.ajustes.painel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public abstract class AbstractCrudPainel extends JPanel {
    protected JTable tabela;
    protected DefaultTableModel modelo;

    public AbstractCrudPainel() {
        setLayout(new BorderLayout(8, 8));
        modelo = criarModelo();
        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Adicionar");
        JButton btnEdit = new JButton("Editar");
        JButton btnDel = new JButton("Remover");

        btnAdd.addActionListener(e -> onAdicionar());
        btnEdit.addActionListener(e -> onEditar());
        btnDel.addActionListener(e -> onRemover());

        botoes.add(btnAdd);
        botoes.add(btnEdit);
        botoes.add(btnDel);
        add(botoes, BorderLayout.NORTH);
    }

    public void abrir() {
        JDialog d = new JDialog((Frame) null, getTitulo(), true);
        d.setContentPane(this);
        d.setSize(700, 400);
        d.setLocationRelativeTo(null);
        d.setVisible(true);
    }

    protected abstract String getTitulo();
    protected abstract String[] getColunas();
    protected abstract void carregarTabela();

    protected Object[] getValoresFake() {
        Object[] dados = new Object[getColunas().length];
        for (int i = 0; i < dados.length; i++) dados[i] = "Valor " + (i + 1);
        return dados;
    }

    protected void onAdicionar() {
        modelo.addRow(getValoresFake());
    }

    protected void onEditar() {
        int row = tabela.getSelectedRow();
        if (row == -1) return;
        modelo.setValueAt("EDITADO", row, 1); // exemplo
    }

    protected void onRemover() {
        int row = tabela.getSelectedRow();
        if (row != -1) modelo.removeRow(row);
    }

    /** Novo método que pode ser sobrescrito para customizar a tabela */
    protected DefaultTableModel criarModelo() {
        return new DefaultTableModel(getColunas(), 0);
    }
}
