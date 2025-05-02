// ui/estoque/dialog/CriarPedidoEstoqueDialog.java
package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CriarPedidoEstoqueDialog extends JDialog {

    private final PedidoCompraDAO pedidoDAO      = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();

    private final List<ProdutoModel> produtos;
    private PedidoCompraModel pedido;

    private final JTextField tfNome  = new JTextField(25);
    private final JDateChooser dcData = new JDateChooser(new Date());
    private final DefaultTableModel model = new DefaultTableModel(
        new String[] {"ID", "Nome", "Estoque Atual", "Qtd a Pedir"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int col) {
            return col == 3;
        }
    };
    private final JTable tabela = new JTable(model);

    public CriarPedidoEstoqueDialog(Frame owner, String categoria, List<ProdutoModel> filtrados) {
        super(owner, "Criar Pedido de Estoque - " + categoria, true);
        this.produtos = filtrados;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));

        // ── Top: Nome e Data ─────────────────────────
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        dcData.setDateFormatString("dd/MM/yyyy");
        tfNome.setText("Pedido de " + getTitle().split(" - ", 2)[1] + " " +
                       new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        topo.add(new JLabel("Nome do Pedido:"));
        topo.add(tfNome);
        topo.add(new JLabel("Data:"));
        topo.add(dcData);
        add(topo, BorderLayout.NORTH);

        // ── Centro: Tabela de Produtos ─────────────────
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabela);
        tabela.setPreferredScrollableViewportSize(new Dimension(600, 300));
        esconderColunaID(tabela);
        add(scroll, BorderLayout.CENTER);
        carregarProdutos();

        // ── Rodapé: Botões ────────────────────────────
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        JButton btSalvar = new JButton("💾 Salvar Pedido");
        JButton btCancelar = new JButton("Cancelar");
        rodape.add(btSalvar);
        rodape.add(btCancelar);
        add(rodape, BorderLayout.SOUTH);

        btSalvar.addActionListener(e -> salvarPedido());
        btCancelar.addActionListener(e -> dispose());
    }

    private void carregarProdutos() {
        model.setRowCount(0);
        for (ProdutoModel p : produtos) {
            model.addRow(new Object[]{
                p.getId(),
                p.getNome(),
                p.getQuantidade(),
                0
            });
        }
    }

    private void salvarPedido() {
        String nome = tfNome.getText().trim();
        Date data = dcData.getDate();
        if (nome.isEmpty() || data == null) {
            JOptionPane.showMessageDialog(this,
                "Preencha o nome e a data do pedido.",
                "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Gera novo pedido
        String idPedido = UUID.randomUUID().toString();
        String dataIso  = new SimpleDateFormat("yyyy-MM-dd").format(data);
        pedido = new PedidoCompraModel(
            idPedido,
            nome,
            dataIso,
            "rascunho",
            null,
            ""
        );
        try {
            pedidoDAO.inserir(pedido);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao criar pedido:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Salva itens vinculados
        boolean any = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            int qtd = 0;
            try {
                Object v = model.getValueAt(i, 3);
                qtd = Integer.parseInt(v.toString());
            } catch (NumberFormatException ignored) {}
            if (qtd > 0) {
                any = true;
                String linkId = UUID.randomUUID().toString();
                String prodId = model.getValueAt(i, 0).toString();
                PedidoEstoqueProdutoModel item = new PedidoEstoqueProdutoModel(
                    linkId,
                    idPedido,
                    prodId,
                    qtd,
                    0,
                    "pendente"
                );
                try {
                    itemDAO.inserir(item);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Erro ao adicionar item:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (!any) {
            JOptionPane.showMessageDialog(this,
                "Nenhum item selecionado para o pedido.",
                "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
            "Pedido criado com sucesso!",
            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void esconderColunaID(JTable t) {
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setPreferredWidth(0);
    }
}
