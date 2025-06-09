package ui.estoque.painel;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import ui.estoque.dialog.EntradaPedidoDialog;
import ui.estoque.dialog.ProdutosDoPedidoDialog;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Lista pedidos de estoque com filtros, Δ e ações.
 */
public class PainelPedidosEstoque extends JDialog {

    /* DAO */
    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO pedProdDAO = new PedidoEstoqueProdutoDAO();
    private List<PedidoCompraModel> cache;

    /* Filtros */
    private final JTextField tfFiltroNome = new JTextField(18);
    private final JComboBox<String> cbFiltroStatus = new JComboBox<>(
            new String[] { "Todos", "rascunho", "enviado", "parcialmente recebido", "recebido" });

    /* Tabela */
    private final DefaultTableModel tm = new DefaultTableModel(
            new String[] { "ID", "Nome", "Data", "Status", "Δ" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 3;
        } // só Status
    };
    private final JTable tabela = new JTable(tm);

    public PainelPedidosEstoque(Frame owner) {
        super(owner, "📄 Pedidos de Estoque", true);
        initUI();
        loadData();
        setSize(820, 430);
        setLocationRelativeTo(owner);
        // Atalhos de Teclado
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "filtrar");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "abrirProdutos");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "abrirEntrada");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluirPedido");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");

        am.put("filtrar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                loadData();
            }
        });
        am.put("abrirProdutos", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                abrirProdutos();
            }
        });
        am.put("abrirEntrada", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                abrirEntrada();
            }
        });
        am.put("excluirPedido", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                excluirPedido();
            }
        });
        am.put("fechar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    /* ---------- UI ---------- */
    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        /* topo filtros */
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filtros.add(new JLabel("Nome:"));
        filtros.add(tfFiltroNome);
        filtros.add(new JLabel("Status:"));
        filtros.add(cbFiltroStatus);
        JButton btFiltrar = new JButton("Filtrar");
        btFiltrar.addActionListener(e -> loadData());
        filtros.add(btFiltrar);
        add(filtros, BorderLayout.NORTH);

        /* tabela */
        tabela.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(tabela);
        tabela.getColumnModel().getColumn(0).setMinWidth(0);
        tabela.getColumnModel().getColumn(0).setMaxWidth(0);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(0);

        /* editor Status */
        JComboBox<String> cb = new JComboBox<>(new String[] {
                "rascunho", "enviado", "parcialmente recebido", "recebido" });
        tabela.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(cb));

        /* render Δ centralizado */
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(4).setCellRenderer(center);

        /* listener status inline */
        tm.addTableModelListener((TableModelEvent e) -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 3) {
                int row = e.getFirstRow();
                atualizarStatusPedido(tm.getValueAt(row, 0).toString(),
                        tm.getValueAt(row, 3).toString());
            }
        });

        add(scroll, BorderLayout.CENTER);

        /* rodapé botões */
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btProdutos = new JButton("🔍 Ver Produtos");
        JButton btEntrada = new JButton("📥 Entrada");
        JButton btExcluir = new JButton("🗑 Excluir");
        JButton btFechar = new JButton("Fechar");
        bot.add(btProdutos);
        bot.add(btEntrada);
        bot.add(btExcluir);
        bot.add(btFechar);
        add(bot, BorderLayout.SOUTH);

        btProdutos.addActionListener(e -> abrirProdutos());
        btEntrada.addActionListener(e -> abrirEntrada());
        btExcluir.addActionListener(e -> excluirPedido());
        btFechar.addActionListener(e -> dispose());
    }

    /* ---------- Carrega + Δ ---------- */
    private void loadData() {
        try {
            cache = pedidoDAO.listarTodos();
            String fNome = tfFiltroNome.getText().trim().toLowerCase();
            String fStatus = cbFiltroStatus.getSelectedItem().toString();

            tm.setRowCount(0);
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat br = new SimpleDateFormat("dd/MM/yyyy");

            for (PedidoCompraModel p : cache) {
                boolean okNome = fNome.isEmpty() || p.getNome().toLowerCase().contains(fNome);
                boolean okStatus = "Todos".equals(fStatus) || p.getStatus().equalsIgnoreCase(fStatus);
                if (!okNome || !okStatus)
                    continue;

                /* calcula faltas/excesso */
                int total = 0, rec = 0;
                for (PedidoEstoqueProdutoModel it : pedProdDAO.listarPorPedido(p.getId())) {
                    total += it.getQuantidadePedida();
                    rec += it.getQuantidadeRecebida();
                }
                int delta = rec - total;
                String deltaStr = (delta < 0) ? "Faltam " + (-delta)
                        : (delta > 0) ? "Excesso +" + delta
                                : "OK";

                String dataVis = p.getData();
                try {
                    dataVis = br.format(iso.parse(p.getData()));
                } catch (Exception ignore) {
                }

                tm.addRow(new Object[] { p.getId(), p.getNome(), dataVis, p.getStatus(), deltaStr });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ---------- Botões ---------- */
    private void abrirProdutos() {
        fila(P -> {
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            new ProdutosDoPedidoDialog(parent, P).setVisible(true);
        });
    }

    private void abrirEntrada() {
        fila(P -> {
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            new EntradaPedidoDialog(parent, P).setVisible(true);
        });
    }

    private void excluirPedido() {
        int[] rows = tabela.getSelectedRows();
        if (rows.length == 0) {
            msg();
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
                "Excluir os " + rows.length + " pedidos selecionados?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (resp != JOptionPane.YES_OPTION)
            return;

        for (int row : rows) {
            String id = tm.getValueAt(row, 0).toString();
            try {
                pedidoDAO.excluir(id);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir:\n" + e.getMessage());
            }
        }
        loadData();
    }

    /* ---------- Aux ---------- */
    private void fila(java.util.function.Consumer<PedidoCompraModel> action) {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            msg();
            return;
        }
        PedidoCompraModel p = buscar(tm.getValueAt(row, 0).toString());
        action.accept(p);
        loadData();
    }

    private void atualizarStatusPedido(String id, String st) {
        try {
            PedidoCompraModel p = buscar(id);
            p.setStatus(st);
            pedidoDAO.atualizar(p);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro:\n" + ex.getMessage());
        }
    }

    private PedidoCompraModel buscar(String id) {
        return cache.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
    }

    private void msg() {
        JOptionPane.showMessageDialog(this, "Selecione um pedido!", "Atenção", JOptionPane.WARNING_MESSAGE);
    }
}
