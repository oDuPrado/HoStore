package ui.ajustes.painel;

import util.UiKit;
import dao.CadastroGenericoDAO;
import ui.ajustes.dialog.TipoCartaDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class TipoCartaPainel extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final CadastroGenericoDAO dao = new CadastroGenericoDAO(
        "tipo_cartas", "id", "nome"
    );

    public TipoCartaPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8,8));

        modelo = new DefaultTableModel(new String[]{"Nome"}, 0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        tabela = new JTable(modelo);
        add(UiKit.scroll(tabela), BorderLayout.CENTER);

        JButton btnConfig = new JButton("⚡ Configurar Tipos de Carta");
        btnConfig.addActionListener(e -> {
            TipoCartaDialog dlg = new TipoCartaDialog(null);
            dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dlg.setVisible(true);
            carregarTabela();
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(btnConfig);
        add(topo, BorderLayout.NORTH);

        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<Map<String,String>> lista = dao.listar();
            for (Map<String,String> m : lista) {
                modelo.addRow(new Object[]{ m.get("nome") });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar tipos.");
        }
    }

    public void abrir() {
        JDialog d = new JDialog((Frame)null, "Tipos de Carta", true);
        d.setContentPane(this);
        d.setSize(400,300);
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
