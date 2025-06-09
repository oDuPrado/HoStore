package ui.ajustes.dialog;

import util.BackupUtils;
import util.BackupUtils.BackupConfig;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Diálogo de configuração de backup/sistema.
 * Permite escolher pasta, ativar auto-backup, intervalo e executar backup agora.
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
        setLayout(new BorderLayout());

        // --- Centro: controles de configuração ---
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        BackupConfig cfg = BackupUtils.loadConfig();

        // 1) Checkbox ativar/desativar
        chkEnable = new JCheckBox("Ativar backup automático");
        chkEnable.setSelected(cfg.enabled);
        center.add(chkEnable, gbc);

        // 2) Pasta de destino
        gbc.gridy++;
        center.add(new JLabel("Pasta de backup:"), gbc);
        gbc.gridx = 1;
        txtFolder = new JTextField(cfg.folderPath, 20);
        center.add(txtFolder, gbc);
        gbc.gridx = 2;
        btnBrowse = new JButton("…");
        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(txtFolder.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtFolder.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        center.add(btnBrowse, gbc);

        // 3) Intervalo numérico
        gbc.gridx = 0; gbc.gridy++;
        center.add(new JLabel("Intervalo:"), gbc);
        gbc.gridx = 1;
        spnInterval = new JSpinner(new SpinnerNumberModel(cfg.interval, 1, 365, 1));
        center.add(spnInterval, gbc);
        gbc.gridx = 2;
        cbUnit = new JComboBox<>(new String[]{"MINUTES","HOURS","DAYS"});
        cbUnit.setSelectedItem(cfg.unit);
        center.add(cbUnit, gbc);

        add(center, BorderLayout.CENTER);

        // --- Sul: botões ---
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBackupNow = new JButton("Backup agora");
        btnBackupNow.addActionListener(e -> {
            BackupUtils.doBackup(txtFolder.getText());
            JOptionPane.showMessageDialog(this, "Backup realizado.");
        });
        south.add(btnBackupNow);

        btnSave = new JButton("Salvar");
        btnSave.addActionListener(e -> onSave());
        south.add(btnSave);

        btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        south.add(btnCancel);

        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Ao clicar em Salvar: persiste config e agenda backup se necessário */
    private void onSave() {
        BackupConfig cfg = new BackupConfig();
        cfg.enabled = chkEnable.isSelected();
        cfg.folderPath = txtFolder.getText();
        cfg.interval = ((Number)spnInterval.getValue()).longValue();
        cfg.unit = (String)cbUnit.getSelectedItem();

        BackupUtils.saveConfig(cfg);
        BackupUtils.applyConfig(cfg);

        JOptionPane.showMessageDialog(this, "Configurações salvas com sucesso.");
        dispose();
    }
}
