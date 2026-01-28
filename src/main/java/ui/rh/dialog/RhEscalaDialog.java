package ui.rh.dialog;

import dao.RhEscalaDAO;
import dao.RhFuncionarioDAO;
import model.RhEscalaModel;
import model.RhFuncionarioModel;
import util.UiKit;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RhEscalaDialog extends JDialog {

    private final RhEscalaDAO dao = new RhEscalaDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();

    private RhEscalaModel model;

    private final JComboBox<RhFuncionarioModel> cbFuncionario = new JComboBox<>();
    private final JFormattedTextField tfData = FormatterFactory.getFormattedDateField();
    private final JTextField tfInicio = new JTextField(6);
    private final JTextField tfFim = new JTextField(6);
    private final JTextArea taObs = new JTextArea(3, 24);

    public RhEscalaDialog(Frame owner, RhEscalaModel m) {
        this(owner, m, null);
    }

    public RhEscalaDialog(Frame owner, RhEscalaModel m, String dataIso) {
        super(owner, "Escala", true);
        UiKit.applyDialogBase(this);
        this.model = m;
        init();
        carregarFuncionarios();
        if (m != null) {
            carregar();
        } else if (dataIso != null && !dataIso.isBlank()) {
            tfData.setText(toBr(dataIso));
        }
        setSize(520, 320);
        setLocationRelativeTo(owner);
    }

    private void init() {
        setLayout(new BorderLayout(8,8));
        JPanel form = UiKit.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.anchor = GridBagConstraints.WEST;

        int r = 0;
        g.gridx=0; g.gridy=r; form.add(new JLabel("Funcionario:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx=1; form.add(cbFuncionario, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; g.weightx=0; form.add(new JLabel("Data:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfData, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Inicio:"), g);
        g.gridx=1; form.add(tfInicio, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Fim:"), g);
        g.gridx=1; form.add(tfFim, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Observacoes:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; taObs.setLineWrap(true); taObs.setWrapStyleWord(true);
        form.add(new JScrollPane(taObs), g);

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnSalvar = UiKit.primary("Salvar");
        JButton btnCancelar = UiKit.ghost("Cancelar");
        actions.add(btnCancelar);
        actions.add(btnSalvar);
        add(actions, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        cbFuncionario.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value == null ? "-" : value.getNome()));
    }

    private void carregarFuncionarios() {
        cbFuncionario.removeAllItems();
        try {
            List<RhFuncionarioModel> lista = funcDAO.listar(false);
            for (RhFuncionarioModel f : lista) cbFuncionario.addItem(f);
        } catch (Exception ignore) { }
    }

    private void carregar() {
        tfData.setText(toBr(model.getData()));
        tfInicio.setText(model.getInicio());
        tfFim.setText(model.getFim());
        taObs.setText(model.getObservacoes());

        for (int i = 0; i < cbFuncionario.getItemCount(); i++) {
            RhFuncionarioModel f = cbFuncionario.getItemAt(i);
            if (f != null && f.getId().equals(model.getFuncionarioId())) {
                cbFuncionario.setSelectedIndex(i);
                break;
            }
        }
    }

    private static String toIso(String br) {
        if (br == null) return null;
        String s = br.trim();
        if (s.isEmpty() || s.contains("_")) return null;
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return java.time.LocalDate.parse(s, f).toString();
        } catch (Exception e) {
            return s;
        }
    }

    private static String toBr(String iso) {
        if (iso == null) return "";
        String s = iso.trim();
        if (s.isEmpty()) return "";
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(s);
            return d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return s;
        }
    }

    private void salvar() {
        try {
            if (model == null) model = new RhEscalaModel();
            RhFuncionarioModel f = (RhFuncionarioModel) cbFuncionario.getSelectedItem();
            model.setFuncionarioId(f != null ? f.getId() : null);
            model.setData(toIso(tfData.getText()));
            model.setInicio(tfInicio.getText().trim());
            model.setFim(tfFim.getText().trim());
            model.setObservacoes(taObs.getText().trim());

            if (model.getId() > 0) dao.atualizar(model); else dao.inserir(model);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage());
        }
    }
}
