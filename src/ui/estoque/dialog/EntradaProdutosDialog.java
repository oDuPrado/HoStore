package ui.estoque.dialog;

import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class EntradaProdutosDialog extends JDialog {

    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final DefaultTableModel modelItens;
    private final JTable tabelaItens;
    private final String pedidoId;

    public EntradaProdutosDialog(Frame owner, String pedidoId) {
        super(owner, "Entrada de Produtos - Pedido " + pedidoId, true);
        this.pedidoId = pedidoId;

        modelItens = new DefaultTableModel(
            new String[]{"ID","Produto","Qtd Pedida","Qtd Recebida","Status"},0
        ) {
            @Override public boolean isCellEditable(int r,int c){return c==3;}
        };
        tabelaItens = new JTable(modelItens);
        add(new JScrollPane(tabelaItens), BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,5));
        JButton btSalvar = new JButton("Atualizar Recebimento");
        JButton btCancelar = new JButton("Cancelar");
        rodape.add(btSalvar);
        rodape.add(btCancelar);
        add(rodape, BorderLayout.SOUTH);

        btSalvar.addActionListener(e -> {
            try {
                for(int i=0;i<modelItens.getRowCount();i++){
                    String linkId = modelItens.getValueAt(i,0).toString();
                    int ped    = Integer.parseInt(modelItens.getValueAt(i,2).toString());
                    int rec    = Integer.parseInt(modelItens.getValueAt(i,3).toString());
                    String status = rec>=ped ? "completo" : "parcial";
                    // atualiza link
                    PedidoEstoqueProdutoModel m = new PedidoEstoqueProdutoModel(
                        linkId,pedidoId,
                        modelItens.getValueAt(i,1).toString(),0,0,status
                    );
                    m.setQuantidadePedida(ped);
                    m.setQuantidadeRecebida(rec);
                    m.setStatus(status);
                    itemDAO.atualizar(m);
                    // atualiza estoque
                    ProdutoModel p = produtoDAO.findById(m.getProdutoId());
                    p.setQuantidade(p.getQuantidade() + rec);
                    produtoDAO.update(p);
                }
                JOptionPane.showMessageDialog(this,"Recebimento registrado!");
                dispose();
            } catch(Exception ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage());
            }
        });

        btCancelar.addActionListener(e -> dispose());

        carregarItens();
        setSize(600,350);
        setLocationRelativeTo(owner);
    }

    private void carregarItens() {
        try {
            List<PedidoEstoqueProdutoModel> itens = itemDAO.listarPorPedido(pedidoId);
            modelItens.setRowCount(0);
            for(PedidoEstoqueProdutoModel it: itens) {
                ProdutoModel p = produtoDAO.findById(it.getProdutoId());
                modelItens.addRow(new Object[]{
                    it.getId(),
                    p.getNome(),
                    it.getQuantidadePedida(),
                    it.getQuantidadeRecebida(),
                    it.getStatus()
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }
}
