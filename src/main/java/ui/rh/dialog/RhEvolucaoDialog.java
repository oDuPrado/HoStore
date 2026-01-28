package ui.rh.dialog;

import dao.RhCargoDAO;
import model.RhCargoModel;
import model.RhFuncionarioModel;
import service.RhService;
import util.FormatterFactory;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class RhEvolucaoDialog extends JDialog {

    private final RhCargoDAO cargoDAO = new RhCargoDAO();
    private final RhService service = new RhService();

    private final RhFuncionarioModel funcionario;

    private final JComboBox<RhCargoModel> cbCargo = new JComboBox<>();
    private final JFormattedTextField tfSalario = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfData = FormatterFactory.getFormattedDateField();
    private final JTextField tfMotivo = new JTextField(20);

    public RhEvolucaoDialog(Frame owner, RhFuncionarioModel func) {
        super(owner, "Evolucao Salarial/Cargo", true);
        UiKit.applyDialogBase(this);
        this.funcionario = func;
        init();
        carregarCargos();
        setSize(480, 260);
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
        g.gridx=0; g.gridy=r; form.add(new JLabel("Cargo:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx=1; form.add(cbCargo, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; g.weightx=0; form.add(new JLabel("Salario base:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfSalario, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Data inicio:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; tfData.setText(toBr(LocalDate.now().toString())); form.add(tfData, g);
        r++;

        g.gridx=0; g.gridy=r; form.add(new JLabel("Motivo:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfMotivo, g);

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnSalvar = UiKit.primary("Salvar");
        JButton btnCancelar = UiKit.ghost("Cancelar");
        actions.add(btnCancelar);
        actions.add(btnSalvar);
        add(actions, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        cbCargo.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value == null ? "-" : value.getNome()));
    }

    private void carregarCargos() {
        cbCargo.removeAllItems();
        try {
            List<RhCargoModel> cargos = cargoDAO.listar();
            for (RhCargoModel c : cargos) cbCargo.addItem(c);
        } catch (Exception ignore) { }
    }

    private void salvar() {
        try {
            if (funcionario == null) {
                JOptionPane.showMessageDialog(this, "Funcionario nao selecionado.");
                return;
            }
            RhCargoModel cargo = (RhCargoModel) cbCargo.getSelectedItem();
            String cargoId = cargo != null ? cargo.getId() : null;
            double salario = UiKit.getDoubleValue(tfSalario, 0.0);
            String data = toIso(tfData.getText());
            String motivo = tfMotivo.getText().trim();

            service.registrarEvolucaoSalarial(funcionario.getId(), cargoId, salario, data, motivo);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage());
        }
    }

    private String toIso(String br) {
        if (br == null)
            return null;
        String s = br.trim();
        if (s.isEmpty() || s.contains("_"))
            return null;
        if (s.contains("/")) {
            java.time.LocalDate d = java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return d.toString();
        }
        return s;
    }

    private String toBr(String iso) {
        if (iso == null || iso.isBlank())
            return "";
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(iso);
            return d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }
}
