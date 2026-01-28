package ui.comandas.painel;

import model.ComandaResumoModel;
import service.ComandaService;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PainelComandas extends JPanel {

    private final ComandaService service = new ComandaService();

    private static final DateTimeFormatter UI_DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final JLabel lblTempoMedio = new JLabel("Tempo médio: —");

    private final JComboBox<String> cbStatus = new JComboBox<>(
            new String[] { "aberta", "pendente", "fechada", "cancelada", "todas" });

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "ID", "Cliente", "Mesa", "Status", "Criado em", "Tempo (h)", "Total", "Pago", "Saldo" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };

    private final JTable table = new JTable(model);

    public PainelComandas() {
        setLayout(new BorderLayout(10, 10));
        UiKit.applyPanelBase(this);

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildCenterCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);

        configurarTabela();

        carregar();
    }

    /* ===================== VISUAL (TOP) ===================== */

    private JPanel buildTopCard() {
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("🧾 Comandas"));
        left.add(UiKit.hint("Filtre por status, abra uma comanda ou crie uma nova"));
        left.add(lblTempoMedio);
        topCard.add(left, BorderLayout.WEST);

        // lado direito em 2 linhas para não virar bagunça em telas menores
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 6, 2, 6);
        gc.anchor = GridBagConstraints.EAST;

        JLabel lblStatus = new JLabel("Status:");
        Dimension comboSize = new Dimension(180, 30);
        cbStatus.setPreferredSize(comboSize);
        cbStatus.setMinimumSize(new Dimension(140, 30));

        JButton btnFiltrar = UiKit.ghost("Filtrar");
        btnFiltrar.addActionListener(e -> carregar());

        JButton btnNova = UiKit.primary("➕ Nova Comanda");
        btnNova.addActionListener(e -> novaComanda());

        // Linha 0: Status + Combo
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        right.add(lblStatus, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        right.add(cbStatus, gc);

        // Linha 1: Botões alinhados à direita
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 1;
        right.add(Box.createHorizontalStrut(1), gc);

        gc.gridx = 1;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnFiltrar);
        buttons.add(btnNova);
        right.add(buttons, gc);

        topCard.add(right, BorderLayout.EAST);

        return topCard;
    }

    /* ===================== VISUAL (CENTER) ===================== */

    private JPanel buildCenterCard() {
        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.title("Lista"), BorderLayout.WEST);
        centerCard.add(header, BorderLayout.NORTH);

        centerCard.add(UiKit.scroll(table), BorderLayout.CENTER);
        return centerCard;
    }

    /* ===================== VISUAL (BOTTOM) ===================== */

    private JPanel buildBottomCard() {
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnAtualizar = UiKit.ghost("Atualizar");
        btnAtualizar.addActionListener(e -> carregar());

        JButton btnAbrir = UiKit.primary("Abrir");
        btnAbrir.addActionListener(e -> abrirSelecionada());

        right.add(btnAtualizar);
        right.add(btnAbrir);

        bottomCard.add(right, BorderLayout.EAST);

        return bottomCard;
    }

    private void configurarTabela() {
        UiKit.tableDefaults(table);

        // Zebra em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Badge no status
        table.getColumnModel().getColumn(3).setCellRenderer(UiKit.badgeStatusRenderer());

        // Alinhar colunas numéricas à direita (Tempo/Total/Pago/Saldo)
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        // Mantém zebra + alinhamento à direita
        DefaultTableCellRenderer zebraRight = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row,
                    int col) {
                Component c = zebra.getTableCellRendererComponent(t, v, sel, focus, row, col);
                JLabel l = (JLabel) c;
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        };

        table.getColumnModel().getColumn(5).setCellRenderer(zebraRight);
        table.getColumnModel().getColumn(6).setCellRenderer(zebraRight);
        table.getColumnModel().getColumn(7).setCellRenderer(zebraRight);
        table.getColumnModel().getColumn(8).setCellRenderer(zebraRight);

        // Ajuste de larguras (só estética, sem lógica)
        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(60); // ID
        tcm.getColumn(1).setPreferredWidth(220); // Cliente
        tcm.getColumn(2).setPreferredWidth(80); // Mesa
        tcm.getColumn(3).setPreferredWidth(110); // Status
        tcm.getColumn(4).setPreferredWidth(160); // Criado em
        tcm.getColumn(5).setPreferredWidth(110); // Tempo (h)
        tcm.getColumn(6).setPreferredWidth(100); // Total
        tcm.getColumn(7).setPreferredWidth(100); // Pago
        tcm.getColumn(8).setPreferredWidth(100); // Saldo

        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true); // visual/usabilidade, não altera lógica do service
    }

    /* ===================== LÓGICA (INTACTA) ===================== */

    private void carregar() {
        try {
            model.setRowCount(0);

            String status = (String) cbStatus.getSelectedItem();
            List<ComandaResumoModel> list = service.listarResumo(status);

            long somaMin = 0;
            int qtdMin = 0;
            for (ComandaResumoModel r : list) {
                Integer tempoMin = r.getTempoPermanenciaMin();
                if (tempoMin != null && tempoMin > 0) {
                    somaMin += tempoMin;
                    qtdMin++;
                }
                model.addRow(new Object[] {
                        r.getId(),
                        r.getCliente(),
                        r.getMesa(),
                        r.getStatus(),
                        formatarData(r.getCriadoEm()),
                        formatarTempoHoras(tempoMin),
                        money(r.getTotalLiquido()),
                        money(r.getTotalPago()),
                        money(r.getSaldo())
                });
            }

            if (qtdMin > 0) {
                long mediaMin = Math.round((double) somaMin / (double) qtdMin);
                lblTempoMedio.setText("Tempo médio: " + formatarTempoHoras((int) mediaMin));
            } else {
                lblTempoMedio.setText("Tempo médio: —");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void novaComanda() {
        Window w = SwingUtilities.getWindowAncestor(this);
        ui.comandas.dialog.ComandaNovaDialog d = new ui.comandas.dialog.ComandaNovaDialog(w);
        d.setVisible(true);

        if (d.getComandaIdCriada() != null) {
            carregar();
            abrirComanda(d.getComandaIdCriada());
        }
    }

    private void abrirSelecionada() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;

        // Se sorter estiver ativo, converte para índice do model
        int modelRow = table.convertRowIndexToModel(row);

        int id = (int) model.getValueAt(modelRow, 0);
        abrirComanda(id);
    }

    private void abrirComanda(int id) {
        Window w = SwingUtilities.getWindowAncestor(this);
        ui.comandas.dialog.ComandaDetalhesDialog d = new ui.comandas.dialog.ComandaDetalhesDialog(w, id);
        d.setVisible(true);
        carregar();
    }

    private static String money(double v) {
        return String.format("R$ %.2f", v);
    }

    private static String formatarData(String iso) {
        if (iso == null || iso.isBlank())
            return "—";
        try {
            return LocalDateTime.parse(iso).format(UI_DTF);
        } catch (Exception e) {
            return iso;
        }
    }

    private static String formatarTempoHoras(Integer minutos) {
        if (minutos == null || minutos <= 0)
            return "—";
        int h = minutos / 60;
        int m = minutos % 60;
        return String.format("%dh%02d", h, m);
    }
}
