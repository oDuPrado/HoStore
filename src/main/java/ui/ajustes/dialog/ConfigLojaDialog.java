package ui.ajustes.dialog;

import dao.ConfigLojaDAO;
import dao.ConfigNfceDAO;
import model.ConfigLojaModel;
import model.ConfigNfceModel;
import service.XmlAssinaturaService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import java.util.Locale;

/**
 * ConfigLojaDialog - visual remodelado com UiKit (sem mudar lógica).
 */
public class ConfigLojaDialog extends JDialog {

    private static File lastCertDir = null;

    // ===== Campos cadastrais =====
    private final JTextField tfNome = new JTextField(25);
    private final JTextField tfNomeFantasia = new JTextField(25);
    private final JFormattedTextField tfCnpj = criarCampoCnpj();
    private final JTextField tfInscricaoEstadual = new JTextField(15);
    private final JComboBox<String> cbRegimeTrib = new JComboBox<>(
            new String[] { "Simples Nacional", "Lucro Presumido", "Lucro Real" });
    private final JTextField tfCnae = new JTextField(10);
    private final JButton btnFiscal = new JButton("Configuração Fiscal");

    // ===== Endereço =====
    private final JTextField tfLogradouro = new JTextField(20);
    private final JTextField tfNumero = new JTextField(6);
    private final JTextField tfComplemento = new JTextField(10);
    private final JTextField tfBairro = new JTextField(15);
    private final JTextField tfMunicipio = new JTextField(15);
    private final JComboBox<String> cbUf = new JComboBox<>(
            new String[] { "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
                    "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO",
                    "RR", "SC", "SP", "SE", "TO" });
    private final JFormattedTextField tfCep = criarCampoCep();

    // ===== Contato =====
    private final JFormattedTextField tfTelefone = criarCampoTelefone();
    private final JTextField tfEmail = new JTextField(25);
    private final JTextArea taSocios = new JTextArea(3, 25);

    // ===== NFC-e / Documentos Fiscais =====
    private final JTextField tfModeloNota = new JTextField(4);
    private final JTextField tfSerieNota = new JTextField(4);
    private final JTextField tfNumeroInicialNota = new JTextField(6);
    private final JComboBox<String> cbAmbienteNfce = new JComboBox<>(
            new String[] { "HOMOLOGACAO", "PRODUCAO" });
    private final JTextField tfCsc = new JTextField(36);
    private final JTextField tfTokenCsc = new JTextField(80);
    private final JTextField tfCertificadoPath = new JTextField(25);
    private final JPasswordField tfCertificadoSenha = new JPasswordField(15);

    // ===== Impressão =====
    private final JTextField tfNomeImpressora = new JTextField(20);
    private final JTextField tfTextoRodapeNota = new JTextField(25);

    // ===== WebService e Proxy =====
    private final JTextField tfUrlWebServiceNfce = new JTextField(30);
    private final JTextField tfProxyHost = new JTextField(20);
    private final JTextField tfProxyPort = new JTextField(6);
    private final JTextField tfProxyUsuario = new JTextField(15);
    private final JPasswordField tfProxySenha = new JPasswordField(15);

    // Configuração carregada (ou null se não existir)
    private ConfigLojaModel currentConfig;

    public ConfigLojaDialog(Frame owner) {
        super(owner, "Configuração Completa - Dados da Loja", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        UiKit.applyDialogBase(this);

        initComponents();
        setSize(980, 720);
        setLocationRelativeTo(owner);

        carregarDadosExistentes();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Enter = salvar
        // (default button é setado no footer)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private JComponent buildHeader() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("🏪 Configurações da Loja"));
        left.add(UiKit.hint("Dados cadastrais, NFC-e, impressão e proxy. Sem drama."));
        card.add(left, BorderLayout.WEST);

        JLabel hint = UiKit.hint("Enter salva | Esc fecha");
        card.add(hint, BorderLayout.EAST);

        return card;
    }

    private JComponent buildBody() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // placeholders úteis
        tfCnae.putClientProperty("JTextField.placeholderText", "ex: 4763-6/02");
        tfModeloNota.putClientProperty("JTextField.placeholderText", "65");
        tfSerieNota.putClientProperty("JTextField.placeholderText", "1");
        tfNumeroInicialNota.putClientProperty("JTextField.placeholderText", "1");
        tfCsc.putClientProperty("JTextField.placeholderText", "CSC (SEFAZ)");
        tfTokenCsc.putClientProperty("JTextField.placeholderText", "Token CSC (SEFAZ)");
        tfCertificadoPath.putClientProperty("JTextField.placeholderText", "Caminho do .pfx");
        tfNomeImpressora.putClientProperty("JTextField.placeholderText", "Nome do Windows/driver");
        tfUrlWebServiceNfce.putClientProperty("JTextField.placeholderText", "URL do WS (se customizar)");
        tfProxyPort.putClientProperty("JTextField.placeholderText", "8080");

        taSocios.setLineWrap(true);
        taSocios.setWrapStyleWord(true);

        content.add(sectionCadastral());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionEndereco());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionContato());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionNfce());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionImpressao());
        content.add(Box.createVerticalStrut(10));
        content.add(sectionWebServiceProxy());
        content.add(Box.createVerticalStrut(6));

        JScrollPane sp = UiKit.scroll(content);
        sp.setBorder(new EmptyBorder(0, 0, 0, 0));
        sp.getVerticalScrollBar().setUnitIncrement(16);

        return sp;
    }

    private JComponent buildFooter() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JLabel hint = UiKit.hint("Atenção: CNPJ precisa ter 14 dígitos e NFC-e exige modelo/série.");
        card.add(hint, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.setOpaque(false);

        JButton bFiscal = UiKit.ghost("Configuração Fiscal");
        bFiscal.setToolTipText("Abrir configurações fiscais (CFOP, CSOSN, NCM, etc.)");
        bFiscal.addActionListener(e -> {
            ConfigFiscalDialog fiscalDialog = new ConfigFiscalDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    "LOJA");
            fiscalDialog.setVisible(true);
        });

        JButton bCancelar = UiKit.ghost("Cancelar (ESC)");
        bCancelar.addActionListener(e -> dispose());

        JButton bSalvar = UiKit.primary("Salvar (ENTER)");
        bSalvar.addActionListener(e -> onSalvar());

        actions.add(bFiscal);
        actions.add(bCancelar);
        actions.add(bSalvar);

        card.add(actions, BorderLayout.EAST);

        getRootPane().setDefaultButton(bSalvar);

        // ESC fecha
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        return card;
    }

    // ===================== SEÇÕES =====================

    private JComponent sectionCadastral() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Dados Cadastrais"), BorderLayout.WEST);
        head.add(UiKit.hint("Razão social, CNPJ, IE, regime, CNAE"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = baseGbc();

        int y = 0;
        addRow(form, g, y++, "Razão Social (Nome):", tfNome);
        addRow(form, g, y++, "Nome Fantasia:", tfNomeFantasia);
        addRow(form, g, y++, "CNPJ:", tfCnpj);
        addRow(form, g, y++, "Inscrição Estadual:", tfInscricaoEstadual);
        addRow(form, g, y++, "Regime Tributário:", cbRegimeTrib);
        addRow(form, g, y++, "CNAE Principal:", tfCnae);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionEndereco() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Endereço"), BorderLayout.WEST);
        head.add(UiKit.hint("Endereço completo para emissão"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = baseGbc();
        int y = 0;

        addRow(form, g, y++, "Logradouro:", tfLogradouro);

        // Número + Complemento na mesma linha
        addRow2(form, g, y++, "Número:", tfNumero, "Complemento:", tfComplemento);

        addRow(form, g, y++, "Bairro:", tfBairro);

        // Município + UF
        addRow2(form, g, y++, "Município:", tfMunicipio, "UF:", cbUf);

        addRow(form, g, y++, "CEP:", tfCep);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionContato() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Contato"), BorderLayout.WEST);
        head.add(UiKit.hint("Telefone, e-mail e observações"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = baseGbc();
        int y = 0;

        addRow(form, g, y++, "Telefone:", tfTelefone);
        addRow(form, g, y++, "E-mail:", tfEmail);

        // Sócios/Obs
        g.gridy = y;
        g.gridx = 0;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Sócios / Observações:"), g);

        JScrollPane sp = UiKit.scroll(taSocios);
        sp.setPreferredSize(new Dimension(0, 80));

        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        form.add(sp, g);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionNfce() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("NFC-e / Fiscal"), BorderLayout.WEST);
        head.add(UiKit.hint("Modelo, série, numeração, CSC e certificado"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = baseGbc();
        int y = 0;

        addRow2(form, g, y++, "Modelo (ex: 65):", tfModeloNota, "Série:", tfSerieNota);
        addRow(form, g, y++, "Número Inicial:", tfNumeroInicialNota);
        addRow(form, g, y++, "Ambiente:", cbAmbienteNfce);

        addRow(form, g, y++, "CSC:", tfCsc);
        addRow(form, g, y++, "Token CSC:", tfTokenCsc);

        JPanel certRow = new JPanel(new BorderLayout(6, 0));
        certRow.setOpaque(false);
        JButton btnBrowseCert = UiKit.ghost("Procurar...");
        btnBrowseCert.addActionListener(e -> escolherCertificado());
        certRow.add(tfCertificadoPath, BorderLayout.CENTER);
        certRow.add(btnBrowseCert, BorderLayout.EAST);
        addRow(form, g, y++, "Certificado (PFX) - Caminho:", certRow);

        // Senha certificado
        addRow(form, g, y++, "Senha do Certificado:", tfCertificadoSenha);

        // Botão testar certificado
        JButton btnTestCert = UiKit.ghost("🔐 Testar Certificado");
        btnTestCert.addActionListener(e -> testarCertificado());
        g.gridy = y;
        g.gridx = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(btnTestCert, g);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Testa se certificado A1 é válido
     */
    private void testarCertificado() {
        String certPath = tfCertificadoPath.getText().trim();
        String certSenha = new String(tfCertificadoSenha.getPassword()).trim();

        if (certPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ Caminho do certificado vazio", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            XmlAssinaturaService signer = new XmlAssinaturaService(certPath, certSenha);
            signer.validarCertificado();
            JOptionPane.showMessageDialog(this, "✅ Certificado válido e não expirado!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent sectionImpressao() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("Impressão"), BorderLayout.WEST);
        head.add(UiKit.hint("Impressora térmica e rodapé"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = baseGbc();
        int y = 0;

        addRow(form, g, y++, "Nome da Impressora Térmica:", tfNomeImpressora);
        addRow(form, g, y++, "Texto de Rodapé do Cupom:", tfTextoRodapeNota);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionWebServiceProxy() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiKit.title("WebService e Proxy"), BorderLayout.WEST);
        head.add(UiKit.hint("Só preencha se você usa proxy/URL custom"), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = baseGbc();
        int y = 0;

        addRow(form, g, y++, "URL WebService NFC-e:", tfUrlWebServiceNfce);
        addRow(form, g, y++, "Proxy Host:", tfProxyHost);
        addRow2(form, g, y++, "Proxy Port:", tfProxyPort, "Proxy Usuário:", tfProxyUsuario);
        addRow(form, g, y++, "Proxy Senha:", tfProxySenha);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ===================== HELPERS DE LAYOUT =====================

    private GridBagConstraints baseGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weighty = 0;
        return g;
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, g);
    }

    private void addRow2(JPanel p, GridBagConstraints g, int row,
            String l1, JComponent f1,
            String l2, JComponent f2) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l1), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f1, g);

        g.gridx = 2;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l2), g);

        g.gridx = 3;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f2, g);
    }

    // ===================== CAMPOS COM MÁSCARA =====================

    private JFormattedTextField criarCampoCnpj() {
        try {
            MaskFormatter mask = new MaskFormatter("##.###.###/####-##");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    private JFormattedTextField criarCampoCep() {
        try {
            MaskFormatter mask = new MaskFormatter("#####-###");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    private JFormattedTextField criarCampoTelefone() {
        try {
            MaskFormatter mask = new MaskFormatter("(##) #####-####");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    // ===================== LÓGICA EXISTENTE (INTACTA) =====================

    private void carregarDadosExistentes() {
        try {
            ConfigLojaDAO dao = new ConfigLojaDAO();
            currentConfig = dao.buscar();
            if (currentConfig != null) {
                tfNome.setText(currentConfig.getNome());
                tfNomeFantasia.setText(currentConfig.getNomeFantasia());
                tfCnpj.setText(currentConfig.getCnpj());
                tfInscricaoEstadual.setText(currentConfig.getInscricaoEstadual());
                cbRegimeTrib.setSelectedItem(currentConfig.getRegimeTributario());
                tfCnae.setText(currentConfig.getCnae());

                tfLogradouro.setText(currentConfig.getEnderecoLogradouro());
                tfNumero.setText(currentConfig.getEnderecoNumero());
                tfComplemento.setText(currentConfig.getEnderecoComplemento());
                tfBairro.setText(currentConfig.getEnderecoBairro());
                tfMunicipio.setText(currentConfig.getEnderecoMunicipio());
                cbUf.setSelectedItem(currentConfig.getEnderecoUf());
                tfCep.setText(currentConfig.getEnderecoCep());

                tfTelefone.setText(currentConfig.getTelefone());
                tfEmail.setText(currentConfig.getEmail());

                tfModeloNota.setText(currentConfig.getModeloNota());
                tfSerieNota.setText(currentConfig.getSerieNota());
                tfNumeroInicialNota.setText(String.valueOf(currentConfig.getNumeroInicialNota()));
                cbAmbienteNfce.setSelectedItem(currentConfig.getAmbienteNfce());
                tfCsc.setText(currentConfig.getCsc());
                tfTokenCsc.setText(currentConfig.getTokenCsc());
                tfCertificadoPath.setText(currentConfig.getCertificadoPath());
                tfCertificadoSenha.setText(currentConfig.getCertificadoSenha());
                String certDir = currentConfig.getCertificadoDir();
                if (certDir != null && !certDir.isBlank()) {
                    File dir = new File(certDir);
                    if (dir.isDirectory()) {
                        lastCertDir = dir;
                    }
                }

                tfNomeImpressora.setText(currentConfig.getNomeImpressora());
                tfTextoRodapeNota.setText(currentConfig.getTextoRodapeNota());

                tfUrlWebServiceNfce.setText(currentConfig.getUrlWebServiceNfce());
                tfProxyHost.setText(currentConfig.getProxyHost());
                tfProxyPort.setText(String.valueOf(currentConfig.getProxyPort()));
                tfProxyUsuario.setText(currentConfig.getProxyUsuario());
                tfProxySenha.setText(currentConfig.getProxySenha());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar configuração:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSalvar() {
        // === seu método original aqui, intacto ===
        // (não alterei nada, só mantive o corpo como estava no seu código)
        String nome = tfNome.getText().trim();
        String nomeFantasia = tfNomeFantasia.getText().trim();
        String cnpj = tfCnpj.getText().trim();
        String inscricaoEstadual = tfInscricaoEstadual.getText().trim();
        String regimeTributario = (String) cbRegimeTrib.getSelectedItem();
        String cnae = tfCnae.getText().trim();

        String logradouro = tfLogradouro.getText().trim();
        String numero = tfNumero.getText().trim();
        String complemento = tfComplemento.getText().trim();
        String bairro = tfBairro.getText().trim();
        String municipio = tfMunicipio.getText().trim();
        String uf = (String) cbUf.getSelectedItem();
        String cep = tfCep.getText().trim();

        String telefone = tfTelefone.getText().trim();
        String email = tfEmail.getText().trim();
        String socios = taSocios.getText().trim(); // (mantido, mesmo que não salve no model atual)

        String modeloNota = tfModeloNota.getText().trim();
        String serieNota = tfSerieNota.getText().trim();
        int numeroInicialNota;
        try {
            numeroInicialNota = Integer.parseInt(tfNumeroInicialNota.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Número Inicial da Nota deve ser um número válido.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            tfNumeroInicialNota.requestFocus();
            return;
        }
        String ambienteNfce = (String) cbAmbienteNfce.getSelectedItem();
        String csc = tfCsc.getText().trim();
        String tokenCsc = tfTokenCsc.getText().trim();
        String certificadoPath = tfCertificadoPath.getText().trim();
        String certificadoSenha = new String(tfCertificadoSenha.getPassword()).trim();
        String certificadoDir = null;
        if (lastCertDir != null) {
            certificadoDir = lastCertDir.getAbsolutePath();
        } else if (!certificadoPath.isEmpty()) {
            File f = new File(certificadoPath);
            File parent = f.getParentFile();
            if (parent != null) {
                certificadoDir = parent.getAbsolutePath();
            }
        }

        String nomeImpressora = tfNomeImpressora.getText().trim();
        String textoRodapeNota = tfTextoRodapeNota.getText().trim();

        String urlWebServiceNfce = tfUrlWebServiceNfce.getText().trim();
        String proxyHost = tfProxyHost.getText().trim();
        int proxyPort;
        try {
            proxyPort = Integer.parseInt(tfProxyPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Proxy Port deve ser um número válido.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            tfProxyPort.requestFocus();
            return;
        }
        String proxyUsuario = tfProxyUsuario.getText().trim();
        String proxySenha = new String(tfProxySenha.getPassword()).trim();

        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "O campo 'Razão Social (Nome)' é obrigatório.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfNome.requestFocus();
            return;
        }
        String cnpjSomenteDigitos = cnpj.replaceAll("[^\\d]", "");
        if (cnpjSomenteDigitos.length() != 14) {
            JOptionPane.showMessageDialog(this,
                    "CNPJ inválido. Verifique o formato.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            tfCnpj.requestFocus();
            return;
        }
        if (modeloNota.isEmpty() || serieNota.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Os campos 'Modelo da Nota' e 'Série da Nota' são obrigatórios.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfModeloNota.requestFocus();
            return;
        }

        try {
            ConfigLojaDAO dao = new ConfigLojaDAO();
            if (currentConfig == null) {
                ConfigLojaModel cfg = new ConfigLojaModel(
                        nome,
                        nomeFantasia,
                        cnpjSomenteDigitos,
                        inscricaoEstadual,
                        regimeTributario,
                        cnae,
                        logradouro,
                        numero,
                        complemento,
                        bairro,
                        municipio,
                        uf,
                        cep,
                        telefone,
                        email,
                        modeloNota,
                        serieNota,
                        numeroInicialNota,
                        ambienteNfce,
                        csc,
                        tokenCsc,
                        certificadoPath,
                        certificadoDir,
                        certificadoSenha,
                        nomeImpressora,
                        textoRodapeNota,
                        urlWebServiceNfce,
                        proxyHost,
                        proxyPort,
                        proxyUsuario,
                        proxySenha);
                dao.inserir(cfg);
                syncNfceConfig(cfg);
            } else {
                currentConfig.setNome(nome);
                currentConfig.setNomeFantasia(nomeFantasia);
                currentConfig.setCnpj(cnpjSomenteDigitos);
                currentConfig.setInscricaoEstadual(inscricaoEstadual);
                currentConfig.setRegimeTributario(regimeTributario);
                currentConfig.setCnae(cnae);

                currentConfig.setEnderecoLogradouro(logradouro);
                currentConfig.setEnderecoNumero(numero);
                currentConfig.setEnderecoComplemento(complemento);
                currentConfig.setEnderecoBairro(bairro);
                currentConfig.setEnderecoMunicipio(municipio);
                currentConfig.setEnderecoUf(uf);
                currentConfig.setEnderecoCep(cep);

                currentConfig.setTelefone(telefone);
                currentConfig.setEmail(email);

                currentConfig.setModeloNota(modeloNota);
                currentConfig.setSerieNota(serieNota);
                currentConfig.setNumeroInicialNota(numeroInicialNota);
                currentConfig.setAmbienteNfce(ambienteNfce);
                currentConfig.setCsc(csc);
                currentConfig.setTokenCsc(tokenCsc);
                currentConfig.setCertificadoPath(certificadoPath);
                currentConfig.setCertificadoDir(certificadoDir);
                currentConfig.setCertificadoSenha(certificadoSenha);

                currentConfig.setNomeImpressora(nomeImpressora);
                currentConfig.setTextoRodapeNota(textoRodapeNota);

                currentConfig.setUrlWebServiceNfce(urlWebServiceNfce);
                currentConfig.setProxyHost(proxyHost);
                currentConfig.setProxyPort(proxyPort);
                currentConfig.setProxyUsuario(proxyUsuario);
                currentConfig.setProxySenha(proxySenha);

                dao.atualizar(currentConfig);
                syncNfceConfig(currentConfig);
            }

            JOptionPane.showMessageDialog(this,
                    "Configurações salvas com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar configuração:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void escolherCertificado() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar certificado (.pfx/.p12)");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Certificados (*.pfx, *.p12)", "pfx", "p12"));
        if (lastCertDir != null) {
            chooser.setCurrentDirectory(lastCertDir);
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            File selected = chooser.getSelectedFile();
            String name = selected.getName().toLowerCase(Locale.ROOT);
            if (!(name.endsWith(".pfx") || name.endsWith(".p12"))) {
                JOptionPane.showMessageDialog(this,
                        "Arquivo inv\u00e1lido. Selecione um certificado .pfx ou .p12.",
                        "Certificado", JOptionPane.WARNING_MESSAGE);
                return;
            }
            lastCertDir = selected.getParentFile();
            tfCertificadoPath.setText(selected.getAbsolutePath());
        }
    }

    private void syncNfceConfig(ConfigLojaModel loja) throws SQLException {
        ConfigNfceModel nfce = new ConfigNfceModel();
        nfce.setId("CONFIG_PADRAO");
        nfce.setEmitirNfce(1);
        nfce.setAmbiente(loja.getAmbienteNfce());
        nfce.setSerieNfce(parseIntSafe(loja.getSerieNota(), 1));
        nfce.setNumeroInicialNfce(loja.getNumeroInicialNota());

        nfce.setNomeEmpresa(loja.getNome());
        nfce.setNomeFantasia(loja.getNomeFantasia());
        nfce.setCnpj(loja.getCnpj());
        nfce.setInscricaoEstadual(loja.getInscricaoEstadual());
        nfce.setRegimeTributario(loja.getRegimeTributario());
        nfce.setUf(loja.getEnderecoUf());

        nfce.setEnderecoLogradouro(loja.getEnderecoLogradouro());
        nfce.setEnderecoNumero(loja.getEnderecoNumero());
        nfce.setEnderecoComplemento(loja.getEnderecoComplemento());
        nfce.setEnderecoBairro(loja.getEnderecoBairro());
        nfce.setEnderecoMunicipio(loja.getEnderecoMunicipio());
        nfce.setEnderecoCep(loja.getEnderecoCep());

        String token = resolveCscToken(loja.getCsc(), loja.getTokenCsc());
        int idCsc = resolveIdCsc(loja.getCsc(), loja.getTokenCsc());
        nfce.setCsc(token);
        nfce.setIdCsc(idCsc);

        nfce.setCertA1Path(loja.getCertificadoPath());
        nfce.setCertA1Senha(loja.getCertificadoSenha());

        boolean online = nfce.getCertA1Path() != null && !nfce.getCertA1Path().isBlank()
                && nfce.getCertA1Senha() != null && !nfce.getCertA1Senha().isBlank()
                && nfce.getIdCsc() > 0
                && nfce.getCsc() != null && !nfce.getCsc().isBlank();
        nfce.setModoEmissao(online ? "ONLINE_SEFAZ" : "OFFLINE_VALIDACAO");

        new ConfigNfceDAO().saveConfig(nfce);
    }

    private int resolveIdCsc(String csc, String token) {
        int id = parseIntSafe(csc, 0);
        if (id > 0) return id;
        return parseIntSafe(token, 0);
    }

    private String resolveCscToken(String csc, String token) {
        if (token != null && !token.isBlank()) return token.trim();
        return csc != null ? csc.trim() : null;
    }

    private int parseIntSafe(String value, int fallback) {
        if (value == null) return fallback;
        String raw = value.trim();
        if (raw.isEmpty()) return fallback;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
