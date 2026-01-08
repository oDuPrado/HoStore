// src/ui/estoque/dialog/MovimentacaoEstoqueDialog.java
package ui.estoque.dialog;

import util.DB;
import util.UiKit;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class MovimentacaoEstoqueDialog extends JDialog {

    private static final DateTimeFormatter FMT_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter FMT_EXIBIR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private JTable tabela;
    private DefaultTableModel tabelaModel;
    private JComboBox<String> cboTipo;
    private JDateChooser dtInicio, dtFim;

    public MovimentacaoEstoqueDialog(Window owner) {
        super(owner, "📦 Movimentações de Estoque", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);
        setSize(980, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        carregarMovimentacoes();
    }

    private JPanel buildHeader() {
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Movimentações de Estoque"));
        left.add(UiKit.hint("Filtre por tipo e período. Duplo clique para ver detalhes."));
        header.add(left, BorderLayout.WEST);

        return header;
    }

    private JComponent buildCenter() {
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);

        body.add(buildFiltersCard(), BorderLayout.NORTH);
        body.add(buildTableCard(), BorderLayout.CENTER);

        return body;
    }

    private JPanel buildFiltersCard() {
        JPanel filtros = UiKit.card();
        filtros.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        cboTipo = new JComboBox<>(new String[] { "Todos", "entrada", "saida" });

        dtInicio = new JDateChooser();
        dtInicio.setDateFormatString("dd/MM/yyyy");

        dtFim = new JDateChooser();
        dtFim.setDateFormatString("dd/MM/yyyy");

        JButton btFiltrar = UiKit.primary("Filtrar");
        JButton btLimpar = UiKit.ghost("Limpar");

        // Linha 0
        g.gridx = 0;
        g.gridy = 0;
        filtros.add(new JLabel("Tipo:"), g);
        g.gridx = 1;
        filtros.add(cboTipo, g);

        g.gridx = 2;
        filtros.add(new JLabel("Início:"), g);
        g.gridx = 3;
        filtros.add(dtInicio, g);

        g.gridx = 4;
        filtros.add(new JLabel("Fim:"), g);
        g.gridx = 5;
        filtros.add(dtFim, g);

        g.gridx = 6;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        filtros.add(Box.createHorizontalGlue(), g);

        g.gridx = 7;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        filtros.add(btLimpar, g);

        g.gridx = 8;
        filtros.add(btFiltrar, g);

        btFiltrar.addActionListener(e -> carregarMovimentacoes());
        btLimpar.addActionListener(e -> {
            cboTipo.setSelectedIndex(0);
            dtInicio.setDate(null);
            dtFim.setDate(null);
            carregarMovimentacoes();
        });

        return filtros;
    }

    private JPanel buildTableCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        tabelaModel = new DefaultTableModel(
                new String[] { "ID", "Produto", "Tipo", "Quantidade", "Motivo", "Data/Hora", "Usuário" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0 || col == 3)
                    return Integer.class;
                return String.class;
            }
        };

        tabela = new JTable(tabelaModel);
        UiKit.tableDefaults(tabela);

        // Sort
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tabelaModel);
        tabela.setRowSorter(sorter);
        sorter.toggleSortOrder(5);
        sorter.toggleSortOrder(5);

        // Renderers
        TableColumnModel cm = tabela.getColumnModel();

        // Zebra para quase tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(zebra);
        }

        // Centralizar colunas numéricas/curtas
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBorder(new EmptyBorder(0, 8, 0, 8));
        cm.getColumn(0).setCellRenderer(center); // ID
        cm.getColumn(3).setCellRenderer(center); // Quantidade
        cm.getColumn(5).setCellRenderer(center); // Data/Hora

        // Coluna Tipo como “badge”
        cm.getColumn(2).setCellRenderer(new TipoBadgeRenderer());

        // Coluna Motivo com elipse + tooltip
        cm.getColumn(4).setCellRenderer(new MotivoEllipsisRenderer());

        // Larguras decentes
        cm.getColumn(0).setMaxWidth(80);
        cm.getColumn(2).setMaxWidth(120);
        cm.getColumn(3).setMaxWidth(120);
        cm.getColumn(5).setPreferredWidth(170);
        cm.getColumn(6).setPreferredWidth(140);

        // Duplo clique: detalhes
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() != -1) {
                    exibirDetalhesComTabela();
                }
            }
        });

        card.add(UiKit.scroll(tabela), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Dica: use filtros para auditoria rápida de entradas/saídas."), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btFechar = UiKit.ghost("Fechar");
        btFechar.addActionListener(e -> dispose());

        actions.add(btFechar);
        footer.add(actions, BorderLayout.EAST);

        return footer;
    }

    private void carregarMovimentacoes() {
        tabelaModel.setRowCount(0);

        String tipoSelecionado = (String) cboTipo.getSelectedItem();

        LocalDate dataIni = null;
        LocalDate dataFimLocal = null;
        if (dtInicio.getDate() != null) {
            dataIni = dtInicio.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (dtFim.getDate() != null) {
            dataFimLocal = dtFim.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.produto_id, p.nome AS produto_nome, " +
                        "m.tipo_mov, m.quantidade, m.motivo, m.data, m.usuario " +
                        "FROM estoque_movimentacoes m " +
                        "LEFT JOIN produtos p ON m.produto_id = p.id " +
                        "WHERE 1=1 ");
        Vector<Object> params = new Vector<>();

        if (!"Todos".equalsIgnoreCase(tipoSelecionado)) {
            sql.append("AND m.tipo_mov = ? ");
            params.add(tipoSelecionado);
        }
        if (dataIni != null) {
            sql.append("AND SUBSTR(m.data,1,10) >= ? ");
            params.add(dataIni.toString());
        }
        if (dataFimLocal != null) {
            sql.append("AND SUBSTR(m.data,1,10) <= ? ");
            params.add(dataFimLocal.toString());
        }

        sql.append("ORDER BY m.data DESC");

        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");

                    String produtoNome = rs.getString("produto_nome");
                    if (produtoNome == null)
                        produtoNome = "ID:" + rs.getString("produto_id");

                    String tipo = rs.getString("tipo_mov");
                    int quantidade = rs.getInt("quantidade");
                    String motivo = rs.getString("motivo");
                    String dataBruta = rs.getString("data");
                    String usuario = rs.getString("usuario");

                    String dataFormatada;
                    try {
                        LocalDateTime dt = LocalDateTime.parse(dataBruta, FMT_ISO);
                        dataFormatada = FMT_EXIBIR.format(dt);
                    } catch (Exception ex) {
                        dataFormatada = dataBruta;
                    }

                    tabelaModel.addRow(new Object[] {
                            id, produtoNome, tipo, quantidade, motivo, dataFormatada, usuario
                    });
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar movimentações:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void exibirDetalhesComTabela() {
        int viewRow = tabela.getSelectedRow();
        if (viewRow < 0)
            return;

        int modelRow = tabela.convertRowIndexToModel(viewRow);

        Object idObj = tabelaModel.getValueAt(modelRow, 0);
        Object produtoObj = tabelaModel.getValueAt(modelRow, 1);
        Object tipoObj = tabelaModel.getValueAt(modelRow, 2);
        Object qtdObj = tabelaModel.getValueAt(modelRow, 3);
        Object motivoObj = tabelaModel.getValueAt(modelRow, 4);
        Object dataHoraObj = tabelaModel.getValueAt(modelRow, 5);
        Object usuarioObj = tabelaModel.getValueAt(modelRow, 6);

        DefaultTableModel detalhesModel = new DefaultTableModel(new String[] { "Campo", "Valor" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        detalhesModel.addRow(new Object[] { "ID", idObj });
        detalhesModel.addRow(new Object[] { "Produto", produtoObj });
        detalhesModel.addRow(new Object[] { "Tipo", tipoObj });
        detalhesModel.addRow(new Object[] { "Quantidade", qtdObj });
        detalhesModel.addRow(new Object[] { "Motivo", motivoObj });
        detalhesModel.addRow(new Object[] { "Data/Hora", dataHoraObj });
        detalhesModel.addRow(new Object[] { "Usuário", usuarioObj });

        JTable tabelaDetalhes = new JTable(detalhesModel);
        UiKit.tableDefaults(tabelaDetalhes);

        TableColumnModel cm = tabelaDetalhes.getColumnModel();
        cm.getColumn(0).setMaxWidth(140);

        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));
        card.add(UiKit.scroll(tabelaDetalhes), BorderLayout.CENTER);

        JDialog dlg = new JDialog(this, "Detalhes • Movimentação #" + idObj, true);
        UiKit.applyDialogBase(dlg);
        dlg.setLayout(new BorderLayout(12, 12));

        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout());
        header.add(UiKit.title("Detalhes da Movimentação"), BorderLayout.WEST);
        header.add(UiKit.hint("Registro de auditoria"), BorderLayout.EAST);

        dlg.add(header, BorderLayout.NORTH);
        dlg.add(card, BorderLayout.CENTER);

        JPanel footer = UiKit.card();
        footer.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton fechar = UiKit.primary("Fechar");
        fechar.addActionListener(e -> dlg.dispose());
        footer.add(fechar);

        dlg.add(footer, BorderLayout.SOUTH);

        dlg.setSize(640, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ===== Renderers =====

    /** Badge para coluna Tipo: entrada/saida */
    private static class TipoBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String tipo = value == null ? "" : value.toString().toLowerCase();
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setText(tipo.isBlank() ? "" : (" " + tipo + " "));
            l.setBorder(new EmptyBorder(4, 10, 4, 10));
            l.setOpaque(true);

            if (isSelected)
                return l;

            boolean dark = isDarkTheme();
            Color fg = UIManager.getColor("Label.foreground");
            if (fg == null)
                fg = dark ? new Color(0xE6E8EB) : new Color(0x111827);

            Color bg;
            if ("entrada".equals(tipo))
                bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
            else if ("saida".equals(tipo))
                bg = dark ? new Color(0x3A1E1E) : new Color(0xFEE2E2);
            else
                bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);

            l.setForeground(fg);
            l.setBackground(bg);
            return l;
        }

        private static boolean isDarkTheme() {
            Object o = UIManager.get("laf.dark");
            if (o instanceof Boolean b)
                return b;

            Color bg = UIManager.getColor("Panel.background");
            if (bg == null)
                bg = Color.WHITE;
            int lum = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
            return lum < 140;
        }
    }

    /** Motivo: corta texto grande (sem mudar conteúdo) e mostra tooltip completa */
    private static class MotivoEllipsisRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String txt = value == null ? "" : value.toString();
            l.setToolTipText(txt);

            // “visual”: encurta se for enorme
            if (txt.length() > 80) {
                l.setText(txt.substring(0, 77) + "...");
            } else {
                l.setText(txt);
            }

            l.setBorder(new EmptyBorder(0, 8, 0, 8));
            return l;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MovimentacaoEstoqueDialog dlg = new MovimentacaoEstoqueDialog(null);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setVisible(true);
        });
    }
}
