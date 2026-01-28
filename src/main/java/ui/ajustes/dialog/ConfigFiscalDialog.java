package ui.ajustes.dialog;

import dao.ConfigFiscalDAO;
import dao.ConfigFiscalDefaultDAO;
import dao.CfopDAO;
import dao.CsosnDAO;
import dao.NcmDAO;
import dao.OrigemDAO;
import dao.FiscalCatalogDAO;
import model.CodigoDescricaoModel;
import model.CfopModel;
import model.ConfigFiscalModel;
import model.CsosnModel;
import model.NcmModel;
import model.OrigemModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Diálogo para configuração fiscal de um cliente:
 * - Regime tributário
 * - CFOP padrão
 * - CSOSN padrão
 * - Origem padrão
 * - NCM padrão
 * - Unidade padrão
 */
public class ConfigFiscalDialog extends JDialog {

    private final String clienteId;

    private final JComboBox<String> cbRegime = new JComboBox<>(new String[] {
            "Simples Nacional",
            "Lucro Presumido",
            "Lucro Real"
    });

    private final JComboBox<String> cbCfop = new JComboBox<>();
    private final JComboBox<String> cbCsosn = new JComboBox<>();
    private final JComboBox<String> cbOrigem = new JComboBox<>();
    private final JComboBox<String> cbNcm = new JComboBox<>();
    private final JComboBox<String> cbUnidade = new JComboBox<>();

    private ConfigFiscalModel currentConfig;
    private final boolean isDefaultConfig;

    public ConfigFiscalDialog(Frame owner, String clienteId) {
        super(owner, "Configuração Fiscal", true);
        this.clienteId = clienteId;
        this.isDefaultConfig = "LOJA".equalsIgnoreCase(clienteId) || "DEFAULT".equalsIgnoreCase(clienteId);

        UiKit.applyDialogBase(this);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        initComponents();

        pack();
        setMinimumSize(new Dimension(760, 420));
        setLocationRelativeTo(owner);

        popularCombos();
        carregarDadosExistentes();
    }

    private void initComponents() {
        // ===================== CARD =====================
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(12, 12));
        add(card, BorderLayout.CENTER);

        // ===================== HEADER =====================
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Dados fiscais padrão"));
        header.add(UiKit.hint((isDefaultConfig ? "Padrão da loja" : "Cliente: " + clienteId) + " • ENTER salva • ESC cancela"));
        card.add(header, BorderLayout.NORTH);

        // ===================== FORM (SCROLL) =====================
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JScrollPane scroll = UiKit.scroll(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        // Padrões visuais
        Dimension labelSize = new Dimension(150, 26);
        Dimension fieldSize = new Dimension(420, 32);

        padCombo(cbRegime, fieldSize);
        padCombo(cbCfop, fieldSize);
        padCombo(cbCsosn, fieldSize);
        padCombo(cbOrigem, fieldSize);
        padCombo(cbNcm, fieldSize);
        padCombo(cbUnidade, new Dimension(140, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        addRow(form, gbc, labelSize, "Regime tributário:", cbRegime);
        addRow(form, gbc, labelSize, "CFOP padrão:", cbCfop);
        addRow(form, gbc, labelSize, "CSOSN padrão:", cbCsosn);
        addRow(form, gbc, labelSize, "Origem padrão:", cbOrigem);
        addRow(form, gbc, labelSize, "NCM padrão:", cbNcm);

        // Unidade em linha (fica mais “compacto” e bonito)
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lUn = new JLabel("Unidade padrão:");
        lUn.setPreferredSize(labelSize);
        form.add(lUn, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel unidadeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        unidadeWrap.setOpaque(false);
        unidadeWrap.add(cbUnidade);
        form.add(unidadeWrap, gbc);
        gbc.gridy++;

        // “empurra” pra cima
        gbc.gridx = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        form.add(Box.createVerticalGlue(), gbc);

        // ===================== FOOTER BUTTONS =====================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        card.add(footer, BorderLayout.SOUTH);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        JButton btnSalvar = UiKit.primary("Salvar");

        btnCancelar.setPreferredSize(new Dimension(110, 32));
        btnSalvar.setPreferredSize(new Dimension(110, 32));

        btnSalvar.addActionListener(e -> onSalvar());
        btnCancelar.addActionListener(e -> dispose());

        footer.add(btnCancelar);
        footer.add(btnSalvar);

        // Enter salva
        getRootPane().setDefaultButton(btnSalvar);

        // ESC fecha
        bindEscapeToClose();
    }

    private void addRow(JPanel form, GridBagConstraints gbc, Dimension labelSize, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel l = new JLabel(label);
        l.setPreferredSize(labelSize);
        form.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, gbc);

        gbc.gridy++;
    }

    private void padCombo(JComboBox<String> cb, Dimension size) {
        cb.setPreferredSize(size);
        cb.setMinimumSize(size);
    }

    private void bindEscapeToClose() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    /** Preenche os JComboBox com dados vindos das tabelas fiscais. */
    private void popularCombos() {
        try {
            cbCfop.removeAllItems();
            cbCsosn.removeAllItems();
            cbOrigem.removeAllItems();
            cbNcm.removeAllItems();
            cbUnidade.removeAllItems();

            // CFOP
            List<CfopModel> listaCfops = new CfopDAO().buscarTodos();
            for (CfopModel item : listaCfops) {
                cbCfop.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // CSOSN
            List<CsosnModel> listaCsosn = new CsosnDAO().buscarTodos();
            for (CsosnModel item : listaCsosn) {
                cbCsosn.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // Origem
            List<OrigemModel> listaOrigem = new OrigemDAO().buscarTodos();
            for (OrigemModel item : listaOrigem) {
                cbOrigem.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // NCM
            List<NcmModel> listaNcm = new NcmDAO().findAll();
            for (NcmModel item : listaNcm) {
                cbNcm.addItem(item.getCodigo() + " - " + item.getDescricao());
            }

            // Unidades
            List<CodigoDescricaoModel> unidades = new FiscalCatalogDAO().findAll("unidades");
            for (CodigoDescricaoModel u : unidades) {
                cbUnidade.addItem(u.getCodigo() + " - " + u.getDescricao());
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
            if (isDefaultConfig) {
                currentConfig = new ConfigFiscalDefaultDAO().getDefault();
            } else {
                currentConfig = new ConfigFiscalDAO().buscarPorCliente(clienteId);
            }
            if (currentConfig == null)
                return;

            cbRegime.setSelectedItem(currentConfig.getRegimeTributario());
            selecionarPorCodigoPrefix(cbCfop, currentConfig.getCfopPadrao());
            selecionarPorCodigoPrefix(cbCsosn, currentConfig.getCsosnPadrao());
            selecionarPorCodigoPrefix(cbOrigem, currentConfig.getOrigemPadrao());
            selecionarPorCodigoPrefix(cbNcm, currentConfig.getNcmPadrao());
            selecionarPorCodigoPrefix(cbUnidade, currentConfig.getUnidadePadrao());

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar configuração fiscal:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selecionarPorCodigoPrefix(JComboBox<String> combo, String codigo) {
        if (codigo == null || codigo.isBlank())
            return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String it = combo.getItemAt(i);
            if (it != null && it.startsWith(codigo + " ")) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Valida e persiste as escolhas do usuário. */
    private void onSalvar() {
        String regime = (String) cbRegime.getSelectedItem();

        String cfop = firstToken((String) cbCfop.getSelectedItem());
        String csosn = firstToken((String) cbCsosn.getSelectedItem());
        String origem = firstToken((String) cbOrigem.getSelectedItem());
        String ncm = firstToken((String) cbNcm.getSelectedItem());
        String unidade = firstToken((String) cbUnidade.getSelectedItem());

        if (regime == null || regime.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecione o regime tributário.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbRegime.requestFocusInWindow();
            return;
        }
        if (cfop.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecione um CFOP padrão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbCfop.requestFocusInWindow();
            return;
        }
        if (csosn.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecione um CSOSN padrão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbCsosn.requestFocusInWindow();
            return;
        }
        if (origem.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecione uma origem padrão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbOrigem.requestFocusInWindow();
            return;
        }
        if (ncm.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecione um NCM padrão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbNcm.requestFocusInWindow();
            return;
        }
        if (unidade == null || unidade.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecione uma unidade padrão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            cbUnidade.requestFocusInWindow();
            return;
        }

        try {
            if (isDefaultConfig) {
                ConfigFiscalModel cfg = new ConfigFiscalModel("DEFAULT", regime, cfop, csosn, origem, ncm, unidade);
                new ConfigFiscalDefaultDAO().saveDefault(cfg);
            } else {
                ConfigFiscalDAO dao = new ConfigFiscalDAO();
                if (currentConfig == null) {
                    ConfigFiscalModel cfg = new ConfigFiscalModel(clienteId, regime, cfop, csosn, origem, ncm, unidade);
                    dao.inserir(cfg);
                } else {
                    currentConfig.setRegimeTributario(regime);
                    currentConfig.setCfopPadrao(cfop);
                    currentConfig.setCsosnPadrao(csosn);
                    currentConfig.setOrigemPadrao(origem);
                    currentConfig.setNcmPadrao(ncm);
                    currentConfig.setUnidadePadrao(unidade);
                    dao.atualizar(currentConfig);
                }
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

    private String firstToken(String s) {
        if (s == null)
            return "";
        String[] parts = s.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}
