package ui.comandas.dialog;

import dao.ProdutoDAO;
import model.ComandaItemModel;
import model.ComandaModel;
import model.ComandaPagamentoModel;
import model.ProdutoModel;
import model.VendaItemModel;
import service.ComandaService;
import service.SessaoService;
import ui.venda.dialog.VendaFinalizarDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import controller.VendaController;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class ComandaDetalhesDialog extends JDialog {

    private final int comandaId;
    private final ComandaService service = new ComandaService();

    private JLabel lblTopo = new JLabel("Carregando...");
    private JLabel lblTotais = new JLabel("");

    private final DefaultTableModel itensModel = new DefaultTableModel(
            new Object[] { "ItemID", "Produto", "Qtd", "Preço", "Desc", "Acr", "Total" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable itensTable = new JTable(itensModel);

    private final DefaultTableModel pagModel = new DefaultTableModel(
            new Object[] { "PgID", "Tipo", "Valor", "Data", "Usuário" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable pagTable = new JTable(pagModel);

    public ComandaDetalhesDialog(Window owner, int comandaId) {
        super(owner, "Comanda #" + comandaId, ModalityType.APPLICATION_MODAL);
        this.comandaId = comandaId;

        UiKit.applyDialogBase(this);

        setSize(980, 660);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        configurarTabelas();

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildCenterCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        bindKeys();

        carregarTudo();
    }

    /* ===================== VISUAL ===================== */

    private JComponent buildTopCard() {
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        // Esquerda: título + hint
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);

        lblTopo.setFont(lblTopo.getFont().deriveFont(Font.BOLD, 15f));
        left.add(lblTopo);
        left.add(UiKit.hint("Itens em cima | Pagamentos embaixo | Se fechar como venda, vira venda normal"));

        topCard.add(left, BorderLayout.WEST);

        // Direita: totais
        lblTotais.setFont(lblTotais.getFont().deriveFont(Font.BOLD, 13f));
        topCard.add(lblTotais, BorderLayout.EAST);

        return topCard;
    }

    private JComponent buildCenterCard() {
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.title("Detalhes"), BorderLayout.WEST);
        centerCard.add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                UiKit.scroll(itensTable),
                UiKit.scroll(pagTable));
        split.setResizeWeight(0.65);
        split.setBorder(null);
        split.setOpaque(false);

        centerCard.add(split, BorderLayout.CENTER);
        return centerCard;
    }

    private JComponent buildBottomCard() {
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        // Grupo esquerda (itens)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.setOpaque(false);

        JButton btnAddItem = UiKit.primary("➕ Item");
        btnAddItem.addActionListener(e -> {
            new ComandaItemDialog(this, comandaId).setVisible(true);
            carregarTudo();
        });

        JButton btnRemItem = UiKit.ghost("🗑 Remover Item");
        btnRemItem.addActionListener(e -> removerItemSelecionado());

        left.add(btnAddItem);
        left.add(btnRemItem);

        // Grupo direita (fechamento)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnPagamento = UiKit.primary("💳 Fechar como Venda");
        btnPagamento.addActionListener(e -> abrirPagamento());

        JButton btnFechar = UiKit.ghost("✅ Fechar");
        btnFechar.addActionListener(e -> fechar(false));

        JButton btnFecharPendente = UiKit.ghost("🕒 Fechar Pendente");
        btnFecharPendente.addActionListener(e -> fechar(true));

        JButton btnCancelar = UiKit.ghost("⛔ Cancelar");
        btnCancelar.addActionListener(e -> cancelarComanda());

        JButton btnSair = UiKit.ghost("Sair (ESC)");
        btnSair.addActionListener(e -> dispose());

        right.add(btnPagamento);
        right.add(btnFechar);
        right.add(btnFecharPendente);
        right.add(btnCancelar);
        right.add(btnSair);

        bottomCard.add(left, BorderLayout.WEST);
        bottomCard.add(right, BorderLayout.EAST);

        return bottomCard;
    }

    private void configurarTabelas() {
        UiKit.tableDefaults(itensTable);
        UiKit.tableDefaults(pagTable);

        // Zebra em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < itensTable.getColumnCount(); i++) {
            itensTable.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        for (int i = 0; i < pagTable.getColumnCount(); i++) {
            pagTable.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Alinhamento: Qtd e valores à direita (sem mudar conteúdo)
        DefaultTableCellRenderer zebraRight = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row,
                    int col) {
                JLabel l = (JLabel) zebra.getTableCellRendererComponent(t, v, sel, focus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        };

        // Itens: Qtd + colunas monetárias à direita
        itensTable.getColumnModel().getColumn(2).setCellRenderer(zebraRight);
        itensTable.getColumnModel().getColumn(3).setCellRenderer(zebraRight);
        itensTable.getColumnModel().getColumn(4).setCellRenderer(zebraRight);
        itensTable.getColumnModel().getColumn(5).setCellRenderer(zebraRight);
        itensTable.getColumnModel().getColumn(6).setCellRenderer(zebraRight);

        // Pagamentos: Valor à direita
        pagTable.getColumnModel().getColumn(2).setCellRenderer(zebraRight);

        // Ajuste de larguras (só estética)
        TableColumnModel it = itensTable.getColumnModel();
        it.getColumn(0).setPreferredWidth(60); // ItemID
        it.getColumn(1).setPreferredWidth(320); // Produto
        it.getColumn(2).setPreferredWidth(60); // Qtd
        it.getColumn(3).setPreferredWidth(90); // Preço
        it.getColumn(4).setPreferredWidth(90); // Desc
        it.getColumn(5).setPreferredWidth(90); // Acr
        it.getColumn(6).setPreferredWidth(90); // Total

        TableColumnModel pg = pagTable.getColumnModel();
        pg.getColumn(0).setPreferredWidth(60); // PgID
        pg.getColumn(1).setPreferredWidth(140); // Tipo
        pg.getColumn(2).setPreferredWidth(110); // Valor
        pg.getColumn(3).setPreferredWidth(190); // Data
        pg.getColumn(4).setPreferredWidth(180); // Usuário

        itensTable.setRowHeight(30);
        pagTable.setRowHeight(30);

        itensTable.setAutoCreateRowSorter(true);
        pagTable.setAutoCreateRowSorter(true);
    }

    private void bindKeys() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "sair");
        am.put("sair", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                carregarTudo();
            }
        });
    }

    /* ===================== LÓGICA (INTACTA) ===================== */

    private void carregarTudo() {
        try {
            ComandaModel c = service.getComanda(comandaId);
            if (c == null)
                throw new Exception("Comanda não encontrada.");

            String cliente = (c.getNomeCliente() != null && !c.getNomeCliente().isBlank())
                    ? c.getNomeCliente()
                    : (c.getClienteId() != null ? "ClienteID: " + c.getClienteId() : "—");

            lblTopo.setText("Comanda #" + comandaId + " | Cliente: " + cliente + " | Mesa: "
                    + (c.getMesa() == null ? "—" : c.getMesa())
                    + " | Status: " + c.getStatus()
                    + " | Tempo: " + formatarTempoHoras(c.getTempoPermanenciaMin()));

            lblTotais.setText("Total: " + money(c.getTotalLiquido())
                    + " | Pago: " + money(c.getTotalPago())
                    + " | Saldo: " + money(c.getSaldo()));

            itensModel.setRowCount(0);
            pagModel.setRowCount(0);

            try (var conn = util.DB.get()) {
                List<ComandaItemModel> itens = service.listarItens(comandaId, conn);
                List<ComandaPagamentoModel> pags = service.listarPagamentos(comandaId, conn);

                ProdutoDAO produtoDAO = new ProdutoDAO();

                for (ComandaItemModel it : itens) {
                    ProdutoModel p = produtoDAO.findById(it.getProdutoId());
                    String nomeProd = (p != null ? p.getNome() : it.getProdutoId());

                    itensModel.addRow(new Object[] {
                            it.getId(),
                            nomeProd,
                            it.getQtd(),
                            money(it.getPreco()),
                            money(it.getDesconto()),
                            money(it.getAcrescimo()),
                            money(it.getTotalItem())
                    });
                }

                for (ComandaPagamentoModel pg : pags) {
                    pagModel.addRow(new Object[] {
                            pg.getId(),
                            pg.getTipo(),
                            money(pg.getValor()),
                            (pg.getData() == null ? "—" : pg.getData().toString()),
                            (pg.getUsuario() == null ? "—" : pg.getUsuario())
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerItemSelecionado() {
        int row = itensTable.getSelectedRow();
        if (row < 0)
            return;

        int modelRow = itensTable.convertRowIndexToModel(row);
        int itemId = (int) itensModel.getValueAt(modelRow, 0);

        int ok = JOptionPane.showConfirmDialog(this,
                "Remover item " + itemId + " da comanda?",
                "Confirmar", JOptionPane.YES_NO_OPTION);

        if (ok != JOptionPane.YES_OPTION)
            return;

        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.removerItem(itemId, usuario);
            carregarTudo();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirPagamento() {
        try {
            ComandaModel c = service.getComanda(comandaId);
            if (c == null)
                return;

            if ("cancelada".equalsIgnoreCase(c.getStatus())) {
                JOptionPane.showMessageDialog(this,
                        "Comanda cancelada não pode ser fechada como venda.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";

            List<ComandaItemModel> itensComanda;
            try (var conn = util.DB.get()) {
                itensComanda = service.listarItens(comandaId, conn);
            }

            if (itensComanda == null || itensComanda.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Comanda sem itens.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            VendaController vendaController = new VendaController();

            for (ComandaItemModel ci : itensComanda) {
                VendaItemModel vi = new VendaItemModel();
                vi.setProdutoId(ci.getProdutoId());
                vi.setQtd(ci.getQtd());

                double precoUnit = ci.getPreco();

                int qtd = ci.getQtd();
                if (qtd > 0) {
                    double totalEfetivo = ci.getTotalItem();
                    double precoEfetivoUnit = totalEfetivo / qtd;
                    vi.setPreco(precoEfetivoUnit);
                } else {
                    vi.setPreco(precoUnit);
                }

                vi.setDesconto(0.0);
                vendaController.adicionarItem(vi);
            }

            String clienteId = (c.getClienteId() == null || c.getClienteId().isBlank()) ? "AVULSO" : c.getClienteId();

            VendaFinalizarDialog dlg = new VendaFinalizarDialog(
                    this,
                    vendaController,
                    clienteId,
                    null);
            dlg.setVisible(true);

            Integer vendaId = dlg.getVendaIdGerada();
            if (vendaId != null) {
                service.fecharComandaComVenda(comandaId, vendaId, usuario);
            }

            carregarTudo();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fechar(boolean pendente) {
        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.fecharComanda(comandaId, pendente, usuario);
            carregarTudo();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarComanda() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Cancelar a comanda #" + comandaId + "?\nIsso devolve estoque dos itens.",
                "Confirmar cancelamento", JOptionPane.YES_NO_OPTION);

        if (ok != JOptionPane.YES_OPTION)
            return;

        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.cancelarComanda(comandaId, usuario);
            carregarTudo();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String money(double v) {
        return String.format("R$ %.2f", v);
    }

    private static String formatarTempoHoras(Integer minutos) {
        if (minutos == null || minutos <= 0)
            return "—";
        int h = minutos / 60;
        int m = minutos % 60;
        return String.format("%dh%02d", h, m);
    }
}
