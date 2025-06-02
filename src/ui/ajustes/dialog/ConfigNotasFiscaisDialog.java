package ui.ajustes.dialog;

import dao.ConfigLojaDAO;
import model.ConfigLojaModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Diálogo de configuração de parâmetros para emissão de notas fiscais físicas,
 * como modelo, série, próximo número, impressora e texto de rodapé.
 * Recebe um ConfigLojaModel existente ou null (se for a primeira vez).
 * Valida campos mínimos e persiste alterações via ConfigLojaDAO.
 */
public class ConfigNotasFiscaisDialog extends JDialog {

    private final JTextField tfModeloNota     = new JTextField(20);
    private final JTextField tfSerieNota      = new JTextField(8);
    private final JSpinner   spNumeroInicial  = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final JTextField tfNomeImpressora = new JTextField(20);
    private final JTextArea  taTextoRodape    = new JTextArea(4, 25);

    // Guarda a configuração atual ou null se não existir
    private ConfigLojaModel currentConfig;

    /**
     * Construtor que recebe o pai e o ConfigLojaModel.
     * Se config for null, entenderemos que ainda não há registro no banco.
     */
    public ConfigNotasFiscaisDialog(Window owner, ConfigLojaModel config) {
        super(owner, "Configuração - Notas Fiscais", ModalityType.APPLICATION_MODAL);
        this.currentConfig = config;
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
        formPanel.setBorder(BorderFactory.createTitledBorder("Configuração de Notas Fiscais"));
        painelPrincipal.add(formPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // --- Linha 1: Modelo de Nota ---
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Modelo de Nota:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfModeloNota, gbc);

        // --- Linha 2: Série da Nota ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Série:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        formPanel.add(tfSerieNota, gbc);

        // --- Linha 3: Próximo Número de Nota ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Próximo Nº de Nota:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        formPanel.add(spNumeroInicial, gbc);

        // --- Linha 4: Nome da Impressora ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Nome da Impressora:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(tfNomeImpressora, gbc);

        // --- Linha 5: Texto de Rodapé ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Texto de Rodapé:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JScrollPane spRodape = new JScrollPane(taTextoRodape);
        spRodape.setPreferredSize(new Dimension(0, 80));
        formPanel.add(spRodape, gbc);

        // ================= Botões na parte inferior =================
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        painelPrincipal.add(rodape, BorderLayout.SOUTH);

        // --- Botão: Salvar configurações de notas fiscais ---
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
     * Carrega valores existentes de currentConfig (se não for null) e preenche os campos.
     */
    private void carregarDadosExistentes() {
        if (currentConfig != null) {
            tfModeloNota.setText(currentConfig.getModeloNota());
            tfSerieNota.setText(currentConfig.getSerieNota());
            spNumeroInicial.setValue(currentConfig.getNumeroInicialNota());
            tfNomeImpressora.setText(currentConfig.getNomeImpressora());
            taTextoRodape.setText(currentConfig.getTextoRodapeNota());
        }
    }

    /**
     * Lógica chamada ao clicar em “Salvar”:
     * valida campos mínimos e persiste via ConfigLojaDAO,
     * criando novo registro se currentConfig for null, ou atualizando caso contrário.
     */
    private void onSalvar() {
        String modeloNota    = tfModeloNota.getText().trim();
        String serieNota     = tfSerieNota.getText().trim();
        int    numeroInicial = (int) spNumeroInicial.getValue();
        String impressora    = tfNomeImpressora.getText().trim();
        String rodape        = taTextoRodape.getText().trim();

        // --- Validações básicas ---
        if (modeloNota.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "O campo 'Modelo de Nota' é obrigatório.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfModeloNota.requestFocus();
            return;
        }
        if (serieNota.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "O campo 'Série' é obrigatório.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfSerieNota.requestFocus();
            return;
        }
        if (numeroInicial < 1) {
            JOptionPane.showMessageDialog(this,
                    "O 'Próximo Nº de Nota' deve ser >= 1.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            spNumeroInicial.requestFocus();
            return;
        }

        try {
            ConfigLojaDAO dao = new ConfigLojaDAO();

            if (currentConfig == null) {
                // Não havia configuração anterior: buscar o registro de loja principal
                // para usar seus campos de nome, cnpj, telefone e sócios.
                // Caso você deseje que esses dados já estejam preenchidos, 
                // é preciso carregar uma instância de ConfigLojaModel antes de chamar este diálogo.
                //
                // Aqui assumimos que currentConfig foi passado como null apenas se o usuário não 
                // configurou nada antes. Para manter consistência, pode-se buscar um registro 
                // vazio ou criar um novo id genérico:
                String novoId = java.util.UUID.randomUUID().toString();
                ConfigLojaModel cfg = new ConfigLojaModel(
                        novoId,
                        "",   // nome da loja (preservado do fluxo principal)
                        "",   // cnpj
                        "",   // telefone
                        "",   // sócios
                        modeloNota,
                        serieNota,
                        numeroInicial,
                        impressora,
                        rodape
                );
                dao.inserir(cfg);
            } else {
                // Já existe: atualiza APENAS campos de notas fiscais, preservando outros dados
                currentConfig.setModeloNota(modeloNota);
                currentConfig.setSerieNota(serieNota);
                currentConfig.setNumeroInicialNota(numeroInicial);
                currentConfig.setNomeImpressora(impressora);
                currentConfig.setTextoRodapeNota(rodape);
                dao.atualizar(currentConfig);
            }

            JOptionPane.showMessageDialog(this,
                    "Configurações de Notas Fiscais salvas com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar configurações de notas fiscais:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
