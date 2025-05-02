package ui.estoque.painel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import ui.estoque.dialog.NovoPedidoEstoqueDialog;
import ui.estoque.dialog.EntradaProdutosDialog;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;

public class PainelPedidosEstoque extends JPanel {

    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();

    private final DefaultTableModel modelPedidos;
    private final JTable tabelaPedidos;

    public PainelPedidosEstoque() {
        setLayout(new BorderLayout(10,10));

        // Tabela de Pedidos
        modelPedidos = new DefaultTableModel(
            new String[]{"ID","Nome","Data","Status","Fornecedor"}, 0
        ) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        tabelaPedidos = new JTable(modelPedidos);
        configurarTabela();
        add(new JScrollPane(tabelaPedidos), BorderLayout.CENTER);

        // Botões
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,5));
        JButton btNovo = new JButton("➕ Novo Pedido");
        JButton btExcluir = new JButton("🗑️ Excluir");
        JButton btAtualizar = new JButton("🔄 Atualizar");

        btNovo.addActionListener(e -> {
            NovoPedidoEstoqueDialog dlg = new NovoPedidoEstoqueDialog(
                (Frame) SwingUtilities.getWindowAncestor(this)
            );
            dlg.setVisible(true);
            carregarPedidos();
        });
        btExcluir.addActionListener(e -> {
            int sel = tabelaPedidos.getSelectedRow();
            if (sel<0) { JOptionPane.showMessageDialog(this,"Selecione um pedido."); return; }
            String id = (String)modelPedidos.getValueAt(sel,0);
            if (JOptionPane.showConfirmDialog(this,
                    "Excluir pedido e itens?", "Confirmar",
                    JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            try {
                // excluir itens
                List<PedidoEstoqueProdutoModel> itens = itemDAO.listarPorPedido(id);
                for(PedidoEstoqueProdutoModel it:itens)
                    itemDAO.excluir(it.getId());
                // excluir pedido
                pedidoDAO.excluir(id);
            } catch(Exception ex){ ex.printStackTrace(); }
            carregarPedidos();
        });
        btAtualizar.addActionListener(e -> carregarPedidos());

        rodape.add(btNovo);
        rodape.add(btExcluir);
        rodape.add(btAtualizar);
        add(rodape, BorderLayout.SOUTH);

        // Duplo-clique = Entrada de produtos
        tabelaPedidos.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                if(e.getClickCount()==2){
                    int sel = tabelaPedidos.getSelectedRow();
                    if(sel<0) return;
                    String id = (String)modelPedidos.getValueAt(sel,0);
                    EntradaProdutosDialog dlg = new EntradaProdutosDialog(
                        (Frame) SwingUtilities.getWindowAncestor(PainelPedidosEstoque.this),
                        id
                    );
                    dlg.setVisible(true);
                    carregarPedidos();
                }
            }
        });

        carregarPedidos();
    }

    private void configurarTabela() {
        tabelaPedidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        esconderColunaID(tabelaPedidos);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for(int c=2;c<modelPedidos.getColumnCount();c++)
            tabelaPedidos.getColumnModel().getColumn(c).setCellRenderer(center);
    }

    private void esconderColunaID(JTable t) {
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setPreferredWidth(0);
    }

    private void carregarPedidos() {
        modelPedidos.setRowCount(0);
        try {
            for(PedidoCompraModel p : pedidoDAO.listarTodos()) {
                modelPedidos.addRow(new Object[]{
                    p.getId(),
                    p.getNome(),
                    p.getData(),
                    p.getStatus(),
                    p.getFornecedorId() // nome do fornecedor, se preferir pode buscar via FornecedorDAO
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }
}
