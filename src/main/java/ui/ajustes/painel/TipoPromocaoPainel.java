package ui.ajustes.painel;

import dao.TipoPromocaoDAO;
import model.TipoPromocaoModel;
import ui.ajustes.dialog.TipoPromocaoDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TipoPromocaoPainel extends AbstractCrudPainel {

    private final TipoPromocaoDAO dao = new TipoPromocaoDAO();

    @Override
    protected String getTitulo() {
        return "Tipos de Promoção";
    }

    @Override
    protected String[] getColunas() {
        return new String[]{"ID", "Nome", "Descrição"};
    }

    @Override
    protected void onAdicionar() {
        new TipoPromocaoDialog(null, modelo).setVisible(true);
        carregarTabela();
    }

    @Override
    protected void onEditar() {
        int row = tabela.getSelectedRow();
        if (row != -1) {
            new TipoPromocaoDialog(row, modelo).setVisible(true);
            carregarTabela();
        }
    }

    @Override
    protected void onRemover() {
        int row = tabela.getSelectedRow();
        if (row != -1) {
            String id = modelo.getValueAt(row, 0).toString();
            try {
                dao.excluir(id);
                modelo.removeRow(row);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir: " + e.getMessage());
            }
        }
    }

    @Override
    protected void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<TipoPromocaoModel> tipos = dao.listarTodos();
            for (TipoPromocaoModel tipo : tipos) {
                modelo.addRow(new Object[]{
                    tipo.getId(),
                    tipo.getNome(),
                    tipo.getDescricao()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + e.getMessage());
        }
    }
}
