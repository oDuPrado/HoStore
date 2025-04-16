package ui;

import ui.dialog.VendaNovaDialog;
import util.AlertUtils;
import util.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/** Lista de vendas com visão resumida do dia + filtros rápidos */
public class PainelVendas extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final JLabel resumoDiaLbl = new JLabel("Total do dia: R$ 0,00");

    private final JFrame owner;

    public PainelVendas(JFrame owner) {
        this.owner = owner;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        modelo = new DefaultTableModel(new String[]{
                "ID", "Data", "Cliente", "Total", "Forma PG", "Parcelas", "Status"
        }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tabela = new JTable(modelo);
        personalizarTabela();

        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(toolbarSuperior(), BorderLayout.NORTH);
        add(rodape(), BorderLayout.SOUTH);

        carregarVendas(LocalDate.now().toString());  // carrega vendas de hoje por padrão
    }

    /** Filtro de datas rápidas + botão nova venda */
    private JComponent toolbarSuperior() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton novaVendaBtn = criarBotao("➕ Nova Venda");
        novaVendaBtn.addActionListener(e -> new VendaNovaDialog(owner, this).setVisible(true));

        JComboBox<String> filtro = new JComboBox<>(new String[]{"Hoje", "Últimos 7 dias", "Todas"});
        filtro.addActionListener(e -> {
            switch (filtro.getSelectedIndex()) {
                case 0:
                    carregarVendas(LocalDate.now().toString());
                    break;
                case 1:
                    carregarVendas(LocalDate.now().minusDays(7).toString());
                    break;
                default:
                    carregarVendas(null);
                    break;
            }            
        });

        top.add(novaVendaBtn);
        top.add(new JLabel("Mostrar:"));
        top.add(filtro);
        return top;
    }

    private JLabel rodape() {
        resumoDiaLbl.setFont(resumoDiaLbl.getFont().deriveFont(Font.BOLD, 14f));
        return resumoDiaLbl;
    }

    /** Atualiza tabela e resumo do total */
    public void carregarVendas(String dataMin) {
        modelo.setRowCount(0);
        double totalDia = 0;
        try (Statement st = DB.get().createStatement();
             ResultSet rs = st.executeQuery(
                     dataMin == null
                             ? "SELECT * FROM vendas ORDER BY id DESC"
                             : "SELECT * FROM vendas WHERE date(data_venda) >= '" + dataMin + "' ORDER BY id DESC")) {

            while (rs.next()) {
                double total = rs.getDouble("total");
                modelo.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("data_venda"),
                        rs.getString("cliente_id"),
                        total,
                        rs.getString("forma_pagamento"),
                        rs.getInt("parcelas"),
                        rs.getString("status")
                });
                if (dataMin != null) totalDia += total;
            }
        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar vendas:\n" + ex.getMessage());
        }
        resumoDiaLbl.setText("Total do período: " +
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(totalDia));
    }

    /** Renderizador com cor por status + currency */
    private void personalizarTabela() {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        tabela.getColumnModel().getColumn(3).setCellRenderer((table, value, isSel, hasFoc, row, col) -> {
            JLabel l = new JLabel(NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                    .format((double) value));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            return l;
        });
        tabela.getColumnModel().getColumn(6).setCellRenderer((table, value, isSel, hasFoc, row, col) -> {
            JLabel l = new JLabel(value.toString());
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setOpaque(true);
            l.setForeground(Color.WHITE);
            switch (value.toString()) {
                case "fechada":
                    l.setBackground(new Color(46, 139, 87));   // verde
                    break;
                case "estornada":
                    l.setBackground(new Color(178, 34, 34));   // vermelho
                    break;
                default:
                    l.setBackground(new Color(184, 134, 11));  // dourado
                    break;
            }            
            return l;
        });
    }

    private JButton criarBotao(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}
