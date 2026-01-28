// src/ui/venda/dialog/VendaDetalhesDialog.java
package ui.venda.dialog;

import util.DB;
import util.AlertUtils;
import util.UiKit;

import java.awt.*;
import java.io.File;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.sql.Connection;
import javax.swing.table.TableCellRenderer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.VendaItemModel;
import model.ProdutoModel;
import model.PedidoCompraModel;
import dao.ProdutoDAO;
import dao.PedidoCompraDAO;
import dao.DocumentoFiscalDAO;
import dao.ConfigNfceDAO;
import model.ConfigNfceModel;
import model.DocumentoFiscalModel;
import service.DocumentoFiscalService;
import model.DocumentoFiscalAmbiente;
import ui.ajustes.dialog.ConfigNfceDialog;
import ui.estoque.dialog.ProdutosDoPedidoDialog;

/**
 * Dialog que exibe detalhes completos de uma venda.
 */
public class VendaDetalhesDialog extends JDialog {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final java.util.List<VendaItemModel> itensDaVenda = new java.util.ArrayList<>();
    private final int vendaId;
    private final java.util.Map<Integer, String> pedidoIdPorRow = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> loteTooltipPorRow = new java.util.HashMap<>();

    private static final Locale PTBR = new Locale("pt", "BR");
    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(PTBR);

    public VendaDetalhesDialog(Frame owner, int vendaId) {
        super(owner, "Detalhes da Venda #" + vendaId, true);
        this.vendaId = vendaId;

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
        JLabel lblNfce = new JLabel("-");

        // Resumo financeiro (devoluções/estornos/efetivo)
        JLabel lblSomaDevolucoes = new JLabel(BRL.format(0));
        JLabel lblSomaEstornos = new JLabel(BRL.format(0));
        JLabel lblValorEfetivo = new JLabel(BRL.format(0));

        // =========================
        // Models das abas
        // =========================
        DefaultTableModel itensModel = new DefaultTableModel(
                new String[] { "Produto", "Tipo", "Lote", "Qtd", "V.Unit.", "Preco Lote", "Desc (%)", "Promo", "Total" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 3 -> Integer.class;
                    case 4, 5, 6, 7 -> Double.class;
                    default -> String.class;
                };
            }
        };

        DefaultTableModel pagamentosModel = new DefaultTableModel(
                new String[] { "Forma", "Valor", "Bandeira", "Parcelas", "Taxa %", "Taxa Valor", "Taxa Quem" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return (col == 1 || col == 4 || col == 5) ? Double.class : String.class;
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
            boolean pagamentosOk = false;
            try (PreparedStatement pp = c.prepareStatement(
                    "SELECT tipo, valor, bandeira, parcelas, intervalo_dias, taxa_pct, taxa_valor, taxa_quem " +
                    "FROM vendas_pagamentos WHERE venda_id = ?")) {
                pp.setInt(1, vendaId);
                try (ResultSet rp = pp.executeQuery()) {
                    StringBuilder formasDet = new StringBuilder();
                    while (rp.next()) {
                        String tp = rp.getString("tipo");
                        double v = rp.getDouble("valor");
                        String bandeira = rp.getString("bandeira");
                        Integer parc = (Integer) rp.getObject("parcelas");
                        Integer intervalo = (Integer) rp.getObject("intervalo_dias");
                        Double taxaPct = (Double) rp.getObject("taxa_pct");
                        Double taxaValor = (Double) rp.getObject("taxa_valor");
                        String taxaQuem = rp.getString("taxa_quem");

                        pagamentosModel.addRow(new Object[] { tp, v, bandeira, parc, taxaPct, taxaValor, taxaQuem });
                        if (formasDet.length() > 0) formasDet.append(" | ");
                        formasDet.append(tp).append(" ").append(BRL.format(v));
                        if ("CARTAO".equalsIgnoreCase(tp)) {
                            valorCartao = v;
                            if (parc != null && parc > 0) {
                                numParcelas = parc;
                            }
                            if (intervalo != null && intervalo > 0) {
                                intervaloDias = intervalo;
                            }
                        }
                    }
                    if (formasDet.length() > 0) {
                        lblFormaPagamento.setText(formasDet.toString());
                    }
                    pagamentosOk = true;
                }
            } catch (SQLException ex) {
                pagamentosOk = false;
            }

            // fallback para bases antigas (sem colunas novas)
            if (!pagamentosOk) {
                try (PreparedStatement pp = c.prepareStatement(
                        "SELECT tipo, valor FROM vendas_pagamentos WHERE venda_id = ?")) {
                    pp.setInt(1, vendaId);
                    try (ResultSet rp = pp.executeQuery()) {
                        while (rp.next()) {
                            String tp = rp.getString("tipo");
                            double v = rp.getDouble("valor");
                            pagamentosModel.addRow(new Object[] { tp, v, null, null, null, null, null });
                        }
                    }
                }
            }

            // fallback legado: sem linhas em vendas_pagamentos
            if (pagamentosModel.getRowCount() == 0 && totalLiquido > 0) {
                String formaLegacy = lblFormaPagamento.getText();
                if (formaLegacy != null && !formaLegacy.isBlank() && !"-".equals(formaLegacy)) {
                    pagamentosModel.addRow(new Object[] { formaLegacy, totalLiquido, null, null, null, null, null });
                }
            }

            // 3) Parcelas (contas a receber)
            boolean parcelasCarregadas = false;
            try (PreparedStatement psParc = c.prepareStatement(
                    "SELECT p.numero_parcela, p.vencimento, p.valor_nominal, p.valor_juros, " +
                            "p.valor_acrescimo, p.valor_desconto, t.codigo_selecao " +
                            "FROM parcelas_contas_receber p " +
                            "JOIN titulos_contas_receber t ON t.id = p.titulo_id " +
                            "WHERE t.codigo_selecao IN (?, ?) " +
                            "ORDER BY t.codigo_selecao, p.numero_parcela")) {
                psParc.setString(1, "venda-" + vendaId);
                psParc.setString(2, "venda-" + vendaId + "-cartao");
                try (ResultSet rp = psParc.executeQuery()) {
                    while (rp.next()) {
                        int num = rp.getInt("numero_parcela");
                        String vencStr = rp.getString("vencimento");
                        double valor = rp.getDouble("valor_nominal")
                                + rp.getDouble("valor_juros")
                                + rp.getDouble("valor_acrescimo")
                                - rp.getDouble("valor_desconto");
                        String codigo = rp.getString("codigo_selecao");
                        String label = String.valueOf(num);
                        if (codigo != null && codigo.endsWith("-cartao")) {
                            label = "Cartão " + num;
                        }
                        if (vencStr != null && !vencStr.isBlank()) {
                            try {
                                vencStr = LocalDate.parse(vencStr).format(BR);
                            } catch (Exception ignored) {
                            }
                        }
                        parcelasModel.addRow(new Object[] { label, vencStr, valor });
                        parcelasCarregadas = true;
                    }
                }
            }

            // fallback: cronograma simples
            if (!parcelasCarregadas && dataVenda != null && numParcelas > 1 && valorCartao > 0 && intervaloDias > 0) {
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

            // 4) Itens (quebra por lote quando houver)
            java.util.Set<Integer> itensAdicionados = new java.util.HashSet<>();
            try (PreparedStatement pi = c.prepareStatement(
                    "SELECT vi.id AS venda_item_id, vi.*, p.nome, p.tipo, " +
                            "pr.nome AS promo_nome, " +
                            "vil.lote_id AS lote_id, vil.qtd AS qtd_lote, vil.custo_unit AS custo_lote, " +
                            "l.codigo_lote, l.preco_venda_unit AS preco_lote, l.origem AS origem_lote, l.legado AS lote_legado, l.data_entrada " +
                            "FROM vendas_itens vi " +
                            "JOIN produtos p ON vi.produto_id = p.id " +
                            "LEFT JOIN promocoes pr ON pr.id = vi.promocao_id " +
                            "LEFT JOIN vendas_itens_lotes vil ON vil.venda_item_id = vi.id " +
                            "LEFT JOIN estoque_lotes l ON l.id = vil.lote_id " +
                            "WHERE vi.venda_id = ? " +
                            "ORDER BY vi.id ASC, l.data_entrada ASC, l.id ASC")) {
                pi.setInt(1, vendaId);
                try (ResultSet ri = pi.executeQuery()) {
                    while (ri.next()) {
                        int qtdItem = ri.getInt("qtd");
                        double preco = ri.getDouble("preco");
                        double desconto = ri.getDouble("desconto");
                        String nome = ri.getString("nome");
                        String tipo = ri.getString("tipo");

                        Integer loteId = (Integer) ri.getObject("lote_id");
                        int qtdLote = (loteId != null) ? ri.getInt("qtd_lote") : qtdItem;
                        double precoLote = (ri.getObject("preco_lote") != null) ? ri.getDouble("preco_lote") : 0.0;

                        String codigoLote = ri.getString("codigo_lote");
                        String loteLabel = formatLoteLabel(codigoLote);
                        String pedidoId = parsePedidoIdFromCodigoLote(codigoLote);
                        if (pedidoId != null) {
                            loteLabel = formatPedidoLabel(pedidoId);
                        }

                        double totalIt = (preco * qtdLote) * (1 - desconto / 100.0);

                        int row = itensModel.getRowCount();
                        String promoNome = ri.getString("promo_nome");
                        String promoLabel = (promoNome != null && !promoNome.isBlank()) ? promoNome : "";
                        itensModel.addRow(new Object[] { nome, tipo, loteLabel, qtdLote, preco, precoLote, desconto, promoLabel, totalIt });
                        if (pedidoId != null) {
                            pedidoIdPorRow.put(row, pedidoId);
                        }
                        if (codigoLote != null && !codigoLote.isBlank()) {
                            loteTooltipPorRow.put(row, codigoLote);
                        }

                        Integer vendaItemId = (Integer) ri.getObject("venda_item_id");
                        if (vendaItemId != null && !itensAdicionados.contains(vendaItemId)) {
                            VendaItemModel it = new VendaItemModel();
                            it.setProdutoId(ri.getString("produto_id"));
                            it.setQtd(qtdItem);
                            it.setPreco(preco);
                            it.setDesconto(desconto);
                            itensDaVenda.add(it);
                            itensAdicionados.add(vendaItemId);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar detalhes:\n" + ex.getMessage());
        }

        // 5) NFC-e (documento fiscal vinculado)
        DocumentoFiscalModel docFiscal = null;
        try (Connection c = DB.get()) {
            docFiscal = new DocumentoFiscalDAO().buscarPorVenda(c, vendaId);
        } catch (Exception ignored) {
        }
        if (docFiscal != null) {
            String nfceLabel = docFiscal.serie + "/" + docFiscal.numero + " - " + docFiscal.status;
            lblNfce.setText(nfceLabel);
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
                    "SELECT COALESCE(SUM(valor_estornado), 0) AS total_est " +
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
        r = addKV(infoGrid, gc, r, "NFC-e:", lblNfce, "", new JLabel(""));
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
        abas.addTab("NFC-e", criarAbaNfce(docFiscal));
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

        // colunas monet?rias
        if (itensTable) {
            // V.Unit, Preco Lote, Total
            t.getColumnModel().getColumn(4).setCellRenderer(currencyZebra(zebra));
            t.getColumnModel().getColumn(5).setCellRenderer(currencyZebra(zebra));
            t.getColumnModel().getColumn(8).setCellRenderer(currencyZebra(zebra));

            // % desc
            t.getColumnModel().getColumn(6).setCellRenderer(percentZebra(zebra));

            // qtd central
            DefaultTableCellRenderer centerZebra = centerZebra(zebra);
            t.getColumnModel().getColumn(3).setCellRenderer(centerZebra);
            t.getColumnModel().getColumn(7).setCellRenderer(centerZebra);

            // lote link + tooltip
            t.getColumnModel().getColumn(2).setCellRenderer(loteLinkRenderer(zebra));
            t.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 1)
                        return;
                    int row = t.rowAtPoint(e.getPoint());
                    int col = t.columnAtPoint(e.getPoint());
                    if (row < 0 || col != 2)
                        return;
                    int modelRow = t.convertRowIndexToModel(row);
                    String pedidoId = pedidoIdPorRow.get(modelRow);
                    if (pedidoId == null || pedidoId.isBlank())
                        return;
                    abrirPedidoCompra(pedidoId);
                }
            });
        } else {
            for (int i = 0; i < t.getColumnCount(); i++) {
                String name = t.getColumnName(i).toLowerCase();
                if (name.contains("valor")) {
                    t.getColumnModel().getColumn(i).setCellRenderer(currencyZebra(zebra));
                } else if (name.contains("%")) {
                    t.getColumnModel().getColumn(i).setCellRenderer(percentZebra(zebra));
                } else if (name.contains("parcela")) {
                    t.getColumnModel().getColumn(i).setCellRenderer(centerZebra(zebra));
                }
            }
        }

        return t;
    }

    private TableCellRenderer loteLinkRenderer(DefaultTableCellRenderer zebraBase) {
        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            int modelRow = table.convertRowIndexToModel(row);
            String pedidoId = pedidoIdPorRow.get(modelRow);
            String tooltip = loteTooltipPorRow.get(modelRow);
            if (tooltip != null) {
                l.setToolTipText(tooltip);
            } else {
                l.setToolTipText(null);
            }
            if (pedidoId != null && !pedidoId.isBlank() && !isSelected) {
                l.setForeground(new Color(0, 102, 204));
                l.setText("<html><u>" + Objects.toString(value, "-") + "</u></html>");
            } else {
                l.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                l.setText(Objects.toString(value, "-"));
            }
            return l;
        };
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

    private static TableCellRenderer percentZebra(DefaultTableCellRenderer zebraBase) {
        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                    column);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            l.setText(String.format(new Locale("pt", "BR"), "%.2f%%", v));
            return l;
        };
    }

    private static String formatLoteLabel(String codigo) {
        if (codigo == null || codigo.isBlank())
            return "-";
        if ("MIGRACAO_INICIAL".equalsIgnoreCase(codigo))
            return "Migracao inicial";
        if (codigo.startsWith("PEDIDO_"))
            return codigo;
        if (codigo.length() <= 14)
            return codigo;
        return codigo.substring(0, 12) + "...";
    }

    private static String parsePedidoIdFromCodigoLote(String codigo) {
        if (codigo == null)
            return null;
        if (!codigo.startsWith("PEDIDO_"))
            return null;
        String rest = codigo.substring("PEDIDO_".length());
        int idx = rest.indexOf('_');
        if (idx <= 0)
            return null;
        return rest.substring(0, idx);
    }

    private static String formatPedidoLabel(String pedidoId) {
        if (pedidoId == null || pedidoId.isBlank())
            return "Pedido";
        String shortId = pedidoId.length() > 8 ? pedidoId.substring(0, 8) : pedidoId;
        return "Pedido #" + shortId;
    }

    private void abrirPedidoCompra(String pedidoId) {
        try {
            PedidoCompraModel pedido = new PedidoCompraDAO().buscarPorId(pedidoId);
            if (pedido == null) {
                AlertUtils.warn("Pedido nao encontrado: " + pedidoId);
                return;
            }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ProdutosDoPedidoDialog(owner, pedido).setVisible(true);
        } catch (Exception ex) {
            AlertUtils.error("Erro ao abrir pedido:\n" + ex.getMessage());
        }
    }

    // =========================
    // NFC-e Tab
    // =========================
    private JComponent criarAbaNfce(DocumentoFiscalModel doc) {
        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);

        if (doc == null) {
            JPanel top = new JPanel(new BorderLayout(8, 8));
            top.setOpaque(false);
            top.add(UiKit.hint("Nenhuma NFC-e vinculada a esta venda."), BorderLayout.NORTH);

            JButton btnGerar = UiKit.primary("Gerar NFC-e Offline");
            btnGerar.addActionListener(e -> gerarNfceOffline(btnGerar));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            actions.setOpaque(false);
            actions.add(btnGerar);
            top.add(actions, BorderLayout.WEST);

            wrap.add(top, BorderLayout.NORTH);
            return wrap;
        }

        JPanel info = UiKit.card();
        info.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.anchor = GridBagConstraints.WEST;

        JLabel lblNumero = new JLabel(doc.serie + "/" + doc.numero);
        JLabel lblStatus = new JLabel(doc.status != null ? doc.status : "-");
        JLabel lblAmb = new JLabel(doc.ambiente != null ? doc.ambiente : "-");
        JLabel lblChave = new JLabel(doc.chaveAcesso != null ? doc.chaveAcesso : "-");
        JLabel lblProtocolo = new JLabel(doc.protocolo != null ? doc.protocolo : "-");
        JLabel lblRecibo = new JLabel(doc.recibo != null ? doc.recibo : "-");
        JLabel lblCriado = new JLabel(doc.criadoEm != null ? doc.criadoEm : "-");
        JLabel lblAtualizado = new JLabel(doc.atualizadoEm != null ? doc.atualizadoEm : "-");

        int r = 0;
        r = addKV(info, gc, r, "Numero:", lblNumero, "Status:", lblStatus);
        r = addKV(info, gc, r, "Ambiente:", lblAmb, "Criado em:", lblCriado);
        r = addKV(info, gc, r, "Atualizado:", lblAtualizado, "Recibo:", lblRecibo);
        r = addKV(info, gc, r, "Protocolo:", lblProtocolo, "Chave:", lblChave);

        JPanel topStack = new JPanel(new BorderLayout(6, 6));
        topStack.setOpaque(false);
        topStack.add(info, BorderLayout.NORTH);

        String status = doc.status != null ? doc.status.trim().toUpperCase() : "-";
        JLabel lblResultado = new JLabel("Resultado SEFAZ: " + status);
        if ("AUTORIZADA".equals(status) || "APROVADA".equals(status)) {
            lblResultado.setForeground(new Color(0, 128, 0));
        } else if ("REJEITADA".equals(status) || "ERRO".equals(status)) {
            lblResultado.setForeground(new Color(170, 0, 0));
        }
        JPanel resultWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resultWrap.setOpaque(false);
        resultWrap.add(lblResultado);

        topStack.add(resultWrap, BorderLayout.SOUTH);
        wrap.add(topStack, BorderLayout.NORTH);

        String erro = doc.erro != null ? doc.erro.trim() : "";
        if (!erro.isEmpty()) {
            JTextArea taErro = new JTextArea(erro);
            taErro.setEditable(false);
            taErro.setLineWrap(true);
            taErro.setWrapStyleWord(true);
            taErro.setFont(new Font("SansSerif", Font.PLAIN, 12));

            JPanel erroCard = UiKit.card();
            erroCard.setLayout(new BorderLayout(6, 6));
            erroCard.add(new JLabel("Erro / RejeiÃ§Ã£o"), BorderLayout.NORTH);
            erroCard.add(UiKit.scroll(taErro), BorderLayout.CENTER);

            JPanel erroActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            erroActions.setOpaque(false);
            JButton btnAjustes = UiKit.ghost("Ajustes NFC-e");
            btnAjustes.addActionListener(e -> new ConfigNfceDialog((Frame) SwingUtilities.getWindowAncestor(this)).setVisible(true));
            erroActions.add(btnAjustes);
            erroCard.add(erroActions, BorderLayout.SOUTH);

            wrap.add(erroCard, BorderLayout.CENTER);
        } else {
            wrap.add(UiKit.hint("Nenhum erro registrado para esta NFC-e."), BorderLayout.CENTER);
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnGerar = UiKit.ghost("Gerar NFC-e Offline");
        btnGerar.setEnabled(doc.xmlPath == null || doc.xmlPath.isBlank());
        btnGerar.addActionListener(e -> gerarNfceOffline(btnGerar));
        actions.add(btnGerar);

        JButton btnEnviar = UiKit.primary("Enviar SEFAZ");
        btnEnviar.setEnabled(doc.xmlPath != null && !doc.xmlPath.isBlank());
        btnEnviar.addActionListener(e -> enviarSefaz(doc));
        actions.add(btnEnviar);

        JButton btnXml = UiKit.ghost("Abrir XML");
        btnXml.setEnabled(doc.xmlPath != null && !doc.xmlPath.isBlank());
        btnXml.addActionListener(e -> abrirXmlArquivo(doc));
        actions.add(btnXml);

        wrap.add(actions, BorderLayout.SOUTH);
        return wrap;
    }

        private void abrirXmlArquivo(DocumentoFiscalModel doc) {
        if (doc == null || doc.xmlPath == null || doc.xmlPath.isBlank()) {
            AlertUtils.warn("Documento ainda nao tem XML.");
            return;
        }
        try {
            File f = new File(doc.xmlPath);
            if (!f.exists()) {
                AlertUtils.warn("Arquivo XML nao encontrado:\n" + doc.xmlPath);
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            } else {
                AlertUtils.warn("Abertura de arquivo nao suportada neste ambiente.");
            }
        } catch (Exception e) {
            AlertUtils.error("Erro ao abrir XML:\n" + e.getMessage());
        }
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
                        "SELECT pagamento_id, valor_estornado, data, observacao, tipo_estorno, taxa_quem FROM vendas_estornos_pagamentos WHERE venda_id = ?")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idPag = rs.getInt("pagamento_id");
                    double valor = rs.getDouble("valor_estornado");
                    String data = rs.getString("data");
                    String motivo = rs.getString("observacao");
                    String tipoEstorno = rs.getString("tipo_estorno");
                    String taxaQuem = rs.getString("taxa_quem");

                    estornosModel.addRow(new Object[] {
                            "Estorno Pagamento",
                            "Pagamento ID: " + idPag
                                    + (tipoEstorno != null ? " | " + tipoEstorno : "")
                                    + (taxaQuem != null ? " | " + taxaQuem : ""),
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
                new service.EstornoService().estornarVendaCompleta(c, vendaId, itensDaVenda);
            }

            c.commit();
            AlertUtils.info("Venda cancelada com sucesso.");
            dispose();

        } catch (Exception ex) {
            AlertUtils.error("Erro ao cancelar venda:\n" + ex.getMessage());
        }
    }

    private void gerarNfceOffline(JButton btn) {
        if (btn != null) {
            btn.setEnabled(false);
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Exception last = null;
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        DocumentoFiscalService docService = new DocumentoFiscalService();
                        DocumentoFiscalModel doc = docService.criarDocumentoPendenteParaVenda(
                                vendaId, "sistema", DocumentoFiscalAmbiente.OFF);
                        docService.gerarXml(doc.id);
                        return null;
                    } catch (Exception ex) {
                        last = ex;
                        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
                        boolean busy = msg.contains("sqlite_busy_snapshot") || msg.contains("database is locked");
                        if (!busy || attempt == 3) {
                            throw ex;
                        }
                        try {
                            Thread.sleep(150L * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                if (last != null) throw last;
                return null;
            }

            @Override
            protected void done() {
                if (btn != null) {
                    btn.setEnabled(true);
                }
                try {
                    get();
                    AlertUtils.info("NFC-e offline gerada. Reabra os detalhes para visualizar.");
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    AlertUtils.error("Erro ao gerar NFC-e offline:\n" + msg);
                }
            }
        }.execute();
    }


    private void enviarSefaz(DocumentoFiscalModel doc) {
        try {
            ConfigNfceModel cfg = new ConfigNfceDAO().getConfig();
            if (cfg == null) {
                AlertUtils.warn("Configuração NFC-e não encontrada.");
                return;
            }
            String cert = cfg.getCertA1Path();
            String senha = cfg.getCertA1Senha();
            if (cert == null || cert.isBlank() || senha == null || senha.isBlank()) {
                AlertUtils.warn("Certificado A1 não configurado. Preencha em Ajustes > Loja/NFC-e.");
                return;
            }

            boolean producao = "PRODUCAO".equalsIgnoreCase(cfg.getAmbiente());
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DocumentoFiscalService svc = new DocumentoFiscalService();
                    svc.enviarSefaz(doc.id, cert, senha, producao);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        AlertUtils.info("Envio para SEFAZ concluído. Reabra os detalhes para ver o status.");
                    } catch (Exception e) {
                        AlertUtils.error("Erro ao enviar SEFAZ:\n" + e.getMessage());
                    }
                }
            }.execute();
        } catch (Exception ex) {
            AlertUtils.error("Erro ao enviar SEFAZ:\n" + ex.getMessage());
        }
    }

}
