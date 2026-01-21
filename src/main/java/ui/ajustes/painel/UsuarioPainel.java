// ui/ajustes/panel/UsuariosPanel.java
package ui.ajustes.painel;

import util.UiKit;
import dao.UsuarioDAO;
import model.UsuarioModel;
import ui.ajustes.dialog.UsuarioDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

public class UsuarioPainel extends JPanel {
    private final UsuarioDAO dao = new UsuarioDAO();
    private final UsuariosTableModel tableModel = new UsuariosTableModel();
    private final JTable table = new JTable(tableModel);

    public UsuarioPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(5,5));

        // toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
        JButton btnNovo = new JButton("Novo");
        JButton btnEditar = new JButton("Editar");
        JButton btnDesativar = new JButton("Desativar");
        toolbar.add(btnNovo);
        toolbar.add(btnEditar);
        toolbar.add(btnDesativar);
        add(toolbar, BorderLayout.NORTH);

        // tabela
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        // ações
        btnNovo.addActionListener(e -> openDialog(null));
        btnEditar.addActionListener(e -> {
            UsuarioModel sel = getSelected();
            if (sel != null) openDialog(sel);
        });
        btnDesativar.addActionListener(e -> {
            UsuarioModel sel = getSelected();
            if (sel != null && JOptionPane.showConfirmDialog(this,
                "Desativar usuário "+sel.getUsuario()+"?", "Confirma",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                try {
                    dao.excluir(sel.getId());
                    refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Erro: "+ex.getMessage());
                }
            }
        });

        refresh();
    }

    private void openDialog(UsuarioModel u) {
        // pega o ancestral (Frame ou JDialog)
        Window win = SwingUtilities.getWindowAncestor(this);
    
        // só usa como Frame se for mesmo um Frame
        Frame owner = (win instanceof Frame) ? (Frame) win : null;
    
        UsuarioDialog dlg = new UsuarioDialog(owner, u);
        dlg.setVisible(true);
        refresh();
    }

    private UsuarioModel getSelected() {
        int idx = table.getSelectedRow();
        return idx<0 ? null : tableModel.getUsuario(idx);
    }

    private void refresh() {
        try {
            List<UsuarioModel> lista = dao.listar();
            tableModel.setUsuarios(lista);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro: "+ex.getMessage());
        }
    }

    public static void abrir() {
        JDialog dialog = new JDialog((Frame) null, "Usuários", true);
        dialog.setContentPane(new UsuarioPainel());
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }    

    private static class UsuariosTableModel extends AbstractTableModel {
        private List<UsuarioModel> usuarios = List.of();
        private final String[] colunas = {"Nome","Usuário","Tipo","Ativo"};

        public void setUsuarios(List<UsuarioModel> u) {
            this.usuarios = u;
            fireTableDataChanged();
        }

        public UsuarioModel getUsuario(int row) {
            return usuarios.get(row);
        }

        @Override public int getRowCount() { return usuarios.size(); }
        @Override public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int col) { return colunas[col]; }
        @Override public Class<?> getColumnClass(int col) {
            return col==3 ? Boolean.class : String.class;
        }
        @Override public Object getValueAt(int row,int col) {
            UsuarioModel u = usuarios.get(row);
            return switch(col) {
                case 0 -> u.getNome();
                case 1 -> u.getUsuario();
                case 2 -> u.getTipo();
                case 3 -> u.isAtivo();
                default -> "";
            };
        }
    }
}
