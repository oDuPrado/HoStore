package ui.financeiro.painel;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import dao.ClienteDAO;
import dao.ParcelaContaReceberDAO;
import dao.TituloContaReceberDAO;
import model.ClienteModel;
import model.ParcelaContaReceberModel;
import model.TituloContaReceberModel;
import service.ContaReceberService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * @CR: Tabela + filtros de Contas a Receber.
 *      É chamado a partir de PainelFinanceiro.
 */
public class PainelContasReceber extends JPanel {

    /* ─── DAO / Services ────────────────────────────────────────────── */
    private final TituloContaReceberDAO tituloDAO = new TituloContaReceberDAO();
    private final ParcelaContaReceberDAO parcelaDAO = new ParcelaContaReceberDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    @SuppressWarnings("unused")
    private final ContaReceberService crService = new ContaReceberService();

    /* ─── Componentes UI ────────────────────────────────────────────── */
    private DefaultTableModel model;
    private JTable tabela;
    private JComboBox<String> cbCliente, cbStatus;
    private JDateChooser dtInicio, dtFim;

    private final SimpleDateFormat sqlFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00");

    public PainelContasReceber() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(10, 10));

        add(criarTopo(), BorderLayout.NORTH);
        add(criarCentro(), BorderLayout.CENTER);
        add(criarPainelBotoes(), BorderLayout.SOUTH);

        carregarTabela(); // inicial
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* Topo (título + filtros) */
    /* ──────────────────────────────────────────────────────────────── */
    private JComponent criarTopo() {
        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 2, 0, 2));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(UiKit.title("Contas a Receber"));
        titles.add(UiKit.hint("Use os filtros para localizar títulos. Por padrão: de hoje em diante."));
        header.add(titles, BorderLayout.CENTER);

        wrap.add(header, BorderLayout.NORTH);
        wrap.add(criarPainelFiltros(), BorderLayout.CENTER);
        return wrap;
    }

    private JPanel criarPainelFiltros() {
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        // Cliente
        gc.gridx = 0;
        gc.weightx = 0;
        card.add(new JLabel("Cliente"), gc);

        cbCliente = new JComboBox<>();
        cbCliente.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbCliente.addItem("Todos");
        try {
            for (ClienteModel c : clienteDAO.findAll())
                cbCliente.addItem(c.getNome());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        gc.gridx = 1;
        gc.weightx = 0.35;
        card.add(cbCliente, gc);

        // Status
        gc.gridx = 2;
        gc.weightx = 0;
        card.add(new JLabel("Status"), gc);

        cbStatus = new JComboBox<>(new String[] { "Todos", "aberto", "quitado", "vencido", "cancelado" });
        cbStatus.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        gc.gridx = 3;
        gc.weightx = 0.2;
        card.add(cbStatus, gc);

        // De
        gc.gridx = 4;
        gc.weightx = 0;
        card.add(new JLabel("De"), gc);

        dtInicio = criarDateChooserHoje();
        gc.gridx = 5;
        gc.weightx = 0.18;
        card.add(dtInicio, gc);

        // Até
        gc.gridx = 6;
        gc.weightx = 0;
        card.add(new JLabel("Até"), gc);

        dtFim = criarDateChooserFuturo();
        gc.gridx = 7;
        gc.weightx = 0.18;
        card.add(dtFim, gc);

        // Botão filtrar
        JButton btFiltrar = UiKit.primary("Filtrar");
        btFiltrar.addActionListener(e -> carregarTabela());
        gc.gridx = 8;
        gc.weightx = 0;
        card.add(btFiltrar, gc);

        // Enter filtra
        cbCliente.addActionListener(e -> {
        }); // mantém UI responsiva
        cbStatus.addActionListener(e -> {
        });
        return card;
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* Centro (tabela) */
    /* ──────────────────────────────────────────────────────────────── */
    private JComponent criarCentro() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        card.add(criarTabela(), BorderLayout.CENTER);
        return card;
    }

    private JScrollPane criarTabela() {
        String[] cols = { "ID", "Cliente", "Data Geração", "Total", "Pago", "Aberto", "Status" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tabela = new JTable(model);
        UiKit.tableDefaults(tabela);

        esconderColunaID(tabela);

        // Renderers (zebra + alinhamentos)
        aplicarRenderers(tabela);

        // Duplo clique abre parcelas
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    abrirParcelas();
            }
        });

        return UiKit.scroll(tabela);
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* Rodapé (ações) */
    /* ──────────────────────────────────────────────────────────────── */
    private JPanel criarPainelBotoes() {
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acoes.setOpaque(false);
        acoes.setBorder(new EmptyBorder(0, 0, 0, 2));

        JButton btNovo = UiKit.primary("Novo");
        JButton btParcelas = UiKit.ghost("Parcelas");
        JButton btExcluir = UiKit.ghost("Excluir");
        JButton btRefresh = UiKit.ghost("Atualizar");

        btNovo.addActionListener(e -> {
            new ui.financeiro.dialog.ContaReceberDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
            carregarTabela();
        });

        btParcelas.addActionListener(e -> abrirParcelas());

        btExcluir.addActionListener(e -> {
            String id = idSelecionado();
            if (id == null)
                return;

            int op = JOptionPane.showConfirmDialog(
                    this,
                    "Excluir Título e TODAS as parcelas?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (op == JOptionPane.YES_OPTION) {
                try {
                    tituloDAO.excluir(id);
                    carregarTabela();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        btRefresh.addActionListener(e -> carregarTabela());

        acoes.add(btNovo);
        acoes.add(btParcelas);
        acoes.add(btExcluir);
        acoes.add(btRefresh);
        return acoes;
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* Data load */
    /* ──────────────────────────────────────────────────────────────── */
    private void carregarTabela() {
        model.setRowCount(0);

        try {
            String cliFiltro = (String) cbCliente.getSelectedItem();
            String stFiltro = (String) cbStatus.getSelectedItem();

            Date dIni = dtInicio.getDate();
            Date dFim = dtFim.getDate();

            // Segurança: se o usuário limpar alguma data
            if (dIni == null)
                dIni = hoje();
            if (dFim == null)
                dFim = futuroDistante();

            // Normaliza intervalo (se o humano inverter)
            if (dIni.after(dFim)) {
                Date tmp = dIni;
                dIni = dFim;
                dFim = tmp;
                dtInicio.setDate(dIni);
                dtFim.setDate(dFim);
            }

            List<TituloContaReceberModel> titulos = tituloDAO.listarTodos();

            for (TituloContaReceberModel t : titulos) {
                ClienteModel cli = clienteDAO.buscarPorId(t.getClienteId());
                String cliNome = (cli != null && cli.getNome() != null) ? cli.getNome() : "(sem cliente)";

                if (!"Todos".equals(cliFiltro) && !cliNome.equals(cliFiltro))
                    continue;
                if (!"Todos".equals(stFiltro) && (t.getStatus() == null || !t.getStatus().equalsIgnoreCase(stFiltro)))
                    continue;

                // soma parcelas
                List<ParcelaContaReceberModel> parcelas = parcelaDAO.listarPorTitulo(t.getId());
                double total = parcelas.stream().mapToDouble(ParcelaContaReceberModel::getValorNominal).sum();
                double pago = parcelas.stream().mapToDouble(ParcelaContaReceberModel::getValorPago).sum();

                // filtro data geração (inclusive)
                Date ger = sqlFmt.parse(t.getDataGeracao());
                if (ger.before(inicioDoDia(dIni)) || ger.after(fimDoDia(dFim)))
                    continue;

                model.addRow(new Object[] {
                        t.getId(),
                        cliNome,
                        visFmt.format(ger),
                        moneyFmt.format(total),
                        moneyFmt.format(pago),
                        moneyFmt.format(total - pago),
                        safe(t.getStatus())
                });
            }

            // Sort default: Data Geração desc (mais recentes em cima)
            if (tabela.getRowSorter() == null)
                tabela.setAutoCreateRowSorter(true);
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) tabela.getRowSorter();
            sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* Helpers */
    /* ──────────────────────────────────────────────────────────────── */
    private void abrirParcelas() {
        String id = idSelecionado();
        if (id == null)
            return;

        new ui.financeiro.dialog.ParcelasContaReceberDialog(
                SwingUtilities.getWindowAncestor(this), id).setVisible(true);

        carregarTabela();
    }

    private String idSelecionado() {
        int viewRow = tabela.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha");
            return null;
        }

        int modelRow = tabela.convertRowIndexToModel(viewRow);
        return (String) model.getValueAt(modelRow, 0);
    }

    private JDateChooser criarDateChooserHoje() {
        JDateChooser dc = new JDateChooser(hoje());
        prepararDateChooser(dc);
        return dc;
    }

    private JDateChooser criarDateChooserFuturo() {
        JDateChooser dc = new JDateChooser(futuroDistante());
        prepararDateChooser(dc);
        return dc;
    }

    private void prepararDateChooser(JDateChooser dc) {
        dc.setPreferredSize(new Dimension(130, 30));
        dc.setDateFormatString("dd/MM/yyyy");

        // Campo editor com arc (FlatLaf)
        if (dc.getDateEditor() != null && dc.getDateEditor().getUiComponent() instanceof JComponent editor) {
            editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        }

        // Botão do calendário: deixa consistente e “bonito” no padrão UiKit
        JButton calBtn = dc.getCalendarButton();
        if (calBtn != null) {
            calBtn.setText("📅");
            calBtn.setFocusPainted(false);
            calBtn.setMargin(new Insets(2, 8, 2, 8));
            calBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0; font: +1;");
            calBtn.setToolTipText("Selecionar data");
        }
    }

    private void esconderColunaID(JTable t) {
        TableColumn col = t.getColumnModel().getColumn(0);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }

    private void aplicarRenderers(JTable t) {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();

        // Zebra em tudo
        for (int c = 0; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(zebra);
        }

        // Alinhamentos
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        // Data central
        t.getColumnModel().getColumn(2).setCellRenderer(new DelegatingRenderer(zebra, center));

        // Valores à direita
        t.getColumnModel().getColumn(3).setCellRenderer(new DelegatingRenderer(zebra, right));
        t.getColumnModel().getColumn(4).setCellRenderer(new DelegatingRenderer(zebra, right));
        t.getColumnModel().getColumn(5).setCellRenderer(new DelegatingRenderer(zebra, right));

        // Badge no status
        t.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private Date hoje() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date futuroDistante() {
        LocalDate far = LocalDate.now().plusYears(10);
        return Date.from(far.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date inicioDoDia(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date fimDoDia(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /* ──────────────────────────────────────────────────────────────── */
    /* Renderers */
    /* ──────────────────────────────────────────────────────────────── */

    /**
     * Combina zebra (fundo/borda/padding) com alinhamento específico.
     */
    static class DelegatingRenderer extends DefaultTableCellRenderer {
        private final DefaultTableCellRenderer base;
        private final DefaultTableCellRenderer aligner;

        DelegatingRenderer(DefaultTableCellRenderer base, DefaultTableCellRenderer aligner) {
            this.base = base;
            this.aligner = aligner;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel l) {
                l.setHorizontalAlignment(aligner.getHorizontalAlignment());
            }
            return c;
        }
    }

    /**
     * Badge de status específico para Contas a Receber.
     */
    static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String st = (value == null) ? "" : value.toString().toLowerCase(Locale.ROOT).trim();
            l.setText(" " + st + " ");
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
            l.setBorder(new EmptyBorder(4, 10, 4, 10));

            if (isSelected) {
                l.setOpaque(true);
                l.setBackground(table.getSelectionBackground());
                l.setForeground(table.getSelectionForeground());
                return l;
            }

            boolean dark = Boolean.TRUE.equals(UIManager.get("laf.dark"));
            Color fg = UIManager.getColor("Label.foreground");
            if (fg == null)
                fg = dark ? new Color(0xE6E8EB) : new Color(0x111827);

            Color bg;
            switch (st) {
                case "aberto" -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
                case "quitado" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
                case "vencido" -> bg = dark ? new Color(0x4A1D1D) : new Color(0xFEE2E2);
                case "cancelado" -> bg = dark ? new Color(0x3A2A2A) : new Color(0xE5E7EB);
                default -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
            }

            l.setOpaque(true);
            l.setBackground(bg);
            l.setForeground(fg);

            Color border = UIManager.getColor("Component.borderColor");
            if (border == null)
                border = dark ? new Color(0x313844) : new Color(0xE5E7EB);

            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    new EmptyBorder(4, 10, 4, 10)));

            return l;
        }
    }
}
