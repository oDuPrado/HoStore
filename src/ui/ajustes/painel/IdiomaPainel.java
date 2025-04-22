package ui.ajustes.painel;

import dao.CadastroGenericoDAO;
import ui.ajustes.dialog.IdiomaDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class IdiomaPainel extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final CadastroGenericoDAO dao = new CadastroGenericoDAO(
        "linguagens", "id", "nome"
    );

    public IdiomaPainel() {
        setLayout(new BorderLayout(8, 8));

        // Tabela só com a coluna "Nome"
        modelo = new DefaultTableModel(new String[]{"Nome"}, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // Botão único
        JButton btnConfigurar = new JButton("🌐 Configurar Idiomas Disponíveis");
        btnConfigurar.addActionListener(e -> {
            IdiomaDialog dialog = new IdiomaDialog(null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            carregarTabela();
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
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar idiomas."
            );
        }
    }

    public void abrir() {
        JDialog d = new JDialog((Frame) null, "Idiomas", true);
        d.setContentPane(this);
        d.setSize(400, 300);
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
