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

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import controller.VendaController;

import java.awt.*;
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

        setSize(900, 600);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        itensTable.setRowHeight(26);
        pagTable.setRowHeight(26);

        JPanel top = new JPanel(new BorderLayout());
        lblTopo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        top.add(lblTopo, BorderLayout.WEST);
        top.add(lblTotais, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(itensTable),
                new JScrollPane(pagTable));
        split.setResizeWeight(0.65);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        carregarTudo();
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton btnAddItem = new JButton("➕ Item");
        btnAddItem.addActionListener(e -> {
            new ComandaItemDialog(this, comandaId).setVisible(true);
            carregarTudo();
        });

        JButton btnRemItem = new JButton("🗑 Remover Item");
        btnRemItem.addActionListener(e -> removerItemSelecionado());

        JButton btnFechar = new JButton("✅ Fechar");
        btnFechar.addActionListener(e -> fechar(false));

        JButton btnFecharPendente = new JButton("🕒 Fechar Pendente");
        btnFecharPendente.addActionListener(e -> fechar(true));

        JButton btnPagamento = new JButton("💳 Fechar Comanda");
        btnPagamento.addActionListener(e -> abrirPagamento());

        JButton btnCancelar = new JButton("⛔ Cancelar");
        btnCancelar.addActionListener(e -> cancelarComanda());

        JButton btnSair = new JButton("Sair");
        btnSair.addActionListener(e -> dispose());

        p.add(btnAddItem);
        p.add(btnRemItem);
        p.add(btnPagamento);
        p.add(btnFechar);
        p.add(btnFecharPendente);
        p.add(btnCancelar);
        p.add(btnSair);
        return p;
    }

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
                    + " | Status: " + c.getStatus());

            lblTotais.setText("Total: " + money(c.getTotalLiquido()) + " | Pago: " + money(c.getTotalPago())
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

        int itemId = (int) itensModel.getValueAt(row, 0);

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
                JOptionPane.showMessageDialog(this, "Comanda cancelada não pode ser fechada como venda.", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";

            // 1) Pega itens da comanda (usando o método REAL: listarItens(comandaId, conn))
            List<ComandaItemModel> itensComanda;
            try (var conn = util.DB.get()) {
                itensComanda = service.listarItens(comandaId, conn);
            }

            if (itensComanda == null || itensComanda.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Comanda sem itens.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2) Monta controller de venda com carrinho carregado
            VendaController vendaController = new VendaController();

            for (ComandaItemModel ci : itensComanda) {
                VendaItemModel vi = new VendaItemModel();
                vi.setProdutoId(ci.getProdutoId());
                vi.setQtd(ci.getQtd());

                // Preço base do item (unitário)
                double precoUnit = ci.getPreco();

                // Se a comanda usa desconto/acréscimo como VALOR (e não %), convertemos para
                // efetivo.
                // Total item = qtd*preco - desconto + acrescimo.
                // Então preço efetivo unitário = (total_item / qtd).
                int qtd = ci.getQtd();
                if (qtd > 0) {
                    double totalEfetivo = ci.getTotalItem();
                    double precoEfetivoUnit = totalEfetivo / qtd;
                    vi.setPreco(precoEfetivoUnit);
                } else {
                    vi.setPreco(precoUnit);
                }

                // VendaFinalizarDialog trata desconto como %, então aqui não faz sentido jogar
                // valor.
                // Como a comanda já “embutiu” desconto/acréscimo no total efetivo, deixamos % =
                // 0.
                vi.setDesconto(0.0);

                vendaController.adicionarItem(vi);
            }

            // 3) Cliente da comanda (fallback AVULSO)
            String clienteId = (c.getClienteId() == null || c.getClienteId().isBlank()) ? "AVULSO" : c.getClienteId();

            // 4) Abre finalizar venda (painelPai = null)
            VendaFinalizarDialog dlg = new VendaFinalizarDialog(
                    this,
                    vendaController,
                    clienteId,
                    null);
            dlg.setVisible(true);

            // 5) Se gerou venda, fecha comanda e vincula venda
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
}
