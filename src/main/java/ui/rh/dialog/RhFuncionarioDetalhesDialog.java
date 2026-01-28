package ui.rh.dialog;

import dao.*;
import model.*;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class RhFuncionarioDetalhesDialog extends JDialog {

    private final RhFuncionarioDAO funcionarioDAO = new RhFuncionarioDAO();
    private final RhCargoDAO cargoDAO = new RhCargoDAO();
    private final RhSalarioDAO salarioDAO = new RhSalarioDAO();
    private final RhPontoDAO pontoDAO = new RhPontoDAO();
    private final RhEscalaDAO escalaDAO = new RhEscalaDAO();
    private final RhFeriasDAO feriasDAO = new RhFeriasDAO();
    private final RhComissaoDAO comissaoDAO = new RhComissaoDAO();
    private final RhFolhaDAO folhaDAO = new RhFolhaDAO();

    private RhFuncionarioModel funcionario;

    private final DefaultTableModel pontoModel = new DefaultTableModel(
            new Object[]{"Data", "Entrada", "Saida", "Intervalo", "Horas", "Origem"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel escalaModel = new DefaultTableModel(
            new Object[]{"Data", "Inicio", "Fim", "Observacoes"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel salarioModel = new DefaultTableModel(
            new Object[]{"Cargo", "Salario", "Inicio", "Fim", "Motivo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel feriasModel = new DefaultTableModel(
            new Object[]{"Inicio", "Fim", "Abono", "Status", "Observacoes"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel comissaoModel = new DefaultTableModel(
            new Object[]{"Data", "Venda", "%", "Valor", "Obs"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel folhaModel = new DefaultTableModel(
            new Object[]{"Competencia", "Salario", "Comissao", "Total Liquido", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public RhFuncionarioDetalhesDialog(Frame owner, RhFuncionarioModel f) {
        super(owner, "Detalhes do Funcionario", true);
        UiKit.applyDialogBase(this);
        this.funcionario = f;
        init();
        carregar();
        setSize(900, 620);
        setLocationRelativeTo(owner);
    }

    private void init() {
        setLayout(new BorderLayout(8, 8));

        JPanel header = UiKit.card();
        header.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        int r = 0;
        g.gridx = 0; g.gridy = r; header.add(new JLabel("Nome:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; header.add(new JLabel(), g);
        g.gridx = 2; g.fill = GridBagConstraints.NONE; g.weightx = 0; header.add(new JLabel("Cargo:"), g);
        g.gridx = 3; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; header.add(new JLabel(), g);
        r++;

        g.gridx = 0; g.gridy = r; g.fill = GridBagConstraints.NONE; g.weightx = 0; header.add(new JLabel("Tipo:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; header.add(new JLabel(), g);
        g.gridx = 2; g.fill = GridBagConstraints.NONE; g.weightx = 0; header.add(new JLabel("Documento:"), g);
        g.gridx = 3; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; header.add(new JLabel(), g);
        r++;

        g.gridx = 0; g.gridy = r; header.add(new JLabel("Salario base:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        g.gridx = 2; g.fill = GridBagConstraints.NONE; header.add(new JLabel("Comissao %:"), g);
        g.gridx = 3; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        r++;

        g.gridx = 0; g.gridy = r; header.add(new JLabel("Admissao:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        g.gridx = 2; g.fill = GridBagConstraints.NONE; header.add(new JLabel("Demissao:"), g);
        g.gridx = 3; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        r++;

        g.gridx = 0; g.gridy = r; header.add(new JLabel("Email:"), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        g.gridx = 2; g.fill = GridBagConstraints.NONE; header.add(new JLabel("Telefone:"), g);
        g.gridx = 3; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        r++;

        g.gridx = 0; g.gridy = r; header.add(new JLabel("Endereco:"), g);
        g.gridx = 1; g.gridwidth = 3; g.fill = GridBagConstraints.HORIZONTAL; header.add(new JLabel(), g);
        g.gridwidth = 1;

        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Ponto", tablePanel(pontoModel, new int[]{0}, new int[]{4}, new int[]{}));
        tabs.addTab("Escalas", tablePanel(escalaModel, new int[]{0}, new int[]{}, new int[]{}));
        tabs.addTab("Salarios", tablePanel(salarioModel, new int[]{2, 3}, new int[]{1}, new int[]{}));
        tabs.addTab("Ferias", tablePanel(feriasModel, new int[]{0, 1}, new int[]{}, new int[]{}));
        tabs.addTab("Comissoes", tablePanel(comissaoModel, new int[]{0}, new int[]{3}, new int[]{2}));
        tabs.addTab("Folha", tablePanel(folhaModel, new int[]{}, new int[]{1, 2, 3}, new int[]{}));

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel tablePanel(DefaultTableModel model, int[] dateCols, int[] moneyCols, int[] percentCols) {
        JTable table = new JTable(model);
        UiKit.tableDefaults(table);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        for (int col : dateCols) {
            table.getColumnModel().getColumn(col).setCellRenderer(dateRenderer(zebra));
        }
        for (int col : moneyCols) {
            table.getColumnModel().getColumn(col).setCellRenderer(moneyRenderer(zebra));
        }
        for (int col : percentCols) {
            table.getColumnModel().getColumn(col).setCellRenderer(percentRenderer(zebra));
        }

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(UiKit.scroll(table), BorderLayout.CENTER);
        return p;
    }

    private void carregar() {
        if (funcionario == null) return;

        try {
            funcionario = funcionarioDAO.buscarPorId(funcionario.getId());
        } catch (Exception ignore) { }

        preencherHeader();
        carregarPonto();
        carregarEscalas();
        carregarSalarios();
        carregarFerias();
        carregarComissoes();
        carregarFolha();
    }

    private void preencherHeader() {
        JPanel header = (JPanel) getContentPane().getComponent(0);
        Component[] cs = header.getComponents();
        if (cs.length < 22) return;

        String cargo = "-";
        try {
            if (funcionario.getCargoId() != null) {
                RhCargoModel c = cargoDAO.buscarPorId(funcionario.getCargoId());
                if (c != null) cargo = c.getNome();
            }
        } catch (Exception ignore) { }

        String doc = (funcionario.getCpf() != null && !funcionario.getCpf().isBlank())
                ? funcionario.getCpf() : funcionario.getCnpj();

        ((JLabel) cs[1]).setText(safe(funcionario.getNome()));
        ((JLabel) cs[3]).setText(safe(cargo));
        ((JLabel) cs[5]).setText(safe(funcionario.getTipoContrato()));
        ((JLabel) cs[7]).setText(safe(doc));
        ((JLabel) cs[9]).setText(formatMoney(funcionario.getSalarioBase()));
        ((JLabel) cs[11]).setText(formatPercent(funcionario.getComissaoPct()));
        ((JLabel) cs[13]).setText(formatDateBr(funcionario.getDataAdmissao()));
        ((JLabel) cs[15]).setText(formatDateBr(funcionario.getDataDemissao()));
        ((JLabel) cs[17]).setText(safe(funcionario.getEmail()));
        ((JLabel) cs[19]).setText(safe(funcionario.getTelefone()));
        ((JLabel) cs[21]).setText(safe(funcionario.getEndereco()));
    }

    private void carregarPonto() {
        pontoModel.setRowCount(0);
        try {
            List<RhPontoModel> lista = pontoDAO.listar(funcionario.getId(), null, null);
            for (RhPontoModel p : lista) {
                String intervalo = safe(p.getIntervaloInicio());
                if (!safe(p.getIntervaloFim()).isBlank()) intervalo += "-" + safe(p.getIntervaloFim());
                pontoModel.addRow(new Object[]{p.getData(), p.getEntrada(), p.getSaida(), intervalo, p.getHorasTrabalhadas(), p.getOrigem()});
            }
        } catch (Exception ignore) { }
    }

    private void carregarEscalas() {
        escalaModel.setRowCount(0);
        try {
            List<RhEscalaModel> lista = escalaDAO.listar(funcionario.getId(), null, null);
            for (RhEscalaModel e : lista) {
                escalaModel.addRow(new Object[]{e.getData(), e.getInicio(), e.getFim(), e.getObservacoes()});
            }
        } catch (Exception ignore) { }
    }

    private void carregarSalarios() {
        salarioModel.setRowCount(0);
        try {
            List<RhSalarioModel> lista = salarioDAO.listarPorFuncionario(funcionario.getId());
            for (RhSalarioModel s : lista) {
                String cargo = s.getCargoId();
                try {
                    if (s.getCargoId() != null) {
                        RhCargoModel c = cargoDAO.buscarPorId(s.getCargoId());
                        if (c != null) cargo = c.getNome();
                    }
                } catch (Exception ignore) { }
                salarioModel.addRow(new Object[]{cargo, s.getSalarioBase(), s.getDataInicio(), s.getDataFim(), s.getMotivo()});
            }
        } catch (Exception ignore) { }
    }

    private void carregarFerias() {
        feriasModel.setRowCount(0);
        try {
            List<RhFeriasModel> lista = feriasDAO.listar(funcionario.getId());
            for (RhFeriasModel f : lista) {
                feriasModel.addRow(new Object[]{f.getDataInicio(), f.getDataFim(), f.getAbono(), f.getStatus(), f.getObservacoes()});
            }
        } catch (Exception ignore) { }
    }

    private void carregarComissoes() {
        comissaoModel.setRowCount(0);
        try {
            List<RhComissaoModel> lista = comissaoDAO.listarPorFuncionario(funcionario.getId());
            for (RhComissaoModel c : lista) {
                comissaoModel.addRow(new Object[]{c.getData(), c.getVendaId(), c.getPercentual(), c.getValor(), c.getObservacoes()});
            }
        } catch (Exception ignore) { }
    }

    private void carregarFolha() {
        folhaModel.setRowCount(0);
        try {
            List<RhFolhaModel> lista = folhaDAO.listarPorFuncionario(funcionario.getId());
            for (RhFolhaModel f : lista) {
                folhaModel.addRow(new Object[]{formatCompetencia(f.getCompetencia()), f.getSalarioBase(), f.getComissao(), f.getTotalLiquido(), f.getStatus()});
            }
        } catch (Exception ignore) { }
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

    private DefaultTableCellRenderer percentRenderer(DefaultTableCellRenderer zebraBase) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double v = (value instanceof Number n) ? n.doubleValue() : 0.0;
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setText(nf.format(v) + "%");
                return l;
            }
        };
    }

    private DefaultTableCellRenderer dateRenderer(DefaultTableCellRenderer zebraBase) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebraBase.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setText(formatDateBr(value != null ? value.toString() : ""));
                return l;
            }
        };
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

    private String formatCompetencia(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            java.time.YearMonth ym = java.time.YearMonth.parse(iso);
            return ym.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }

    private String formatMoney(double v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return nf.format(v);
    }

    private String formatPercent(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v) + "%";
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
