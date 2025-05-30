// src/ui/venda/painel/PainelVendas.java
package ui.venda.painel;

import com.toedter.calendar.JDateChooser; // picker de data
import dao.ClienteDAO;
import model.ClienteModel;
import ui.venda.dialog.VendaDetalhesDialog;
import ui.venda.dialog.VendaNovaDialog;
import util.AlertUtils;
import util.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
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

    // Tabela e modelo de dados
    private final JTable tabela;
    private final DefaultTableModel modelo;

    // Componentes de filtro
    private final JDateChooser inicioChooser = new JDateChooser();
    private final JDateChooser fimChooser = new JDateChooser();
    private final JComboBox<String> clienteCombo = new JComboBox<>();
    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[] { "Todos", "Fechada", "Cancelada", "Estornada", "Pendente" });

    // Labels de resumo
    private final JLabel resumoLbl = new JLabel();

    // Formatter para SQL (yyyy-MM-dd) e BR (dd/MM/yyyy)
    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JFrame owner;

    public PainelVendas(JFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Modelo com colunas: ID, Data, Cliente, Total, Forma PG, Parc., Status,
        // Detalhes
        modelo = new DefaultTableModel(new String[] {
                "ID", "Data", "Cliente", "Total Líquido", "Forma PG", "Parcelas", "Status", "Detalhes"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Apenas a coluna "Detalhes" será clicável
                return col == 7;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                // Define tipos para correto sort/render
                if (col == 0)
                    return Integer.class;
                if (col == 3)
                    return Double.class;
                return String.class;
            }
        };
        tabela = new JTable(modelo);

        // Monta a UI
        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);

        personalizarTabela();
        configurarEventos();

        carregarClientes(); // popula lista de clientes no combo
        // define formato de exibição dos pickers
        inicioChooser.setDateFormatString("dd/MM/yyyy");
        fimChooser.setDateFormatString("dd/MM/yyyy");
        // carrega vendas de hoje por padrão
        String hoje = LocalDate.now().format(SQL_DATE);
        carregarVendas(hoje, hoje, "Todos", "Todos");
    }

    // =========================
    // Monta painel de filtros
    // =========================
    private JComponent criarToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        toolbar.add(new JLabel("De:"));
        toolbar.add(inicioChooser);

        toolbar.add(new JLabel("Até:"));
        toolbar.add(fimChooser);

        toolbar.add(new JLabel("Cliente:"));
        clienteCombo.setEditable(true); // permite digitar para buscar
        toolbar.add(clienteCombo);

        toolbar.add(new JLabel("Status:"));
        toolbar.add(statusCombo);

        JButton filtrar = new JButton("🔍 Filtrar");
        filtrar.addActionListener(e -> {
            // lê valores dos filtros
            String d1 = formatarDataParaSQL(inicioChooser);
            String d2 = formatarDataParaSQL(fimChooser);
            String cli = (String) clienteCombo.getSelectedItem();
            String stat = (String) statusCombo.getSelectedItem();
            carregarVendas(d1, d2, cli, stat);
        });
        toolbar.add(filtrar);

        return toolbar;
    }

    // =========================
    // Monta rodapé com resumo e botões
    // =========================
    private JComponent criarRodape() {
        JPanel rodape = new JPanel(new BorderLayout());

        // --- Resumo à esquerda ---
        resumoLbl.setFont(resumoLbl.getFont().deriveFont(Font.BOLD, 14f));
        JPanel resumoPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        resumoPane.add(resumoLbl);
        rodape.add(resumoPane, BorderLayout.WEST);

        // --- Botões à direita ---
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        // Nova Venda
        JButton btnNova = new JButton("➕ Nova Venda");
        btnNova.addActionListener(e -> new VendaNovaDialog(owner, this).setVisible(true));
        botoes.add(btnNova);

        // Editar (abre detalhes por enquanto)
        JButton btnEditar = new JButton("✏️ Editar");
        btnEditar.addActionListener(e -> {
            int row = tabela.getSelectedRow();
            if (row < 0) {
                AlertUtils.info("Selecione uma venda para editar.");
                return;
            }
            int id = (int) modelo.getValueAt(row, 0);

            new VendaDetalhesDialog(owner, id).setVisible(true);
        });
        botoes.add(btnEditar);

        // Excluir
        JButton btnExcluir = new JButton("🗑️ Excluir");
        btnExcluir.addActionListener(e -> {
            int row = tabela.getSelectedRow();
            if (row < 0) {
                AlertUtils.info("Selecione uma venda para excluir.");
                return;
            }
            int id = (int) modelo.getValueAt(row, 0);
            int op = JOptionPane.showConfirmDialog(owner,
                    "Confirma exclusão da venda ID " + id + "?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) {
                excluirVenda(id);
                // recarrega mantendo filtros atuais
                String d1 = formatarDataParaSQL(inicioChooser);
                String d2 = formatarDataParaSQL(fimChooser);
                carregarVendas(d1, d2,
                        (String) clienteCombo.getSelectedItem(),
                        (String) statusCombo.getSelectedItem());
            }
        });
        botoes.add(btnExcluir);

        rodape.add(botoes, BorderLayout.EAST);
        return rodape;
    }

    // =========================
    // Configura double-click na tabela
    // =========================
    private void configurarEventos() {
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tabela.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        int id = (int) modelo.getValueAt(row, 0);
                        new VendaDetalhesDialog(owner, id).setVisible(true);
                    }
                }
            }
        });
    }

    // =========================
    // Render e estilo da tabela
    // =========================
    private void personalizarTabela() {
        // centraliza coluna ID
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(0).setCellRenderer(center);

        // formata valores monetários à direita
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        tabela.getColumnModel().getColumn(3).setCellRenderer((tbl, val, isSel, hasFocus, row, col) -> {
            JLabel l = new JLabel(cf.format((Double) val));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            return l;
        });

        // coloriza status
        tabela.getColumnModel().getColumn(6).setCellRenderer((tbl, val, isSel, hasFocus, row, col) -> {
            String s = val.toString();
            JLabel l = new JLabel(s);
            l.setOpaque(true);
            l.setForeground(Color.WHITE);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            switch (s.toLowerCase()) {
                case "fechada":
                    l.setBackground(new Color(46, 139, 87));
                    break;
                case "estornada":
                    l.setBackground(new Color(178, 34, 34));
                    break;
                case "cancelada":
                    l.setBackground(new Color(184, 134, 11));
                    break;
                default:
                    l.setBackground(Color.GRAY);
                    break;
                case "pendente":
                    l.setBackground(new Color(70, 130, 180));
                    break; // azul
                case "devolvida":
                    l.setBackground(new Color(128, 0, 128));
                    break; // roxo

            }
            return l;
        });

        // botão "Detalhes"
        TableColumnModel tcm = tabela.getColumnModel();
        tcm.getColumn(7).setCellRenderer(new ButtonRenderer());
        tcm.getColumn(7).setCellEditor(new ButtonEditor());
    }

    // =========================
    // Carrega lista de clientes
    // =========================
    private void carregarClientes() {
        try {
            clienteCombo.removeAllItems();
            clienteCombo.addItem("Todos");
            List<ClienteModel> lista = new ClienteDAO().findAll();
            if (lista != null) {
                for (ClienteModel c : lista) {
                    clienteCombo.addItem(c.getNome());
                }
            }
        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar clientes:\n" + ex.getMessage());
        }
    }

    // =========================
    // Constrói e executa consulta com filtros
    // =========================
    public void carregarVendas(String dataIni, String dataFim, String cliente, String status) {
        modelo.setRowCount(0);
        double total = 0;

        try (Statement st = DB.get().createStatement()) {

            // monta SQL dinamicamente
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
            if (status != null && !"Todos".equals(status))
                where.add("v.status = '" + status.toLowerCase() + "'");
            if (where.length() > 0)
                sql.append(" WHERE ").append(where.toString());
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

                    // Verifica se tem devolução
                    boolean teveDevolucao = false;
                    try (Statement stDev = DB.get().createStatement();
                            ResultSet rsDev = stDev
                                    .executeQuery("SELECT COUNT(*) FROM vendas_devolucoes WHERE venda_id = " + id)) {
                        if (rsDev.next() && rsDev.getInt(1) > 0) {
                            teveDevolucao = true;
                        }
                    }

                    // Verifica se há parcelas pendentes (status = 'aberto' em contas a receber)
                    boolean temParcelasPendentes = false;
                    try (Statement stParc = DB.get().createStatement();
                            ResultSet rsParc = stParc.executeQuery(
                                    "SELECT COUNT(*) FROM parcelas_contas_receber WHERE titulo_id = (" +
                                            "  SELECT id FROM titulos_contas_receber WHERE codigo_selecao = 'venda-"
                                            + id + "'" +
                                            ") AND status = 'aberto'")) {
                        if (rsParc.next() && rsParc.getInt(1) > 0) {
                            temParcelasPendentes = true;
                        }
                    }

                    // Define status final com base nas regras
                    String statusFinal;
                    if ("cancelada".equals(statusOriginal)) {
                        statusFinal = "cancelada";
                    } else if (teveDevolucao) {
                        statusFinal = "devolvida";
                    } else if (temParcelasPendentes) {
                        statusFinal = "pendente";
                    } else {
                        statusFinal = "fechada";
                    }

                    modelo.addRow(new Object[] { id, data, cliNome, val, pg, parc, statusFinal, "Detalhes" });
                    total += val;
                }
            }

        } catch (Exception ex) {
            AlertUtils.error("Erro ao carregar vendas:\n" + ex.getMessage());
        }

        // atualiza resumo: total, qtd e ticket médio
        int qtd = modelo.getRowCount();
        double ticket = qtd > 0 ? total / qtd : 0;
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        resumoLbl.setText(
                "Total: " + cf.format(total)
                        + " | Qtde: " + qtd
                        + " | Ticket Médio: " + cf.format(ticket));
    }

    // =========================
    // Exclui venda (itens + cabeçalho)
    // =========================
    private void excluirVenda(int vendaId) {
        try (Statement st = DB.get().createStatement()) {
            st.executeUpdate("DELETE FROM vendas_itens WHERE venda_id = " + vendaId);
            st.executeUpdate("DELETE FROM vendas WHERE id = " + vendaId);
        } catch (Exception ex) {
            AlertUtils.error("Erro ao excluir venda:\n" + ex.getMessage());
        }
    }

    // =========================
    // Formata data do chooser para string SQL
    // =========================
    private String formatarDataParaSQL(JDateChooser chooser) {
        if (chooser.getDate() == null)
            return "";
        LocalDate ld = chooser.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return ld.format(SQL_DATE);
    }

    // Renderer para coluna de botão "Detalhes"
    private class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        ButtonRenderer() {
            setText("Detalhes");
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val,
                boolean sel, boolean foc,
                int row, int col) {
            return this;
        }
    }

    // Editor para coluna de botão "Detalhes"
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton btn = new JButton("Detalhes");
        private int row;

        ButtonEditor() {
            super(new JCheckBox());
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                int id = (int) modelo.getValueAt(row, 0);
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
