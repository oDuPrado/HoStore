package ui.ajustes.dialog;

import dao.ConfigNfceDAO;
import model.ConfigNfceModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class ConfigNfceDialog extends JDialog {

    private final ConfigNfceDAO dao = new ConfigNfceDAO();
    private ConfigNfceModel config;

    private final JCheckBox chkEmitirNfce = new JCheckBox("Habilitar emissão de NFC-e");
    private final JComboBox<String> cbAmbiente = new JComboBox<>(new String[]{"Homologação", "Produção"});
    private final JTextField txtCertPath = new JTextField();
    private final JPasswordField txtCertPass = new JPasswordField();
    private final JTextField txtIdCsc = new JTextField();
    private final JTextField txtCsc = new JTextField();
    private final JLabel lblModoEmissao = new JLabel();

    public ConfigNfceDialog(Frame owner) {
        super(owner, "Configuração de NFC-e", true);
        loadConfig();
        initComponents();
        populateForm();
    }

    private void loadConfig() {
        try {
            config = dao.getConfig();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar configuração NFC-e.", "Erro", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(getOwner());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(chkEmitirNfce, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Ambiente:"), gbc);
        gbc.gridx = 1;
        formPanel.add(cbAmbiente, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Caminho do Certificado A1:"), gbc);
        gbc.gridx = 1;
        JPanel certPanel = new JPanel(new BorderLayout());
        certPanel.add(txtCertPath, BorderLayout.CENTER);
        JButton btnSelectCert = new JButton("...");
        btnSelectCert.addActionListener(e -> selectCertFile());
        certPanel.add(btnSelectCert, BorderLayout.EAST);
        formPanel.add(certPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Senha do Certificado:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtCertPass, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("ID do CSC:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtIdCsc, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("CSC:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtCsc, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Modo de Emissão:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lblModoEmissao, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Salvar");
        btnSave.addActionListener(e -> saveConfig());
        buttonPanel.add(btnSave);
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        buttonPanel.add(btnCancel);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateForm() {
        if (config != null) {
            chkEmitirNfce.setSelected(config.getEmitirNfce() == 1);
            cbAmbiente.setSelectedItem(formatarAmbienteParaUI(config.getAmbiente()));
            txtCertPath.setText(config.getCertA1Path());
            txtCertPass.setText(config.getCertA1Senha());
            txtIdCsc.setText(config.getIdCsc() > 0 ? String.valueOf(config.getIdCsc()) : "");
            txtCsc.setText(config.getCsc());
            lblModoEmissao.setText(config.getModoEmissao());
        }
    }

    private void selectCertFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            txtCertPath.setText(selectedFile.getAbsolutePath());
        }
    }

    private void saveConfig() {
        if (config == null) {
            config = new ConfigNfceModel();
            config.setId("CONFIG_PADRAO");
        }

        config.setEmitirNfce(chkEmitirNfce.isSelected() ? 1 : 0);
        config.setAmbiente(parseAmbienteUI((String) cbAmbiente.getSelectedItem()));
        config.setCertA1Path(txtCertPath.getText());
        config.setCertA1Senha(new String(txtCertPass.getPassword()));
        config.setIdCsc(parseIdCsc(txtIdCsc.getText()));
        config.setCsc(txtCsc.getText());

        // Auto-switch modo_emissao
        if (chkEmitirNfce.isSelected() &&
                !txtCertPath.getText().isBlank() &&
                txtCertPass.getPassword().length > 0 &&
                !txtIdCsc.getText().isBlank() &&
                !txtCsc.getText().isBlank()) {
            config.setModoEmissao("ONLINE_SEFAZ");
        } else {
            config.setModoEmissao("OFFLINE_VALIDACAO");
        }
        lblModoEmissao.setText(config.getModoEmissao());

        try {
            dao.saveConfig(config);
            JOptionPane.showMessageDialog(this, "Configuração salva com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar configuração.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int parseIdCsc(String value) {
        String raw = value != null ? value.trim() : "";
        if (raw.isEmpty()) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatarAmbienteParaUI(String ambiente) {
        if (ambiente == null) return "Homologação";
        String normalized = ambiente.trim().toUpperCase();
        if ("PRODUCAO".equals(normalized) || "PRODUÇÃO".equals(normalized)) {
            return "Produção";
        }
        return "Homologação";
    }

    private String parseAmbienteUI(String ambienteUi) {
        if (ambienteUi == null) return "HOMOLOGACAO";
        String normalized = ambienteUi.trim().toUpperCase();
        return normalized.contains("PROD") ? "PRODUCAO" : "HOMOLOGACAO";
    }
}
