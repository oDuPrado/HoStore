package ui.ajustes.dialog;

import util.BackupUtils;
import util.BackupUtils.BackupConfig;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Diálogo de configuração de backup/sistema.
 * (Visual atualizado com UiKit, lógica intacta.)
 */
public class ConfigSistemaDialog extends JDialog {

    private JCheckBox chkEnable;
    private JTextField txtFolder;
    private JButton btnBrowse;
    private JSpinner spnInterval;
    private JComboBox<String> cbUnit;
    private JButton btnSave, btnCancel, btnBackupNow;

    public ConfigSistemaDialog(JFrame owner) {
        super(owner, "Backup / Sistema", true);

        UiKit.applyDialogBase(this);
        setLayout(new BorderLayout(10, 10));

        BackupConfig cfg = BackupUtils.loadConfig();

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(cfg), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Atalhos: ESC fecha, ENTER salva
        getRootPane().setDefaultButton(btnSave);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setMinimumSize(new Dimension(720, 260));
        pack();
        setLocationRelativeTo(owner);
    }

    private JComponent buildHeader() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);

        left.add(UiKit.title("🗄️ Backup / Sistema"));
        left.add(UiKit.hint("Escolha a pasta, configure intervalo e rode backup manual quando precisar."));

        card.add(left, BorderLayout.WEST);
        card.add(UiKit.hint("Enter salva | Esc fecha"), BorderLayout.EAST);

        return card;
    }

    private JComponent buildCenter(BackupConfig cfg) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BorderLayout(10, 10));

        // Card principal: configurações
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // 1) Checkbox
        chkEnable = new JCheckBox("Ativar backup automático");
        chkEnable.setSelected(cfg.enabled);

        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 3;
        g.weightx = 1;
        form.add(chkEnable, g);
        y++;

        // 2) Pasta
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy = y;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Pasta de backup:"), g);

        txtFolder = new JTextField(cfg.folderPath, 28);
        txtFolder.putClientProperty("JTextField.placeholderText", "Selecione uma pasta para salvar os backups...");

        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(txtFolder, g);

        btnBrowse = UiKit.ghost("Procurar…");
        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(txtFolder.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtFolder.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        g.gridx = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(btnBrowse, g);

        y++;

        // 3) Intervalo + unidade
        g.gridx = 0;
        g.gridy = y;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Intervalo:"), g);

        spnInterval = new JSpinner(new SpinnerNumberModel(cfg.interval, 1, 365, 1));
        ((JSpinner.DefaultEditor) spnInterval.getEditor()).getTextField().setColumns(6);

        g.gridx = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(spnInterval, g);

        cbUnit = new JComboBox<>(new String[] { "MINUTES", "HOURS", "DAYS" });
        cbUnit.setSelectedItem(cfg.unit);

        g.gridx = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(cbUnit, g);

        // Hint
        y++;
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 3;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        JLabel hint = UiKit.hint("Sugestão: use HOURS para lojas, DAYS para backup de segurança extra.");
        hint.setBorder(new EmptyBorder(4, 0, 0, 0));
        form.add(hint, g);

        card.add(form, BorderLayout.CENTER);

        // Card secundário: ações rápidas
        JPanel actions = UiKit.card();
        actions.setLayout(new BorderLayout(10, 10));

        JPanel actionsLeft = new JPanel(new GridLayout(2, 1, 0, 2));
        actionsLeft.setOpaque(false);
        actionsLeft.add(UiKit.title("Ações"));
        actionsLeft.add(UiKit.hint("Executa um backup manual agora usando a pasta informada."));
        actions.add(actionsLeft, BorderLayout.WEST);

        btnBackupNow = UiKit.ghost("Backup agora");
        btnBackupNow.addActionListener(e -> {
            BackupUtils.doBackup(txtFolder.getText());
            JOptionPane.showMessageDialog(this, "Backup realizado.");
        });

        JPanel actionsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actionsRight.setOpaque(false);
        actionsRight.add(btnBackupNow);
        actions.add(actionsRight, BorderLayout.EAST);

        wrap.add(card, BorderLayout.NORTH);
        wrap.add(actions, BorderLayout.SOUTH);

        return wrap;
    }

    private JComponent buildFooter() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        card.add(UiKit.hint("Essas configurações afetam o agendador de backup do sistema."), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        btnCancel = UiKit.ghost("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        btnSave = UiKit.primary("Salvar");
        btnSave.addActionListener(e -> onSave());

        right.add(btnCancel);
        right.add(btnSave);

        card.add(right, BorderLayout.EAST);

        return card;
    }

    /** Ao clicar em Salvar: persiste config e agenda backup se necessário */
    private void onSave() {
        BackupConfig cfg = new BackupConfig();
        cfg.enabled = chkEnable.isSelected();
        cfg.folderPath = txtFolder.getText();
        cfg.interval = ((Number) spnInterval.getValue()).longValue();
        cfg.unit = (String) cbUnit.getSelectedItem();

        BackupUtils.saveConfig(cfg);
        BackupUtils.applyConfig(cfg);

        JOptionPane.showMessageDialog(this, "Configurações salvas com sucesso.");
        dispose();
    }
}
