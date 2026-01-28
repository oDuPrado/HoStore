package ui.rh.dialog;

import dao.RhFuncionarioDAO;
import dao.RhPontoDAO;
import model.RhFuncionarioModel;
import model.RhPontoModel;
import util.UiKit;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RhPontoDialog extends JDialog {

    private final RhPontoDAO dao = new RhPontoDAO();
    private final RhFuncionarioDAO funcDAO = new RhFuncionarioDAO();

    private RhPontoModel model;

    private final JComboBox<RhFuncionarioModel> cbFuncionario = new JComboBox<>();
    private final JFormattedTextField tfData = FormatterFactory.getFormattedDateField();
    private final JTextField tfEntrada = new JTextField(6);
    private final JTextField tfSaida = new JTextField(6);
    private final JTextField tfIntervaloIni = new JTextField(6);
    private final JTextField tfIntervaloFim = new JTextField(6);
    private final JFormattedTextField tfHoras = FormatterFactory.getFormattedDoubleField(0.0);
    private final JTextField tfOrigem = new JTextField(12);

    public RhPontoDialog(Frame owner, RhPontoModel m) {
        super(owner, "Registro de Ponto", true);
        UiKit.applyDialogBase(this);
        this.model = m;
        init();
        carregarFuncionarios();
        if (m != null) carregar();
        setSize(520, 360);
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

        g.gridx=0; g.gridy=r; form.add(new JLabel("Entrada:"), g);
        g.gridx=1; form.add(tfEntrada, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Saida:"), g);
        g.gridx=1; form.add(tfSaida, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Intervalo Ini:"), g);
        g.gridx=1; form.add(tfIntervaloIni, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Intervalo Fim:"), g);
        g.gridx=1; form.add(tfIntervaloFim, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Horas trabalhadas:"), g);
        g.gridx=1; form.add(tfHoras, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Origem:"), g);
        g.gridx=1; form.add(tfOrigem, g);

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
        tfEntrada.setText(model.getEntrada());
        tfSaida.setText(model.getSaida());
        tfIntervaloIni.setText(model.getIntervaloInicio());
        tfIntervaloFim.setText(model.getIntervaloFim());
        if (model.getHorasTrabalhadas() != 0.0) {
            tfHoras.setValue(model.getHorasTrabalhadas());
        } else {
            tfHoras.setValue(null);
        }
        tfOrigem.setText(model.getOrigem());

        for (int i = 0; i < cbFuncionario.getItemCount(); i++) {
            RhFuncionarioModel f = cbFuncionario.getItemAt(i);
            if (f != null && f.getId().equals(model.getFuncionarioId())) {
                cbFuncionario.setSelectedIndex(i);
                break;
            }
        }
    }

    private void salvar() {
        try {
            if (model == null) model = new RhPontoModel();
            RhFuncionarioModel f = (RhFuncionarioModel) cbFuncionario.getSelectedItem();
            model.setFuncionarioId(f != null ? f.getId() : null);
            model.setData(toIso(tfData.getText()));
            model.setEntrada(tfEntrada.getText().trim());
            model.setSaida(tfSaida.getText().trim());
            model.setIntervaloInicio(tfIntervaloIni.getText().trim());
            model.setIntervaloFim(tfIntervaloFim.getText().trim());
            model.setHorasTrabalhadas(UiKit.getDoubleValue(tfHoras, 0.0));
            model.setOrigem(tfOrigem.getText().trim());

            if (model.getId() > 0) dao.atualizar(model); else dao.inserir(model);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage());
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

}
