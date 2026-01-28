package ui.rh.painel;

import dao.RhEscalaDAO;
import dao.RhFuncionarioDAO;
import model.RhEscalaModel;
import model.RhFuncionarioModel;
import ui.rh.dialog.RhEscalaDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RhEscalaPanel extends JPanel {

    private final RhEscalaDAO dao = new RhEscalaDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel calendarGrid = new JPanel();
    private final JLabel lblMes = new JLabel();
    private YearMonth mesAtual = YearMonth.now();
    private List<RhEscalaModel> listaAtual;
    private Map<String, String> nomesAtual = new HashMap<>();

    public RhEscalaPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Escalas"));

        JButton btnAdd = UiKit.primary("Adicionar");
        JButton btnEdit = UiKit.ghost("Editar");
        JButton btnDel = UiKit.ghost("Excluir");
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnDel);

        model = new DefaultTableModel(new Object[]{"ID", "Funcionario", "Data", "Inicio", "Fim"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Calendario", criarCalendarioPanel());
        tabs.addTab("Lista", UiKit.scroll(table));

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhEscalaDialog((Frame) w, null).setVisible(true);
            carregar();
        });
        btnEdit.addActionListener(e -> editar());
        btnDel.addActionListener(e -> excluir());

        carregar();
    }

    private JPanel criarCalendarioPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        header.setOpaque(false);
        JButton btnPrev = UiKit.ghost("<");
        JButton btnNext = UiKit.ghost(">");
        header.add(btnPrev);
        header.add(lblMes);
        header.add(btnNext);

        btnPrev.addActionListener(e -> {
            mesAtual = mesAtual.minusMonths(1);
            carregar();
        });
        btnNext.addActionListener(e -> {
            mesAtual = mesAtual.plusMonths(1);
            carregar();
        });

        calendarGrid.setOpaque(false);
        calendarGrid.setLayout(new GridLayout(0, 7, 6, 6));

        root.add(header, BorderLayout.NORTH);
        root.add(calendarGrid, BorderLayout.CENTER);
        return root;
    }

    private void carregar() {
        model.setRowCount(0);
        try {
            List<RhFuncionarioModel> funcs = funcDAO.listar(false);
            Map<String, String> nomes = new HashMap<>();
            for (RhFuncionarioModel f : funcs) nomes.put(f.getId(), f.getNome());
            nomesAtual = nomes;

            List<RhEscalaModel> lista = dao.listar(null, null, null);
            listaAtual = lista;
            for (RhEscalaModel e : lista) {
                model.addRow(new Object[]{
                        e.getId(),
                        nomes.getOrDefault(e.getFuncionarioId(), e.getFuncionarioId()),
                        formatDateBr(e.getData()),
                        e.getInicio(),
                        e.getFim()
                });
            }
            renderCalendar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar escala: " + ex.getMessage());
        }
    }

    private void editar() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        try {
            RhEscalaModel esc = null;
            for (RhEscalaModel m : dao.listar(null, null, null)) {
                if (m.getId() == id) { esc = m; break; }
            }
            if (esc != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new RhEscalaDialog((Frame) w, esc).setVisible(true);
                carregar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao editar: " + ex.getMessage());
        }
    }

    private void excluir() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        int ok = JOptionPane.showConfirmDialog(this, "Excluir escala?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.excluir(id);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }

    private void aplicarRenderers() {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
    }

    private void renderCalendar() {
        calendarGrid.removeAll();

        String[] dias = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab"};
        for (String d : dias) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
            calendarGrid.add(l);
        }

        LocalDate primeiro = mesAtual.atDay(1);
        int dow = primeiro.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        int blanks = dow % 7; // domingo=0
        int diasNoMes = mesAtual.lengthOfMonth();

        for (int i = 0; i < blanks; i++) {
            calendarGrid.add(new JLabel(""));
        }

        Map<LocalDate, StringBuilder> map = new HashMap<>();
        if (listaAtual != null) {
            for (RhEscalaModel e : listaAtual) {
                if (e.getData() == null || e.getData().isBlank()) continue;
                try {
                    LocalDate d = LocalDate.parse(e.getData());
                    if (!YearMonth.from(d).equals(mesAtual)) continue;
                    String nome = nomesAtual.getOrDefault(e.getFuncionarioId(), e.getFuncionarioId());
                    String hora = safe(e.getInicio()) + "-" + safe(e.getFim());
                    String linha = "<b>" + nome + "</b> " + hora;
                    map.computeIfAbsent(d, k -> new StringBuilder()).append(linha).append("\n");
                } catch (Exception ignore) { }
            }
        }

        for (int day = 1; day <= diasNoMes; day++) {
            LocalDate d = mesAtual.atDay(day);
            JPanel cell = UiKit.card();
            cell.setLayout(new BorderLayout());
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JLabel head = new JLabel(String.valueOf(day));
            head.setFont(head.getFont().deriveFont(Font.BOLD, 12f));
            JLabel body = new JLabel();
            StringBuilder sb = map.get(d);
            if (sb != null) {
                String[] linhas = sb.toString().split("\n");
                StringBuilder html = new StringBuilder("<html>");
                for (String linha : linhas) {
                    if (!linha.isBlank()) {
                        html.append(linha).append("<br/>");
                    }
                }
                html.append("</html>");
                body.setText(html.toString());
            } else {
                body.setText("");
            }
            cell.add(head, BorderLayout.NORTH);
            cell.add(body, BorderLayout.CENTER);
            calendarGrid.add(cell);

            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Window w = SwingUtilities.getWindowAncestor(RhEscalaPanel.this);
                    new RhEscalaDialog((Frame) w, null, d.toString()).setVisible(true);
                    carregar();
                }
            });
        }

        int total = blanks + diasNoMes;
        int linhas = (int) Math.ceil(total / 7.0);
        int totalCells = linhas * 7;
        for (int i = total; i < totalCells; i++) {
            calendarGrid.add(new JLabel(""));
        }

        lblMes.setText(mesAtual.format(DateTimeFormatter.ofPattern("MMMM yyyy", new java.util.Locale("pt", "BR"))));
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String formatDateBr(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            String s = iso.trim();
            if (s.length() >= 10) s = s.substring(0, 10);
            LocalDate d = LocalDate.parse(s);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }
}
