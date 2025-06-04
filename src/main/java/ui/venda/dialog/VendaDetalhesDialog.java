// src/ui/venda/dialog/VendaDetalhesDialog.java
package ui.venda.dialog;

import util.DB;
import util.AlertUtils;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import model.VendaItemModel;
import model.ProdutoModel;
import dao.ProdutoDAO;


/**
 * Dialog que exibe detalhes completos de uma venda, incluindo:
 *  - Resumo no topo: total bruto, desconto, total líquido, soma de devoluções,
 *    soma de estornos e valor efetivamente recebido.
 *  - Abas: Itens, Pagamentos, Parcelas e Estornos.
 *  - Aba "Estornos" mostra lista de devoluções, estornos de pagamento e cancelamento.
 */
public class VendaDetalhesDialog extends JDialog {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final java.util.List<VendaItemModel> itensDaVenda = new java.util.ArrayList<>();

    public VendaDetalhesDialog(Frame owner, int vendaId) {
        super(owner, "Detalhes da Venda #" + vendaId, true);
        setSize(900, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // ─── Topo: informações gerais e resumo financeiro ───────────────────────────────────
        JPanel topo = new JPanel(new GridLayout(0, 2, 4, 4));

        // Rótulos que serão preenchidos com valores do banco
        JLabel lblDataVenda        = new JLabel();
        JLabel lblCliente          = new JLabel();
        JLabel lblFormaPagamento   = new JLabel();
        JLabel lblParcelas         = new JLabel();
        JLabel lblJuros            = new JLabel();
        JLabel lblIntervalo        = new JLabel();
        JLabel lblTotalBruto       = new JLabel();
        JLabel lblDesconto         = new JLabel();
        JLabel lblTotalLiquido     = new JLabel();
        JLabel lblStatus           = new JLabel();

        // Resumo financeiro (novos campos)
        JLabel lblSomaDevolucoes   = new JLabel("R$ 0,00");
        JLabel lblSomaEstornos     = new JLabel("R$ 0,00");
        JLabel lblValorEfetivo     = new JLabel("R$ 0,00");

        // Preparação das tabelas para itens, pagamentos e parcelas
        DefaultTableModel itensModel = new DefaultTableModel(
                new String[] { "Produto", "Tipo", "Qtd", "V.Unit.", "Desc (%)", "Total" }, 0);
        DefaultTableModel pagamentosModel = new DefaultTableModel(
                new String[] { "Forma", "Valor" }, 0);
        DefaultTableModel parcelasModel = new DefaultTableModel(
                new String[] { "Parcela", "Vencimento", "Valor" }, 0);

        // Dados do banco
        double totalBruto   = 0.0;
        double descontoV    = 0.0;
        double totalLiquido = 0.0;
        String statusVenda  = "";
        LocalDate dataVenda = null;
        int numParcelas     = 0;
        double valorCartao  = 0.0;
        double jurosPct     = 0.0;
        int intervaloDias   = 0;

        try (Connection c = DB.get()) {
            // 1) Carrega dados básicos da venda
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT v.data_venda, cl.nome AS cliente_nome, " +
                            "       v.forma_pagamento, v.parcelas, v.juros, v.intervalo_dias, " +
                            "       v.total_bruto, v.desconto, v.total_liquido, v.status " +
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
                        lblTotalBruto.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                .format(totalBruto));
                        descontoV = rs.getDouble("desconto");
                        lblDesconto.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                .format(descontoV));
                        totalLiquido = rs.getDouble("total_liquido");
                        lblTotalLiquido.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                .format(totalLiquido));
                        statusVenda = rs.getString("status");
                        lblStatus.setText(statusVenda);
                    }
                }
            }

            // 2) Carrega pagamentos e calcula valor de cartão
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

            // 3) Se houver parcelamento de cartão, monta cronograma
            if (numParcelas > 1 && valorCartao > 0) {
                double valorParcela = valorCartao / numParcelas;
                LocalDate venc = dataVenda.plusDays(intervaloDias);
                for (int i = 1; i <= numParcelas; i++) {
                    parcelasModel.addRow(new Object[] {
                            i + "/" + numParcelas,
                            venc.format(BR),
                            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                    .format(valorParcela)
                    });
                    venc = venc.plusDays(intervaloDias);
                }
            }

            // 4) Carrega itens da venda
            try (PreparedStatement pi = c.prepareStatement(
                    "SELECT vi.*, p.nome, p.tipo " +
                            "FROM vendas_itens vi " +
                            "JOIN produtos p ON vi.produto_id = p.id " +
                            "WHERE vi.venda_id = ?")) {
                pi.setInt(1, vendaId);
                try (ResultSet ri = pi.executeQuery()) {
                    while (ri.next()) {
                        int    qtd      = ri.getInt("qtd");
                        double preco    = ri.getDouble("preco");
                        double desconto = ri.getDouble("desconto");
                        String nome     = ri.getString("nome");
                        String tipo     = ri.getString("tipo");
                        double totalIt  = ri.getDouble("total_item");

                        itensModel.addRow(new Object[] {
                                nome, tipo, qtd, preco, desconto, totalIt
                        });

                        // Monta modelo de VendaItemModel para uso em devolução/estorno
                        model.VendaItemModel it = new model.VendaItemModel();
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

        // ─── Calcula soma de devoluções e estornos para resumo ─────────────────────────────
        double somaDevolucoes = 0.0;
        double somaEstornos   = 0.0;

        try (Connection c = DB.get()) {
            // Soma devoluções (coluna qtd * valor_unit) de vendas_devolucoes
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT SUM(qtd * valor_unit) AS total_dev FROM vendas_devolucoes WHERE venda_id = ?")) {
                ps.setInt(1, vendaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        somaDevolucoes = rs.getDouble("total_dev");
                    }
                }
            }

            // Soma estornos (coluna valor) de vendas_estornos_pagamentos
            try (PreparedStatement ps2 = c.prepareStatement(
                    "SELECT SUM(valor) AS total_est FROM vendas_estornos_pagamentos WHERE venda_id = ?")) {
                ps2.setInt(1, vendaId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        somaEstornos = rs2.getDouble("total_est");
                    }
                }
            }
        } catch (Exception e) {
            // Se der erro, mantemos soma em 0.0
        }

        double valorEfetivo = totalLiquido - somaDevolucoes - somaEstornos;
        lblSomaDevolucoes.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(somaDevolucoes));
        lblSomaEstornos.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(somaEstornos));
        lblValorEfetivo.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(valorEfetivo));

        // ─── Monta painel topo com todas as informações ───────────────────────────────────
        topo.add(new JLabel("Data:"));           topo.add(lblDataVenda);
        topo.add(new JLabel("Cliente:"));        topo.add(lblCliente);
        topo.add(new JLabel("Forma PG:"));       topo.add(lblFormaPagamento);
        topo.add(new JLabel("Parcelas:"));       topo.add(lblParcelas);
        topo.add(new JLabel("Juros %:"));        topo.add(lblJuros);
        topo.add(new JLabel("Intervalo:"));      topo.add(lblIntervalo);
        topo.add(new JLabel("Total Bruto:"));    topo.add(lblTotalBruto);
        topo.add(new JLabel("Desconto:"));       topo.add(lblDesconto);
        topo.add(new JLabel("Total Líquido:"));  topo.add(lblTotalLiquido);
        topo.add(new JLabel("Status:"));         topo.add(lblStatus);

        // Espaço em branco para separar
        topo.add(new JLabel("")); topo.add(new JLabel(""));

        topo.add(new JLabel("Total Devoluções:")); topo.add(lblSomaDevolucoes);
        topo.add(new JLabel("Total Estornos:"));   topo.add(lblSomaEstornos);
        topo.add(new JLabel("Valor Efetivo:"));    topo.add(lblValorEfetivo);

        // ─── Abas centrais: Itens / Pagamentos / Parcelas / Estornos ──────────────────
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Itens", new JScrollPane(new JTable(itensModel)));
        abas.addTab("Pagamentos", new JScrollPane(new JTable(pagamentosModel)));
        abas.addTab("Parcelas", new JScrollPane(new JTable(parcelasModel)));
        abas.addTab("Estornos", criarPainelEstornos(vendaId, statusVenda));

        add(topo, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);

        // ─── Rodapé: botões de ação ─────────────────────────────────────────────
        JButton fechar      = new JButton("Fechar");
        JButton btnDevolver = new JButton("Registrar Devolução");
        JButton btnCancelar = new JButton("❌ Cancelar Venda");
        JButton btnEstornar = new JButton("💸 Estornar Pagamento");

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

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bot.add(btnCancelar);
        bot.add(btnEstornar);
        bot.add(btnDevolver);
        bot.add(fechar);
        add(bot, BorderLayout.SOUTH);
    }

    // ─── Cria painel "Estornos" ────────────────────────────────────────────────────
    private JScrollPane criarPainelEstornos(int vendaId, String statusVenda) {
        DefaultTableModel estornosModel = new DefaultTableModel(
                new String[]{ "Tipo", "Detalhes", "Qtde/Valor", "Data", "Motivo" }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable estornosTable = new JTable(estornosModel);
        estornosTable.setRowHeight(24);

        // Preencher linhas de devoluções
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT produto_id, qtd, data, motivo FROM vendas_devolucoes WHERE venda_id = ?")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String prodId = rs.getString("produto_id");
                    int qtd       = rs.getInt("qtd");
                    String data   = rs.getString("data");
                    String motivo = rs.getString("motivo");

                    // Busca nome do produto
                    String nome;
                    try {
                        ProdutoModel pm = new ProdutoDAO().findById(prodId);
                        nome = (pm != null ? pm.getNome() : prodId);
                    } catch (Exception e) {
                        nome = prodId;
                    }

                    estornosModel.addRow(new Object[]{
                            "Devolução",
                            nome,
                            qtd,
                            data,
                            motivo
                    });
                }
            }
        } catch (Exception ex) {
            // Ignorar se não houver devoluções
        }

        // Preencher linhas de estornos financeiros
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT pagamento_id, valor, data, motivo FROM vendas_estornos_pagamentos WHERE venda_id = ?")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idPag     = rs.getInt("pagamento_id");
                    double valor  = rs.getDouble("valor");
                    String data   = rs.getString("data");
                    String motivo = rs.getString("motivo");

                    estornosModel.addRow(new Object[]{
                            "Estorno Pagamento",
                            "Pagamento ID: " + idPag,
                            NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor),
                            data,
                            motivo
                    });
                }
            }
        } catch (Exception ex) {
            // Ignorar se não houver estornos
        }

        // Se status for "cancelada", adicionar linha de cancelamento
        if ("cancelada".equalsIgnoreCase(statusVenda)) {
            estornosModel.addRow(new Object[]{
                    "Cancelamento",
                    "-",
                    "-",
                    LocalDate.now().format(BR),
                    "Venda cancelada"
            });
        }

        return new JScrollPane(estornosTable);
    }

    // ─── Método para cancelar venda ────────────────────────────────────────────────
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

            // 1) Marca como cancelada
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE vendas SET status = 'cancelada' WHERE id = ?")) {
                ps.setInt(1, vendaId);
                ps.executeUpdate();
            }

            // 2) Opcional: devolver estoque via VendaDevolucaoDAO
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
