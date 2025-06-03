package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import service.PedidoCompraService;
import service.ProdutoEstoqueService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog para registrar entrada (total ou parcial) de um pedido.
 * – Coluna Δ indica falta/excesso por item.
 * – Ajusta estoque + movimentação.
 * – Atualiza status DOS ITENS e DO PEDIDO automaticamente.
 */
public class EntradaPedidoDialog extends JDialog {

    private final PedidoCompraModel pedido;
    private final PedidoEstoqueProdutoDAO pedProdDAO = new PedidoEstoqueProdutoDAO();
    private final ProdutoEstoqueService prodSrv = new ProdutoEstoqueService();
    private final PedidoCompraDAO pedDAO = new PedidoCompraDAO();

    private JTable table;
    private DefaultTableModel tm;

    public EntradaPedidoDialog(Frame parent, PedidoCompraModel pedido) {
        super(parent, "Entrada de Pedido • " + pedido.getNome(), true);
        this.pedido = pedido;
        initComponents();
        loadData();
        pack();
        setLocationRelativeTo(parent);
    }

    /* ---------- UI ---------- */
    private void initComponents() {
        tm = new DefaultTableModel(
                new Object[] { "Produto", "Qtd Pedida", "Qtd Recebida", "Δ" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 2;
            } // só “Qtd Recebida”
        };

        table = new JTable(tm);
        JScrollPane scroll = new JScrollPane(table);

        JButton btOk = new JButton("Confirmar Entrada");
        btOk.addActionListener(e -> onConfirm());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(btOk, BorderLayout.SOUTH);
    }

    /* ---------- Carrega tabela ---------- */
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
                        : (delta > 0) ? "Excesso +" + delta
                                : "OK";

                tm.addRow(new Object[] {
                        nome,
                        it.getQuantidadePedida(),
                        it.getQuantidadeRecebida(),
                        deltaStr
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar itens:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /* ---------- Confirma entrada ---------- */
    /**
     * Confirma entrada de pedido COM LOGS detalhados para debugar estoque.
     */
    private void onConfirm() {
    // 1) Pergunta ao usuário se ele confirma a entrada
    int opcao = JOptionPane.showConfirmDialog(this,
            "Confirma a entrada? Esta ação ajustará o estoque.",
            "Confirmar Entrada", JOptionPane.YES_NO_OPTION);
    if (opcao != JOptionPane.YES_OPTION) {
        System.out.println(">> [LOG] Entrada CANCELADA pelo usuário.");
        return;
    }

    try {
        // 2) Se a tabela estiver em edição, finalize a célula reaproveitada
        if (table.isEditing()) {
            System.out.println(">> [LOG] Stop cell editing.");
            table.getCellEditor().stopCellEditing();
        }

        // 3) Carrega DA TABELA (só para saber quantas linhas existem; não usa os valores aqui)
        List<PedidoEstoqueProdutoModel> itens = pedProdDAO.listarPorPedido(pedido.getId());
        System.out.println(">> [LOG] Itens carregados: " + itens.size());

        // 4) Monta um Map<itemId, quantidadeRecebidaNova> com os valores que o usuário digitou
        Map<String, Integer> mapaRecebimento = new HashMap<>();
        for (int r = 0; r < tm.getRowCount(); r++) {
            String itemId = itens.get(r).getId(); 
            // A coluna 2 (índice 2) é "Qtd Recebida"
            int qtdRecebidaUI = Integer.parseInt(tm.getValueAt(r, 2).toString());
            mapaRecebimento.put(itemId, qtdRecebidaUI);
            System.out.println(">> [LOG] Linha " + r + " | ItemID=" + itemId
                    + " | Recebida(UI)=" + qtdRecebidaUI);
        }

        // 5) Chama o service refatorado, passando apenas o mapa de recebimento
        //    (Este método deve ser implementado em PedidoCompraService)
        PedidoCompraService service = new PedidoCompraService();
        service.receberPedido(pedido.getId(), mapaRecebimento, "sistema");
        System.out.println(">> [LOG] PedidoCompraService.receberPedido() executado.");

        // 6) Depois que o service ajustou estoque e atualizou status, recarrega a tabela
        loadData();
        System.out.println(">> [LOG] loadData() chamado para ressincronizar UI.");

        // 7) Feedback para o usuário e fechamento do diálogo
        JOptionPane.showMessageDialog(this, "Entrada registrada com sucesso!");
        dispose();
        System.out.println(">> [LOG] onConfirm() finalizado, diálogo fechado.");

    } catch (Exception ex) {
        System.err.println(">> [ERROR] ao registrar entrada: " + ex.getMessage());
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Erro ao registrar entrada:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}

}
