// PainelFinanceiro atualizado - Novo fluxo de Títulos/Parcelas (com UiKit)
package ui.financeiro.painel;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import dao.*;
import model.*;
import service.ContaPagarService;
import ui.financeiro.dialog.*;
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

public class PainelFinanceiro extends JPanel {

    /* ────── services/daos ────── */
    @SuppressWarnings("unused")
    private final ContaPagarService contaPagarService = new ContaPagarService();

    private final TituloContaPagarDAO tituloDAO = new TituloContaPagarDAO();
    private final ParcelaContaPagarDAO parcelaDAO = new ParcelaContaPagarDAO();
    private final FornecedorDAO fornecedorDAO = new FornecedorDAO();

    /* ────── componentes ────── */
    private DefaultTableModel modelPagar;
    private JTable tabelaPagar;
    private JComboBox<String> cbFornecedor, cbStatusPagar;
    private JDateChooser dtInicioPagar, dtFimPagar;

    private final SimpleDateFormat sqlFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00");

    public PainelFinanceiro() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.putClientProperty(FlatClientProperties.STYLE, "tabHeight: 34;");

        tabs.addTab("Contas a Pagar", criarPainelContasPagar());
        tabs.addTab("Contas a Receber", criarPainelContasReceber());
        tabs.addTab("Resumo Financeiro", criarPainelResumo());

        add(tabs, BorderLayout.CENTER);
    }

    /* ───────────────────────────────────── Contas a PAGAR ── */
    private JPanel criarPainelContasPagar() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        UiKit.applyPanelBase(root);

        // Topo (título + filtros em card)
        root.add(criarTopoPagar(), BorderLayout.NORTH);

        // Centro (tabela em card)
        root.add(criarCentroPagar(), BorderLayout.CENTER);

        // Rodapé (ações)
        root.add(criarAcoesPagar(), BorderLayout.SOUTH);

        carregarTabelaPagar(); // inicial
        return root;
    }

    private JComponent criarTopoPagar() {
        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 2, 0, 2));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(UiKit.title("Contas a Pagar"));
        titles.add(UiKit.hint("Por padrão: de hoje em diante. Porque esconder conta do dia é uma ideia genial, né."));
        header.add(titles, BorderLayout.CENTER);

        wrap.add(header, BorderLayout.NORTH);
        wrap.add(criarFiltrosPagar(), BorderLayout.CENTER);
        return wrap;
    }

    private JPanel criarFiltrosPagar() {
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        // Fornecedor
        gc.gridx = 0;
        gc.weightx = 0;
        card.add(new JLabel("Fornecedor"), gc);

        cbFornecedor = new JComboBox<>();
        cbFornecedor.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbFornecedor.addItem("Todos");
        try {
            fornecedorDAO.listar(null, null, null, null).forEach(f -> cbFornecedor.addItem(f.getNome()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        gc.gridx = 1;
        gc.weightx = 0.35;
        card.add(cbFornecedor, gc);

        // Status
        gc.gridx = 2;
        gc.weightx = 0;
        card.add(new JLabel("Status"), gc);

        cbStatusPagar = new JComboBox<>(new String[] { "Todos", "aberto", "pago", "vencido", "cancelado" });
        cbStatusPagar.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        gc.gridx = 3;
        gc.weightx = 0.20;
        card.add(cbStatusPagar, gc);

        // De
        gc.gridx = 4;
        gc.weightx = 0;
        card.add(new JLabel("De"), gc);

        dtInicioPagar = criarDateChooserHoje();
        gc.gridx = 5;
        gc.weightx = 0.18;
        card.add(dtInicioPagar, gc);

        // Até
        gc.gridx = 6;
        gc.weightx = 0;
        card.add(new JLabel("Até"), gc);

        dtFimPagar = criarDateChooserFuturo();
        gc.gridx = 7;
        gc.weightx = 0.18;
        card.add(dtFimPagar, gc);

        // Filtrar
        JButton btnFiltrar = UiKit.primary("Filtrar");
        btnFiltrar.addActionListener(e -> carregarTabelaPagar());
        gc.gridx = 8;
        gc.weightx = 0;
        card.add(btnFiltrar, gc);

        return card;
    }

    private JComponent criarCentroPagar() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        String[] cols = { "ID", "Fornecedor", "Data Geração", "Valor Total", "Valor Pago", "Em Aberto", "Status" };
        modelPagar = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tabelaPagar = new JTable(modelPagar);
        UiKit.tableDefaults(tabelaPagar);
        esconderColunaID(tabelaPagar);
        aplicarRenderersPagar(tabelaPagar);

        // Duplo clique abre parcelas
        tabelaPagar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    abrirParcelasPagar();
            }
        });

        card.add(UiKit.scroll(tabelaPagar), BorderLayout.CENTER);
        return card;
    }

    private JPanel criarAcoesPagar() {
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acoes.setOpaque(false);
        acoes.setBorder(new EmptyBorder(0, 0, 0, 2));

        JButton btNovo = UiKit.primary("Novo");
        JButton btParcelas = UiKit.ghost("Parcelas");
        JButton btExcluir = UiKit.ghost("Excluir");
        JButton btRefresh = UiKit.ghost("Atualizar");

        btRefresh.addActionListener(e -> carregarTabelaPagar());

        btNovo.addActionListener(e -> {
            new ContaPagarDialog((Frame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
            carregarTabelaPagar();
        });

        btParcelas.addActionListener(e -> abrirParcelasPagar());

        btExcluir.addActionListener(e -> excluirTituloPagar());

        acoes.add(btNovo);
        acoes.add(btParcelas);
        acoes.add(btExcluir);
        acoes.add(btRefresh);
        return acoes;
    }

    private void abrirParcelasPagar() {
        String idSel = idSelecionadoPagar();
        if (idSel == null)
            return;

        try {
            TituloContaPagarModel titulo = tituloDAO.buscarPorId(idSel);
            if (titulo != null) {
                new ParcelasTituloDialog(
                        SwingUtilities.getWindowAncestor(this),
                        titulo.getId()).setVisible(true);
                carregarTabelaPagar();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void excluirTituloPagar() {
        String id = idSelecionadoPagar();
        if (id == null)
            return;

        int op = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente excluir este Título e todas as suas parcelas?",
                "Confirmar Exclusão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (op != JOptionPane.YES_OPTION)
            return;

        try {
            // excluir parcelas associadas
            parcelaDAO.listarPorTitulo(id).forEach(p -> {
                try {
                    parcelaDAO.excluir(p.getId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // excluir título
            tituloDAO.excluir(id);
            carregarTabelaPagar();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao excluir:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarTabelaPagar() {
        modelPagar.setRowCount(0);

        try {
            String fornecedorFiltro = (String) cbFornecedor.getSelectedItem();
            String statusFiltro = (String) cbStatusPagar.getSelectedItem();

            Date dIni = dtInicioPagar.getDate();
            Date dFim = dtFimPagar.getDate();

            if (dIni == null)
                dIni = hoje();
            if (dFim == null)
                dFim = futuroDistante();

            // se o humano inverter as datas, a gente salva a UI dele de si mesmo
            if (dIni.after(dFim)) {
                Date tmp = dIni;
                dIni = dFim;
                dFim = tmp;
                dtInicioPagar.setDate(dIni);
                dtFimPagar.setDate(dFim);
            }

            Date ini = inicioDoDia(dIni);
            Date fim = fimDoDia(dFim);

            List<TituloContaPagarModel> titulos = tituloDAO.listarTodos();

            for (TituloContaPagarModel t : titulos) {
                FornecedorModel forn = fornecedorDAO.buscarPorId(t.getFornecedorId());
                String fornNome = (forn != null && forn.getNome() != null) ? forn.getNome() : "(sem fornecedor)";

                if (!"Todos".equals(fornecedorFiltro) && !fornNome.equals(fornecedorFiltro))
                    continue;
                if (!"Todos".equals(statusFiltro)
                        && (t.getStatus() == null || !t.getStatus().equalsIgnoreCase(statusFiltro)))
                    continue;

                // calcula valores baseados nas parcelas
                List<ParcelaContaPagarModel> parcelas = parcelaDAO.listarPorTitulo(t.getId());
                double valorTotal = parcelas.stream().mapToDouble(ParcelaContaPagarModel::getValorNominal).sum();
                double valorPago = parcelas.stream().mapToDouble(ParcelaContaPagarModel::getValorPago).sum();
                double valorAberto = valorTotal - valorPago;

                // filtro por data de geração
                Date dataGeracao = optDate(t.getDataGeracao());
                if (dataGeracao == null)
                    continue;

                if (dataGeracao.before(ini) || dataGeracao.after(fim))
                    continue;

                modelPagar.addRow(new Object[] {
                        t.getId(),
                        fornNome,
                        visFmt.format(dataGeracao),
                        moneyFmt.format(valorTotal),
                        moneyFmt.format(valorPago),
                        moneyFmt.format(valorAberto),
                        safe(t.getStatus())
                });
            }

            // Sort default: Data Geração desc
            if (tabelaPagar.getRowSorter() == null)
                tabelaPagar.setAutoCreateRowSorter(true);
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) tabelaPagar.getRowSorter();
            sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ─────────────────────────── Contas a Receber e Resumo ── */
    private JPanel criarPainelContasReceber() {
        return new ui.financeiro.painel.PainelContasReceber();
    }

    private JPanel criarPainelResumo() {
        JPanel p = UiKit.card();
        p.setLayout(new BorderLayout());
        p.add(new JLabel("<html><h2>Resumo Financeiro em desenvolvimento...</h2></html>", SwingConstants.CENTER),
                BorderLayout.CENTER);
        return p;
    }

    /* ─────────────────────────── Helpers ── */

    private Date optDate(String iso) {
        try {
            return iso == null ? null : sqlFmt.parse(iso);
        } catch (Exception e) {
            return null;
        }
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

        if (dc.getDateEditor() != null && dc.getDateEditor().getUiComponent() instanceof JComponent editor) {
            editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        }

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

    private void aplicarRenderersPagar(JTable t) {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();

        // zebra em tudo
        for (int c = 0; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(zebra);
        }

        // alinhamentos
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        // Data centralizada
        t.getColumnModel().getColumn(2).setCellRenderer(new DelegatingRenderer(zebra, center));

        // Valores à direita
        t.getColumnModel().getColumn(3).setCellRenderer(new DelegatingRenderer(zebra, right));
        t.getColumnModel().getColumn(4).setCellRenderer(new DelegatingRenderer(zebra, right));
        t.getColumnModel().getColumn(5).setCellRenderer(new DelegatingRenderer(zebra, right));

        // Status como badge
        t.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgePagarRenderer());
    }

    private String idSelecionadoPagar() {
        int viewRow = tabelaPagar.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha");
            return null;
        }
        int modelRow = tabelaPagar.convertRowIndexToModel(viewRow);
        return (String) modelPagar.getValueAt(modelRow, 0);
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

    /* ─────────────────────────── Renderers auxiliares ── */

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

    static class StatusBadgePagarRenderer extends DefaultTableCellRenderer {
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
                case "pago" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
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
