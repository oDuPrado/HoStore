package ui.rh.dialog;

import dao.RhCargoDAO;
import dao.RhFuncionarioDAO;
import dao.UsuarioDAO;
import model.RhCargoModel;
import model.RhFuncionarioModel;
import model.UsuarioModel;
import util.UiKit;
import util.FormatterFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class RhFuncionarioDialog extends JDialog {

    private final RhFuncionarioDAO dao = new RhFuncionarioDAO();
    private final RhCargoDAO cargoDAO = new RhCargoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private RhFuncionarioModel model;

    private final JTextField tfNome = new JTextField(26);
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"CLT", "CNPJ"});
    private final JTextField tfCpf = new JTextField(14);
    private final JTextField tfCnpj = new JTextField(18);
    private final JTextField tfRg = new JTextField(12);
    private final JTextField tfPis = new JTextField(12);
    private final JComboBox<RhCargoModel> cbCargo = new JComboBox<>();
    private final JFormattedTextField tfSalario = FormatterFactory.getMoneyField(0.0);
    private final JFormattedTextField tfComissao = FormatterFactory.getFormattedDoubleField(0.0);
    private final JComboBox<UsuarioModel> cbUsuario = new JComboBox<>();
    private final JTextField tfEmail = new JTextField(20);
    private final JTextField tfTelefone = new JTextField(14);
    private final JTextField tfEndereco = new JTextField(24);
    private final JFormattedTextField tfAdmissao = FormatterFactory.getFormattedDateField();
    private final JFormattedTextField tfDemissao = FormatterFactory.getFormattedDateField();
    private final JTextArea taObs = new JTextArea(3, 24);
    private final JCheckBox ckAtivo = new JCheckBox("Ativo", true);

    public RhFuncionarioDialog(Frame owner, RhFuncionarioModel m) {
        super(owner, "Funcionario", true);
        UiKit.applyDialogBase(this);
        this.model = m;
        init();
        carregarCombos();
        if (m != null) carregar();
        setSize(720, 520);
        setLocationRelativeTo(owner);
    }

    private void init() {
        setLayout(new BorderLayout(8, 8));
        JPanel form = UiKit.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.anchor = GridBagConstraints.WEST;

        int r = 0;
        g.gridx=0; g.gridy=r; form.add(new JLabel("Nome:"), g);
        g.gridx=1; g.gridwidth=3; g.fill = GridBagConstraints.HORIZONTAL; g.weightx=1; form.add(tfNome, g);
        r++;

        g.gridwidth=1; g.weightx=0; g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Tipo:"), g);
        g.gridx=1; form.add(cbTipo, g);
        g.gridx=2; form.add(new JLabel("CPF:"), g);
        g.gridx=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfCpf, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("CNPJ:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfCnpj, g);
        g.gridx=2; form.add(new JLabel("Cargo:"), g);
        g.gridx=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(cbCargo, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("RG:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfRg, g);
        g.gridx=2; form.add(new JLabel("PIS:"), g);
        g.gridx=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfPis, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Salario base:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfSalario, g);
        g.gridx=2; form.add(new JLabel("Comissao %:"), g);
        g.gridx=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfComissao, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Usuario do sistema:"), g);
        g.gridx=1; g.gridwidth=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(cbUsuario, g);
        r++;

        g.gridwidth=1; g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Email:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfEmail, g);
        g.gridx=2; form.add(new JLabel("Telefone:"), g);
        g.gridx=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfTelefone, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Endereco:"), g);
        g.gridx=1; g.gridwidth=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfEndereco, g);
        r++;

        g.gridwidth=1; g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Admissao:"), g);
        g.gridx=1; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfAdmissao, g);
        g.gridx=2; form.add(new JLabel("Demissao:"), g);
        g.gridx=3; g.fill = GridBagConstraints.HORIZONTAL; form.add(tfDemissao, g);
        r++;

        g.gridx=0; g.gridy=r; g.fill = GridBagConstraints.NONE; form.add(new JLabel("Observacoes:"), g);
        g.gridx=1; g.gridwidth=3; g.fill = GridBagConstraints.HORIZONTAL; taObs.setLineWrap(true); taObs.setWrapStyleWord(true);
        form.add(new JScrollPane(taObs), g);
        r++;

        g.gridx=1; g.gridy=r; g.gridwidth=1; g.fill = GridBagConstraints.NONE; form.add(ckAtivo, g);

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

    private void carregarCombos() {
        cbCargo.removeAllItems();
        cbCargo.addItem(new RhCargoModel(null, "-", null, 0.0, 1));
        try {
            List<RhCargoModel> cargos = cargoDAO.listar();
            for (RhCargoModel c : cargos) cbCargo.addItem(c);
        } catch (Exception ignore) { }

        cbUsuario.removeAllItems();
        cbUsuario.addItem(null);
        try {
            for (UsuarioModel u : usuarioDAO.listar()) cbUsuario.addItem(u);
        } catch (Exception ignore) { }

        cbCargo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(value == null ? "-" : value.getNome());
            return l;
        });
        cbUsuario.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel(value == null ? "-" : value.getNome());
            return l;
        });
    }

    private void carregar() {
        tfNome.setText(model.getNome());
        cbTipo.setSelectedItem(model.getTipoContrato());
        tfCpf.setText(model.getCpf());
        tfCnpj.setText(model.getCnpj());
        tfRg.setText(model.getRg());
        tfPis.setText(model.getPis());
        tfSalario.setValue(model.getSalarioBase());
        tfComissao.setValue(model.getComissaoPct());
        tfEmail.setText(model.getEmail());
        tfTelefone.setText(model.getTelefone());
        tfEndereco.setText(model.getEndereco());
        tfAdmissao.setText(toBr(model.getDataAdmissao()));
        tfDemissao.setText(toBr(model.getDataDemissao()));
        taObs.setText(model.getObservacoes());
        ckAtivo.setSelected(model.getAtivo() == 1);

        for (int i = 0; i < cbCargo.getItemCount(); i++) {
            RhCargoModel c = cbCargo.getItemAt(i);
            if (c != null && c.getId() != null && c.getId().equals(model.getCargoId())) {
                cbCargo.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < cbUsuario.getItemCount(); i++) {
            UsuarioModel u = cbUsuario.getItemAt(i);
            if (u != null && u.getId().equals(model.getUsuarioId())) {
                cbUsuario.setSelectedIndex(i);
                break;
            }
        }
    }

    private void salvar() {
        try {
            if (model == null) {
                model = new RhFuncionarioModel();
                model.setId(UUID.randomUUID().toString());
            }
            model.setNome(tfNome.getText().trim());
            model.setTipoContrato((String) cbTipo.getSelectedItem());
            model.setCpf(tfCpf.getText().trim());
            model.setCnpj(tfCnpj.getText().trim());
            model.setRg(tfRg.getText().trim());
            model.setPis(tfPis.getText().trim());

            RhCargoModel cargo = (RhCargoModel) cbCargo.getSelectedItem();
            model.setCargoId(cargo != null ? cargo.getId() : null);

            model.setSalarioBase(UiKit.getDoubleValue(tfSalario, 0.0));
            model.setComissaoPct(UiKit.getDoubleValue(tfComissao, 0.0));

            UsuarioModel u = (UsuarioModel) cbUsuario.getSelectedItem();
            model.setUsuarioId(u != null ? u.getId() : null);

            model.setEmail(tfEmail.getText().trim());
            model.setTelefone(tfTelefone.getText().trim());
            model.setEndereco(tfEndereco.getText().trim());
            model.setDataAdmissao(toIso(tfAdmissao.getText()));
            model.setDataDemissao(toIso(tfDemissao.getText()));
            model.setObservacoes(taObs.getText().trim());
            model.setAtivo(ckAtivo.isSelected() ? 1 : 0);

            if (dao.buscarPorId(model.getId()) == null) {
                dao.inserir(model);
            } else {
                dao.atualizar(model);
            }
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
