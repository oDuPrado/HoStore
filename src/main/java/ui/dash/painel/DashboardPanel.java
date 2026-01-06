package ui.dash.painel;

import model.*;
import service.RelatoriosService;
import ui.dash.DashboardNavigator;
import ui.dash.component.DashboardCard;
import ui.relatorios.dialog.InfoFonteDialog;
import ui.relatorios.dialog.RelatorioTabelaDialog;
import util.MoedaUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final RelatoriosService service = new RelatoriosService();
    private final DashboardNavigator navigator;
    private final Window owner;

    // Filtros
    private final JComboBox<String> cbPeriodo = new JComboBox<>(new String[]{
            "Mês atual", "Mês passado", "Hoje", "Ontem", "Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias", "Personalizado"
    });
    private final JFormattedTextField tfIni = new JFormattedTextField(DateTimeFormatter.ofPattern("dd/MM/yyyy").toFormat());
    private final JFormattedTextField tfFim = new JFormattedTextField(DateTimeFormatter.ofPattern("dd/MM/yyyy").toFormat());
    private final JButton btAtualizar = new JButton("Atualizar");

    // Ações rápidas
    private final JButton btFechDia = new JButton("Fechamento do Dia");
    private final JButton btFechMes = new JButton("Fechamento do Mês");
    private final JButton btEstoqueCrit = new JButton("Estoque Crítico");
    private final JButton btInadimpl = new JButton("Inadimplência");
    private final JButton btPendFiscal = new JButton("Pendências Fiscais");
    private final JButton btRankingProd = new JButton("Ranking Produtos");
    private final JButton btAuditoriaEst = new JButton("Auditoria Estoque");

    // Alertas clicáveis
    private final JButton alEstoque = new JButton("⚠ Estoque crítico: ...");
    private final JButton alReceber = new JButton("⚠ Receber vencido: ...");
    private final JButton alPagar = new JButton("⚠ Pagar vencido: ...");
    private final JButton alFiscal = new JButton("⚠ Fiscal pendente: ...");

    // Cards KPI
    private final DashboardCard cFat = new DashboardCard("Faturamento (período)");
    private final DashboardCard cLucro = new DashboardCard("Lucro estimado (período)");
    private final DashboardCard cMargem = new DashboardCard("Margem % (período)");
    private final DashboardCard cTicket = new DashboardCard("Ticket médio (período)");
    private final DashboardCard cVendas = new DashboardCard("Vendas (período)");
    private final DashboardCard cItens = new DashboardCard("Itens vendidos (período)");
    private final DashboardCard cDesc = new DashboardCard("Descontos (período)");
    private final DashboardCard cTaxa = new DashboardCard("Taxa cartão estimada");

    private final DashboardCard cDev = new DashboardCard("Devoluções (R$)");
    private final DashboardCard cEst = new DashboardCard("Estornos (R$)");
    private final DashboardCard cCanc = new DashboardCard("Cancelamentos");

    // Tabelas
    private final DefaultTableModel tmEstoqueBaixo = new DefaultTableModel(new Object[]{"Produto", "Qtd"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tbEstoqueBaixo = new JTable(tmEstoqueBaixo);

    private final DefaultTableModel tmEncalhados = new DefaultTableModel(new Object[]{"Produto", "Qtd"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tbEncalhados = new JTable(tmEncalhados);

    private final DefaultTableModel tmTopQtd = new DefaultTableModel(new Object[]{"Produto", "Qtd", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tbTopQtd = new JTable(tmTopQtd);

    private final DefaultTableModel tmTopTotal = new DefaultTableModel(new Object[]{"Produto", "Qtd", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tbTopTotal = new JTable(tmTopTotal);

    private final DefaultTableModel tmSerie = new DefaultTableModel(new Object[]{"Dia", "Vendas", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tbSerie = new JTable(tmSerie);

    private final DefaultTableModel tmMix = new DefaultTableModel(new Object[]{"Pagamento", "Valor", "%"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tbMix = new JTable(tmMix);

    public DashboardPanel(Window owner, DashboardNavigator navigator) {
        this.owner = owner;
        this.navigator = navigator;
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        wire();
        defaults();
        carregarAsync();
    }

    public DashboardPanel() {
        // fallback pra quando alguém instanciar sem passar owner/navigator
        this(null, destino -> {});
    }

    private JPanel buildTop() {
        JPanel top = new JPanel(new BorderLayout(10,10));

        JLabel title = new JLabel("📊 Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tfIni.setColumns(10);
        tfFim.setColumns(10);

        filtros.add(new JLabel("Período:"));
        filtros.add(cbPeriodo);
        filtros.add(new JLabel("Início:"));
        filtros.add(tfIni);
        filtros.add(new JLabel("Fim:"));
        filtros.add(tfFim);
        filtros.add(btAtualizar);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(btFechDia);
        actions.add(btFechMes);
        actions.add(btEstoqueCrit);
        actions.add(btInadimpl);
        actions.add(btPendFiscal);
        actions.add(btRankingProd);
        actions.add(btAuditoriaEst);

        JPanel line1 = new JPanel(new BorderLayout());
        line1.setOpaque(false);
        line1.add(title, BorderLayout.WEST);
        line1.add(filtros, BorderLayout.EAST);

        top.add(line1, BorderLayout.NORTH);
        top.add(actions, BorderLayout.CENTER);
        top.add(buildAlerts(), BorderLayout.SOUTH);

        return top;
    }

    private JPanel buildAlerts() {
        JPanel alerts = new JPanel(new GridLayout(1, 4, 8, 8));

        styleAlertButton(alEstoque);
        styleAlertButton(alReceber);
        styleAlertButton(alPagar);
        styleAlertButton(alFiscal);

        alerts.add(alEstoque);
        alerts.add(alReceber);
        alerts.add(alPagar);
        alerts.add(alFiscal);

        return alerts;
    }

    private void styleAlertButton(JButton b) {
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusable(false);
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(10,10));

        JPanel kpiGrid = new JPanel(new GridLayout(3, 4, 10, 10));
        kpiGrid.add(cFat);
        kpiGrid.add(cLucro);
        kpiGrid.add(cMargem);
        kpiGrid.add(cTaxa);

        kpiGrid.add(cVendas);
        kpiGrid.add(cItens);
        kpiGrid.add(cTicket);
        kpiGrid.add(cDesc);

        kpiGrid.add(cDev);
        kpiGrid.add(cEst);
        kpiGrid.add(cCanc);
        kpiGrid.add(new JPanel()); // slot vazio

        // Tabs de listas
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Top produtos (Qtd)", wrapTable(tbTopQtd));
        tabs.addTab("Top produtos (R$)", wrapTable(tbTopTotal));
        tabs.addTab("Série diária", wrapTable(tbSerie));
        tabs.addTab("Mix pagamentos", wrapTable(tbMix));
        tabs.addTab("Estoque baixo", wrapTable(tbEstoqueBaixo));
        tabs.addTab("Encalhados", wrapTable(tbEncalhados));

        center.add(kpiGrid, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        return center;
    }

    private JScrollPane wrapTable(JTable t) {
        t.setRowHeight(22);
        return new JScrollPane(t);
    }

    private void wire() {
        btAtualizar.addActionListener(e -> carregarAsync());
        cbPeriodo.addActionListener(e -> aplicarModoPeriodoUI());

        // ações rápidas
        btFechDia.addActionListener(e -> abrirFechamentoDia());
        btFechMes.addActionListener(e -> abrirFechamentoMes());
        btEstoqueCrit.addActionListener(e -> abrirEstoqueCritico());
        btInadimpl.addActionListener(e -> abrirInadimplencia());
        btPendFiscal.addActionListener(e -> abrirPendenciasFiscais());
        btRankingProd.addActionListener(e -> abrirRankingProdutos());
        btAuditoriaEst.addActionListener(e -> abrirAuditoriaEstoque());

        // alertas clicáveis -> mesmos drilldowns
        alEstoque.addActionListener(e -> abrirEstoqueCritico());
        alReceber.addActionListener(e -> abrirInadimplencia());
        alPagar.addActionListener(e -> abrirInadimplencia());
        alFiscal.addActionListener(e -> abrirPendenciasFiscais());

        // drill-down nos cards
        cFat.setOnClick(this::abrirFechamentoPeriodo);
        cLucro.setOnClick(this::abrirFechamentoPeriodo);
        cMargem.setOnClick(this::abrirFechamentoPeriodo);
        cTicket.setOnClick(this::abrirFechamentoPeriodo);
        cVendas.setOnClick(this::abrirFechamentoPeriodo);
        cItens.setOnClick(this::abrirFechamentoPeriodo);

        cDesc.setOnClick(this::abrirFechamentoPeriodo);
        cTaxa.setOnClick(this::abrirFechamentoPeriodo);

        cDev.setOnClick(this::abrirFechamentoPeriodo);
        cEst.setOnClick(this::abrirFechamentoPeriodo);
        cCanc.setOnClick(this::abrirFechamentoPeriodo);

        // info “ⓘ fonte do dado”
        cFat.setOnInfo(() -> info("Faturamento", """
            Fonte: vendas.total_liquido
            Filtro: vendas.status <> 'cancelada'
            Período: date(vendas.data_venda) entre início e fim (ISO)
        """));
        cLucro.setOnInfo(() -> info("Lucro estimado", """
            Fonte: vendas_itens.preco, produtos.preco_compra
            Regra: SUM((vi.preco - COALESCE(p.preco_compra,0)) * vi.qtd)
            Filtro: venda não cancelada + período
            Observação: se custo por lote existir, substitui o custo aqui.
        """));
        cMargem.setOnInfo(() -> info("Margem", """
            Regra: lucro_estimado / faturamento
            Se faturamento = 0 => margem = 0
        """));
        cTaxa.setOnInfo(() -> info("Taxa cartão (estimada)", """
            Estimativa simples (limitação do modelo atual):
            - Sem bandeira/tipo/parcelas por pagamento, não dá precisão total.
            - Usa taxa média do mês em taxas_cartao + total de vendas com forma_pagamento contendo 'CARTAO'.
            Para 100% real: salvar metadados no pagamento (bandeira/tipo/parcelas).
        """));
        cDev.setOnInfo(() -> info("Devoluções", """
            Fonte: vendas_devolucoes (qtd, valor_unit)
            Regra: SUM(qtd * valor_unit)
            Observação: se valor_unit vier 0, seu fluxo ainda está gravando errado.
        """));
        cEst.setOnInfo(() -> info("Estornos", """
            Fonte: vendas_estornos_pagamentos.valor_estornado
            Regra: SUM(valor_estornado) no período
        """));
        cCanc.setOnInfo(() -> info("Cancelamentos", """
            Fonte: vendas.status='cancelada' no período
        """));
    }

    private void info(String titulo, String texto) {
        new InfoFonteDialog(owner, "ⓘ " + titulo, texto).setVisible(true);
    }

    private void defaults() {
        cbPeriodo.setSelectedItem("Mês atual");
        tfIni.setValue(LocalDate.now().withDayOfMonth(1));
        tfFim.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        aplicarModoPeriodoUI();
    }

    private void aplicarModoPeriodoUI() {
        boolean custom = "Personalizado".equals(cbPeriodo.getSelectedItem());
        tfIni.setEnabled(custom);
        tfFim.setEnabled(custom);
    }

    private PeriodoFiltro resolvePeriodo() {
        String sel = (String) cbPeriodo.getSelectedItem();

        return switch (sel) {
            case "Hoje" -> PeriodoFiltro.hoje();
            case "Ontem" -> PeriodoFiltro.ontem();
            case "Últimos 7 dias" -> PeriodoFiltro.ultimosDias(7);
            case "Últimos 30 dias" -> PeriodoFiltro.ultimosDias(30);
            case "Últimos 90 dias" -> PeriodoFiltro.ultimosDias(90);
            case "Mês passado" -> PeriodoFiltro.mesPassado();
            case "Mês atual" -> PeriodoFiltro.mesAtual();
            default -> { // personalizado
                try {
                    LocalDate ini = (LocalDate) tfIni.getValue();
                    LocalDate fim = (LocalDate) tfFim.getValue();
                    if (ini == null || fim == null) throw new IllegalArgumentException("Datas inválidas");
                    yield new PeriodoFiltro(ini, fim);
                } catch (Exception ex) {
                    yield PeriodoFiltro.mesAtual();
                }
            }
        };
    }

    private void carregarAsync() {
        btAtualizar.setEnabled(false);

        PeriodoFiltro periodo = resolvePeriodo();
        int threshold = 5;

        new SwingWorker<DashboardHomeModel, Void>() {
            @Override
            protected DashboardHomeModel doInBackground() {
                return service.carregarDashboardHome(periodo, threshold);
            }

            @Override
            protected void done() {
                try {
                    DashboardHomeModel home = get();
                    aplicar(home);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this,
                            "Erro ao carregar dashboard: " + ex.getMessage(),
                            "Dashboard", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btAtualizar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void aplicar(DashboardHomeModel home) {
        DashboardKpisModel k = home.kpis;

        // Cards principais
        cFat.setMoney(k.faturamento);
        cLucro.setMoney(k.lucroEstimado);
        cMargem.setPercent(k.margemPct);
        cTicket.setMoney(k.ticketMedio);
        cVendas.setNumber(k.qtdVendas);
        cItens.setNumber(k.itensVendidos);
        cDesc.setMoney(k.descontoTotal);
        cTaxa.setMoney(k.taxaCartaoEstimada);

        cDev.setMoney(k.devolucoesValor);
        cEst.setMoney(k.estornosValor);
        cCanc.setNumber(k.cancelamentosQtd);

        // Comparativos simples (período vs período anterior mesmo tamanho)
        Color good = new Color(0, 200, 0);
        Color bad = new Color(200, 70, 70);

        cFat.setDelta(deltaText("Δ", k.cmpFaturamento), k.cmpFaturamento.deltaAbs >= 0 ? good : bad);
        cLucro.setDelta(deltaText("Δ", k.cmpLucro), k.cmpLucro.deltaAbs >= 0 ? good : bad);

        // Alertas
        alFiscal.setText("⚠ Fiscal pendente: " + k.docsFiscaisPendentes);
        alReceber.setText("⚠ Receber vencido: " + MoedaUtil.brl(k.receberVencido));
        alPagar.setText("⚠ Pagar vencido: " + MoedaUtil.brl(k.pagarVencido));
        alEstoque.setText("⚠ Estoque baixo: " + home.estoqueBaixo.size() + " itens");

        // Tabelas
        tmEstoqueBaixo.setRowCount(0);
        for (EstoqueBaixoItemModel it : home.estoqueBaixo) {
            tmEstoqueBaixo.addRow(new Object[]{it.nome, it.quantidade});
        }

        tmEncalhados.setRowCount(0);
        for (EstoqueBaixoItemModel it : home.encalhados) {
            tmEncalhados.addRow(new Object[]{it.nome, it.quantidade});
        }

        tmTopQtd.setRowCount(0);
        for (ProdutoVendaResumoModel p : home.topProdutosQtd) {
            tmTopQtd.addRow(new Object[]{p.nome, p.quantidade, MoedaUtil.brl(p.total)});
        }

        tmTopTotal.setRowCount(0);
        for (ProdutoVendaResumoModel p : home.topProdutosTotal) {
            tmTopTotal.addRow(new Object[]{p.nome, p.quantidade, MoedaUtil.brl(p.total)});
        }

        tmSerie.setRowCount(0);
        for (SerieDiariaModel s : home.vendasPorDia) {
            tmSerie.addRow(new Object[]{s.diaIso, s.qtdVendas, MoedaUtil.brl(s.total)});
        }

        tmMix.setRowCount(0);
        for (PagamentoMixItemModel m : k.mixPagamentos) {
            tmMix.addRow(new Object[]{m.tipo, MoedaUtil.brl(m.valor), MoedaUtil.pct(m.pct)});
        }
    }

    private String deltaText(String prefix, ComparativoModel c) {
        String s = prefix + " " + MoedaUtil.brl(c.deltaAbs) + " (" + MoedaUtil.pct(c.deltaPct) + ")";
        return s;
    }

    // ----------------- Ações / relatórios -----------------

    private void abrirFechamentoDia() {
        PeriodoFiltro p = PeriodoFiltro.hoje();
        abrirFechamento(p, "Fechamento do Dia (" + p + ")");
    }

    private void abrirFechamentoMes() {
        PeriodoFiltro p = PeriodoFiltro.mesAtual();
        abrirFechamento(p, "Fechamento do Mês (" + p + ")");
    }

    private void abrirFechamentoPeriodo() {
        PeriodoFiltro p = resolvePeriodo();
        abrirFechamento(p, "Fechamento (Período) (" + p + ")");
    }

    private void abrirFechamento(PeriodoFiltro p, String titulo) {
        // Fechamento: top produtos + série diária + mix + KPIs (resumo)
        // Aqui mostramos uma tabela “resumo” de KPIs.
        new SwingWorker<DashboardHomeModel, Void>() {
            @Override protected DashboardHomeModel doInBackground() {
                return service.carregarDashboardHome(p, 5);
            }
            @Override protected void done() {
                try {
                    DashboardHomeModel home = get();
                    DashboardKpisModel k = home.kpis;

                    RelatorioTabelaDialog d = new RelatorioTabelaDialog(owner, titulo,
                            new Object[]{"Métrica", "Valor"});
                    List<Object[]> rows = new ArrayList<>();
                    rows.add(new Object[]{"Faturamento", MoedaUtil.brl(k.faturamento)});
                    rows.add(new Object[]{"Lucro estimado", MoedaUtil.brl(k.lucroEstimado)});
                    rows.add(new Object[]{"Margem", MoedaUtil.pct(k.margemPct)});
                    rows.add(new Object[]{"Vendas", k.qtdVendas});
                    rows.add(new Object[]{"Ticket médio", MoedaUtil.brl(k.ticketMedio)});
                    rows.add(new Object[]{"Itens vendidos", k.itensVendidos});
                    rows.add(new Object[]{"Descontos", MoedaUtil.brl(k.descontoTotal)});
                    rows.add(new Object[]{"Acréscimos", MoedaUtil.brl(k.acrescimoTotal)});
                    rows.add(new Object[]{"Devoluções (qtd)", k.devolucoesQtd});
                    rows.add(new Object[]{"Devoluções (R$)", MoedaUtil.brl(k.devolucoesValor)});
                    rows.add(new Object[]{"Estornos (R$)", MoedaUtil.brl(k.estornosValor)});
                    rows.add(new Object[]{"Cancelamentos", k.cancelamentosQtd});
                    rows.add(new Object[]{"Taxa cartão estimada", MoedaUtil.brl(k.taxaCartaoEstimada)});
                    rows.add(new Object[]{"Docs fiscais pendentes", k.docsFiscaisPendentes});
                    rows.add(new Object[]{"Receber vencido", MoedaUtil.brl(k.receberVencido)});
                    rows.add(new Object[]{"Pagar vencido", MoedaUtil.brl(k.pagarVencido)});
                    d.setRows(rows);
                    d.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + ex.getMessage(),
                            "Fechamento", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void abrirEstoqueCritico() {
        PeriodoFiltro p = resolvePeriodo();
        // Mostra duas coisas úteis: estoque baixo e encalhados
        new SwingWorker<DashboardHomeModel, Void>() {
            @Override protected DashboardHomeModel doInBackground() {
                return service.carregarDashboardHome(p, 5);
            }
            @Override protected void done() {
                try {
                    DashboardHomeModel home = get();

                    RelatorioTabelaDialog d = new RelatorioTabelaDialog(owner, "Estoque crítico (<=5) e Encalhados (" + p + ")",
                            new Object[]{"Tipo", "Produto", "Qtd"});
                    List<Object[]> rows = new ArrayList<>();
                    for (EstoqueBaixoItemModel it : home.estoqueBaixo) rows.add(new Object[]{"BAIXO", it.nome, it.quantidade});
                    for (EstoqueBaixoItemModel it : home.encalhados) rows.add(new Object[]{"PARADO", it.nome, it.quantidade});
                    d.setRows(rows);
                    d.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + ex.getMessage(),
                            "Estoque", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void abrirInadimplencia() {
        new SwingWorker<List<ParcelaVencidaModel>, Void>() {
            @Override protected List<ParcelaVencidaModel> doInBackground() {
                List<ParcelaVencidaModel> out = new ArrayList<>();
                out.addAll(service.listarVencidosReceber(50));
                out.addAll(service.listarVencidosPagar(50));
                return out;
            }
            @Override protected void done() {
                try {
                    List<ParcelaVencidaModel> list = get();
                    RelatorioTabelaDialog d = new RelatorioTabelaDialog(owner, "Inadimplência / Vencidos",
                            new Object[]{"Origem", "Cliente/Fornecedor", "Vencimento", "Valor em aberto", "Dias atraso"});
                    List<Object[]> rows = new ArrayList<>();
                    for (ParcelaVencidaModel p : list) {
                        rows.add(new Object[]{p.origem, p.clienteOuFornecedor, p.vencimentoIso, MoedaUtil.brl(p.valorAberto), p.diasAtraso});
                    }
                    d.setRows(rows);
                    d.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + ex.getMessage(),
                            "Vencidos", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void abrirPendenciasFiscais() {
        new SwingWorker<List<DocFiscalPendenteModel>, Void>() {
            @Override protected List<DocFiscalPendenteModel> doInBackground() {
                return service.listarDocsFiscaisPendentes(100);
            }
            @Override protected void done() {
                try {
                    List<DocFiscalPendenteModel> list = get();
                    RelatorioTabelaDialog d = new RelatorioTabelaDialog(owner, "Pendências fiscais",
                            new Object[]{"ID", "Venda", "Modelo", "Série", "Número", "Ambiente", "Status", "Erro"});
                    List<Object[]> rows = new ArrayList<>();
                    for (DocFiscalPendenteModel doc : list) {
                        rows.add(new Object[]{doc.id, doc.vendaId, doc.modelo, doc.serie, doc.numero, doc.ambiente, doc.status, doc.erro});
                    }
                    d.setRows(rows);
                    d.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + ex.getMessage(),
                            "Fiscal", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void abrirRankingProdutos() {
        PeriodoFiltro p = resolvePeriodo();
        new SwingWorker<DashboardHomeModel, Void>() {
            @Override protected DashboardHomeModel doInBackground() {
                return service.carregarDashboardHome(p, 5);
            }
            @Override protected void done() {
                try {
                    DashboardHomeModel home = get();
                    RelatorioTabelaDialog d = new RelatorioTabelaDialog(owner, "Ranking de produtos (" + p + ")",
                            new Object[]{"Tipo", "Produto", "Qtd", "Total"});
                    List<Object[]> rows = new ArrayList<>();
                    for (ProdutoVendaResumoModel pr : home.topProdutosQtd) {
                        rows.add(new Object[]{"TOP QTD", pr.nome, pr.quantidade, MoedaUtil.brl(pr.total)});
                    }
                    for (ProdutoVendaResumoModel pr : home.topProdutosTotal) {
                        rows.add(new Object[]{"TOP R$", pr.nome, pr.quantidade, MoedaUtil.brl(pr.total)});
                    }
                    d.setRows(rows);
                    d.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + ex.getMessage(),
                            "Ranking", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void abrirAuditoriaEstoque() {
        new SwingWorker<List<MovEstoqueModel>, Void>() {
            @Override protected List<MovEstoqueModel> doInBackground() {
                return service.listarUltimasMovimentacoesEstoque(200);
            }
            @Override protected void done() {
                try {
                    List<MovEstoqueModel> list = get();
                    RelatorioTabelaDialog d = new RelatorioTabelaDialog(owner, "Auditoria de Movimentações de Estoque",
                            new Object[]{"ID", "Produto", "Tipo", "Qtd", "Motivo", "Data", "Usuário"});
                    List<Object[]> rows = new ArrayList<>();
                    for (MovEstoqueModel m : list) {
                        rows.add(new Object[]{m.id, m.produtoNome, m.tipoMov, m.quantidade, m.motivo, m.dataIso, m.usuario});
                    }
                    d.setRows(rows);
                    d.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + ex.getMessage(),
                            "Auditoria", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
