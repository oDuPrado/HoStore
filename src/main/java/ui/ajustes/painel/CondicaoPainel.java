package ui.ajustes.painel;

import ui.ajustes.dialog.CondicaoDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import dao.CadastroGenericoDAO;

public class CondicaoPainel extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final CadastroGenericoDAO dao = new CadastroGenericoDAO(
        "condicoes", "id", "nome"
    );

    public CondicaoPainel() {
        setLayout(new BorderLayout(8, 8));

        modelo = new DefaultTableModel(new String[]{"Nome"}, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tabela = new JTable(modelo);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton btnConfigurar = new JButton("⚙️ Configurar Condições");
        btnConfigurar.addActionListener(e -> {
            CondicaoDialog dialog = new CondicaoDialog(null);
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
            List<Map<String,String>> lista = dao.listar();
            for (Map<String,String> item : lista) {
                modelo.addRow(new Object[]{ item.get("nome") });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar condições."
            );
        }
    }

    public void abrir() {
        JDialog d = new JDialog((Frame)null, "Condições de Produto", true);
        d.setContentPane(this);
        d.setSize(400, 300);
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
