package ui.financeiro.painel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.toedter.calendar.JDateChooser;

import dao.*;
import model.*;
import service.ContaReceberService;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.*;
import java.util.*;
import java.util.List;

/**
 * @CR: Tabela + filtros de Contas a Receber.
 *      É chamado a partir de PainelFinanceiro.
 */
public class PainelContasReceber extends JPanel {

    /* ─── DAO / Services ────────────────────────────────────────────── */
    private final TituloContaReceberDAO tituloDAO   = new TituloContaReceberDAO();
    private final ParcelaContaReceberDAO parcelaDAO = new ParcelaContaReceberDAO();
    private final ClienteDAO clienteDAO             = new ClienteDAO();
    private final ContaReceberService crService     = new ContaReceberService();

    /* ─── Componentes UI ────────────────────────────────────────────── */
    private DefaultTableModel model;
    private JTable tabela;
    private JComboBox<String> cbCliente, cbStatus;
    private JDateChooser dtInicio, dtFim;

    private final SimpleDateFormat sqlFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat moneyFmt   = new DecimalFormat("#,##0.00");

    /* ─── Construtor ────────────────────────────────────────────────── */
    public PainelContasReceber() {
        setLayout(new BorderLayout());
        add(criarPainelFiltros(), BorderLayout.NORTH);
        add(criarTabela(),        BorderLayout.CENTER);
        add(criarPainelBotoes(),  BorderLayout.SOUTH);
        carregarTabela(); // inicial
    }

    /* Filtros (cliente, status, período) */
    private JPanel criarPainelFiltros() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p.setBorder(BorderFactory.createTitledBorder("Filtros"));

        p.add(new JLabel("Cliente:"));
        cbCliente = new JComboBox<>();
        cbCliente.addItem("Todos");
        try {
            clienteDAO.findAll().forEach(c -> cbCliente.addItem(c.getNome()));
        } catch (Exception ex) { ex.printStackTrace(); }
        p.add(cbCliente);

        p.add(new JLabel("Status:"));
        cbStatus = new JComboBox<>(new String[] { "Todos", "aberto", "quitado", "vencido", "cancelado" });
        p.add(cbStatus);

        p.add(new JLabel("De:"));
        dtInicio = criarDateChooser(); p.add(dtInicio);
        p.add(new JLabel("Até:"));
        dtFim = criarDateChooser();    p.add(dtFim);

        JButton btFiltrar = new JButton("Filtrar");
        btFiltrar.addActionListener(e -> carregarTabela());
        p.add(btFiltrar);

        return p;
    }

    /* Tabela */
    private JScrollPane criarTabela() {
        String[] cols = { "ID", "Cliente", "Data Geração", "Total", "Pago", "Aberto", "Status" };
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(model);
        esconderColunaID(tabela);
        aplicarRenderers(tabela);

        /* Duplo clique abre parcelas */
        tabela.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirParcelas();
            }
        });
        return new JScrollPane(tabela);
    }

    /* Botões de ação */
    private JPanel criarPainelBotoes() {
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btNovo     = new JButton("Novo");
        JButton btParcelas = new JButton("Parcelas");
        JButton btExcluir  = new JButton("Excluir");
        JButton btRefresh  = new JButton("Atualizar");

        btNovo.addActionListener(e -> {
            new ui.financeiro.dialog.ContaReceberDialog(
                 (Frame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
            carregarTabela();
        });

        btParcelas.addActionListener(e -> abrirParcelas());

        btExcluir.addActionListener(e -> {
            String id = idSelecionado();
            if (id == null) return;
            int op = JOptionPane.showConfirmDialog(
                    this, "Excluir Título e TODAS as parcelas?", "Confirmar",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                try { tituloDAO.excluir(id); carregarTabela(); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        btRefresh.addActionListener(e -> carregarTabela());

        Arrays.asList(btNovo, btParcelas, btExcluir, btRefresh).forEach(acoes::add);
        return acoes;
    }

    /* Carrega dados da tabela com filtros */
    private void carregarTabela() {
        model.setRowCount(0);
        try {
            String cliFiltro = (String) cbCliente.getSelectedItem();
            String stFiltro  = (String) cbStatus.getSelectedItem();
            Date   dIni      = dtInicio.getDate();
            Date   dFim      = dtFim.getDate();

            List<TituloContaReceberModel> titulos = tituloDAO.listarTodos();
            for (TituloContaReceberModel t : titulos) {
                String cliNome = clienteDAO.buscarPorId(t.getClienteId()).getNome();
                if (!"Todos".equals(cliFiltro) && !cliNome.equals(cliFiltro)) continue;
                if (!"Todos".equals(stFiltro)  && !t.getStatus().equalsIgnoreCase(stFiltro)) continue;

                /* soma parcelas */
                List<ParcelaContaReceberModel> parcelas = parcelaDAO.listarPorTitulo(t.getId());
                double total = parcelas.stream().mapToDouble(ParcelaContaReceberModel::getValorNominal).sum();
                double pago  = parcelas.stream().mapToDouble(ParcelaContaReceberModel::getValorPago).sum();

                /* filtro data geração */
                Date ger = sqlFmt.parse(t.getDataGeracao());
                if (ger.before(dIni) || ger.after(dFim)) continue;

                model.addRow(new Object[] {
                        t.getId(), cliNome, visFmt.format(ger),
                        moneyFmt.format(total), moneyFmt.format(pago),
                        moneyFmt.format(total - pago), t.getStatus()
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /* Helpers -------------------------------------------------------- */
    private void abrirParcelas() {
        String id = idSelecionado();
        if (id == null) return;
        new ui.financeiro.dialog.ParcelasContaReceberDialog(
            SwingUtilities.getWindowAncestor(this), id).setVisible(true);
        carregarTabela();
    }
    private String idSelecionado() {
        int sel = tabela.getSelectedRow();
        if (sel < 0) { JOptionPane.showMessageDialog(this,"Selecione uma linha"); return null; }
        return (String) model.getValueAt(sel, 0);
    }
    private JDateChooser criarDateChooser() {
        JDateChooser dc = new JDateChooser(new Date());
        dc.setPreferredSize(new Dimension(120,25));
        dc.setDateFormatString("dd/MM/yyyy");
        return dc;
    }
    private void esconderColunaID(JTable t) {
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
    }
    private void aplicarRenderers(JTable t) {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c = 2; c < t.getColumnCount(); c++)
            t.getColumnModel().getColumn(c).setCellRenderer(center);
    }
}
