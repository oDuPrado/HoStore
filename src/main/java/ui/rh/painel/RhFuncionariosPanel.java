package ui.rh.painel;

import dao.RhCargoDAO;
import dao.RhFuncionarioDAO;
import model.RhCargoModel;
import model.RhFuncionarioModel;
import ui.rh.dialog.RhFuncionarioDetalhesDialog;
import ui.rh.dialog.RhFuncionarioDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RhFuncionariosPanel extends JPanel {

    private final RhFuncionarioDAO dao = new RhFuncionarioDAO();
    private final RhCargoDAO cargoDAO = new RhCargoDAO();
    private final DefaultTableModel model;
    private final JTable table;

    public RhFuncionariosPanel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setOpaque(false);
        top.add(UiKit.title("Funcionarios"));

        JButton btnAdd = UiKit.primary("Adicionar");
        JButton btnEdit = UiKit.ghost("Editar");
        JButton btnEvolucao = UiKit.ghost("Evolucao");
        JButton btnDetalhes = UiKit.ghost("Detalhes");
        JButton btnDel = UiKit.ghost("Desativar");
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnEvolucao);
        top.add(btnDetalhes);
        top.add(btnDel);

        model = new DefaultTableModel(new Object[]{"ID", "Nome", "Tipo", "Documento", "Cargo", "Salario", "Comissao %", "Ativo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UiKit.tableDefaults(table);
        aplicarRenderers();

        add(top, BorderLayout.NORTH);
        add(UiKit.scroll(table), BorderLayout.CENTER);

        btnAdd.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new RhFuncionarioDialog((Frame) w, null).setVisible(true);
            carregar();
        });
        btnEdit.addActionListener(e -> editarSelecionado());
        btnEvolucao.addActionListener(e -> evolucaoSelecionado());
        btnDetalhes.addActionListener(e -> detalhesSelecionado());
        btnDel.addActionListener(e -> desativarSelecionado());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    detalhesSelecionado();
                }
            }
        });

        carregar();
    }

    private void carregar() {
        model.setRowCount(0);
        try {
            List<RhCargoModel> cargos = cargoDAO.listar();
            Map<String, String> cargoMap = new HashMap<>();
            for (RhCargoModel c : cargos) cargoMap.put(c.getId(), c.getNome());

            List<RhFuncionarioModel> lista = dao.listar(false);
            for (RhFuncionarioModel f : lista) {
                String doc = (f.getCpf() != null && !f.getCpf().isBlank()) ? f.getCpf() : f.getCnpj();
                String cargo = cargoMap.getOrDefault(f.getCargoId(), "-");
                model.addRow(new Object[]{f.getId(), f.getNome(), f.getTipoContrato(), doc, cargo, f.getSalarioBase(), f.getComissaoPct(), f.getAtivo()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar funcionarios: " + ex.getMessage());
        }
    }

    private void aplicarRenderers() {
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
        table.getColumnModel().getColumn(5).setCellRenderer(moneyRenderer(zebra));
        table.getColumnModel().getColumn(6).setCellRenderer(percentRenderer(zebra));
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

    private void editarSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = model.getValueAt(row, 0).toString();
        try {
            RhFuncionarioModel f = dao.buscarPorId(id);
            if (f != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new RhFuncionarioDialog((Frame) w, f).setVisible(true);
                carregar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao editar: " + ex.getMessage());
        }
    }

    private void evolucaoSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = model.getValueAt(row, 0).toString();
        try {
            RhFuncionarioModel f = dao.buscarPorId(id);
            if (f != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new ui.rh.dialog.RhEvolucaoDialog((Frame) w, f).setVisible(true);
                carregar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao registrar evolucao: " + ex.getMessage());
        }
    }

    private void desativarSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = model.getValueAt(row, 0).toString();
        int ok = JOptionPane.showConfirmDialog(this, "Desativar funcionario selecionado?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            RhFuncionarioModel f = dao.buscarPorId(id);
            if (f != null) {
                f.setAtivo(0);
                dao.atualizar(f);
                carregar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao desativar: " + ex.getMessage());
        }
    }

    private void detalhesSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = model.getValueAt(row, 0).toString();
        try {
            RhFuncionarioModel f = dao.buscarPorId(id);
            if (f != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                new RhFuncionarioDetalhesDialog((Frame) w, f).setVisible(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir detalhes: " + ex.getMessage());
        }
    }
}
