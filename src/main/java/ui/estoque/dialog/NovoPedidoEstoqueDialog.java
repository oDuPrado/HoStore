package ui.estoque.dialog;

import util.UiKit;
import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import dao.ProdutoDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class NovoPedidoEstoqueDialog extends JDialog {

    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    private PedidoCompraModel pedido;
    private final DefaultTableModel modelItens;
    private final JTable tabelaItens;

    public NovoPedidoEstoqueDialog(Frame owner) {
        super(owner, "Novo Pedido de Estoque", true);
        UiKit.applyDialogBase(this);

        // 1. Detalhes do pedido
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,5));
        JTextField tfNome = new JTextField(20);
        JFormattedTextField ftData = new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd"));
        ftData.setValue(new Date());
        top.add(new JLabel("Nome:"));      top.add(tfNome);
        top.add(new JLabel("Data (yyyy-MM-dd):")); top.add(ftData);

        JButton btCriar = new JButton("Definir");
        top.add(btCriar);
        add(top, BorderLayout.NORTH);

        // 2. Tabela de produtos
        modelItens = new DefaultTableModel(
            new String[]{"ID","Nome","Estoque","Qtd Pedir","Preço Custo"}, 0
        ) {
            @Override public boolean isCellEditable(int r,int c){return c==3;}
        };
        tabelaItens = new JTable(modelItens);
        add(UiKit.scroll(tabelaItens), BorderLayout.CENTER);

        // 3. Botões Salvar/Cancelar
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,5));
        JButton btSalvar = new JButton("💾 Salvar Pedido");
        JButton btCancelar = new JButton("Cancelar");
        rodape.add(btSalvar);
        rodape.add(btCancelar);
        add(rodape, BorderLayout.SOUTH);

        // ação Definir: cria pedido e carrega itens
        btCriar.addActionListener(e -> {
            String nome = tfNome.getText().trim();
            if(nome.isEmpty()) {
                JOptionPane.showMessageDialog(this,"Nome é obrigatório.");
                return;
            }
            String data = ftData.getText().trim();
            String id = UUID.randomUUID().toString();
            pedido = new PedidoCompraModel(id,nome,data,"rascunho",null,"");
            try {
                pedidoDAO.inserir(pedido);
            } catch(Exception ex){ ex.printStackTrace(); }
            carregarProdutos();
        });

        // ação Salvar Pedido e Itens
        btSalvar.addActionListener(e -> {
            if(pedido==null) {
                JOptionPane.showMessageDialog(this,"Defina os detalhes primeiro.");
                return;
            }
            try {
                for(int i=0;i<modelItens.getRowCount();i++){
                    int qtd = Integer.parseInt(modelItens.getValueAt(i,3).toString());
                    if(qtd>0) {
                        String pid    = modelItens.getValueAt(i,0).toString();
                        String linkId = UUID.randomUUID().toString();
                        ProdutoModel prod = produtoDAO.findById(pid);
                        String fornecedorId = (prod != null) ? prod.getFornecedorId() : null;
                        double custo = (prod != null) ? prod.getPrecoCompra() : 0.0;
                        double preco = (prod != null) ? prod.getPrecoVenda() : 0.0;
                        PedidoEstoqueProdutoModel item = new PedidoEstoqueProdutoModel(
                            linkId,
                            pedido.getId(),
                            pid,
                            fornecedorId,
                            custo,
                            preco,
                            qtd,
                            0,
                            "pendente"
                        );
                        itemDAO.inserir(item);
                    }
                }
                JOptionPane.showMessageDialog(this,"Pedido criado com sucesso!");
                dispose();
            } catch(Exception ex){
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,"Erro ao salvar itens: "+ex.getMessage());
            }
        });

        btCancelar.addActionListener(e -> dispose());

        setSize(700,400);
        setLocationRelativeTo(owner);
    }

    private void carregarProdutos() {
        try {
            List<ProdutoModel> lista = produtoDAO.listAll();
            modelItens.setRowCount(0);
            for(ProdutoModel p:lista) {
                modelItens.addRow(new Object[]{
                    p.getId(),
                    p.getNome(),
                    p.getQuantidade(),
                    0,
                    p.getPrecoCompra()
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }
}
