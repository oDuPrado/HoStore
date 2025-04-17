// src/ui/dialog/VendaDetalhesDialog.java
package ui.dialog;

import util.DB;
import util.DateUtils;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class VendaDetalhesDialog extends JDialog {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public VendaDetalhesDialog(Frame owner, int vendaId) {
        super(owner, "Detalhes da Venda #" + vendaId, true);
        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8,8));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        JPanel topo = new JPanel(new GridLayout(0,2,4,4));
        DefaultTableModel itensModel = new DefaultTableModel(new String[]{
            "Carta","Tipo","Subtipo","Raridade","Sub-Raridade","Ilustração","Qtd","R$ Unit.","Desc (%)","Total"
        }, 0);
        JTable itensTable = new JTable(itensModel);

        try (Connection c = DB.get()) {
            // Carrega dados da venda
            PreparedStatement sv = c.prepareStatement(
                "SELECT v.*, cl.nome AS cliente_nome " +
                "FROM vendas v " +
                "JOIN clientes cl ON v.cliente_id = cl.id " +
                "WHERE v.id = ?");
            sv.setInt(1, vendaId);
            try (ResultSet rv = sv.executeQuery()) {
                if (rv.next()) {
                    LocalDate dt = LocalDate.parse(rv.getString("data_venda").substring(0, 10));
                    topo.add(new JLabel("Data:"));           topo.add(new JLabel(dt.format(BR)));
                    topo.add(new JLabel("Cliente:"));        topo.add(new JLabel(rv.getString("cliente_nome")));
                    topo.add(new JLabel("Forma PG:"));       topo.add(new JLabel(rv.getString("forma_pagamento")));
                    topo.add(new JLabel("Parcelas:"));       topo.add(new JLabel(String.valueOf(rv.getInt("parcelas"))));
                    topo.add(new JLabel("Próx. Parcela:"));
                    topo.add(new JLabel(dt.plusMonths(1).format(BR)));
                    topo.add(new JLabel("Total Bruto:"));    topo.add(new JLabel("R$ "+rv.getDouble("total_bruto")));
                    topo.add(new JLabel("Desconto (R$):"));  topo.add(new JLabel("R$ "+rv.getDouble("desconto")));
                    topo.add(new JLabel("Total Líquido:"));  topo.add(new JLabel("R$ "+rv.getDouble("total_liquido")));
                    topo.add(new JLabel("Status:"));         topo.add(new JLabel(rv.getString("status")));
                }
            }

            // Carrega itens + classificações
            PreparedStatement si = c.prepareStatement(
                "SELECT vi.*, c.tipo_id, c.subtipo_id, c.raridade_id, c.sub_raridade_id, c.ilustracao_id, " +
                "       co.nome AS cond, li.nome AS ling, tc.nome AS tipo, stc.nome AS subtipo, ra.nome AS rar, sr.nome AS subrar, il.nome AS ilustr " +
                "FROM vendas_itens vi " +
                "JOIN cartas c ON vi.carta_id = c.id " +
                "LEFT JOIN condicoes co ON c.condicao_id = co.id " +
                "LEFT JOIN linguagens li ON c.linguagem_id = li.id " +
                "LEFT JOIN tipo_cartas tc ON c.tipo_id = tc.id " +
                "LEFT JOIN subtipo_cartas stc ON c.subtipo_id = stc.id " +
                "LEFT JOIN raridades ra ON c.raridade_id = ra.id " +
                "LEFT JOIN sub_raridades sr ON c.sub_raridade_id = sr.id " +
                "LEFT JOIN ilustracoes il ON c.ilustracao_id = il.id " +
                "WHERE vi.venda_id = ?");
            si.setInt(1, vendaId);
            try (ResultSet ri = si.executeQuery()) {
                while (ri.next()) {
                    double unit  = ri.getDouble("preco");
                    double descP = ri.getDouble("desconto");
                    int    qtd   = ri.getInt("qtd");
                    double tot   = ri.getDouble("total_item");

                    itensModel.addRow(new Object[]{
                      ri.getString("carta_id"),
                      ri.getString("tipo"),
                      ri.getString("subtipo"),
                      ri.getString("rar"),
                      ri.getString("subrar"),
                      ri.getString("ilustr"),
                      qtd,
                      unit,
                      descP,
                      tot
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar detalhes:\n" + ex.getMessage(),
                                          "Erro", JOptionPane.ERROR_MESSAGE);
        }

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(itensTable), BorderLayout.CENTER);
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bot.add(fechar);
        add(bot, BorderLayout.SOUTH);
    }
}
