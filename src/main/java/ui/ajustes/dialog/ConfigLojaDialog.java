package ui.ajustes.dialog;

import dao.ConfigLojaDAO;
import model.ConfigLojaModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

/**
 * Diálogo para configuração completa dos dados da loja, incluindo:
 * - Dados cadastrais (Razão Social, Nome Fantasia, CNPJ, IE, Regime, CNAE)
 * - Endereço completo (Logradouro, Número, Complemento, Bairro, Município, UF,
 * CEP)
 * - Contato (Telefone, E-mail)
 * - Parâmetros para NFC-e (Modelo, Série, Número Inicial, Ambiente, CSC, Token
 * CSC, Certificado Digital)
 * - Impressão (Nome da impressora térmica, Texto de rodapé)
 * - URL do WebService NFC-e e configurações de proxy
 */
public class ConfigLojaDialog extends JDialog {

    // ===== Campos cadastrais =====
    private final JTextField tfNome = new JTextField(25);
    private final JTextField tfNomeFantasia = new JTextField(25);
    private final JFormattedTextField tfCnpj = criarCampoCnpj();
    private final JTextField tfInscricaoEstadual = new JTextField(15);
    private final JComboBox<String> cbRegimeTrib = new JComboBox<>(
            new String[] { "Simples Nacional", "Lucro Presumido", "Lucro Real" });
    private final JTextField tfCnae = new JTextField(10);

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
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        carregarDadosExistentes();
    }

    private void initComponents() {
        JPanel painelPrincipal = new JPanel(new BorderLayout());
        painelPrincipal.setBorder(new EmptyBorder(12, 12, 12, 12));
        getContentPane().add(painelPrincipal);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Dados da Loja e NFC-e"));
        JScrollPane scroll = new JScrollPane(formPanel);
        scroll.setBorder(null); // aparência limpa
        scroll.getVerticalScrollBar().setUnitIncrement(16); // rolagem suave
        painelPrincipal.add(scroll, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // --- Linha 1: Razão Social ---
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Razão Social (Nome):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNome, gbc);

        // --- Nome Fantasia ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Nome Fantasia:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNomeFantasia, gbc);

        // --- CNPJ ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("CNPJ:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCnpj, gbc);

        // --- Inscrição Estadual ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Inscrição Estadual:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfInscricaoEstadual, gbc);

        // --- Regime Tributário ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Regime Tributário:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(cbRegimeTrib, gbc);

        // --- CNAE ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("CNAE Principal:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCnae, gbc);

        // --- Linha em branco separando seções ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // ===== ENDEREÇO =====
        // --- Logradouro ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Logradouro:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfLogradouro, gbc);

        // --- Número ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Número:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNumero, gbc);

        // --- Complemento ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Complemento:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfComplemento, gbc);

        // --- Bairro ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Bairro:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfBairro, gbc);

        // --- Município ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Município:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfMunicipio, gbc);

        // --- UF ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("UF:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(cbUf, gbc);

        // --- CEP ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("CEP:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCep, gbc);

        // --- Linha em branco separando seções ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // ===== CONTATO =====
        // --- Telefone ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Telefone:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfTelefone, gbc);

        // --- E-mail ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("E-mail:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfEmail, gbc);

        // --- Sócios / Observações ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Sócios / Observações:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JScrollPane spSocios = new JScrollPane(taSocios);
        spSocios.setPreferredSize(new Dimension(0, 60));
        formPanel.add(spSocios, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        // --- Linha em branco separando seções ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // ===== NFC-e / DOCUMENTO FISCAL =====
        // --- Modelo da Nota ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Modelo da Nota (ex: 65):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfModeloNota, gbc);

        // --- Série da Nota ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Série da Nota:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfSerieNota, gbc);

        // --- Número Inicial da Nota ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Número Inicial da Nota:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNumeroInicialNota, gbc);

        // --- Ambiente NFC-e ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Ambiente NFC-e:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(cbAmbienteNfce, gbc);

        // --- CSC ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("CSC (Código de Segurança):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCsc, gbc);

        // --- Token CSC ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Token CSC:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfTokenCsc, gbc);

        // --- Caminho do Certificado Digital (.pfx) ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Certificado Digital (PFX) - Caminho:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCertificadoPath, gbc);

        // --- Senha do Certificado ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Senha do Certificado Digital:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCertificadoSenha, gbc);

        // --- Linha em branco separando seções ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // ===== IMPRESSÃO =====
        // --- Nome da Impressora ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Nome da Impressora Térmica:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNomeImpressora, gbc);

        // --- Texto de Rodapé do Cupom ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Texto de Rodapé do Cupom:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfTextoRodapeNota, gbc);

        // --- Linha em branco separando seções ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // ===== WEBSERVICE NFC-e E PROXY =====
        // --- URL WebService NFC-e ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("URL WebService NFC-e:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfUrlWebServiceNfce, gbc);

        // --- Proxy Host ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Proxy Host:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfProxyHost, gbc);

        // --- Proxy Port ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Proxy Port:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfProxyPort, gbc);

        // --- Proxy Usuário ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Proxy Usuário:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfProxyUsuario, gbc);

        // --- Proxy Senha ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Proxy Senha:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfProxySenha, gbc);

        // ===== RODAPÉ COM BOTÕES =====
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        painelPrincipal.add(rodape, BorderLayout.SOUTH);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setPreferredSize(new Dimension(100, 30));
        btnSalvar.addActionListener(e -> onSalvar());
        rodape.add(btnSalvar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(100, 30));
        btnCancelar.addActionListener(e -> dispose());
        rodape.add(btnCancelar);

        // Ajusta Enter para acionar "Salvar"
        getRootPane().setDefaultButton(btnSalvar);

        // Fecha como cancelado se usuário fechar a janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    /**
     * Cria um JFormattedTextField com máscara para CNPJ: ##.###.###/####-##
     */
    private JFormattedTextField criarCampoCnpj() {
        try {
            MaskFormatter mask = new MaskFormatter("##.###.###/####-##");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    /**
     * Cria JFormattedTextField com máscara para CEP: #####-###
     */
    private JFormattedTextField criarCampoCep() {
        try {
            MaskFormatter mask = new MaskFormatter("#####-###");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    /**
     * Cria JFormattedTextField com máscara para Telefone: (##) #####-####
     */
    private JFormattedTextField criarCampoTelefone() {
        try {
            MaskFormatter mask = new MaskFormatter("(##) #####-####");
            mask.setPlaceholderCharacter('_');
            return new JFormattedTextField(mask);
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    /**
     * Carrega valores existentes do banco via ConfigLojaDAO e preenche todos os
     * campos.
     */
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

    /**
     * Valida campos obrigatórios e persiste a configuração via DAO.
     */
    private void onSalvar() {
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
        String socios = taSocios.getText().trim();

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

        // Validações básicas
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
                // Cria novo model com todos os valores
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
                        certificadoSenha,
                        nomeImpressora,
                        textoRodapeNota,
                        urlWebServiceNfce,
                        proxyHost,
                        proxyPort,
                        proxyUsuario,
                        proxySenha);
                dao.inserir(cfg);
            } else {
                // Atualiza o objeto existente e persiste
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
                currentConfig.setCertificadoSenha(certificadoSenha);

                currentConfig.setNomeImpressora(nomeImpressora);
                currentConfig.setTextoRodapeNota(textoRodapeNota);

                currentConfig.setUrlWebServiceNfce(urlWebServiceNfce);
                currentConfig.setProxyHost(proxyHost);
                currentConfig.setProxyPort(proxyPort);
                currentConfig.setProxyUsuario(proxyUsuario);
                currentConfig.setProxySenha(proxySenha);

                dao.atualizar(currentConfig);
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
}
