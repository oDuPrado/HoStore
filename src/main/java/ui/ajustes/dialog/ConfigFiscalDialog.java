package ui.ajustes.dialog;

import dao.ConfigFiscalDAO;
import dao.CfopDAO;
import dao.CsosnDAO;
import dao.NcmDAO;
import dao.OrigemDAO;
import model.CfopModel;
import model.ConfigFiscalModel;
import model.CsosnModel;
import model.NcmModel;
import model.OrigemModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Diálogo para configuração fiscal de um cliente:
 * - Regime tributário
 * - CFOP padrão
 * - CSOSN padrão
 * - Origem padrão
 * - NCM padrão
 * - Unidade padrão
 *
 * A configuração é salva na tabela config_fiscal, vinculada a um cliente (clienteId).
 */
public class ConfigFiscalDialog extends JDialog {

    private final String clienteId; // id do cliente para quem estamos configurando

    // ===== Campos do formulário =====
    private final JComboBox<String> cbRegime = new JComboBox<>(new String[] {
        "Simples Nacional",
        "Lucro Presumido",
        "Lucro Real"
    });

    private final JComboBox<String> cbCfop = new JComboBox<>();   // será populado com dados da tabela cfop
    private final JComboBox<String> cbCsosn = new JComboBox<>();  // populado da tabela csosn
    private final JComboBox<String> cbOrigem = new JComboBox<>(); // populado da tabela origem
    private final JComboBox<String> cbNcm = new JComboBox<>();    // populado da tabela ncm (códigos + descrição)
    private final JComboBox<String> cbUnidade = new JComboBox<>(new String[] {
        "UN", "CX", "KG", "LT", "M", "M2", "M3"
    });

    // Configuração carregada (ou null se não existir)
    private ConfigFiscalModel currentConfig;

    public ConfigFiscalDialog(Frame owner, String clienteId) {
        super(owner, "Configuração Fiscal - Cliente " + clienteId, true);
        this.clienteId = clienteId;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        carregarDadosExistentes();
    }

    private void initComponents() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new EmptyBorder(12, 12, 12, 12));
        getContentPane().add(painel);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Dados Fiscais Padrão"));
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        painel.add(scroll, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // --- Linha 1: Regime Tributário ---
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("Regime Tributário:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(cbRegime, gbc);

        // --- CFOP padrão ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("CFOP Padrão:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(cbCfop, gbc);

        // --- CSOSN padrão ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("CSOSN Padrão:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(cbCsosn, gbc);

        // --- Origem padrão ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("Origem Padrão:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(cbOrigem, gbc);

        // --- NCM padrão ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("NCM Padrão:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(cbNcm, gbc);

        // --- Unidade padrão ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel("Unidade Padrão:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(cbUnidade, gbc);

        // --- Rodapé com botões ---
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        painel.add(rodape, BorderLayout.SOUTH);

        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.setPreferredSize(new Dimension(100, 30));
        btnSalvar.addActionListener(e -> onSalvar());
        rodape.add(btnSalvar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setPreferredSize(new Dimension(100, 30));
        btnCancelar.addActionListener(e -> dispose());
        rodape.add(btnCancelar);

        // Enter aciona salvar
        getRootPane().setDefaultButton(btnSalvar);

        // Fecha como cancelado se usuário fechar
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // Carrega dados de CFOP, CSOSN, Origem e NCM
        popularCombos();
    }

    /** Preenche os JComboBox com dados vindos das tabelas fiscais. */
    private void popularCombos() {
        try {
            // CFOP
            CfopDAO cfopDAO = new CfopDAO();
            List<CfopModel> listaCfops = cfopDAO.buscarTodos();
            for (CfopModel item : listaCfops) {
                cbCfop.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // CSOSN
            CsosnDAO csosnDAO = new CsosnDAO();
            List<CsosnModel> listaCsosn = csosnDAO.buscarTodos();
            for (CsosnModel item : listaCsosn) {
                cbCsosn.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // Origem
            OrigemDAO origemDAO = new OrigemDAO();
            List<OrigemModel> listaOrigem = origemDAO.buscarTodos();
            for (OrigemModel item : listaOrigem) {
                cbOrigem.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // NCM
            NcmDAO ncmDAO = new NcmDAO();
            List<NcmModel> listaNcm = ncmDAO.buscarTodos();
            for (NcmModel item : listaNcm) {
                // Exibe "95044000 - Jogos de cartas..."
                cbNcm.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar listas fiscais:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Se já existir configuração para este cliente, pré-preenche os campos. */
    private void carregarDadosExistentes() {
        try {
            ConfigFiscalDAO dao = new ConfigFiscalDAO();
            currentConfig = dao.buscarPorCliente(clienteId);
            if (currentConfig != null) {
                // Regime
                cbRegime.setSelectedItem(currentConfig.getRegimeTributario());

                // CFOP — temos itens no formato "5102 - ...", então só buscamos o prefixo
                String cfop = currentConfig.getCfopPadrao();
                for (int i = 0; i < cbCfop.getItemCount(); i++) {
                    if (cbCfop.getItemAt(i).startsWith(cfop + " ")) {
                        cbCfop.setSelectedIndex(i);
                        break;
                    }
                }

                // CSOSN
                String csosn = currentConfig.getCsosnPadrao();
                for (int i = 0; i < cbCsosn.getItemCount(); i++) {
                    if (cbCsosn.getItemAt(i).startsWith(csosn + " ")) {
                        cbCsosn.setSelectedIndex(i);
                        break;
                    }
                }

                // Origem
                String origem = currentConfig.getOrigemPadrao();
                for (int i = 0; i < cbOrigem.getItemCount(); i++) {
                    if (cbOrigem.getItemAt(i).startsWith(origem + " ")) {
                        cbOrigem.setSelectedIndex(i);
                        break;
                    }
                }

                // NCM
                String ncm = currentConfig.getNcmPadrao();
                for (int i = 0; i < cbNcm.getItemCount(); i++) {
                    if (cbNcm.getItemAt(i).startsWith(ncm + " ")) {
                        cbNcm.setSelectedIndex(i);
                        break;
                    }
                }

                // Unidade
                cbUnidade.setSelectedItem(currentConfig.getUnidadePadrao());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar configuração fiscal:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Valida e persiste as escolhas do usuário. */
    private void onSalvar() {
        // Regime tributário
        String regime = (String) cbRegime.getSelectedItem();

        // CFOP selecionado no formato "5102 - descrição"
        String cfopFull = (String) cbCfop.getSelectedItem();
        String cfop = cfopFull != null ? cfopFull.split(" ")[0] : "";

        // CSOSN
        String csosnFull = (String) cbCsosn.getSelectedItem();
        String csosn = csosnFull != null ? csosnFull.split(" ")[0] : "";

        // Origem
        String origemFull = (String) cbOrigem.getSelectedItem();
        String origem = origemFull != null ? origemFull.split(" ")[0] : "";

        // NCM
        String ncmFull = (String) cbNcm.getSelectedItem();
        String ncm = ncmFull != null ? ncmFull.split(" ")[0] : "";

        // Unidade
        String unidade = (String) cbUnidade.getSelectedItem();

        // Validações básicas
        if (regime == null || regime.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selecione o regime tributário.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cfop.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um CFOP padrão.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (csosn.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um CSOSN padrão.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (origem.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma origem padrão.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ncm.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um NCM padrão.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (unidade == null || unidade.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma unidade padrão.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ConfigFiscalDAO dao = new ConfigFiscalDAO();
            if (currentConfig == null) {
                // Cria nova configuração
                ConfigFiscalModel cfg = new ConfigFiscalModel(
                    clienteId, regime, cfop, csosn, origem, ncm, unidade
                );
                dao.inserir(cfg);
            } else {
                // Atualiza a configuração existente
                currentConfig.setRegimeTributario(regime);
                currentConfig.setCfopPadrao(cfop);
                currentConfig.setCsosnPadrao(csosn);
                currentConfig.setOrigemPadrao(origem);
                currentConfig.setNcmPadrao(ncm);
                currentConfig.setUnidadePadrao(unidade);
                dao.atualizar(currentConfig);
            }

            JOptionPane.showMessageDialog(this,
                    "Configuração fiscal salva com sucesso.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar configuração fiscal:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
