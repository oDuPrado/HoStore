// src/ui/estoque/painel/PainelPedidosEstoque.java
package ui.estoque.painel;

import dao.PedidoCompraDAO;
import javax.swing.border.*;
import dao.PedidoEstoqueProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import ui.estoque.dialog.EntradaPedidoDialog;
import ui.estoque.dialog.ProdutosDoPedidoDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

public class PainelPedidosEstoque extends JDialog {

    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO pedProdDAO = new PedidoEstoqueProdutoDAO();
    private List<PedidoCompraModel> cache;

    private final JTextField tfFiltroNome = new JTextField(20);
    private final JComboBox<String> cbFiltroStatus = new JComboBox<>(
            new String[] { "Todos", "rascunho", "enviado", "parcialmente recebido", "recebido" });

    private final DefaultTableModel tm = new DefaultTableModel(
            new String[] { "ID", "Data", "Nome", "Status", "Δ" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 3;
        } // mantém sua lógica
    };

    private final JTable tabela = new JTable(tm);

    public PainelPedidosEstoque(Frame owner) {
        super(owner, "📄 Pedidos de Estoque", true);
        UiKit.applyDialogBase(this);
        initUI();
        loadData();
        setSize(920, 520);
        setLocationRelativeTo(owner);
        bindHotkeys();
    }

    private void bindHotkeys() {
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

    private void initUI() {
        setLayout(new BorderLayout(12, 12));

        // Header card
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 8));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Pedidos de Estoque"));
        left.add(UiKit.hint("F2 filtrar • F3 itens • F4 entrada • DEL excluir • ESC fechar"));
        header.add(left, BorderLayout.WEST);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Nome:"));
        filtros.add(tfFiltroNome);
        filtros.add(new JLabel("Status:"));
        filtros.add(cbFiltroStatus);
        JButton btFiltrar = UiKit.primary("Filtrar (F2)");
        btFiltrar.addActionListener(e -> loadData());
        filtros.add(btFiltrar);
        header.add(filtros, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Table card
        JPanel center = UiKit.card();
        center.setLayout(new BorderLayout(8, 8));

        UiKit.tableDefaults(tabela);

        // hide ID
        TableColumnModel cm = tabela.getColumnModel();
        cm.getColumn(0).setMinWidth(0);
        cm.getColumn(0).setMaxWidth(0);
        cm.getColumn(0).setPreferredWidth(0);

        // Status editor (mantém sua lógica)
        JComboBox<String> cb = new JComboBox<>(new String[] {
                "rascunho", "enviado", "parcialmente recebido", "recebido"
        });
        cm.getColumn(3).setCellEditor(new DefaultCellEditor(cb));

        // Renderers: zebra + badge status + delta central
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++)
            cm.getColumn(i).setCellRenderer(zebra);

        cm.getColumn(3).setCellRenderer(UiKit.badgeStatusRenderer());

        DefaultTableCellRenderer deltaCenter = new DefaultTableCellRenderer();
        deltaCenter.setHorizontalAlignment(SwingConstants.CENTER);
        deltaCenter.setBorder(new javax.swing.border.EmptyBorder(0, 8, 0, 8));
        cm.getColumn(4).setCellRenderer(deltaCenter);

        JScrollPane scroll = UiKit.scroll(tabela);
        center.add(scroll, BorderLayout.CENTER);

        center.add(scroll, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        // Footer actions card
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        leftActions.add(UiKit.hint("Dica: clique duplo abre itens"));
        footer.add(leftActions, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btProdutos = UiKit.ghost("🔍 Ver Itens (F3)");
        JButton btEntrada = UiKit.primary("📥 Entrada (F4)");
        JButton btExcluir = UiKit.ghost("🗑 Excluir (DEL)");
        JButton btFechar = UiKit.ghost("Fechar (ESC)");

        actions.add(btProdutos);
        actions.add(btEntrada);
        actions.add(btExcluir);
        actions.add(btFechar);

        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        btProdutos.addActionListener(e -> abrirProdutos());
        btEntrada.addActionListener(e -> abrirEntrada());
        btExcluir.addActionListener(e -> excluirPedido());
        btFechar.addActionListener(e -> dispose());

        // double click: abrir produtos (visual)
        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0)
                    abrirProdutos();
            }
        });

        // listener status inline (mantém)
        tm.addTableModelListener((TableModelEvent e) -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 3) {
                int row = e.getFirstRow();
                atualizarStatusPedido(tm.getValueAt(row, 0).toString(), tm.getValueAt(row, 3).toString());
            }
        });
    }

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

                int total = 0, rec = 0;
                for (PedidoEstoqueProdutoModel it : pedProdDAO.listarPorPedido(p.getId())) {
                    total += it.getQuantidadePedida();
                    rec += it.getQuantidadeRecebida();
                }
                int delta = rec - total;
                String deltaStr = (delta < 0) ? "Faltam " + (-delta)
                        : (delta > 0) ? "Excesso +" + delta : "OK";

                String dataVis = p.getData();
                try {
                    dataVis = br.format(iso.parse(p.getData()));
                } catch (Exception ignore) {
                }

                tm.addRow(new Object[] { p.getId(), dataVis, p.getNome(), p.getStatus(), deltaStr });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar:\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

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
                "Confirmar", JOptionPane.YES_NO_OPTION);

        if (resp != JOptionPane.YES_OPTION)
            return;

        // cuidado: índices mudam depois de deletar, então vamos pegar IDs primeiro
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int row : rows)
            ids.add(tm.getValueAt(row, 0).toString());

        for (String id : ids) {
            try {
                pedidoDAO.excluir(id);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir:\n" + e.getMessage());
            }
        }
        loadData();
    }

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
