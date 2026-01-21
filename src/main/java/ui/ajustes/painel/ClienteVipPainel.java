package ui.ajustes.painel;

import util.UiKit;
import dao.CadastroGenericoDAO;
import ui.ajustes.dialog.ClienteVipDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ClienteVipPainel extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final CadastroGenericoDAO dao = new CadastroGenericoDAO(
        "clientes_vip",
        "id", "nome", "cpf", "telefone", "categoria", "criado_em"
    );

    public ClienteVipPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8,8));

        modelo = new DefaultTableModel(
            new String[]{"Nome","CPF","Telefone","Categoria","Criado Em"}, 0
        ) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tabela = new JTable(modelo);
        add(UiKit.scroll(tabela), BorderLayout.CENTER);

        JButton btnGerenciar = new JButton("👑 Gerenciar VIPs");
        btnGerenciar.addActionListener(e -> {
            ClienteVipDialog dlg = new ClienteVipDialog(null);
            dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dlg.setVisible(true);
            carregarTabela();
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(btnGerenciar);
        add(topo, BorderLayout.NORTH);

        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<Map<String,String>> lista = dao.listar();
            for (Map<String,String> m : lista) {
                modelo.addRow(new Object[]{
                    m.get("nome"),
                    m.get("cpf"),
                    m.get("telefone"),
                    m.get("categoria"),
                    m.get("criado_em")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar VIPs:\n"+ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void abrir() {
        JDialog d = new JDialog((Frame)null, "Clientes VIP", true);
        d.setContentPane(this);
        d.setSize(600,400);
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
