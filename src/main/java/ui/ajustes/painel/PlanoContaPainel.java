package ui.ajustes.painel;

import util.UiKit;
import model.PlanoContaModel;
import service.PlanoContaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PlanoContaPainel extends JPanel {

    private final PlanoContaService service = new PlanoContaService();
    // colunas: Descrição, Tipo, Conta Pai; última coluna oculta armazena o objeto
    private final DefaultTableModel model = new DefaultTableModel(
        new String[] { "Descrição", "Tipo", "Conta Pai", "OBJETO" }, 0
    );
    private final JTable tabela = new JTable(model);

    public PlanoContaPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(10,10));
        add(UiKit.scroll(tabela), BorderLayout.CENTER);

        // remove coluna de objeto da visão
        tabela.removeColumn(tabela.getColumnModel().getColumn(3));

        // botões
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnNovo   = new JButton("Novo");
        JButton btnEditar = new JButton("Editar");
        JButton btnExcluir= new JButton("Excluir");
        botoes.add(btnNovo); botoes.add(btnEditar); botoes.add(btnExcluir);
        add(botoes, BorderLayout.SOUTH);

        btnNovo.addActionListener(e -> {
            new ui.ajustes.dialog.PlanoContaDialog(
                SwingUtilities.getWindowAncestor(this), null
            ).setVisible(true);
            atualizarLista();
        });

        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha<0) return;
            PlanoContaModel m = (PlanoContaModel) model.getValueAt(linha, 3);
            new ui.ajustes.dialog.PlanoContaDialog(
                SwingUtilities.getWindowAncestor(this), m
            ).setVisible(true);
            atualizarLista();
        });

        btnExcluir.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha<0) return;
            PlanoContaModel m = (PlanoContaModel) model.getValueAt(linha, 3);
            try {
                service.excluir(m.getId());
                atualizarLista();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir:\n"+ex.getMessage());
            }
        });

        atualizarLista();
    }

    /** Atualiza linhas da tabela */
    private void atualizarLista() {
        try {
            model.setRowCount(0);
            List<PlanoContaModel> list = service.listarTodos();
            for (PlanoContaModel p : list) {
                String pai = "";
                if (p.getParentId()!=null) {
                    PlanoContaModel pr = service.buscarPorId(p.getParentId());
                    pai = (pr!=null? pr.getDescricao() : "");
                }
                model.addRow(new Object[]{
                    p.getDescricao(),
                    p.getTipo(),
                    pai,
                    p // objeto oculto para editar/excluir
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar:\n"+ex.getMessage());
        }
    }

    /** Abre em janela */
    public void abrir() {
        JFrame f = new JFrame("Plano de Contas");
        f.setContentPane(this);
        f.setSize(600,400);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
