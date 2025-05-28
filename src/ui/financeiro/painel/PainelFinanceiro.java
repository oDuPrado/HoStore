// PainelFinanceiro atualizado - Novo fluxo de Títulos/Parcelas
package ui.financeiro.painel;

import javax.swing.*;
import javax.swing.table.*;
import com.toedter.calendar.JDateChooser;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.*;
import java.util.*;
import java.util.List;

import dao.*;
import model.*;
import service.ContaPagarService;
import ui.financeiro.dialog.*;

public class PainelFinanceiro extends JPanel {

    /* ────── services/daos ────── */
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
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Contas a Pagar", criarPainelContasPagar());
        tabs.addTab("Contas a Receber", criarPainelContasReceber());
        tabs.addTab("Resumo Financeiro", criarPainelResumo());
        add(tabs, BorderLayout.CENTER);
    }

    /* ───────────────────────────────────── Contas a PAGAR ── */
    private JPanel criarPainelContasPagar() {
        JPanel painel = new JPanel(new BorderLayout());

        /* Filtros */
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        filtros.add(new JLabel("Fornecedor:"));
        cbFornecedor = new JComboBox<>();
        cbFornecedor.addItem("Todos");
        try {
            fornecedorDAO.listar(null, null, null, null)
                    .forEach(f -> cbFornecedor.addItem(f.getNome()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        filtros.add(cbFornecedor);

        filtros.add(new JLabel("Status:"));
        cbStatusPagar = new JComboBox<>(new String[] { "Todos", "aberto", "pago", "vencido" });
        filtros.add(cbStatusPagar);

        filtros.add(new JLabel("De:"));
        dtInicioPagar = criarDateChooser();
        filtros.add(dtInicioPagar);

        filtros.add(new JLabel("Até:"));
        dtFimPagar = criarDateChooser();
        filtros.add(dtFimPagar);

        JButton btnFiltrar = new JButton("Filtrar");
        filtros.add(btnFiltrar);

        painel.add(filtros, BorderLayout.NORTH);

        /* Tabela */
        String[] cols = { "ID", "Fornecedor", "Data Geração", "Valor Total", "Valor Pago", "Em Aberto", "Status" };
        modelPagar = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabelaPagar = new JTable(modelPagar);
        esconderColunaID(tabelaPagar);
        aplicarRenderers(tabelaPagar);
        painel.add(new JScrollPane(tabelaPagar), BorderLayout.CENTER);

        /* Botões */
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btNovo = new JButton("Novo");
        JButton btEditar = new JButton("Parcelas");
        JButton btExcluir = new JButton("Excluir");
        JButton btRefresh = new JButton("Atualizar");
        Arrays.asList(btNovo, btEditar, btExcluir, btRefresh).forEach(acoes::add);
        painel.add(acoes, BorderLayout.SOUTH);

        /* Ações */
        btRefresh.addActionListener(e -> carregarTabela());
        btnFiltrar.addActionListener(e -> carregarTabela());

        btNovo.addActionListener(e -> {
            new ContaPagarDialog((Frame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
            carregarTabela();
        });

        btEditar.addActionListener(e -> {
            String idSel = idSelecionado();
            if (idSel == null)
                return;
            try {
                TituloContaPagarModel titulo = tituloDAO.buscarPorId(idSel);
                if (titulo != null) {
                    new ParcelasTituloDialog(
                            SwingUtilities.getWindowAncestor(this), titulo.getId()).setVisible(true);

                    carregarTabela();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btExcluir.addActionListener(e -> {
            String id = idSelecionado();
            if (id == null)
                return;
            int op = JOptionPane.showConfirmDialog(
                    this,
                    "Deseja realmente excluir este Título e todas as suas parcelas?",
                    "Confirmar Exclusão",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                try {
                    // excluir parcelas associadas
                    parcelaDAO.listarPorTitulo(id)
                            .forEach(p -> {
                                try {
                                    parcelaDAO.excluir(p.getId());
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                    // excluir título
                    tituloDAO.excluir(id);
                    carregarTabela();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            this,
                            "Erro ao excluir:\n" + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Duplo clique na linha
        tabelaPagar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btEditar.doClick();
                }
            }
        });

        carregarTabela(); // inicial
        return painel;
    }

    private void carregarTabela() {
        modelPagar.setRowCount(0);
        try {
            String fornecedorFiltro = (String) cbFornecedor.getSelectedItem();
            String statusFiltro = (String) cbStatusPagar.getSelectedItem();
            Date dIni = dtInicioPagar.getDate();
            Date dFim = dtFimPagar.getDate();

            List<TituloContaPagarModel> titulos = tituloDAO.listarTodos();
            for (TituloContaPagarModel t : titulos) {
                String fornNome = fornecedorDAO.buscarPorId(t.getFornecedorId()).getNome();
                if (!"Todos".equals(fornecedorFiltro) && !fornNome.equals(fornecedorFiltro))
                    continue;
                if (!"Todos".equals(statusFiltro) && !t.getStatus().equalsIgnoreCase(statusFiltro))
                    continue;

                // calcula valores baseados nas parcelas
                List<ParcelaContaPagarModel> parcelas = parcelaDAO.listarPorTitulo(t.getId());
                double valorTotal = parcelas.stream().mapToDouble(ParcelaContaPagarModel::getValorNominal).sum();
                double valorPago = parcelas.stream().mapToDouble(ParcelaContaPagarModel::getValorPago).sum();
                double valorAberto = valorTotal - valorPago;

                // filtro por data de geração
                Date dataGeracao = optDate(t.getDataGeracao());
                if (dataGeracao != null && (dataGeracao.before(dIni) || dataGeracao.after(dFim)))
                    continue;

                modelPagar.addRow(new Object[] {
                        t.getId(),
                        fornNome,
                        visFmt.format(dataGeracao),
                        moneyFmt.format(valorTotal),
                        moneyFmt.format(valorPago),
                        moneyFmt.format(valorAberto),
                        t.getStatus()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* helpers */
    private Date optDate(String iso) {
        try {
            return iso == null ? null : sqlFmt.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    private JDateChooser criarDateChooser() {
        JDateChooser dc = new JDateChooser(new Date());
        dc.setPreferredSize(new Dimension(120, 25));
        dc.setDateFormatString("dd/MM/yyyy");
        return dc;
    }

    private void esconderColunaID(JTable t) {
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setPreferredWidth(0);
    }

    private void aplicarRenderers(JTable t) {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c = 2; c < t.getColumnCount(); c++) {
            t.getColumnModel().getColumn(c).setCellRenderer(center);
        }
    }

    private String idSelecionado() {
        int sel = tabelaPagar.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha");
            return null;
        }
        return (String) modelPagar.getValueAt(sel, 0);
    }

    /* ─────────────────────────── Contas a Receber e Resumo ── */
    private JPanel criarPainelContasReceber() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("Contas a Receber – em implementação", SwingConstants.CENTER), BorderLayout.CENTER);
        return p;
    }

    private JPanel criarPainelResumo() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("<html><h2>Resumo Financeiro em desenvolvimento...</h2></html>", SwingConstants.CENTER),
                BorderLayout.CENTER);
        return p;
    }
}
