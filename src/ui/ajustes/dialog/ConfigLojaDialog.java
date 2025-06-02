package ui.ajustes.dialog;

import dao.ConfigLojaDAO;
import model.ConfigLojaModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Diálogo de configuração dos dados da loja (nome, CNPJ, telefone e sócios).
 * Também expõe um botão que abre o diálogo de Configuração de Notas Fiscais.
 * Inclui máscara e validação de CNPJ e persiste via ConfigLojaDAO/ConfigLojaModel.
 */
public class ConfigLojaDialog extends JDialog {

    private final JTextField tfNome           = new JTextField(25);
    private final JFormattedTextField tfCnpj  = criarCampoCnpj();
    private final JTextField tfFone           = new JTextField(15);
    private final JTextArea  taSocios         = new JTextArea(4, 25);

    // Guarda a configuração carregada (ou null se for a primeira vez)
    private ConfigLojaModel currentConfig;

    public ConfigLojaDialog(Frame owner) {
        super(owner, "Configuração - Dados da Loja", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        carregarDadosExistentes();
    }

    private void initComponents() {
        // ================= Painel principal =================
        JPanel painelPrincipal = new JPanel(new BorderLayout());
        painelPrincipal.setBorder(new EmptyBorder(12, 12, 12, 12));
        getContentPane().add(painelPrincipal);

        // ================= Conteúdo (formulário) =================
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Dados da Loja"));
        painelPrincipal.add(formPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // --- Linha 1: Nome da Loja ---
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Nome da Loja:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNome, gbc);

        // --- Linha 2: CNPJ ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("CNPJ:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfCnpj, gbc);

        // --- Linha 3: Telefone ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Telefone:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfFone, gbc);

        // --- Linha 4: Sócios (área de texto) ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Sócios / Observações:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JScrollPane spSocios = new JScrollPane(taSocios);
        spSocios.setPreferredSize(new Dimension(0, 80));
        formPanel.add(spSocios, gbc);

        // ================= Botões na parte inferior =================
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        painelPrincipal.add(rodape, BorderLayout.SOUTH);

        // --- Botão: Abrir diálogo de Configuração de Notas Fiscais ---
        JButton btnConfigNotas = new JButton("Configurar Notas Fiscais");
        btnConfigNotas.setPreferredSize(new Dimension(180, 30));
        btnConfigNotas.addActionListener(e -> {
            ConfigNotasFiscaisDialog notasDialog = new ConfigNotasFiscaisDialog(this, currentConfig);
            notasDialog.setVisible(true);
            // after closing notasDialog, you could refresh currentConfig if notas were updated there
        });
        rodape.add(btnConfigNotas);

        // --- Botão: Salvar dados da loja ---
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setPreferredSize(new Dimension(100, 30));
        btnSalvar.addActionListener(e -> onSalvar());
        rodape.add(btnSalvar);

        // --- Botão: Cancelar / Fechar ---
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(100, 30));
        btnCancelar.addActionListener(e -> dispose());
        rodape.add(btnCancelar);

        // Ajustar tecla Enter para acionar "Salvar"
        getRootPane().setDefaultButton(btnSalvar);

        // Se o usuário fechar a janela, trata como "Cancelar"
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
            e.printStackTrace();
            return new JFormattedTextField();
        }
    }

    /**
     * Carrega valores existentes do banco via ConfigLojaDAO e preenche os campos.
     */
    private void carregarDadosExistentes() {
        try {
            ConfigLojaDAO dao = new ConfigLojaDAO();
            currentConfig = dao.buscar();
            if (currentConfig != null) {
                tfNome.setText(currentConfig.getNome());
                tfCnpj.setText(currentConfig.getCnpj());
                tfFone.setText(currentConfig.getTelefone());
                taSocios.setText(currentConfig.getSocios());
                // Se quiser, pode passar currentConfig para ConfigNotasFiscaisDialog
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar configuração:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Valida um CNPJ, retornando true se válido, false caso contrário.
     */
    private boolean isCnpjValido(String cnpj) {
        cnpj = cnpj.replaceAll("[^\\d]", ""); // remove máscara
        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        try {
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += (cnpj.charAt(i) - '0') * pesos1[i];
            }
            int dig1 = soma % 11 < 2 ? 0 : 11 - (soma % 11);

            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += (cnpj.charAt(i) - '0') * pesos2[i];
            }
            int dig2 = soma % 11 < 2 ? 0 : 11 - (soma % 11);

            return (cnpj.charAt(12) - '0') == dig1 && (cnpj.charAt(13) - '0') == dig2;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lógica chamada ao clicar em “Salvar”:
     * valida campos obrigatórios, faz validação de CNPJ e persiste via DAO.
     */
    private void onSalvar() {
        String nome   = tfNome.getText().trim();
        String cnpj   = tfCnpj.getText().trim();
        String fone   = tfFone.getText().trim();
        String socios = taSocios.getText().trim();

        // --- Validações básicas ---
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "O campo 'Nome da Loja' é obrigatório.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfNome.requestFocus();
            return;
        }

        // Remove caracteres não numéricos e valida CNPJ
        String cnpjSemMascara = cnpj.replaceAll("[^\\d]", "");
        if (cnpjSemMascara.length() != 14 || !isCnpjValido(cnpj)) {
            JOptionPane.showMessageDialog(this,
                    "CNPJ inválido. Verifique o formato e os dígitos.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            tfCnpj.requestFocus();
            return;
        }

        try {
            ConfigLojaDAO dao = new ConfigLojaDAO();
            if (currentConfig == null) {
                // Nenhuma configuração existente → cria nova
                ConfigLojaModel cfg = new ConfigLojaModel(
                        nome,
                        cnpj,
                        fone,
                        socios,
                        "",  // modeloNota vazio por ora
                        "",  // serieNota vazio
                        1,   // numeroInicialNota padrão
                        "",  // nomeImpressora vazio
                        ""   // textoRodapeNota vazio
                );
                dao.inserir(cfg);
            } else {
                // Já existe → atualiza apenas campos de loja, mantendo campos de notas fiscais
                currentConfig.setNome(nome);
                currentConfig.setCnpj(cnpj);
                currentConfig.setTelefone(fone);
                currentConfig.setSocios(socios);
                dao.atualizar(currentConfig);
            }

            JOptionPane.showMessageDialog(this,
                    "Configurações salvas com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar configuração:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
