// src/ui/PainelVendas.java
package ui.venda.painel;

import util.AlertUtils;
import util.DB;

import javax.swing.*;
import java.text.NumberFormat;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

import ui.venda.dialog.VendaDetalhesDialog;
import ui.venda.dialog.VendaNovaDialog;

import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PainelVendas extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final JLabel resumoDiaLbl = new JLabel("Total do período: R$ 0,00");
    private final JFrame owner;

    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BR_DATE  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PainelVendas(JFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        modelo = new DefaultTableModel(new String[]{
            "ID", "Data", "Cliente", "Total Líquido", "Forma PG", "Parcelas", "Status", "Detalhes"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 7; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 0) return Integer.class;
                if (col == 3) return Double.class;
                return String.class;
            }
        };

        tabela = new JTable(modelo);
        personalizarTabela();

        add(toolbarSuperior(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(rodape(), BorderLayout.SOUTH);

        carregarVendas(LocalDate.now().format(SQL_DATE));
    }

    private JComponent toolbarSuperior() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton novaVendaBtn = criarBotao("➕ Nova Venda");
        novaVendaBtn.addActionListener(e ->
            new VendaNovaDialog(owner, this).setVisible(true)
        );

        JComboBox<String> filtro = new JComboBox<>(new String[]{"Hoje", "Últimos 7 dias", "Todas"});
        filtro.addActionListener(e -> {
            switch (filtro.getSelectedIndex()) {
                case 0: carregarVendas(LocalDate.now().format(SQL_DATE)); break;
                case 1: carregarVendas(LocalDate.now().minusDays(7).format(SQL_DATE)); break;
                default: carregarVendas(null); break;
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

    public void carregarVendas(String dataMin) {
        modelo.setRowCount(0);
        double totalPeriodo = 0;
        try (Statement st = DB.get().createStatement();
             ResultSet rs = st.executeQuery(
                dataMin == null
                ? "SELECT v.*, c.nome AS cliente_nome FROM vendas v JOIN clientes c ON v.cliente_id = c.id ORDER BY v.id DESC"
                : "SELECT v.*, c.nome AS cliente_nome FROM vendas v JOIN clientes c ON v.cliente_id = c.id WHERE date(v.data_venda) >= '" + dataMin + "' ORDER BY v.id DESC"                
             )) {
            while (rs.next()) {
                int    id     = rs.getInt("id");
                String rawDt  = rs.getString("data_venda");
                String data = LocalDate.parse(rawDt.substring(0, 10)).format(BR_DATE);
                String cli = rs.getString("cliente_nome");
                double liquido= rs.getDouble("total_liquido");
                String forma  = rs.getString("forma_pagamento");
                int    parc   = rs.getInt("parcelas");
                String stat   = rs.getString("status");

                modelo.addRow(new Object[]{ id, data, cli, liquido, forma, parc, stat, "Detalhes" });
                if (dataMin != null) totalPeriodo += liquido;
            }
        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar vendas:\n" + ex.getMessage());
        }
        resumoDiaLbl.setText("Total do período: " +
            NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(totalPeriodo));
    }

    private void personalizarTabela() {
        // centraliza ID
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(0).setCellRenderer(center);

        // formata currency
        tabela.getColumnModel().getColumn(3).setCellRenderer((table, value, isSel, hasFocus, row, col) -> {
            JLabel l = new JLabel(
                NumberFormat.getCurrencyInstance(new Locale("pt","BR"))
                    .format((Double) value)
            );
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            return l;
        });

        // renderiza status
        tabela.getColumnModel().getColumn(6).setCellRenderer((table, value, isSel, hasFocus, row, col) -> {
            JLabel l = new JLabel(value.toString());
            l.setOpaque(true);
            l.setForeground(Color.WHITE);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            switch (value.toString()) {
                case "fechada":   l.setBackground(new Color(46,139,87)); break;
                case "estornada": l.setBackground(new Color(178,34,34)); break;
                default:          l.setBackground(new Color(184,134,11)); break;
            }
            return l;
        });

        // coluna de detalhes
        TableColumnModel tcm = tabela.getColumnModel();
        tcm.getColumn(7).setCellRenderer(new ButtonRenderer());
        tcm.getColumn(7).setCellEditor(new ButtonEditor());
    }

    private JButton criarBotao(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(60,63,65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    // Renderer para o botão "Detalhes"
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setText("Detalhes");
            setFocusPainted(false);
        }
        @Override public Component getTableCellRendererComponent(
            JTable tbl, Object val, boolean sel, boolean foc, int row, int col
        ) {
            return this;
        }
    }

    // Editor que abre o dialog de detalhes
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton btn = new JButton("Detalhes");
        private int           row;

        ButtonEditor() {
            super(new JCheckBox());
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                int vendaId = (int) modelo.getValueAt(row, 0);
                new VendaDetalhesDialog(owner, vendaId).setVisible(true);
                fireEditingStopped();
            });
        }

        @Override public Component getTableCellEditorComponent(
            JTable tbl, Object val, boolean sel, int row, int col
        ) {
            this.row = row;
            return btn;
        }
    }
}
