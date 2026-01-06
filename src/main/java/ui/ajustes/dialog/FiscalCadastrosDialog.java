// src/main/java/ui/ajustes/dialog/FiscalCadastrosDialog.java
package ui.ajustes.dialog;

import model.CodigoDescricaoModel;
import service.FiscalCatalogService;
import service.FiscalCatalogService.CatalogType;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class FiscalCadastrosDialog extends JDialog {

    private final FiscalCatalogService service = FiscalCatalogService.getInstance();

    public FiscalCadastrosDialog(Window owner) {
        super(owner, "Configuração - Cadastros Fiscais", ModalityType.APPLICATION_MODAL);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("NCM", new CatalogTab(CatalogType.NCM));
        tabs.addTab("CFOP", new CatalogTab(CatalogType.CFOP));
        tabs.addTab("CSOSN", new CatalogTab(CatalogType.CSOSN));
        tabs.addTab("Origem", new CatalogTab(CatalogType.ORIGEM));
        tabs.addTab("Unidades", new CatalogTab(CatalogType.UNIDADES));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnClose = new JButton("Fechar");
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);

        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        setPreferredSize(new Dimension(980, 620));
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    // ========================= TAB GENÉRICA =========================

    private class CatalogTab extends JPanel {

        private final CatalogType type;

        private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Código", "Descrição"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        private final JTable table = new JTable(tableModel);

        // AGORA é criado no construtor (porque depende do type)
        private final JFormattedTextField txtCodigo;
        private final JTextField txtDescricao = new JTextField(40);

        CatalogTab(CatalogType type) {
            super(new BorderLayout(10, 10));
            this.type = type;

            // cria o campo depois que o type existe
            this.txtCodigo = buildCodigoField();

            // fonte e altura decentes
            txtCodigo.setFont(txtCodigo.getFont().deriveFont(15f));
            txtDescricao.setFont(txtDescricao.getFont().deriveFont(14f));

            txtDescricao.setPreferredSize(new Dimension(520, 30));

            JPanel input = buildInputPanel();
            JScrollPane scroll = buildTablePanel();
            JPanel buttons = buildButtonsPanel();

            add(input, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);

            // Renderer NCM (só na aba NCM)
            if (type == CatalogType.NCM) {
                table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                   boolean hasFocus, int row, int column) {
                        Object v = value;
                        if (v != null) v = formatNcm(v.toString());
                        return super.getTableCellRendererComponent(table, v, isSelected, hasFocus, row, column);
                    }
                });
            }

            load();
        }

        /**
         * Campo código com máscara para NCM/CFOP/CSOSN/Origem.
         * Unidades fica livre (UN, CX, KG etc).
         */
        private JFormattedTextField buildCodigoField() {
            JFormattedTextField f;

            try {
                MaskFormatter mf = null;

                switch (type) {
                    case NCM:
                        mf = new MaskFormatter("########"); // 8 dígitos
                        break;
                    case CFOP:
                        mf = new MaskFormatter("####"); // 4 dígitos
                        break;
                    case CSOSN:
                        mf = new MaskFormatter("###"); // 3 dígitos
                        break;
                    case ORIGEM:
                        mf = new MaskFormatter("#"); // 1 dígito
                        break;
                    case UNIDADES:
                    default:
                        mf = null; // sem máscara
                }

                if (mf != null) {
                    mf.setPlaceholderCharacter('_');
                    mf.setValueContainsLiteralCharacters(false);
                    f = new JFormattedTextField(mf);
                } else {
                    f = new JFormattedTextField();
                }

            } catch (ParseException e) {
                f = new JFormattedTextField();
            }

            // tamanho e legibilidade
            f.setColumns(type == CatalogType.UNIDADES ? 10 : 12);
            f.setPreferredSize(new Dimension(220, 32));
            f.setMinimumSize(new Dimension(220, 32));

            return f;
        }

        private JPanel buildInputPanel() {
            JPanel inputPanel = new JPanel(new GridBagLayout());
            inputPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    type.getTitle(),
                    TitledBorder.LEFT, TitledBorder.TOP
            ));

            JButton btnAdd = new JButton("Adicionar / Atualizar");
            btnAdd.addActionListener(e -> addOrUpdate());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;

            // Linha 0
            gbc.gridy = 0;

            // Label Código
            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            inputPanel.add(new JLabel(type.getCodeLabel()), gbc);

            // Campo Código (não esmagar)
            gbc.gridx = 1;
            gbc.weightx = 0.25;
            gbc.fill = GridBagConstraints.NONE; // respeita preferred size
            inputPanel.add(txtCodigo, gbc);

            // Label Descrição
            gbc.gridx = 2;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            inputPanel.add(new JLabel("Descrição:"), gbc);

            // Campo Descrição
            gbc.gridx = 3;
            gbc.weightx = 0.75;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            inputPanel.add(txtDescricao, gbc);

            // Botão
            gbc.gridx = 4;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            inputPanel.add(btnAdd, gbc);

            // Linha 1: hint
            JLabel hint = new JLabel(type.getHint());
            hint.setForeground(new Color(120, 120, 120));
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 5;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            inputPanel.add(hint, gbc);

            return inputPanel;
        }

        private JScrollPane buildTablePanel() {
            table.setRowHeight(24);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                        int row = table.getSelectedRow();
                        txtCodigo.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                        txtDescricao.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                        txtCodigo.requestFocus();
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createTitledBorder("Lista"));
            return scroll;
        }

        private JPanel buildButtonsPanel() {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

            JButton btnRemove = new JButton("Remover");
            btnRemove.addActionListener(e -> removeSelected());

            JButton btnSave = new JButton("Salvar");
            btnSave.addActionListener(e -> saveAll());

            JButton btnReload = new JButton("Recarregar");
            btnReload.addActionListener(e -> load());

            buttons.add(btnRemove);
            buttons.add(btnSave);
            buttons.add(btnReload);

            return buttons;
        }

        private void load() {
            tableModel.setRowCount(0);
            try {
                List<CodigoDescricaoModel> list = service.findAll(type);
                for (CodigoDescricaoModel it : list) {
                    tableModel.addRow(new Object[]{ it.getCodigo(), it.getDescricao() });
                }
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this,
                        "Falha ao carregar " + type.getName() + ":\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void addOrUpdate() {
            String codigo = sanitizeCode(txtCodigo.getText(), type);
            String desc = txtDescricao.getText().trim();

            String err = type.validate(codigo, desc);
            if (err != null) {
                JOptionPane.showMessageDialog(this, err, "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (String.valueOf(tableModel.getValueAt(i, 0)).equalsIgnoreCase(codigo)) {
                    tableModel.setValueAt(codigo, i, 0);
                    tableModel.setValueAt(desc, i, 1);
                    clearInputs();
                    return;
                }
            }

            tableModel.addRow(new Object[]{ codigo, desc });
            clearInputs();
        }

        private void removeSelected() {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                tableModel.removeRow(sel);
            }
        }

        private void saveAll() {
            List<CodigoDescricaoModel> list = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String codigo = String.valueOf(tableModel.getValueAt(i, 0));
                String desc = String.valueOf(tableModel.getValueAt(i, 1));

                String err = type.validate(codigo, desc);
                if (err != null) {
                    JOptionPane.showMessageDialog(this,
                            "Erro na linha " + (i + 1) + ":\n" + err,
                            "Atenção", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                list.add(new CodigoDescricaoModel(codigo, desc));
            }

            try {
                FiscalCatalogService.SaveResult r = service.saveAll(type, list);

                String msg = type.getName() + " salvos com sucesso!";
                if (!r.deletedOk.isEmpty() || !r.deletedFailed.isEmpty()) {
                    msg += "\n\nRemoções:";
                    msg += "\n- OK: " + r.deletedOk.size();
                    msg += "\n- Falharam (em uso / FK): " + r.deletedFailed.size();
                    if (!r.deletedFailed.isEmpty()) {
                        msg += "\n\nAlguns códigos não puderam ser removidos porque estão referenciados no sistema.";
                    }
                }

                JOptionPane.showMessageDialog(this, msg, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                load();

            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this,
                        "Falha ao salvar " + type.getName() + ":\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void clearInputs() {
            txtCodigo.setText("");
            txtDescricao.setText("");
            txtCodigo.requestFocus();
        }

        private String sanitizeCode(String input, CatalogType type) {
            if (input == null) return "";
            String s = input.trim();

            if (type == CatalogType.UNIDADES) {
                return s.toUpperCase().replaceAll("[^A-Z0-9]", "");
            }
            return s.replaceAll("\\D", "");
        }

        private String formatNcm(String code) {
            String raw = code == null ? "" : code.replaceAll("\\D", "");
            if (raw.length() != 8) return code;
            return raw.substring(0, 4) + "." + raw.substring(4, 6) + "." + raw.substring(6);
        }
    }
}
