package ui.eventos.painel;

import dao.JogoDAO;
import model.EventoModel;
import model.JogoModel;
import service.EventoService;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PainelEventos extends JPanel {

    private final EventoService service = new EventoService();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Nome", "Jogo", "Inicio", "Fim", "Status", "Taxa" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public PainelEventos() {
        setLayout(new BorderLayout(10, 10));
        UiKit.applyPanelBase(this);

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildCenterCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        configurarTabela();
        carregar();
    }

    private JPanel buildTopCard() {
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("Eventos / Liga"));
        left.add(UiKit.hint("Crie eventos e gerencie inscricoes"));
        topCard.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btnNovo = UiKit.primary("Novo Evento");
        btnNovo.addActionListener(e -> novoEvento());

        JButton btnAtualizar = UiKit.ghost("Atualizar");
        btnAtualizar.addActionListener(e -> carregar());

        right.add(btnAtualizar);
        right.add(btnNovo);

        topCard.add(right, BorderLayout.EAST);
        return topCard;
    }

    private JPanel buildCenterCard() {
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));
        centerCard.add(UiKit.title("Lista"), BorderLayout.NORTH);
        centerCard.add(UiKit.scroll(table), BorderLayout.CENTER);
        return centerCard;
    }

    private JPanel buildBottomCard() {
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnEditar = UiKit.ghost("Editar");
        btnEditar.addActionListener(e -> editarSelecionado());

        JButton btnAbrir = UiKit.primary("Abrir");
        btnAbrir.addActionListener(e -> abrirSelecionado());

        right.add(btnEditar);
        right.add(btnAbrir);
        bottomCard.add(right, BorderLayout.EAST);
        return bottomCard;
    }

    private void configurarTabela() {
        UiKit.tableDefaults(table);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        table.getColumnModel().getColumn(5).setCellRenderer(UiKit.badgeStatusRenderer());

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(6).setCellRenderer(right);

        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setMinWidth(0);
        tcm.getColumn(0).setMaxWidth(0);
        tcm.getColumn(0).setPreferredWidth(0);

        tcm.getColumn(1).setPreferredWidth(240);
        tcm.getColumn(2).setPreferredWidth(140);
        tcm.getColumn(3).setPreferredWidth(110);
        tcm.getColumn(4).setPreferredWidth(110);
        tcm.getColumn(5).setPreferredWidth(110);
        tcm.getColumn(6).setPreferredWidth(90);
    }

    private void carregar() {
        try {
            model.setRowCount(0);
            List<EventoModel> eventos = service.listarEventos();

            Map<String, String> jogos = carregarMapaJogos();
            NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

            for (EventoModel e : eventos) {
                model.addRow(new Object[] {
                        e.getId(),
                        e.getNome(),
                        jogos.getOrDefault(e.getJogoId(), e.getJogoId()),
                        formatarData(e.getDataInicio()),
                        formatarData(e.getDataFim()),
                        e.getStatus(),
                        moeda.format(e.getTaxaInscricao())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, String> carregarMapaJogos() {
        Map<String, String> out = new HashMap<>();
        try {
            List<JogoModel> jogos = new JogoDAO().listarTodos();
            for (JogoModel j : jogos) {
                out.put(j.getId(), j.getNome());
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void novoEvento() {
        Window w = SwingUtilities.getWindowAncestor(this);
        ui.eventos.dialog.EventoFormDialog d = new ui.eventos.dialog.EventoFormDialog(w, null);
        d.setVisible(true);
        if (d.isSalvo()) {
            carregar();
        }
    }

    private void editarSelecionado() {
        String id = getSelectedId();
        if (id == null) {
            return;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        ui.eventos.dialog.EventoFormDialog d = new ui.eventos.dialog.EventoFormDialog(w, id);
        d.setVisible(true);
        if (d.isSalvo()) {
            carregar();
        }
    }

    private void abrirSelecionado() {
        String id = getSelectedId();
        if (id == null) {
            return;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        ui.eventos.dialog.EventoDetalhesDialog d = new ui.eventos.dialog.EventoDetalhesDialog(w, id);
        d.setVisible(true);
        carregar();
    }

    private String getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        return (String) model.getValueAt(modelRow, 0);
    }

    private static String formatarData(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        } catch (Exception e) {
            return iso;
        }
    }
}
