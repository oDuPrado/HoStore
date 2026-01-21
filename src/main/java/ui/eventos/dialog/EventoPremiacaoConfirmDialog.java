package ui.eventos.dialog;

import dao.ClienteDAO;
import dao.ProdutoDAO;
import model.ClienteModel;
import model.EventoParticipanteModel;
import model.EventoPremiacaoModel;
import model.EventoRankingModel;
import model.ProdutoModel;
import service.EventoService;
import util.AlertUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventoPremiacaoConfirmDialog extends JDialog {

    private final String eventoId;
    private final EventoService service = new EventoService();
    private boolean confirmado;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "OK", "ID", "ProdutoId", "Participante", "Tipo", "Produto", "Qtd", "Credito" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0 || col == 6 || col == 7;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            if (columnIndex == 6) return Integer.class;
            if (columnIndex == 7) return Double.class;
            return String.class;
        }
    };
    private final JTable table = new JTable(model);

    public EventoPremiacaoConfirmDialog(Window owner, String eventoId) {
        super(owner, "Confirmar Premiacao", ModalityType.APPLICATION_MODAL);
        this.eventoId = eventoId;

        UiKit.applyDialogBase(this);
        setSize(980, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildCenterCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        configurarTabela();
        carregarPremiacoes();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Confirmar Premiacoes"));
        left.add(UiKit.hint("Desmarque o que nao deve premiar. Ajuste qtd/credito se precisar."));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JPanel buildCenterCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));
        card.add(UiKit.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomCard() {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnEditar = UiKit.ghost("Voltar e Editar");
        btnEditar.addActionListener(e -> dispose());

        JButton btnConfirmar = UiKit.primary("Confirmar Selecionados");
        btnConfirmar.addActionListener(e -> confirmar());

        right.add(btnEditar);
        right.add(btnConfirmar);
        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void configurarTabela() {
        UiKit.tableDefaults(table);
        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(1).setMinWidth(0);
        tcm.getColumn(1).setMaxWidth(0);
        tcm.getColumn(1).setPreferredWidth(0);
        tcm.getColumn(2).setMinWidth(0);
        tcm.getColumn(2).setMaxWidth(0);
        tcm.getColumn(2).setPreferredWidth(0);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tcm.getColumn(6).setCellRenderer(right);
        tcm.getColumn(7).setCellRenderer(new MoneyRenderer());
    }

    private void carregarPremiacoes() {
        try {
            model.setRowCount(0);
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
                String produtoNome = nomeProduto(p.getProdutoId());
                model.addRow(new Object[] {
                        true,
                        p.getId(),
                        p.getProdutoId(),
                        nome,
                        p.getTipo(),
                        produtoNome,
                        p.getQuantidade(),
                        p.getValorCredito()
                });
            }
        } catch (Exception e) {
            AlertUtils.error("Erro ao carregar premiacoes:\n" + e.getMessage());
        }
    }

    private void confirmar() {
        try {
            List<String> remover = new ArrayList<>();
            List<EventoPremiacaoModel> atualizar = new ArrayList<>();

            for (int i = 0; i < model.getRowCount(); i++) {
                boolean ok = Boolean.TRUE.equals(model.getValueAt(i, 0));
                String id = String.valueOf(model.getValueAt(i, 1));
                if (!ok) {
                    remover.add(id);
                    continue;
                }

                EventoPremiacaoModel p = new EventoPremiacaoModel();
                p.setId(id);
                p.setTipo(String.valueOf(model.getValueAt(i, 4)));
                p.setProdutoId(asString(model.getValueAt(i, 2)));
                p.setQuantidade(asInt(model.getValueAt(i, 6)));
                p.setValorCredito(asDouble(model.getValueAt(i, 7)));
                atualizar.add(p);
            }

            if (!atualizar.isEmpty()) {
                service.atualizarPremiacoes(atualizar);
            }
            if (!remover.isEmpty()) {
                service.removerPremiacoes(remover);
            }

            confirmado = true;
            dispose();
        } catch (Exception e) {
            AlertUtils.error("Erro ao confirmar premiacoes:\n" + e.getMessage());
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

    private static String participanteNome(EventoParticipanteModel p, Map<String, String> nomes) {
        if (p.getClienteId() != null) {
            return nomes.getOrDefault(p.getClienteId(), p.getClienteId());
        }
        return p.getNomeAvulso();
    }

    private static String nomeProduto(String produtoId) {
        if (produtoId == null || produtoId.isBlank()) {
            return "";
        }
        ProdutoModel p = new ProdutoDAO().findById(produtoId, true);
        return (p != null) ? p.getNome() : produtoId;
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
