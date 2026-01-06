package ui.comandas.painel;

import model.ComandaResumoModel;
import service.ComandaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PainelComandas extends JPanel {

    private final ComandaService service = new ComandaService();

    private static final DateTimeFormatter UI_DTF =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JComboBox<String> cbStatus = new JComboBox<>(
            new String[]{"aberta", "pendente", "fechada", "cancelada", "todas"}
    );

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Cliente", "Mesa", "Status", "Criado em", "Total", "Pago", "Saldo"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };

    private final JTable table = new JTable(model);

    public PainelComandas() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTop(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        table.setRowHeight(26);

        carregar();
    }

    private JPanel buildTop() {
        JPanel p = new JPanel(new BorderLayout(10, 0));

        JLabel title = new JLabel("🧾 Comandas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(new JLabel("Status:"));
        right.add(cbStatus);

        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> carregar());

        JButton btnNova = new JButton("➕ Nova Comanda");
        btnNova.addActionListener(e -> novaComanda());

        right.add(btnFiltrar);
        right.add(btnNova);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> carregar());

        JButton btnAbrir = new JButton("Abrir");
        btnAbrir.addActionListener(e -> abrirSelecionada());

        p.add(btnAtualizar);
        p.add(btnAbrir);
        return p;
    }

    private void carregar() {
        try {
            model.setRowCount(0);

            String status = (String) cbStatus.getSelectedItem();
            List<ComandaResumoModel> list = service.listarResumo(status);

            for (ComandaResumoModel r : list) {
                model.addRow(new Object[]{
                        r.getId(),
                        r.getCliente(),
                        r.getMesa(),
                        r.getStatus(),
                        formatarData(r.getCriadoEm()),     // ✅ AGORA SIM
                        money(r.getTotalLiquido()),
                        money(r.getTotalPago()),
                        money(r.getSaldo())
                });
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
        if (row < 0) return;

        int id = (int) model.getValueAt(row, 0);
        abrirComanda(id);
    }

    private void abrirComanda(int id) {
        Window w = SwingUtilities.getWindowAncestor(this);
        ui.comandas.dialog.ComandaDetalhesDialog d =
                new ui.comandas.dialog.ComandaDetalhesDialog(w, id);
        d.setVisible(true);
        carregar();
    }

    private static String money(double v) {
        return String.format("R$ %.2f", v);
    }

    private static String formatarData(String iso) {
        if (iso == null || iso.isBlank()) return "—";
        try {
            // Se vier ISO padrão: 2026-01-06T15:02:11
            return LocalDateTime.parse(iso).format(UI_DTF);
        } catch (Exception e) {
            return iso; // fallback defensivo
        }
    }
}
