package ui.eventos.dialog;

import dao.ClienteDAO;
import dao.ProdutoDAO;
import model.ClienteModel;
import model.EventoParticipanteModel;
import model.EventoPremiacaoModel;
import model.EventoPremiacaoRegraModel;
import model.EventoRankingModel;
import model.ProdutoModel;
import service.EventoService;
import service.SessaoService;
import util.AlertUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.RowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventoPremiacaoDialog extends JDialog {

    private final String eventoId;
    private final EventoService service = new EventoService();

    private final DefaultTableModel rankingModel = new DefaultTableModel(
            new Object[] { "ID", "ParticipanteId", "#", "Cliente", "Pontos", "Status" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 2 || col == 4;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return (columnIndex == 2 || columnIndex == 4) ? Integer.class : String.class;
        }
    };
    private final JTable rankingTable = new JTable(rankingModel);
    private final TableRowSorter<DefaultTableModel> rankingSorter = new TableRowSorter<>(rankingModel);

    private final DefaultTableModel regrasModel = new DefaultTableModel(
            new Object[] { "ID", "Inicio", "Fim", "Tipo", "Produto", "Qtd", "Credito", "Obs" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col >= 1;
        }
    };
    private final JTable regrasTable = new JTable(regrasModel);

    private final DefaultTableModel premiacoesModel = new DefaultTableModel(
            new Object[] { "ID", "Participante", "Tipo", "Produto", "Qtd", "Credito", "Status" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable premiacoesTable = new JTable(premiacoesModel);

    public EventoPremiacaoDialog(Window owner, String eventoId) {
        super(owner, "Ranking e Premiacao", ModalityType.APPLICATION_MODAL);
        this.eventoId = eventoId;

        UiKit.applyDialogBase(this);
        setSize(980, 720);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        carregarRanking();
        carregarRegras();
        carregarPremiacoes();
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Ranking e Premiacao"));
        left.add(UiKit.hint("Salve o ranking para gerar premiacoes. Edite regras e reaplique se precisar."));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Ranking", buildRankingTab());
        tabs.addTab("Premiacao", buildPremiacaoTab());
        return tabs;
    }

    private JPanel buildBottomCard() {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnFechar = UiKit.ghost("Fechar");
        btnFechar.addActionListener(e -> dispose());
        right.add(btnFechar);

        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private JPanel buildRankingTab() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.hint("Edite pontos/colocacao. Ordene, gere colocacao e salve para premiar."),
                BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        UiKit.tableDefaults(rankingTable);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        rankingTable.getColumnModel().getColumn(2).setCellRenderer(right);
        rankingTable.getColumnModel().getColumn(4).setCellRenderer(right);
        rankingTable.getColumnModel().getColumn(5).setCellRenderer(UiKit.badgeStatusRenderer());

        TableColumnModel tcm = rankingTable.getColumnModel();
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setPreferredWidth(0);
        tcm.getColumn(1).setMinWidth(0);
        tcm.getColumn(1).setMaxWidth(0);
        tcm.getColumn(1).setPreferredWidth(0);

        rankingTable.setRowSorter(rankingSorter);
        rankingSorter.setSortsOnUpdates(true);
        rankingSorter.setSortable(2, false);
        rankingSorter.setSortable(5, false);

        rankingModel.addTableModelListener(e -> {
            if (e.getColumn() == 2) {
                ordenarPorColocacao();
            }
        });

        card.add(UiKit.scroll(rankingTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnOrdenar = UiKit.ghost("Ordenar por Pontos");
        btnOrdenar.addActionListener(e -> ordenarPorPontos());
        btnOrdenar.setToolTipText("Ordena a tabela por pontos (desc) e nome.");

        JButton btnGerar = UiKit.ghost("Gerar Colocacao");
        btnGerar.addActionListener(e -> gerarColocacao());
        btnGerar.setToolTipText("Preenche a coluna # na ordem atual.");

        JButton btnSalvar = UiKit.primary("Salvar Ranking");
        btnSalvar.addActionListener(e -> salvarRanking());
        btnSalvar.setToolTipText("Salva o ranking e gera premiacoes.");

        actions.add(btnOrdenar);
        actions.add(btnGerar);
        actions.add(btnSalvar);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildPremiacaoTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JPanel regrasCard = UiKit.card();
        regrasCard.setLayout(new BorderLayout(8, 8));
        JPanel regrasHeader = new JPanel(new BorderLayout(10, 0));
        regrasHeader.setOpaque(false);
        JPanel regrasHeaderText = new JPanel(new GridLayout(0, 1, 0, 2));
        regrasHeaderText.setOpaque(false);
        regrasHeaderText.add(UiKit.title("Regras de Premiacao"));
        regrasHeaderText.add(UiKit.hint("Crie regras por faixa e salve antes de aplicar."));
        regrasHeader.add(regrasHeaderText, BorderLayout.WEST);

        UiKit.tableDefaults(regrasTable);
        TableColumnModel tcm = regrasTable.getColumnModel();
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setPreferredWidth(0);

        JComboBox<String> tipoCombo = new JComboBox<>(new String[] { "BOOSTER", "CREDITO", "PRODUTO" });
        regrasTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(tipoCombo));
        regrasTable.getColumnModel().getColumn(1).setCellEditor(new SpinnerEditor(1, 1, 999));
        regrasTable.getColumnModel().getColumn(2).setCellEditor(new SpinnerEditor(1, 1, 999));
        regrasTable.getColumnModel().getColumn(5).setCellEditor(new SpinnerEditor(1, 1, 999));
        regrasTable.getColumnModel().getColumn(6).setCellEditor(new MoneyEditor());
        regrasTable.getColumnModel().getColumn(6).setCellRenderer(new MoneyRenderer());

        regrasCard.add(UiKit.scroll(regrasTable), BorderLayout.CENTER);

        JPanel regrasActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        regrasActions.setOpaque(false);

        JButton btnAdd = UiKit.ghost("Criar Regra");
        btnAdd.addActionListener(e -> regrasModel.addRow(new Object[] { "", 1, 1, "BOOSTER", "", 1, null, "" }));

        JButton btnSelecionarProduto = UiKit.ghost("Selecionar Produto");
        btnSelecionarProduto.addActionListener(e -> selecionarProdutoRegra());

        JButton btnRemover = UiKit.ghost("Remover");
        btnRemover.addActionListener(e -> removerRegraSelecionada());

        JButton btnSalvar = UiKit.primary("Salvar Regras");
        btnSalvar.addActionListener(e -> salvarRegras());

        regrasActions.add(btnAdd);
        regrasActions.add(btnSelecionarProduto);
        regrasActions.add(btnRemover);
        regrasActions.add(btnSalvar);
        regrasHeader.add(regrasActions, BorderLayout.EAST);
        regrasCard.add(regrasHeader, BorderLayout.NORTH);

        JPanel premioCard = UiKit.card();
        premioCard.setLayout(new BorderLayout(8, 8));
        JPanel premioHeader = new JPanel(new GridLayout(0, 1, 0, 2));
        premioHeader.setOpaque(false);
        premioHeader.add(UiKit.title("Premiacoes (por ranking)"));
        premioHeader.add(UiKit.hint("Clique em aplicar regras para gerar premiacoes pendentes."));
        premioCard.add(premioHeader, BorderLayout.NORTH);

        UiKit.tableDefaults(premiacoesTable);
        TableColumnModel ptcm = premiacoesTable.getColumnModel();
        ptcm.getColumn(0).setMinWidth(0);
        ptcm.getColumn(0).setMaxWidth(0);
        ptcm.getColumn(0).setPreferredWidth(0);
        ptcm.getColumn(5).setCellRenderer(new MoneyRenderer());

        premioCard.add(UiKit.scroll(premiacoesTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnAplicar = UiKit.ghost("Aplicar Regras Agora");
        btnAplicar.addActionListener(e -> aplicarRegrasPremiacao());

        JButton btnEntregar = UiKit.primary("Entregar Produto");
        btnEntregar.addActionListener(e -> entregarProduto());

        JButton btnCredito = UiKit.ghost("Aplicar Credito");
        btnCredito.addActionListener(e -> entregarCredito());

        actions.add(btnAplicar);
        actions.add(btnEntregar);
        actions.add(btnCredito);
        premioCard.add(actions, BorderLayout.SOUTH);

        panel.add(regrasCard, BorderLayout.NORTH);
        panel.add(premioCard, BorderLayout.CENTER);
        return panel;
    }

    private void carregarRanking() {
        try {
            rankingModel.setRowCount(0);
            Map<String, String> nomes = carregarMapaClientes();
            List<EventoParticipanteModel> participantes = service.listarParticipantes(eventoId);
            Map<String, EventoParticipanteModel> byId = new HashMap<>();
            for (EventoParticipanteModel p : participantes) {
                byId.put(p.getId(), p);
            }

            List<EventoRankingModel> ranking = service.listarRanking(eventoId);
            if (ranking.isEmpty()) {
                for (EventoParticipanteModel p : participantes) {
                    rankingModel.addRow(new Object[] {
                            "",
                            p.getId(),
                            null,
                            participanteNome(p, nomes),
                            0,
                            p.getStatus()
                    });
                }
                ordenarPorPontos();
                return;
            }

            for (EventoRankingModel r : ranking) {
                EventoParticipanteModel p = byId.get(r.getParticipanteId());
                String nome = (p == null) ? r.getParticipanteId() : participanteNome(p, nomes);
                rankingModel.addRow(new Object[] {
                        r.getId(),
                        r.getParticipanteId(),
                        r.getColocacao(),
                        nome,
                        r.getPontos(),
                        (p != null ? p.getStatus() : "")
                });
            }
            ordenarPorPontos();
        } catch (Exception e) {
            AlertUtils.error("Erro ao carregar ranking:\n" + e.getMessage());
        }
    }

    private void carregarRegras() {
        try {
            regrasModel.setRowCount(0);
            List<EventoPremiacaoRegraModel> regras = service.listarRegrasPremiacao(eventoId);
            for (EventoPremiacaoRegraModel r : regras) {
                regrasModel.addRow(new Object[] {
                        r.getId(),
                        r.getColocacaoInicio(),
                        r.getColocacaoFim(),
                        r.getTipo(),
                        r.getProdutoId(),
                        r.getQuantidade(),
                        r.getValorCredito(),
                        r.getObservacoes()
                });
            }
        } catch (Exception e) {
            AlertUtils.error("Erro ao carregar regras:\n" + e.getMessage());
        }
    }

    private void carregarPremiacoes() {
        try {
            premiacoesModel.setRowCount(0);
            Map<String, String> nomes = carregarMapaClientes();
            Map<String, EventoParticipanteModel> participantes = new HashMap<>();
            for (EventoParticipanteModel p : service.listarParticipantes(eventoId)) {
                participantes.put(p.getId(), p);
            }

            Map<String, Integer> colocacaoPorParticipante = new HashMap<>();
            List<EventoRankingModel> ranking = service.listarRanking(eventoId);
            ranking.sort(Comparator.comparingInt(r -> r.getColocacao() == null ? Integer.MAX_VALUE : r.getColocacao()));
            for (EventoRankingModel r : ranking) {
                colocacaoPorParticipante.put(r.getParticipanteId(),
                        r.getColocacao() == null ? Integer.MAX_VALUE : r.getColocacao());
            }

            List<EventoPremiacaoModel> lista = service.listarPremiacoes(eventoId);
            lista.removeIf(p -> !colocacaoPorParticipante.containsKey(p.getParticipanteId()));
            lista.sort(Comparator.comparingInt(p -> colocacaoPorParticipante.getOrDefault(
                    p.getParticipanteId(), Integer.MAX_VALUE)));
            for (EventoPremiacaoModel p : lista) {
                EventoParticipanteModel part = participantes.get(p.getParticipanteId());
                String nome = (part == null) ? p.getParticipanteId() : participanteNome(part, nomes);
                premiacoesModel.addRow(new Object[] {
                        p.getId(),
                        nome,
                        p.getTipo(),
                        p.getProdutoId(),
                        p.getQuantidade(),
                        p.getValorCredito(),
                        p.getStatus()
                });
            }
        } catch (Exception e) {
            AlertUtils.error("Erro ao carregar premiacoes:\n" + e.getMessage());
        }
    }

    private Map<String, String> carregarMapaClientes() {
        Map<String, String> out = new HashMap<>();
        try {
            for (ClienteModel c : new ClienteDAO().findAll()) {
                out.put(c.getId(), c.getNome());
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void gerarColocacao() {
        int pos = 1;
        for (int viewRow = 0; viewRow < rankingTable.getRowCount(); viewRow++) {
            int modelRow = rankingTable.convertRowIndexToModel(viewRow);
            rankingModel.setValueAt(pos++, modelRow, 2);
        }
        ordenarPorColocacao();
    }

    private void salvarRanking() {
        try {
            List<EventoRankingModel> itens = new ArrayList<>();

            for (int i = 0; i < rankingModel.getRowCount(); i++) {
                EventoRankingModel r = new EventoRankingModel();
                Object id = rankingModel.getValueAt(i, 0);
                if (id != null && !id.toString().isBlank()) {
                    r.setId(id.toString());
                }

                String participanteId = String.valueOf(rankingModel.getValueAt(i, 1));
                r.setParticipanteId(participanteId);

                Object pontosObj = rankingModel.getValueAt(i, 4);
                r.setPontos((pontosObj instanceof Number) ? ((Number) pontosObj).intValue() : 0);

                Object colocObj = rankingModel.getValueAt(i, 2);
                if (colocObj instanceof Number) {
                    r.setColocacao(((Number) colocObj).intValue());
                }

                itens.add(r);
            }
            service.salvarRanking(eventoId, itens);
            carregarRanking();

            if (service.listarRegrasPremiacao(eventoId).isEmpty()) {
                AlertUtils.warn("Nenhuma regra de premiacao salva.");
                return;
            }

            service.aplicarPremiacaoPorRegras(eventoId);
            carregarPremiacoes();
            confirmarPremiacoes();
        } catch (Exception e) {
            AlertUtils.error("Erro ao salvar ranking:\n" + e.getMessage());
        }
    }

    private void salvarRegras() {
        try {
            List<EventoPremiacaoRegraModel> regras = new ArrayList<>();
            ProdutoDAO produtoDAO = new ProdutoDAO();
            for (int i = 0; i < regrasModel.getRowCount(); i++) {
                EventoPremiacaoRegraModel r = new EventoPremiacaoRegraModel();
                Object id = regrasModel.getValueAt(i, 0);
                if (id != null && !id.toString().isBlank()) {
                    r.setId(id.toString());
                }
                r.setColocacaoInicio(asInt(regrasModel.getValueAt(i, 1)));
                r.setColocacaoFim(asInt(regrasModel.getValueAt(i, 2)));
                r.setTipo(String.valueOf(regrasModel.getValueAt(i, 3)));
                r.setProdutoId(asString(regrasModel.getValueAt(i, 4)));
                r.setQuantidade(asInt(regrasModel.getValueAt(i, 5)));
                r.setValorCredito(asDouble(regrasModel.getValueAt(i, 6)));
                r.setObservacoes(asString(regrasModel.getValueAt(i, 7)));

                if ("BOOSTER".equalsIgnoreCase(r.getTipo())) {
                    if (r.getProdutoId() == null || r.getProdutoId().isBlank()) {
                        throw new RuntimeException("Selecione o booster para a regra.");
                    }
                    ProdutoModel p = produtoDAO.findById(r.getProdutoId(), true);
                    if (p == null || !"Booster".equalsIgnoreCase(p.getTipo())) {
                        throw new RuntimeException("Produto invalido para booster.");
                    }
                    if (p.getQuantidade() <= 0) {
                        throw new RuntimeException("Booster sem estoque disponivel.");
                    }
                    if (r.getQuantidade() == null || r.getQuantidade() <= 0) {
                        throw new RuntimeException("Quantidade invalida para booster.");
                    }
                } else if ("PRODUTO".equalsIgnoreCase(r.getTipo())) {
                    if (r.getProdutoId() == null || r.getProdutoId().isBlank()) {
                        throw new RuntimeException("Selecione o produto para a regra.");
                    }
                    if (r.getQuantidade() == null || r.getQuantidade() <= 0) {
                        throw new RuntimeException("Quantidade invalida para produto.");
                    }
                } else if ("CREDITO".equalsIgnoreCase(r.getTipo())) {
                    if (r.getValorCredito() == null || r.getValorCredito() <= 0) {
                        throw new RuntimeException("Informe o valor de credito.");
                    }
                }

                regras.add(r);
            }
            service.salvarRegrasPremiacao(eventoId, regras);
            carregarRegras();
        } catch (Exception e) {
            AlertUtils.error("Erro ao salvar regras:\n" + e.getMessage());
        }
    }

    private void removerRegraSelecionada() {
        int row = regrasTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = regrasTable.convertRowIndexToModel(row);
        regrasModel.removeRow(modelRow);
    }

    private void aplicarRegrasPremiacao() {
        try {
            if (service.listarRegrasPremiacao(eventoId).isEmpty()) {
                AlertUtils.warn("Nenhuma regra de premiacao salva.");
                return;
            }
            service.aplicarPremiacaoPorRegras(eventoId);
            carregarPremiacoes();
            confirmarPremiacoes();
        } catch (Exception e) {
            AlertUtils.error("Erro ao aplicar premiacao:\n" + e.getMessage());
        }
    }

    private void confirmarPremiacoes() {
        EventoPremiacaoConfirmDialog dlg = new EventoPremiacaoConfirmDialog(this, eventoId);
        dlg.setVisible(true);
        carregarPremiacoes();
    }

    private void entregarProduto() {
        String premioId = getPremioSelecionado();
        if (premioId == null) {
            return;
        }
        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.entregarPremioProduto(premioId, usuario);
            carregarPremiacoes();
        } catch (Exception e) {
            AlertUtils.error("Erro ao entregar premio:\n" + e.getMessage());
        }
    }

    private void entregarCredito() {
        String premioId = getPremioSelecionado();
        if (premioId == null) {
            return;
        }
        try {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.entregarPremioCredito(premioId, usuario);
            carregarPremiacoes();
        } catch (Exception e) {
            AlertUtils.error("Erro ao aplicar credito:\n" + e.getMessage());
        }
    }

    private String getPremioSelecionado() {
        int row = premiacoesTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = premiacoesTable.convertRowIndexToModel(row);
        return (String) premiacoesModel.getValueAt(modelRow, 0);
    }

    private static int asInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            return fmt.parse(v.toString()).doubleValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static String participanteNome(EventoParticipanteModel p, Map<String, String> nomes) {
        if (p.getClienteId() != null) {
            return nomes.getOrDefault(p.getClienteId(), p.getClienteId());
        }
        return p.getNomeAvulso();
    }

    private void ordenarPorPontos() {
        rankingSorter.setSortKeys(List.of(
                new RowSorter.SortKey(4, SortOrder.DESCENDING),
                new RowSorter.SortKey(3, SortOrder.ASCENDING)));
        rankingSorter.sort();
    }

    private void ordenarPorColocacao() {
        rankingSorter.setSortKeys(List.of(
                new RowSorter.SortKey(2, SortOrder.ASCENDING),
                new RowSorter.SortKey(3, SortOrder.ASCENDING)));
        rankingSorter.sort();
    }

    private void selecionarProdutoRegra() {
        int row = regrasTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = regrasTable.convertRowIndexToModel(row);
        String tipo = String.valueOf(regrasModel.getValueAt(modelRow, 3));
        if ("CREDITO".equalsIgnoreCase(tipo)) {
            AlertUtils.warn("Credito nao usa produto.");
            return;
        }
        if ("BOOSTER".equalsIgnoreCase(tipo) && !existeBoosterDisponivel()) {
            int op = JOptionPane.showConfirmDialog(this,
                    "Nao ha booster disponivel em estoque.\nDeseja cadastrar um booster agora?",
                    "Sem booster",
                    JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w instanceof JFrame f) {
                    new ui.estoque.dialog.CadastroBoosterDialog(f).setVisible(true);
                } else {
                    AlertUtils.warn("Abra a tela de cadastro de booster e tente novamente.");
                }
            }
            return;
        }
        SelectProdutoDialog dialog = new SelectProdutoDialog(this, tipo);
        dialog.setVisible(true);
        ProdutoModel selecionado = dialog.getSelecionado();
        if (selecionado != null) {
            regrasModel.setValueAt(selecionado.getId(), modelRow, 4);
        }
    }

    private boolean existeBoosterDisponivel() {
        List<ProdutoModel> produtos = new ProdutoDAO().listAll(true);
        for (ProdutoModel p : produtos) {
            if ("Booster".equalsIgnoreCase(p.getTipo()) && p.getQuantidade() > 0) {
                return true;
            }
        }
        return false;
    }

    private static class SpinnerEditor extends DefaultCellEditor {
        private final JSpinner spinner;

        SpinnerEditor(int min, int step, int max) {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(min, min, max, step));
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Number n) {
                spinner.setValue(n.intValue());
            }
            return spinner;
        }

        public Object getCellEditorValue() {
            return spinner.getValue();
        }
    }

    private static class MoneyEditor extends DefaultCellEditor {
        private final JFormattedTextField field;

        MoneyEditor() {
            super(new JTextField());
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            field = new JFormattedTextField(fmt);
            field.setBorder(null);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            field.setValue(value == null ? 0.0 : value);
            return field;
        }

        public Object getCellEditorValue() {
            return field.getValue();
        }
    }

    private static class MoneyRenderer extends DefaultTableCellRenderer {
        private final NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        @Override
        public void setValue(Object value) {
            if (value instanceof Number n) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(fmt.format(n.doubleValue()));
            } else {
                super.setValue(value);
            }
        }
    }
}
