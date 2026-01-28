package ui.rh.painel;

import dao.RhFolhaDAO;
import dao.RhFuncionarioDAO;
import model.RhFolhaModel;
import model.RhFuncionarioModel;
import service.RhService;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RhFolhaPanel extends JPanel {

    private final RhFolhaDAO dao = new RhFolhaDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();
    private final RhService service = new RhService();
    private final DefaultTableModel model;
    private final JTable table;

    private String competenciaAtual = YearMonth.now().toString();

    public RhFolhaPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Folha de Pagamento"));

        JButton btnCompetencia = UiKit.ghost("Competencia");
        JButton btnGerar = UiKit.primary("Gerar folha");
        top.add(btnCompetencia);
        top.add(btnGerar);

        model = new DefaultTableModel(new Object[]{"ID", "Funcionario", "Salario", "Comissao", "Total Liquido", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        add(top, BorderLayout.NORTH);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        btnCompetencia.addActionListener(e -> escolherCompetencia());
        btnGerar.addActionListener(e -> gerarFolha());

        carregar();
    }

    private void escolherCompetencia() {
        String comp = JOptionPane.showInputDialog(this, "Competencia MM/AAAA", toBrCompetencia(competenciaAtual));
        if (comp == null || comp.isBlank()) return;
        competenciaAtual = toIsoCompetencia(comp);
        carregar();
    }

    private void gerarFolha() {
        try {
            int n = service.gerarFolhaCompetencia(competenciaAtual);
            JOptionPane.showMessageDialog(this, "Folha gerada para " + competenciaAtual + ": " + n + " funcionarios");
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar folha: " + ex.getMessage());
        }
    }

    private void carregar() {
        model.setRowCount(0);
        try {
            List<RhFuncionarioModel> funcs = funcDAO.listar(false);
            Map<String, String> nomes = new HashMap<>();
            for (RhFuncionarioModel f : funcs) nomes.put(f.getId(), f.getNome());

            List<RhFolhaModel> lista = dao.listarPorCompetencia(competenciaAtual);
            for (RhFolhaModel f : lista) {
                model.addRow(new Object[]{f.getId(), nomes.getOrDefault(f.getFuncionarioId(), f.getFuncionarioId()), f.getSalarioBase(), f.getComissao(), f.getTotalLiquido(), f.getStatus()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar folha: " + ex.getMessage());
        }
    }

    private void aplicarRenderers() {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(2).setCellRenderer(moneyRenderer(zebra));
        table.getColumnModel().getColumn(3).setCellRenderer(moneyRenderer(zebra));
        table.getColumnModel().getColumn(4).setCellRenderer(moneyRenderer(zebra));
    }

    private DefaultTableCellRenderer moneyRenderer(DefaultTableCellRenderer zebraBase) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double v = (value instanceof Number n) ? n.doubleValue() : 0.0;
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setText(nf.format(v));
                return l;
            }
        };
    }

    private String toIsoCompetencia(String br) {
        if (br == null) return competenciaAtual;
        String s = br.trim();
        if (s.isEmpty()) return competenciaAtual;
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("MM/yyyy");
            return YearMonth.parse(s, f).toString();
        } catch (Exception e) {
            return competenciaAtual;
        }
    }

    private String toBrCompetencia(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            YearMonth ym = YearMonth.parse(iso);
            return ym.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }
}
