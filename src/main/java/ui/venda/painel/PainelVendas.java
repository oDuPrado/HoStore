// src/ui/venda/painel/PainelVendas.java
package ui.venda.painel;

import com.toedter.calendar.JDateChooser;
import dao.ClienteDAO;
import model.ClienteModel;
import ui.venda.dialog.VendaDetalhesDialog;
import ui.venda.dialog.VendaNovaDialog;
import util.AlertUtils;
import util.DB;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class PainelVendas extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;

    private final JDateChooser inicioChooser = new JDateChooser();
    private final JDateChooser fimChooser = new JDateChooser();
    private final JComboBox<String> clienteCombo = new JComboBox<>();
    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[] {
                    "Todos", "Fechada", "Pendente", "Cancelada",
                    "Estornada", "Devolvida", "Parcialmente Devolvida"
            });

    private final JLabel resumoLbl = new JLabel(" ");

    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JFrame owner;

    public PainelVendas(JFrame owner) {
        this.owner = owner;

        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        modelo = new DefaultTableModel(new String[] {
                "ID", "Data", "Cliente", "Total Líquido", "Forma PG", "Parcelas", "Status", ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 7;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0)
                    return Integer.class;
                if (col == 3)
                    return Double.class;
                if (col == 5)
                    return Integer.class;
                return String.class;
            }
        };

        tabela = new JTable(modelo);
        tabela.setRowHeight(34);
        tabela.setFillsViewportHeight(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setShowHorizontalLines(true);
        tabela.setShowVerticalLines(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));

        add(criarHeaderEFiltros(), BorderLayout.NORTH);

        JScrollPane sp = UiKit.scroll(tabela);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        add(criarRodape(), BorderLayout.SOUTH);

        personalizarTabela();
        configurarEventos();

        carregarClientes();

        inicioChooser.setDateFormatString("dd/MM/yyyy");
        fimChooser.setDateFormatString("dd/MM/yyyy");

        String hoje = LocalDate.now().format(SQL_DATE);
        carregarVendas(hoje, hoje, "Todos", "Todos");
    }

    // =========================
    // Header + Filtros (bonito e consistente)
    // =========================
    private JComponent criarHeaderEFiltros() {
        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);

        // Header (título + dica)
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Vendas"));
        left.add(UiKit.hint("Filtre por período, cliente e status. Duplo-clique abre detalhes."));
        header.add(left, BorderLayout.WEST);

        JButton btnNova = UiKit.primary("➕ Nova Venda");
        btnNova.addActionListener(e -> new VendaNovaDialog(owner, this).setVisible(true));
        header.add(btnNova, BorderLayout.EAST);

        wrap.add(header, BorderLayout.NORTH);

        // Card de filtros
        JPanel filtros = UiKit.card();
        filtros.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;

        // linha 0
        g.gridx = 0;
        g.weightx = 0;
        filtros.add(new JLabel("De:"), g);
        g.gridx = 1;
        g.weightx = 0.3;
        filtros.add(inicioChooser, g);

        g.gridx = 2;
        g.weightx = 0;
        filtros.add(new JLabel("Até:"), g);
        g.gridx = 3;
        g.weightx = 0.3;
        filtros.add(fimChooser, g);

        g.gridx = 4;
        g.weightx = 0;
        filtros.add(new JLabel("Status:"), g);
        g.gridx = 5;
        g.weightx = 0.4;
        filtros.add(statusCombo, g);

        // linha 1
        g.gridy = 1;
        g.gridx = 0;
        g.weightx = 0;
        filtros.add(new JLabel("Cliente:"), g);

        clienteCombo.setEditable(true);
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 1.0;
        filtros.add(clienteCombo, g);

        g.gridwidth = 1;
        JButton filtrar = UiKit.ghost("🔍 Filtrar");
        filtrar.addActionListener(e -> {
            String d1 = formatarDataParaSQL(inicioChooser);
            String d2 = formatarDataParaSQL(fimChooser);
            String cli = (String) clienteCombo.getSelectedItem();
            String stat = (String) statusCombo.getSelectedItem();
            carregarVendas(d1, d2, cli, stat);
        });

        JButton limpar = UiKit.ghost("🧹 Limpar");
        limpar.addActionListener(e -> {
            inicioChooser.setDate(null);
            fimChooser.setDate(null);
            clienteCombo.setSelectedItem("Todos");
            statusCombo.setSelectedItem("Todos");
            carregarVendas("", "", "Todos", "Todos");
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(limpar);
        actions.add(filtrar);

        g.gridx = 4;
        g.weightx = 0;
        filtros.add(new JLabel(""), g);
        g.gridx = 5;
        g.weightx = 0;
        filtros.add(actions, g);

        wrap.add(filtros, BorderLayout.CENTER);
        return wrap;
    }

    // =========================
    // Rodapé consistente (resumo + ações)
    // =========================
    private JComponent criarRodape() {
        JPanel rodape = UiKit.card();
        rodape.setLayout(new BorderLayout(12, 0));

        resumoLbl.setFont(resumoLbl.getFont().deriveFont(Font.BOLD, 13f));
        rodape.add(resumoLbl, BorderLayout.WEST);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);

        JButton btnDetalhes = UiKit.ghost("🔎 Abrir Detalhes");
        btnDetalhes.addActionListener(e -> abrirDetalhesSelecionado());

        JButton btnExcluir = UiKit.ghost("🗑️ Excluir");
        btnExcluir.addActionListener(e -> excluirSelecionado());

        botoes.add(btnDetalhes);
        botoes.add(btnExcluir);

        rodape.add(botoes, BorderLayout.EAST);
        return rodape;
    }

    private void abrirDetalhesSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            AlertUtils.info("Selecione uma venda.");
            return;
        }
        int modelRow = tabela.convertRowIndexToModel(row);
        int id = (int) modelo.getValueAt(modelRow, 0);
        new VendaDetalhesDialog(owner, id).setVisible(true);
    }

    private void excluirSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            AlertUtils.info("Selecione uma venda para excluir.");
            return;
        }
        int modelRow = tabela.convertRowIndexToModel(row);
        int id = (int) modelo.getValueAt(modelRow, 0);

        int op = JOptionPane.showConfirmDialog(owner,
                "Confirma exclusão da venda ID " + id + "?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (op == JOptionPane.YES_OPTION) {
            excluirVenda(id);
            String d1 = formatarDataParaSQL(inicioChooser);
            String d2 = formatarDataParaSQL(fimChooser);
            carregarVendas(d1, d2,
                    (String) clienteCombo.getSelectedItem(),
                    (String) statusCombo.getSelectedItem());
        }
    }

    // =========================
    // Eventos (duplo clique + enter)
    // =========================
    private void configurarEventos() {
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    abrirDetalhesSelecionado();
            }
        });

        tabela.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    abrirDetalhesSelecionado();
                }
            }
        });
    }

    // =========================
    // Estilo tabela (alinhamento, moeda, badge status, coluna ações)
    // =========================
    private void personalizarTabela() {
        tabela.setAutoCreateRowSorter(true);

        TableColumnModel tcm = tabela.getColumnModel();

        // larguras
        tcm.getColumn(0).setPreferredWidth(60); // ID
        tcm.getColumn(1).setPreferredWidth(90); // Data
        tcm.getColumn(2).setPreferredWidth(260); // Cliente
        tcm.getColumn(3).setPreferredWidth(120); // Total
        tcm.getColumn(4).setPreferredWidth(110); // PG
        tcm.getColumn(5).setPreferredWidth(80); // Parcelas
        tcm.getColumn(6).setPreferredWidth(160); // Status
        tcm.getColumn(7).setPreferredWidth(60); // botão

        // ID/parcelas central
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tcm.getColumn(0).setCellRenderer(center);
        tcm.getColumn(1).setCellRenderer(center);
        tcm.getColumn(5).setCellRenderer(center);

        // moeda alinhada direita
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        DefaultTableCellRenderer money = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Number)
                    setText(cf.format(((Number) value).doubleValue()));
                else
                    setText(value != null ? value.toString() : "");
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };
        tcm.getColumn(3).setCellRenderer(money);

        // status como “badge”
        tcm.getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        // botão ação (coluna vazia "")
        tcm.getColumn(7).setCellRenderer(new ButtonRenderer());
        tcm.getColumn(7).setCellEditor(new ButtonEditor());
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String s = value == null ? "" : value.toString().toLowerCase();

            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            l.setOpaque(true);

            // mantém seleção do tema
            if (isSelected) {
                l.setBackground(table.getSelectionBackground());
                l.setForeground(table.getSelectionForeground());
                return l;
            }

            // cores (sim, badges precisam de cor)
            l.setForeground(Color.WHITE);

            switch (s) {
                case "fechada" -> l.setBackground(new Color(46, 139, 87));
                case "pendente" -> l.setBackground(new Color(70, 130, 180));
                case "cancelada" -> l.setBackground(new Color(184, 134, 11));
                case "estornada" -> l.setBackground(new Color(178, 34, 34));
                case "devolvida" -> l.setBackground(new Color(128, 0, 128));
                case "parcialmente devolvida" -> l.setBackground(new Color(123, 63, 0));
                default -> {
                    l.setBackground(new Color(120, 120, 120));
                    l.setForeground(Color.WHITE);
                }
            }
            return l;
        }
    }

    // =========================
    // Clientes
    // =========================
    private void carregarClientes() {
        try {
            clienteCombo.removeAllItems();
            clienteCombo.addItem("Todos");
            List<ClienteModel> lista = new ClienteDAO().findAll();
            if (lista != null) {
                for (ClienteModel c : lista)
                    clienteCombo.addItem(c.getNome());
            }
        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar clientes:\n" + ex.getMessage());
        }
    }

    // =========================
    // Query (mantida) + resumo (corrigido)
    // =========================
    public void carregarVendas(String dataIni, String dataFim, String cliente, String status) {
        modelo.setRowCount(0);

        double total = 0; // ✅ agora soma de verdade

        try (Statement st = DB.get().createStatement()) {

            StringBuilder sql = new StringBuilder(
                    "SELECT v.*, c.nome AS cliente_nome " +
                            "FROM vendas v JOIN clientes c ON v.cliente_id = c.id");

            StringJoiner where = new StringJoiner(" AND ");

            if (dataIni != null && !dataIni.isEmpty())
                where.add("date(v.data_venda) >= '" + dataIni + "'");

            if (dataFim != null && !dataFim.isEmpty())
                where.add("date(v.data_venda) <= '" + dataFim + "'");

            if (cliente != null && !"Todos".equals(cliente))
                where.add("c.nome LIKE '%" + cliente + "%'");

            if (status != null && !"Todos".equalsIgnoreCase(status)) {
                boolean statusCalculado = status.equalsIgnoreCase("Devolvida") ||
                        status.equalsIgnoreCase("Parcialmente Devolvida") ||
                        status.equalsIgnoreCase("Pendente");
                if (!statusCalculado) {
                    where.add("v.status = '" + status.toLowerCase() + "'");
                }
            }

            if (where.length() > 0)
                sql.append(" WHERE ").append(where);

            sql.append(" ORDER BY v.id DESC");

            try (ResultSet rs = st.executeQuery(sql.toString())) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String rawDt = rs.getString("data_venda");
                    LocalDate dt = LocalDate.parse(rawDt.substring(0, 10));
                    String data = dt.format(BR_DATE);

                    String cliNome = rs.getString("cliente_nome");
                    double val = rs.getDouble("total_liquido");
                    String pg = rs.getString("forma_pagamento");
                    int parc = rs.getInt("parcelas");
                    String statusOriginal = rs.getString("status").toLowerCase();

                    // devoluções
                    java.util.List<model.VendaDevolucaoModel> devolucoes = new dao.VendaDevolucaoDAO()
                            .listarPorVenda(id);

                    // parcelas pendentes
                    boolean temParcelasPendentes = false;
                    try (Statement stParc = DB.get().createStatement();
                            ResultSet rsParc = stParc.executeQuery(
                                    "SELECT COUNT(*) FROM parcelas_contas_receber WHERE titulo_id = (" +
                                            "  SELECT id FROM titulos_contas_receber WHERE codigo_selecao = 'venda-"
                                            + id + "'" +
                                            ") AND status = 'aberto'")) {
                        if (rsParc.next() && rsParc.getInt(1) > 0)
                            temParcelasPendentes = true;
                    }

                    model.VendaModel vendaTmp = new model.VendaModel(
                            id, rawDt, rs.getString("cliente_id"),
                            rs.getDouble("total_bruto"),
                            rs.getDouble("desconto"),
                            rs.getDouble("total_liquido"),
                            pg, parc, statusOriginal);

                    vendaTmp.setItens(new dao.VendaItemDAO().listarPorVenda(id));

                    String statusFinal;
                    if ("cancelada".equals(statusOriginal))
                        statusFinal = "cancelada";
                    else if (!devolucoes.isEmpty() && vendaTmp.isDevolucaoParcial(devolucoes))
                        statusFinal = "parcialmente devolvida";
                    else if (!devolucoes.isEmpty())
                        statusFinal = "devolvida";
                    else if (temParcelasPendentes)
                        statusFinal = "pendente";
                    else
                        statusFinal = "fechada";

                    if (!"Todos".equals(status) && !status.equalsIgnoreCase(statusFinal))
                        continue;

                    modelo.addRow(new Object[] { id, data, cliNome, val, pg, parc, statusFinal, "↗" });
                    total += val; // ✅ soma real
                }
            }

        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar vendas:\n" + ex.getMessage());
        }

        int qtd = modelo.getRowCount();
        double ticket = qtd > 0 ? total / qtd : 0;
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        resumoLbl.setText(
                "Total: " + cf.format(total) + "  |  Qtde: " + qtd + "  |  Ticket Médio: " + cf.format(ticket));
    }

    private void excluirVenda(int vendaId) {
        try (Statement st = DB.get().createStatement()) {
            st.executeUpdate("DELETE FROM vendas_itens WHERE venda_id = " + vendaId);
            st.executeUpdate("DELETE FROM vendas WHERE id = " + vendaId);
        } catch (Exception ex) {
            AlertUtils.error("Erro ao excluir venda:\n" + ex.getMessage());
        }
    }

    private String formatarDataParaSQL(JDateChooser chooser) {
        if (chooser.getDate() == null)
            return "";
        LocalDate ld = chooser.getDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return ld.format(SQL_DATE);
    }

    // ========= botão ação coluna final =========

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer() {
            setText("↗");
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val,
                boolean sel, boolean foc,
                int row, int col) {
            setText(val != null ? val.toString() : "↗");
            setBackground(sel ? tbl.getSelectionBackground() : tbl.getBackground());
            setForeground(sel ? tbl.getSelectionForeground() : tbl.getForeground());
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private final JButton btn = new JButton("↗");
        private int row;

        ButtonEditor() {
            super(new JCheckBox());
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                int modelRow = tabela.convertRowIndexToModel(row);
                int id = (int) modelo.getValueAt(modelRow, 0);
                new VendaDetalhesDialog(owner, id).setVisible(true);
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object val,
                boolean sel, int row, int col) {
            this.row = row;
            return btn;
        }
    }
}
