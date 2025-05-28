package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import service.ProdutoEstoqueService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

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
        // 1) Confirmação do usuário
        int opcao = JOptionPane.showConfirmDialog(this,
                "Confirma a entrada? Esta ação ajustará o estoque.",
                "Confirmar Entrada", JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            System.out.println(">> [LOG] Entrada CANCELADA pelo usuário.");
            return;
        }

        try {
            // 2) Finaliza edição se estiver ativo
            if (table.isEditing()) {
                System.out.println(">> [LOG] Stop cell editing.");
                table.getCellEditor().stopCellEditing();
            }

            // 3) Carrega itens do pedido
            List<PedidoEstoqueProdutoModel> itens = pedProdDAO.listarPorPedido(pedido.getId());
            System.out.println(">> [LOG] Itens carregados: " + itens.size());

            // 4) Processa cada linha da tabela
            for (int r = 0; r < tm.getRowCount(); r++) {
                // 4.1) Lê valores da UI
                int qtdPedida = Integer.parseInt(tm.getValueAt(r, 1).toString());
                int qtdRecebidaUI = Integer.parseInt(tm.getValueAt(r, 2).toString());
                System.out.println(">> [LOG] Linha " + r +
                        " | Pedida=" + qtdPedida +
                        " | Recebida(UI)=" + qtdRecebidaUI);

                // 4.2) Busca o estado atual do item no banco
                String itemId = itens.get(r).getId();
                PedidoEstoqueProdutoModel itemDB = pedProdDAO.buscarPorId(itemId);
                int qtdRecebidaDB = itemDB.getQuantidadeRecebida();
                System.out.println(">> [LOG] ItemID=" + itemId +
                        " | Recebida(DB) antes=" + qtdRecebidaDB);

                // 4.3) Calcula delta REAL
                int delta = qtdRecebidaUI - qtdRecebidaDB;
                System.out.println(">> [LOG] Delta calculado = " + delta);

                // 4.4) Atualiza o item no pedido_produtos
                itemDB.setQuantidadeRecebida(qtdRecebidaUI);
                itemDB.setStatus((qtdRecebidaUI == 0) ? "pendente"
                        : (qtdRecebidaUI >= qtdPedida) ? "completo"
                                : "parcial");
                pedProdDAO.atualizar(itemDB);
                System.out.println(">> [LOG] Atualizado pedido_produtos: Recebida agora="
                        + itemDB.getQuantidadeRecebida() +
                        " | Status=" + itemDB.getStatus());

                // 4.5) Ajusta estoque conforme delta (positivo ou negativo)
                String prodId = itemDB.getProdutoId();
                if (delta != 0) {
                    ProdutoModel antes = new ProdutoDAO().findById(prodId);
                    System.out.println(">> [LOG] Antes ajuste: ProdutoID=" + prodId
                            + " | Estoque=" + antes.getQuantidade());

                    if (delta > 0) {
                        prodSrv.registrarEntrada(prodId, delta,
                                "Entrada de Pedido", "sistema");
                        System.out.println(">> [LOG] Entrada registrada: +" + delta);
                    } else {
                        prodSrv.registrarSaida(prodId, Math.abs(delta),
                                "Ajuste de Entrada de Pedido", "sistema");
                        System.out.println(">> [LOG] Saída registrada: " + delta);
                    }

                    ProdutoModel depois = new ProdutoDAO().findById(prodId);
                    System.out.println(">> [LOG] Depois ajuste: ProdutoID=" + prodId
                            + " | Estoque=" + depois.getQuantidade());
                } else {
                    System.out.println(">> [LOG] Delta zero, estoque inalterado.");
                }

                // 4.6) Atualiza coluna Δ na tabela
                int novoDelta = qtdRecebidaUI - qtdPedida;
                String deltaStr = (novoDelta < 0) ? "Falta " + (-novoDelta)
                        : (novoDelta > 0) ? "Excesso +" + novoDelta
                                : "OK";
                tm.setValueAt(deltaStr, r, 3);
                System.out.println(">> [LOG] Coluna Δ atualizada para '" + deltaStr + "'.");
            }

            // 5) Recalcula status do pedido
            pedDAO.recalcularStatus(pedido.getId());
            System.out.println(">> [LOG] pedDAO.recalcularStatus executado.");

            // 6) Sincroniza UI para evitar duplicação posterior
            loadData();
            System.out.println(">> [LOG] loadData() chamado para ressincronizar UI.");

            // 7) Finalização
            JOptionPane.showMessageDialog(this, "Entrada registrada!");
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
