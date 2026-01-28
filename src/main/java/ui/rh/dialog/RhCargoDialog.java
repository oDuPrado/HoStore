package ui.rh.dialog;

import dao.RhCargoDAO;
import model.RhCargoModel;
import util.UiKit;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;

public class RhCargoDialog extends JDialog {

    private final RhCargoDAO dao = new RhCargoDAO();
    private RhCargoModel model;

    private final JTextField tfNome = new JTextField(25);
    private final JFormattedTextField tfSalario = FormatterFactory.getMoneyField(0.0);
    private final JTextArea taDescricao = new JTextArea(3, 25);
    private final JCheckBox ckAtivo = new JCheckBox("Ativo", true);

    public RhCargoDialog(Frame owner, RhCargoModel m) {
        super(owner, "Cargo", true);
        UiKit.applyDialogBase(this);
        this.model = m;
        init();
        if (m != null) carregar();
        setSize(520, 320);
        setLocationRelativeTo(owner);
    }

    private void init() {
        setLayout(new BorderLayout(8, 8));
        JPanel form = UiKit.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx=0; g.gridy=0; form.add(new JLabel("Nome:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx=1; form.add(tfNome, g);

        g.gridx=0; g.gridy=1; g.fill = GridBagConstraints.NONE; g.weightx=0; form.add(new JLabel("Salario base:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfSalario, g);

        g.gridx=0; g.gridy=2; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Descricao:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; taDescricao.setLineWrap(true); taDescricao.setWrapStyleWord(true);
        form.add(new JScrollPane(taDescricao), g);

        g.gridx=1; g.gridy=3; g.fill = GridBagConstraints.NONE; form.add(ckAtivo, g);

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnSalvar = UiKit.primary("Salvar");
        JButton btnCancelar = UiKit.ghost("Cancelar");
        actions.add(btnCancelar);
        actions.add(btnSalvar);
        add(actions, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());
    }

    private void carregar() {
        tfNome.setText(model.getNome());
        tfSalario.setText(String.valueOf(model.getSalarioBase()));
        taDescricao.setText(model.getDescricao());
        ckAtivo.setSelected(model.getAtivo() == 1);
    }

    private void salvar() {
        try {
            if (model == null) model = new RhCargoModel();
            model.setNome(tfNome.getText().trim());
            model.setDescricao(taDescricao.getText().trim());
            model.setSalarioBase(UiKit.getDoubleValue(tfSalario, 0.0));
            model.setAtivo(ckAtivo.isSelected() ? 1 : 0);

            if (model.getId() == null || model.getId().isBlank()) {
                dao.inserir(model);
            } else {
                dao.atualizar(model);
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + ex.getMessage());
        }
    }

}
