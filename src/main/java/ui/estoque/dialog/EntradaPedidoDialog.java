// src/ui/estoque/dialog/EntradaPedidoDialog.java
package ui.estoque.dialog;

import dao.PedidoEstoqueProdutoDAO;
import javax.swing.border.*;
import dao.ProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import service.PedidoCompraService;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntradaPedidoDialog extends JDialog {

    private final PedidoCompraModel pedido;
    private final PedidoEstoqueProdutoDAO pedProdDAO = new PedidoEstoqueProdutoDAO();

    private JTable table;
    private DefaultTableModel tm;

    public EntradaPedidoDialog(Frame parent, PedidoCompraModel pedido) {
        super(parent, "📥 Entrada de Pedido", true);
        UiKit.applyDialogBase(this);
        this.pedido = pedido;
        initComponents();
        loadData();
        setSize(820, 520);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));

        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        String dataVis = pedido.getData();
        try {
            dataVis = new SimpleDateFormat("dd/MM/yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(pedido.getData()));
        } catch (Exception ignore) {
        }

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Entrada • " + pedido.getNome()));
        left.add(UiKit.hint("Data: " + dataVis + " • Status atual: " + pedido.getStatus()));
        header.add(left, BorderLayout.WEST);

        header.add(UiKit.hint("Edite apenas 'Qtd Recebida' e confirme."), BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        tm = new DefaultTableModel(new Object[] { "Produto", "Qtd Pedida", "Qtd Recebida", "Δ" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 2;
            }
        };

        table = new JTable(tm);
        UiKit.tableDefaults(table);

        TableColumnModel cm = table.getColumnModel();
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++)
            cm.getColumn(i).setCellRenderer(zebra);

        cm.getColumn(1).setMaxWidth(110);
        cm.getColumn(2).setMaxWidth(120);
        cm.getColumn(3).setMaxWidth(160);

        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));

        JScrollPane scroll = UiKit.scroll(table);
        scroll.setBorder(new LineBorder(new Color(0xEEF0F3), 1, true));
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Isso ajustará o estoque via service (como você já programou)."), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btCancel = UiKit.ghost("Cancelar");
        JButton btOk = UiKit.primary("Confirmar Entrada");
        actions.add(btCancel);
        actions.add(btOk);
        footer.add(actions, BorderLayout.EAST);

        btCancel.addActionListener(e -> dispose());
        btOk.addActionListener(e -> onConfirm());

        add(footer, BorderLayout.SOUTH);
    }

    private void loadData() {
        try {
            List<PedidoEstoqueProdutoModel> itens = pedProdDAO.listarPorPedido(pedido.getId());
            ProdutoDAO prodDAO = new ProdutoDAO();
            tm.setRowCount(0);

            for (PedidoEstoqueProdutoModel it : itens) {
                String nome = it.getProdutoId();
                ProdutoModel prod = prodDAO.findById(it.getProdutoId());
                if (prod != null)
                    nome = prod.getNome();

                int delta = it.getQuantidadeRecebida() - it.getQuantidadePedida();
                String deltaStr = (delta < 0) ? "Falta " + (-delta)
                        : (delta > 0) ? "Excesso +" + delta : "OK";

                tm.addRow(new Object[] { nome, it.getQuantidadePedida(), it.getQuantidadeRecebida(), deltaStr });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar itens:\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    // === sua lógica original, intacta ===
    private void onConfirm() {
        int opcao = JOptionPane.showConfirmDialog(this,
                "Confirma a entrada? Esta ação ajustará o estoque.",
                "Confirmar Entrada", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            System.out.println(">> [LOG] Entrada CANCELADA pelo usuário.");
            return;
        }

        try {
            if (table.isEditing()) {
                System.out.println(">> [LOG] Stop cell editing.");
                table.getCellEditor().stopCellEditing();
            }

            List<PedidoEstoqueProdutoModel> itens = pedProdDAO.listarPorPedido(pedido.getId());
            System.out.println(">> [LOG] Itens carregados: " + itens.size());

            Map<String, Integer> mapaRecebimento = new HashMap<>();
            for (int r = 0; r < tm.getRowCount(); r++) {
                String itemId = itens.get(r).getId();
                int qtdRecebidaUI = Integer.parseInt(tm.getValueAt(r, 2).toString());
                mapaRecebimento.put(itemId, qtdRecebidaUI);
                System.out.println(">> [LOG] Linha " + r + " | ItemID=" + itemId + " | Recebida(UI)=" + qtdRecebidaUI);
            }

            PedidoCompraService service = new PedidoCompraService();
            service.receberPedido(pedido.getId(), mapaRecebimento, "sistema");
            System.out.println(">> [LOG] PedidoCompraService.receberPedido() executado.");

            loadData();
            JOptionPane.showMessageDialog(this, "Entrada registrada com sucesso!");
            dispose();

        } catch (Exception ex) {
            System.err.println(">> [ERROR] ao registrar entrada: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao registrar entrada:\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
