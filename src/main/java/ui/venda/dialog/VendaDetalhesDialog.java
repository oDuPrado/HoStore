// src/ui/venda/dialog/VendaDetalhesDialog.java
package ui.venda.dialog;

import util.DB;
import util.AlertUtils;
import util.UiKit;

import java.awt.*;

import java.sql.Connection;
import javax.swing.table.TableCellRenderer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.VendaItemModel;
import model.ProdutoModel;
import dao.ProdutoDAO;

/**
 * Dialog que exibe detalhes completos de uma venda.
 */
public class VendaDetalhesDialog extends JDialog {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final java.util.List<VendaItemModel> itensDaVenda = new java.util.ArrayList<>();

    private static final Locale PTBR = new Locale("pt", "BR");
    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(PTBR);

    public VendaDetalhesDialog(Frame owner, int vendaId) {
        super(owner, "Detalhes da Venda #" + vendaId, true);

        UiKit.applyDialogBase(this);

        setSize(980, 720);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // =========================
        // Labels topo (preenchidos)
        // =========================
        JLabel lblDataVenda = new JLabel("-");
        JLabel lblCliente = new JLabel("-");
        JLabel lblFormaPagamento = new JLabel("-");
        JLabel lblParcelas = new JLabel("-");
        JLabel lblJuros = new JLabel("-");
        JLabel lblIntervalo = new JLabel("-");
        JLabel lblTotalBruto = new JLabel(BRL.format(0));
        JLabel lblDesconto = new JLabel(BRL.format(0));
        JLabel lblTotalLiquido = new JLabel(BRL.format(0));
        JLabel lblStatus = new JLabel("-");

        // Resumo financeiro (devoluções/estornos/efetivo)
        JLabel lblSomaDevolucoes = new JLabel(BRL.format(0));
        JLabel lblSomaEstornos = new JLabel(BRL.format(0));
        JLabel lblValorEfetivo = new JLabel(BRL.format(0));

        // =========================
        // Models das abas
        // =========================
        DefaultTableModel itensModel = new DefaultTableModel(
                new String[] { "Produto", "Tipo", "Qtd", "V.Unit.", "Desc (%)", "Total" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 2 -> Integer.class;
                    case 3, 4, 5 -> Double.class;
                    default -> String.class;
                };
            }
        };

        DefaultTableModel pagamentosModel = new DefaultTableModel(
                new String[] { "Forma", "Valor" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return (col == 1) ? Double.class : String.class;
            }
        };

        DefaultTableModel parcelasModel = new DefaultTableModel(
                new String[] { "Parcela", "Vencimento", "Valor" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return (col == 2) ? Double.class : String.class;
            }
        };

        // =========================
        // Dados carregados do banco
        // =========================
        double totalBruto = 0.0;
        double descontoV = 0.0;
        double totalLiquido = 0.0;
        String statusVenda = "";
        LocalDate dataVenda = null;
        int numParcelas = 0;
        double valorCartao = 0.0;
        double jurosPct = 0.0;
        int intervaloDias = 0;

        try (Connection c = DB.get()) {

            // 1) Dados básicos
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT v.data_venda, cl.nome AS cliente_nome, " +
                            "v.forma_pagamento, v.parcelas, v.juros, v.intervalo_dias, " +
                            "v.total_bruto, v.desconto, v.total_liquido, v.status " +
                            "FROM vendas v " +
                            "JOIN clientes cl ON v.cliente_id = cl.id " +
                            "WHERE v.id = ?")) {
                ps.setInt(1, vendaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dataVenda = LocalDate.parse(rs.getString("data_venda").substring(0, 10));
                        lblDataVenda.setText(dataVenda.format(BR));
                        lblCliente.setText(rs.getString("cliente_nome"));

                        lblFormaPagamento.setText(rs.getString("forma_pagamento"));
                        numParcelas = rs.getInt("parcelas");
                        lblParcelas.setText(numParcelas + "x");

                        jurosPct = rs.getDouble("juros");
                        lblJuros.setText(jurosPct + "%");

                        intervaloDias = rs.getInt("intervalo_dias");
                        lblIntervalo.setText(intervaloDias + " dias");

                        totalBruto = rs.getDouble("total_bruto");
                        lblTotalBruto.setText(BRL.format(totalBruto));

                        descontoV = rs.getDouble("desconto");
                        lblDesconto.setText(BRL.format(descontoV));

                        totalLiquido = rs.getDouble("total_liquido");
                        lblTotalLiquido.setText(BRL.format(totalLiquido));

                        statusVenda = rs.getString("status");
                        lblStatus.setText(statusVenda);
                    }
                }
            }

            // 2) Pagamentos + valor cartão
            try (PreparedStatement pp = c.prepareStatement(
                    "SELECT tipo, valor FROM vendas_pagamentos WHERE venda_id = ?")) {
                pp.setInt(1, vendaId);
                try (ResultSet rp = pp.executeQuery()) {
                    while (rp.next()) {
                        String tp = rp.getString("tipo");
                        double v = rp.getDouble("valor");
                        pagamentosModel.addRow(new Object[] { tp, v });
                        if ("CARTAO".equalsIgnoreCase(tp)) {
                            valorCartao = v;
                        }
                    }
                }
            }

            // 3) Parcelas (cronograma simples)
            if (dataVenda != null && numParcelas > 1 && valorCartao > 0 && intervaloDias > 0) {
                double valorParcela = valorCartao / numParcelas;
                LocalDate venc = dataVenda.plusDays(intervaloDias);
                for (int i = 1; i <= numParcelas; i++) {
                    parcelasModel.addRow(new Object[] {
                            i + "/" + numParcelas,
                            venc.format(BR),
                            valorParcela
                    });
                    venc = venc.plusDays(intervaloDias);
                }
            }

            // 4) Itens
            try (PreparedStatement pi = c.prepareStatement(
                    "SELECT vi.*, p.nome, p.tipo " +
                            "FROM vendas_itens vi " +
                            "JOIN produtos p ON vi.produto_id = p.id " +
                            "WHERE vi.venda_id = ?")) {
                pi.setInt(1, vendaId);
                try (ResultSet ri = pi.executeQuery()) {
                    while (ri.next()) {
                        int qtd = ri.getInt("qtd");
                        double preco = ri.getDouble("preco");
                        double desconto = ri.getDouble("desconto");
                        String nome = ri.getString("nome");
                        String tipo = ri.getString("tipo");
                        double totalIt = ri.getDouble("total_item");

                        itensModel.addRow(new Object[] { nome, tipo, qtd, preco, desconto, totalIt });

                        VendaItemModel it = new VendaItemModel();
                        it.setProdutoId(ri.getString("produto_id"));
                        it.setQtd(qtd);
                        it.setPreco(preco);
                        it.setDesconto(desconto);
                        itensDaVenda.add(it);
                    }
                }
            }

        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar detalhes:\n" + ex.getMessage());
        }

        // =========================
        // Resumo devoluções/estornos
        // =========================
        double somaDevolucoes = 0.0;
        double somaEstornos = 0.0;

        try (Connection c = DB.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(qtd * valor_unit), 0) AS total_dev " +
                            "FROM vendas_devolucoes WHERE venda_id = ?")) {
                ps.setInt(1, vendaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        somaDevolucoes = rs.getDouble("total_dev");
                }
            }

            try (PreparedStatement ps2 = c.prepareStatement(
                    "SELECT COALESCE(SUM(valor), 0) AS total_est " +
                            "FROM vendas_estornos_pagamentos WHERE venda_id = ?")) {
                ps2.setInt(1, vendaId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next())
                        somaEstornos = rs2.getDouble("total_est");
                }
            }
        } catch (Exception ignored) {
        }

        double valorEfetivo = totalLiquido - somaDevolucoes - somaEstornos;
        lblSomaDevolucoes.setText(BRL.format(somaDevolucoes));
        lblSomaEstornos.setText(BRL.format(somaEstornos));
        lblValorEfetivo.setText(BRL.format(valorEfetivo));

        // =========================
        // TOPO: Card com resumo
        // =========================
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topLeft.setOpaque(false);
        topLeft.add(UiKit.title("Detalhes da Venda #" + vendaId));
        topLeft.add(UiKit.hint("Resumo completo: itens, pagamentos, parcelas e estornos"));
        topCard.add(topLeft, BorderLayout.WEST);

        // Grid de info (mais bonito que GridLayout bruto no container principal)
        JPanel infoGrid = new JPanel(new GridBagLayout());
        infoGrid.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.anchor = GridBagConstraints.WEST;

        int r = 0;
        r = addKV(infoGrid, gc, r, "Data:", lblDataVenda, "Cliente:", lblCliente);
        r = addKV(infoGrid, gc, r, "Forma PG:", lblFormaPagamento, "Status:", lblStatus);
        r = addKV(infoGrid, gc, r, "Parcelas:", lblParcelas, "Intervalo:", lblIntervalo);
        r = addKV(infoGrid, gc, r, "Juros %:", lblJuros, "Total Bruto:", bold(lblTotalBruto));
        r = addKV(infoGrid, gc, r, "Desconto:", bold(lblDesconto), "Total Líquido:", bold(lblTotalLiquido));
        r = addKV(infoGrid, gc, r, "Devoluções:", bold(lblSomaDevolucoes), "Estornos:", bold(lblSomaEstornos));
        r = addKV(infoGrid, gc, r, "Valor Efetivo:", bold(lblValorEfetivo), "", new JLabel(""));

        topCard.add(infoGrid, BorderLayout.SOUTH);

        add(topCard, BorderLayout.NORTH);

        // =========================
        // CENTER: Abas (card)
        // =========================
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Itens", UiKit.scroll(buildTable(itensModel, true)));
        abas.addTab("Pagamentos", UiKit.scroll(buildTable(pagamentosModel, false)));
        abas.addTab("Parcelas", UiKit.scroll(buildTable(parcelasModel, false)));
        abas.addTab("Estornos", criarPainelEstornos(vendaId, statusVenda));

        centerCard.add(abas, BorderLayout.CENTER);
        add(centerCard, BorderLayout.CENTER);

        // =========================
        // FOOTER: botões (card)
        // =========================
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout());

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        bot.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("❌ Cancelar Venda");
        JButton btnEstornar = UiKit.ghost("💸 Estornar Pagamento");
        JButton btnDevolver = UiKit.primary("↩ Registrar Devolução");
        JButton fechar = UiKit.ghost("Fechar");

        fechar.addActionListener(e -> dispose());
        btnDevolver.addActionListener(e -> {
            new VendaDevolucaoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    vendaId,
                    itensDaVenda).setVisible(true);
        });
        btnCancelar.addActionListener(e -> cancelarVenda(vendaId));
        btnEstornar.addActionListener(e -> {
            new VendaEstornoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    vendaId,
                    itensDaVenda).setVisible(true);
        });

        bot.add(btnCancelar);
        bot.add(btnEstornar);
        bot.add(btnDevolver);
        bot.add(fechar);

        bottomCard.add(bot, BorderLayout.EAST);
        add(bottomCard, BorderLayout.SOUTH);
    }

    // =========================
    // Helpers UI
    // =========================
    private static JLabel bold(JLabel l) {
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static int addKV(JPanel panel, GridBagConstraints gc, int row,
            String k1, JComponent v1,
            String k2, JComponent v2) {
        gc.gridy = row;

        gc.gridx = 0;
        gc.weightx = 0;
        panel.add(new JLabel(k1), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panel.add(v1, gc);

        gc.gridx = 2;
        gc.weightx = 0;
        panel.add(new JLabel(k2), gc);
        gc.gridx = 3;
        gc.weightx = 1;
        panel.add(v2, gc);

        return row + 1;
    }

    private JTable buildTable(DefaultTableModel model, boolean itensTable) {
        JTable t = new JTable(model);
        UiKit.tableDefaults(t);

        // zebra em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // colunas monetárias
        if (itensTable) {
            // V.Unit, Desc (%), Total
            t.getColumnModel().getColumn(3).setCellRenderer(currencyZebra(zebra));
            t.getColumnModel().getColumn(5).setCellRenderer(currencyZebra(zebra));

            // qtd central
            DefaultTableCellRenderer centerZebra = centerZebra(zebra);
            t.getColumnModel().getColumn(2).setCellRenderer(centerZebra);
            t.getColumnModel().getColumn(4).setCellRenderer(centerZebra);
        } else {
            // última coluna geralmente é valor
            int last = t.getColumnCount() - 1;
            if (last >= 0) {
                t.getColumnModel().getColumn(last).setCellRenderer(currencyZebra(zebra));
            }
        }

        return t;
    }

    private static DefaultTableCellRenderer centerZebra(DefaultTableCellRenderer zebraBase) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        };
    }

    private static TableCellRenderer currencyZebra(DefaultTableCellRenderer zebraBase) {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            l.setText(cf.format(v));
            return l;
        };
    }

    // =========================
    // Estornos Tab
    // =========================
    private JScrollPane criarPainelEstornos(int vendaId, String statusVenda) {
        DefaultTableModel estornosModel = new DefaultTableModel(
                new String[] { "Tipo", "Detalhes", "Qtde/Valor", "Data", "Motivo" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable estornosTable = new JTable(estornosModel);
        UiKit.tableDefaults(estornosTable);

        // zebra
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < estornosTable.getColumnCount(); i++) {
            estornosTable.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // devoluções
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT produto_id, qtd, data, motivo FROM vendas_devolucoes WHERE venda_id = ?")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String prodId = rs.getString("produto_id");
                    int qtd = rs.getInt("qtd");
                    String data = rs.getString("data");
                    String motivo = rs.getString("motivo");

                    String nome;
                    try {
                        ProdutoModel pm = new ProdutoDAO().findById(prodId);
                        nome = (pm != null ? pm.getNome() : prodId);
                    } catch (Exception e) {
                        nome = prodId;
                    }

                    estornosModel.addRow(new Object[] { "Devolução", nome, qtd, data, motivo });
                }
            }
        } catch (Exception ignored) {
        }

        // estornos financeiros
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT pagamento_id, valor, data, motivo FROM vendas_estornos_pagamentos WHERE venda_id = ?")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idPag = rs.getInt("pagamento_id");
                    double valor = rs.getDouble("valor");
                    String data = rs.getString("data");
                    String motivo = rs.getString("motivo");

                    estornosModel.addRow(new Object[] {
                            "Estorno Pagamento",
                            "Pagamento ID: " + idPag,
                            BRL.format(valor),
                            data,
                            motivo
                    });
                }
            }
        } catch (Exception ignored) {
        }

        if ("cancelada".equalsIgnoreCase(statusVenda)) {
            estornosModel.addRow(new Object[] {
                    "Cancelamento",
                    "-",
                    "-",
                    LocalDate.now().format(BR),
                    "Venda cancelada"
            });
        }

        return UiKit.scroll(estornosTable);
    }

    // =========================
    // Cancelar venda
    // =========================
    private void cancelarVenda(int vendaId) {
        int confirma = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja cancelar esta venda?\nEssa ação não pode ser desfeita.",
                "Confirmar Cancelamento",
                JOptionPane.YES_NO_OPTION);

        if (confirma != JOptionPane.YES_OPTION)
            return;

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE vendas SET status = 'cancelada' WHERE id = ?")) {
                ps.setInt(1, vendaId);
                ps.executeUpdate();
            }

            int devolverEstoque = JOptionPane.showConfirmDialog(
                    this,
                    "Deseja devolver os itens ao estoque automaticamente?",
                    "Repor Estoque",
                    JOptionPane.YES_NO_OPTION);

            if (devolverEstoque == JOptionPane.YES_OPTION) {
                new dao.VendaDevolucaoDAO().registrarDevolucaoCompleta(vendaId, itensDaVenda, c);
            }

            c.commit();
            AlertUtils.info("Venda cancelada com sucesso.");
            dispose();

        } catch (Exception ex) {
            AlertUtils.error("Erro ao cancelar venda:\n" + ex.getMessage());
        }
    }
}
