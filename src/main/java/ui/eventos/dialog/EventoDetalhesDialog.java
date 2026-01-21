package ui.eventos.dialog;

import controller.VendaController;
import dao.ClienteDAO;
import dao.ProdutoDAO;
import model.*;
import service.ComandaService;
import service.EventoService;
import service.SessaoService;
import ui.comandas.dialog.ComandaNovaDialog;
import ui.venda.dialog.VendaFinalizarDialog;
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

public class EventoDetalhesDialog extends JDialog {

    private final String eventoId;
    private final EventoService service = new EventoService();
    private EventoModel evento;

    private final DefaultTableModel participantesModel = new DefaultTableModel(
            new Object[] { "ID", "Participante", "Status", "Venda", "Comanda" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable participantesTable = new JTable(participantesModel);

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

    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblStatus = new JLabel();

    public EventoDetalhesDialog(Window owner, String eventoId) {
        super(owner, "Evento - Detalhes", ModalityType.APPLICATION_MODAL);
        this.eventoId = eventoId;

        UiKit.applyDialogBase(this);
        setSize(980, 720);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        carregarEvento();
        carregarParticipantes();
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(lblTitulo);
        left.add(UiKit.hint("Inscricoes e pagamentos da liga"));
        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(lblStatus);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Inscricoes", buildInscricoesTab());
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

    private JPanel buildInscricoesTab() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        UiKit.tableDefaults(participantesTable);
        participantesTable.getColumnModel().getColumn(2).setCellRenderer(UiKit.badgeStatusRenderer());

        TableColumnModel tcm = participantesTable.getColumnModel();
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setPreferredWidth(0);

        card.add(UiKit.scroll(participantesTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnInscrever = UiKit.primary("Inscrever");
        btnInscrever.addActionListener(e -> inscrever());

        JButton btnCobrar = UiKit.ghost("Cobrar Inscricao");
        btnCobrar.addActionListener(e -> cobrarInscricao());

        JButton btnComanda = UiKit.ghost("Adicionar a Comanda");
        btnComanda.addActionListener(e -> adicionarComanda());

        JButton btnCheckin = UiKit.ghost("Check-in");
        btnCheckin.addActionListener(e -> checkin());

        JButton btnDesistente = UiKit.ghost("Desistente");
        btnDesistente.addActionListener(e -> marcarDesistente());

        JButton btnPremiacao = UiKit.primary("Premiacao / Ranking");
        btnPremiacao.addActionListener(e -> abrirPremiacaoRanking());

        actions.add(btnInscrever);
        actions.add(btnCobrar);
        actions.add(btnComanda);
        actions.add(btnCheckin);
        actions.add(btnDesistente);
        actions.add(btnPremiacao);

        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildRankingTab() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.hint("Edite pontos/colocacao. Ordene por pontos, gere a colocacao e salve para atualizar a premiacao."),
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
        btnSalvar.setToolTipText("Salva o ranking e atualiza a premiacao pendente.");

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
        JPanel regrasHeader = new JPanel(new GridLayout(0, 1, 0, 2));
        regrasHeader.setOpaque(false);
        regrasHeader.add(UiKit.title("Regras de Premiacao"));
        regrasHeader.add(UiKit.hint("Crie regras por faixa e salve antes de aplicar."));
        regrasCard.add(regrasHeader, BorderLayout.NORTH);

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
        btnAdd.setToolTipText("Cria uma nova regra de premiacao.");

        JButton btnSelecionarProduto = UiKit.ghost("Selecionar Produto");
        btnSelecionarProduto.addActionListener(e -> selecionarProdutoRegra());
        btnSelecionarProduto.setToolTipText("Vincula um produto/booster a regra selecionada.");

        JButton btnRemover = UiKit.ghost("Remover");
        btnRemover.addActionListener(e -> removerRegraSelecionada());
        btnRemover.setToolTipText("Remove a regra selecionada.");

        JButton btnSalvar = UiKit.primary("Salvar Regras");
        btnSalvar.addActionListener(e -> salvarRegras());
        btnSalvar.setToolTipText("Salva as regras de premiacao do evento.");

        regrasActions.add(btnAdd);
        regrasActions.add(btnSelecionarProduto);
        regrasActions.add(btnRemover);
        regrasActions.add(btnSalvar);
        regrasCard.add(regrasActions, BorderLayout.SOUTH);

        JPanel premioCard = UiKit.card();
        premioCard.setLayout(new BorderLayout(8, 8));
        JPanel premioHeader = new JPanel(new GridLayout(0, 1, 0, 2));
        premioHeader.setOpaque(false);
        premioHeader.add(UiKit.title("Premiacoes (por ranking)"));
        premioHeader.add(UiKit.hint("Clique em aplicar regras para gerar premiacoes pendentes por colocacao."));
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
        btnAplicar.setToolTipText("Gera premiacoes pendentes com base no ranking.");

        JButton btnEntregar = UiKit.primary("Entregar Produto");
        btnEntregar.addActionListener(e -> entregarProduto());
        btnEntregar.setToolTipText("Baixa estoque e marca premio como entregue.");

        JButton btnCredito = UiKit.ghost("Aplicar Credito");
        btnCredito.addActionListener(e -> entregarCredito());
        btnCredito.setToolTipText("Lanca credito de loja e marca premio como entregue.");

        actions.add(btnAplicar);
        actions.add(btnEntregar);
        actions.add(btnCredito);
        premioCard.add(actions, BorderLayout.SOUTH);

        panel.add(regrasCard, BorderLayout.NORTH);
        panel.add(premioCard, BorderLayout.CENTER);
        return panel;
    }

    private void carregarEvento() {
        evento = service.buscarEvento(eventoId);
        if (evento == null) {
            lblTitulo.setText("Evento nao encontrado");
            return;
        }
        if (evento.getProdutoInscricaoId() == null || evento.getProdutoInscricaoId().isBlank()) {
            try {
                String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
                service.salvarEvento(evento, usuario);
                evento = service.buscarEvento(eventoId);
            } catch (Exception e) {
                AlertUtils.error("Erro ao preparar produto de inscricao:\n" + e.getMessage());
            }
        }
        lblTitulo.setText("Evento: " + evento.getNome());
        lblStatus.setText("Status: " + evento.getStatus());
    }

    private void carregarParticipantes() {
        try {
            participantesModel.setRowCount(0);
            Map<String, String> nomes = carregarMapaClientes();
            List<EventoParticipanteModel> participantes = service.listarParticipantes(eventoId);
            for (EventoParticipanteModel p : participantes) {
                String nome = p.getClienteId() != null
                        ? nomes.getOrDefault(p.getClienteId(), p.getClienteId())
                        : p.getNomeAvulso();
                participantesModel.addRow(new Object[] {
                        p.getId(),
                        nome,
                        p.getStatus(),
                        p.getVendaId(),
                        p.getComandaId()
                });
            }
        } catch (Exception e) {
            AlertUtils.error("Erro ao carregar participantes:\n" + e.getMessage());
        }
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

    private void inscrever() {
        EventoInscricaoDialog d = new EventoInscricaoDialog(this);
        d.setVisible(true);
        if (!d.isConfirmado()) {
            return;
        }

        ClienteModel cliente = d.getClienteSelecionado();
        String nomeAvulso = d.getNomeAvulso();
        if ((cliente == null || cliente.getId() == null) && nomeAvulso.isBlank()) {
            AlertUtils.warn("Selecione um cliente ou informe nome avulso.");
            return;
        }

        String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
        String clienteId = (cliente != null ? cliente.getId() : null);
        EventoParticipanteModel participante = service.inscreverParticipante(
                eventoId, clienteId, nomeAvulso, null, null, usuario);
        carregarParticipantes();
        carregarRanking();

        EventoInscricaoOpcaoDialog opcaoDialog = new EventoInscricaoOpcaoDialog(this);
        opcaoDialog.setVisible(true);
        if (opcaoDialog.getOpcao() == EventoInscricaoOpcaoDialog.Opcao.PAGAR_AGORA) {
            cobrarInscricao(participante.getId());
        } else if (opcaoDialog.getOpcao() == EventoInscricaoOpcaoDialog.Opcao.ADICIONAR_COMANDA) {
            adicionarComanda(participante.getId());
        }
    }

    private void cobrarInscricao() {
        int row = participantesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = participantesTable.convertRowIndexToModel(row);
        String participanteId = (String) participantesModel.getValueAt(modelRow, 0);
        cobrarInscricao(participanteId);
    }

    private void cobrarInscricao(String participanteId) {
        EventoParticipanteModel p = null;
        for (EventoParticipanteModel it : service.listarParticipantes(eventoId)) {
            if (participanteId.equals(it.getId())) {
                p = it;
                break;
            }
        }
        if (p == null) {
            return;
        }
        if (p.getComandaId() != null && "inscrito_comanda".equalsIgnoreCase(p.getStatus())) {
            int op = JOptionPane.showConfirmDialog(this,
                    "Esta inscricao esta vinculada a comanda #" + p.getComandaId()
                            + ".\nDeseja abrir a comanda para pagar e fechar?",
                    "Comanda vinculada",
                    JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) {
                new ui.comandas.dialog.ComandaDetalhesDialog(this, p.getComandaId()).setVisible(true);
                carregarParticipantes();
            }
            return;
        }
        if (p.getVendaId() != null) {
            AlertUtils.warn("Inscricao ja paga.");
            return;
        }
        if (evento.getProdutoInscricaoId() == null) {
            AlertUtils.error("Evento sem produto de inscricao.");
            return;
        }

        String clienteId = (p.getClienteId() != null) ? p.getClienteId() : "AVULSO";

        ProdutoModel prod = new ProdutoDAO().findById(evento.getProdutoInscricaoId(), true);
        if (prod == null) {
            AlertUtils.error("Produto de inscricao nao encontrado.");
            return;
        }

        VendaController controller = new VendaController();
        VendaItemModel item = new VendaItemModel(prod.getId(), 1, evento.getTaxaInscricao(), 0);
        controller.adicionarItem(item);

        VendaFinalizarDialog dialog = new VendaFinalizarDialog(this, controller, clienteId, null);
        dialog.setVisible(true);

        Integer vendaId = dialog.getVendaIdGerada();
        if (vendaId != null) {
            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.vincularVendaParticipante(participanteId, vendaId, usuario);
            carregarParticipantes();
        }
    }

    private void adicionarComanda() {
        int row = participantesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = participantesTable.convertRowIndexToModel(row);
        String participanteId = (String) participantesModel.getValueAt(modelRow, 0);
        adicionarComanda(participanteId);
    }

    private void adicionarComanda(String participanteId) {
        EventoParticipanteModel p = null;
        for (EventoParticipanteModel it : service.listarParticipantes(eventoId)) {
            if (participanteId.equals(it.getId())) {
                p = it;
                break;
            }
        }
        if (p == null) {
            return;
        }
        if (p.getClienteId() == null) {
            AlertUtils.warn("Participante sem cliente cadastrado. Vincule um cliente para usar comanda.");
            return;
        }

        SelecionarComandaAbertaDialog dlg = new SelecionarComandaAbertaDialog(this, p.getClienteId());
        dlg.setVisible(true);

        Integer comandaId = dlg.getComandaIdSelecionada();
        if (dlg.isCriarNova()) {
            ComandaNovaDialog nova = new ComandaNovaDialog(this);
            nova.setVisible(true);
            comandaId = nova.getComandaIdCriada();
        }

        if (comandaId == null) {
            return;
        }

        try {
            ComandaModel c = new ComandaService().getComanda(comandaId);
            if (c == null) {
                AlertUtils.error("Comanda nao encontrada.");
                return;
            }
            if (!p.getClienteId().equals(c.getClienteId())) {
                AlertUtils.error("A comanda selecionada nao pertence ao cliente do participante.");
                return;
            }
            if ("fechada".equalsIgnoreCase(c.getStatus()) || "cancelada".equalsIgnoreCase(c.getStatus())) {
                AlertUtils.error("Comanda nao permite alteracoes.");
                return;
            }

            ProdutoModel prod = new ProdutoDAO().findById(evento.getProdutoInscricaoId(), true);
            if (prod == null) {
                AlertUtils.error("Produto de inscricao nao encontrado.");
                return;
            }

            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            int itemId = new ComandaService().adicionarItemRetornandoId(
                    comandaId, prod.getId(), 1, evento.getTaxaInscricao(), 0, 0,
                    "Inscricao evento " + evento.getNome(), usuario);

            service.vincularComandaParticipante(participanteId, comandaId, itemId, usuario);
            carregarParticipantes();
        } catch (Exception e) {
            AlertUtils.error("Erro ao adicionar na comanda:\n" + e.getMessage());
        }
    }

    private void checkin() {
        int row = participantesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = participantesTable.convertRowIndexToModel(row);
        String participanteId = (String) participantesModel.getValueAt(modelRow, 0);
        String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
        service.registrarCheckin(participanteId, usuario);
        carregarParticipantes();
    }

    private void marcarDesistente() {
        int row = participantesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = participantesTable.convertRowIndexToModel(row);
        String participanteId = (String) participantesModel.getValueAt(modelRow, 0);
        String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
        service.atualizarStatusParticipante(participanteId, "desistente", usuario);
        carregarParticipantes();
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
            if (!service.listarRegrasPremiacao(eventoId).isEmpty()) {
                service.aplicarPremiacaoPorRegras(eventoId);
            }
            carregarPremiacoes();
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
            service.aplicarPremiacaoPorRegras(eventoId);
            carregarPremiacoes();
        } catch (Exception e) {
            AlertUtils.error("Erro ao aplicar premiacao:\n" + e.getMessage());
        }
    }

    private void entregarProduto() {
        String premioId = getPremioSelecionado();
        if (premioId == null) {
            return;
        }
        String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
        service.entregarPremioProduto(premioId, usuario);
        carregarPremiacoes();
    }

    private void entregarCredito() {
        String premioId = getPremioSelecionado();
        if (premioId == null) {
            return;
        }
        String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
        service.entregarPremioCredito(premioId, usuario);
        carregarPremiacoes();
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
        SelectProdutoDialog dialog = new SelectProdutoDialog(this, tipo);
        dialog.setVisible(true);
        ProdutoModel selecionado = dialog.getSelecionado();
        if (selecionado != null) {
            regrasModel.setValueAt(selecionado.getId(), modelRow, 4);
        }
    }

    private void abrirPremiacaoRanking() {
        EventoPremiacaoDialog dlg = new EventoPremiacaoDialog(this, eventoId);
        dlg.setVisible(true);
        carregarParticipantes();
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
