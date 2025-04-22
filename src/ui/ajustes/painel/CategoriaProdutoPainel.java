package ui.ajustes.painel;

import dao.CadastroGenericoDAO;
import ui.ajustes.dialog.CategoriaProdutoDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class CategoriaProdutoPainel extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final CadastroGenericoDAO dao = new CadastroGenericoDAO(
        "categorias_produtos", "id", "nome"
    );

    public CategoriaProdutoPainel() {
        setLayout(new BorderLayout(8, 8));

        modelo = new DefaultTableModel(new String[]{"Nome"}, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton btnConfigurar = new JButton("🗂️ Configurar Categorias Disponíveis");
        btnConfigurar.addActionListener(e -> {
            CategoriaProdutoDialog dialog = new CategoriaProdutoDialog(null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);        // aqui abre **uma** vez
            carregarTabela();               // só depois de fechar
        });
        

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelBotoes.add(btnConfigurar);
        add(painelBotoes, BorderLayout.NORTH);

        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<Map<String, String>> lista = dao.listar();
            for (Map<String, String> item : lista) {
                modelo.addRow(new Object[]{ item.get("nome") });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar categorias.");
        }
    }

    public void abrir() {
        JDialog d = new JDialog((Frame) null, "Categorias de Produtos", true);
        d.setContentPane(this);
        d.setSize(500, 400);
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
