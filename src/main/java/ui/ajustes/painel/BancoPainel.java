package ui.ajustes.painel;

import util.UiKit;
import model.BancoModel;
import service.BancoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class BancoPainel extends JPanel {

    private final BancoService service = new BancoService();
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"ID","Banco","Agência","Conta","Observações"}, 0
    );
    private final JTable tabela = new JTable(model);

    public BancoPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(10,10));
        add(UiKit.scroll(tabela), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnNovo   = new JButton("Novo");
        JButton btnEditar = new JButton("Editar");
        JButton btnExcluir= new JButton("Excluir");
        botoes.add(btnNovo);
        botoes.add(btnEditar);
        botoes.add(btnExcluir);
        add(botoes, BorderLayout.SOUTH);

        btnNovo.addActionListener(e -> {
            new ui.ajustes.dialog.BancoDialog(
                SwingUtilities.getWindowAncestor(this), null
            ).setVisible(true);
            atualizarLista();
        });

        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) return;
            String id = (String)model.getValueAt(linha, 0);
            try {
                new ui.ajustes.dialog.BancoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    service.buscarPorId(id)
                ).setVisible(true);
                atualizarLista();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Erro ao carregar dados do banco:\n"+ex.getMessage());
            }
        });
        

        btnExcluir.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha<0) return;
            String id = (String)model.getValueAt(linha,0);
            try {
                service.excluir(id);
                atualizarLista();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Erro ao excluir:\n"+ex.getMessage());
            }
        });

        atualizarLista();
    }

    public void abrir() {
        JFrame f = new JFrame("Contas Bancárias");
        f.setContentPane(this);
        f.setSize(700,400);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private void atualizarLista() {
        try {
            model.setRowCount(0);
            List<BancoModel> list = service.listarTodos();
            for (BancoModel b : list) {
                model.addRow(new Object[]{
                    b.getId(), b.getNome(), b.getAgencia(),
                    b.getConta(), b.getObservacoes()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Erro ao carregar:\n"+ex.getMessage());
        }
    }
}
