// src/ui/venda/dialog/VendaDetalhesDialog.java
package ui.venda.dialog;

import util.DB;
import util.AlertUtils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class VendaDetalhesDialog extends JDialog {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final java.util.List<model.VendaItemModel> itensDaVenda = new java.util.ArrayList<>();

    public VendaDetalhesDialog(Frame owner, int vendaId) {
        super(owner, "Detalhes da Venda #" + vendaId, true);
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // ─── Topo: informações gerais da venda ───────────────────────────────────
        JPanel topo = new JPanel(new GridLayout(0, 2, 4, 4));

        // modelos para as abas
        DefaultTableModel itensModel = new DefaultTableModel(
                new String[] { "Produto", "Tipo", "Qtd", "V.Unit.", "Desc (%)", "Total" }, 0);
        DefaultTableModel pagamentosModel = new DefaultTableModel(
                new String[] { "Forma", "Valor" }, 0);
        DefaultTableModel parcelasModel = new DefaultTableModel(
                new String[] { "Parcela", "Vencimento", "Valor" }, 0);

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
                        LocalDate data = LocalDate.parse(
                                rs.getString("data_venda").substring(0, 10));

                        topo.add(new JLabel("Data:"));
                        topo.add(new JLabel(data.format(BR)));
                        topo.add(new JLabel("Cliente:"));
                        topo.add(new JLabel(rs.getString("cliente_nome")));
                        topo.add(new JLabel("Forma PG:"));
                        topo.add(new JLabel(rs.getString("forma_pagamento")));
                        topo.add(new JLabel("Parcelas:"));
                        topo.add(new JLabel(rs.getInt("parcelas") + "x"));
                        topo.add(new JLabel("Juros %:"));
                        topo.add(new JLabel(rs.getDouble("juros") + "%"));
                        topo.add(new JLabel("Intervalo:"));
                        topo.add(new JLabel(rs.getInt("intervalo_dias") + " dias"));
                        topo.add(new JLabel("Total Bruto:"));
                        topo.add(new JLabel(
                                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                        .format(rs.getDouble("total_bruto"))));
                        topo.add(new JLabel("Desconto:"));
                        topo.add(new JLabel(
                                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                        .format(rs.getDouble("desconto"))));
                        topo.add(new JLabel("Total Líquido:"));
                        topo.add(new JLabel(
                                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                        .format(rs.getDouble("total_liquido"))));
                        topo.add(new JLabel("Status:"));
                        topo.add(new JLabel(rs.getString("status")));

                        // guarda para parcelas
                        int numParcelas = rs.getInt("parcelas");
                        double valorCartao = 0;
                        double jurosPct = rs.getDouble("juros");
                        int intervalo = rs.getInt("intervalo_dias");
                        LocalDate vendaDate = data;

                        // 2) Carrega pagamentos
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

                        // 3) Se houver cartão com mais de 1 parcela, monta cronograma
                        if (numParcelas > 1 && valorCartao > 0) {
                            double valorParcela = valorCartao / numParcelas;
                            LocalDate venc = vendaDate.plusDays(intervalo);
                            for (int i = 1; i <= numParcelas; i++) {
                                parcelasModel.addRow(new Object[] {
                                        i + "/" + numParcelas,
                                        venc.format(BR),
                                        NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                                                .format(valorParcela)
                                });
                                venc = venc.plusDays(intervalo);
                            }
                        }
                    }
                }
            }

            // 4) Carrega itens da venda (produtos genéricos)
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

                        // Preenche a tabela visual
                        itensModel.addRow(new Object[] {
                                ri.getString("nome"),
                                ri.getString("tipo"),
                                qtd,
                                preco,
                                desconto,
                                ri.getDouble("total_item")
                        });

                        // Preenche a lista de objetos para devolução
                        model.VendaItemModel it = new model.VendaItemModel();
                        it.setProdutoId(ri.getString("produto_id")); // importante para registrar devolução
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

        // ─── Abas centrais: Itens / Pagamentos / Parcelas ────────────────────────
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Itens", new JScrollPane(new JTable(itensModel)));
        abas.addTab("Pagamentos", new JScrollPane(new JTable(pagamentosModel)));
        abas.addTab("Parcelas", new JScrollPane(new JTable(parcelasModel)));

        add(topo, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);

        // ─── Rodapé: botão de fechar ─────────────────────────────────────────────
        JButton fechar = new JButton("Fechar");
        JButton btnDevolver = new JButton("Registrar Devolução");

        fechar.addActionListener(e -> dispose());
        btnDevolver.addActionListener(e -> {
            new VendaDevolucaoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    vendaId,
                    itensDaVenda).setVisible(true);
        });

        JButton btnCancelar = new JButton("❌ Cancelar Venda");

        btnCancelar.addActionListener(e -> cancelarVenda(vendaId));

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bot.add(btnCancelar);
        bot.add(btnDevolver);
        bot.add(fechar);

        add(bot, BorderLayout.SOUTH);

    }
    

    private void cancelarVenda(int vendaId) {
        int confirma = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja cancelar esta venda?\nEssa ação não pode ser desfeita.",
                "Confirmar Cancelamento",
                JOptionPane.YES_NO_OPTION);

        if (confirma != JOptionPane.YES_OPTION)
            return;

        try (Connection c = DB.get()) {
            c.setAutoCommit(false); // garante transação

            // 1) Marca como cancelada
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE vendas SET status = 'cancelada' WHERE id = ?")) {
                ps.setInt(1, vendaId);
                ps.executeUpdate();
            }

            // 2) Opcional: devolver estoque via EstoqueService
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
