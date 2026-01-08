package ui.ajustes.dialog;

import model.NcmModel;
import service.NcmService;

import util.UiKit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para configuração de NCMs.
 */
public class NcmDialog extends JDialog {

    private final NcmService service = NcmService.getInstance();

    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[] { "Código", "Descrição" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private MaskFormatter mask;
    private final JFormattedTextField txtCodigo;
    private final JTextField txtDescricao = new JTextField(28);

    public NcmDialog(Window owner) {
        super(owner, "Configuração de NCM", ModalityType.APPLICATION_MODAL);

        // máscara NCM “####.##.##”
        try {
            mask = new MaskFormatter("####.##.##");
            mask.setPlaceholderCharacter('_');
            mask.setValueContainsLiteralCharacters(false);
        } catch (ParseException e) {
            throw new RuntimeException("Erro ao configurar máscara de NCM", e);
        }

        txtCodigo = new JFormattedTextField(mask);
        txtCodigo.setColumns(10);

        UiKit.applyDialogBase(this);

        initComponents();
        loadNcms();

        setSize(980, 560);
        setMinimumSize(new Dimension(860, 520));
        setLocationRelativeTo(owner);

        SwingUtilities.invokeLater(txtCodigo::requestFocusInWindow);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // ===================== TOP CARD =====================
        JPanel topCard = UiKit.card();
        topCard.setLayout(new BorderLayout(10, 8));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topLeft.setOpaque(false);
        topLeft.add(UiKit.title("NCM"));
        topLeft.add(UiKit
                .hint("Duplo clique em uma linha para editar. Ctrl+F pra focar no nome (se quiser, eu adiciono)."));
        topCard.add(topLeft, BorderLayout.WEST);

        add(topCard, BorderLayout.NORTH);

        // ===================== FORM CARD =====================
        JPanel formCard = UiKit.card();
        formCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblCodigo = new JLabel("Código (8 dígitos)");
        JLabel lblDesc = new JLabel("Descrição");

        Dimension fieldH = new Dimension(0, 30);
        txtCodigo.setPreferredSize(new Dimension(140, 30));
        txtDescricao.setPreferredSize(new Dimension(0, 30));
        txtDescricao.setMinimumSize(new Dimension(220, 30));

        JButton btnAdd = UiKit.primary("➕ Adicionar / Atualizar");
        btnAdd.addActionListener(e -> addOrUpdate());

        // linha 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formCard.add(lblCodigo, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        formCard.add(txtCodigo, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formCard.add(lblDesc, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        formCard.add(txtDescricao, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formCard.add(btnAdd, gbc);

        add(formCard, BorderLayout.PAGE_START);

        // ===================== TABLE CARD =====================
        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        tableHeader.add(UiKit.title("Lista de NCMs"), BorderLayout.WEST);
        tableHeader.add(UiKit.hint("Selecione uma linha e use Remover. Duplo clique para puxar pro formulário."),
                BorderLayout.EAST);

        tableCard.add(tableHeader, BorderLayout.NORTH);

        UiKit.tableDefaults(table);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        // Zebra + alinhamento
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        table.getColumnModel().getColumn(0).setCellRenderer(zebra);
        table.getColumnModel().getColumn(1).setCellRenderer(zebra);

        // Larguras
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(700);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int row = table.getSelectedRow();
                    String formatted = (String) tableModel.getValueAt(row, 0);
                    txtCodigo.setText(formatted);
                    txtDescricao.setText((String) tableModel.getValueAt(row, 1));
                    SwingUtilities.invokeLater(txtDescricao::requestFocusInWindow);
                    txtDescricao.selectAll();
                }
            }
        });

        tableCard.add(UiKit.scroll(table), BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        // ===================== FOOTER CARD =====================
        JPanel bottomCard = UiKit.card();
        bottomCard.setLayout(new BorderLayout(10, 10));

        JLabel footerHint = UiKit.hint("Salvar grava tudo em lote.");
        bottomCard.add(footerHint, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JButton btnRemove = UiKit.ghost("🗑 Remover");
        btnRemove.addActionListener(e -> removeSelected());

        JButton btnSave = UiKit.primary("💾 Salvar");
        btnSave.addActionListener(e -> saveAll());

        JButton btnClose = UiKit.ghost("Fechar");
        btnClose.addActionListener(e -> dispose());

        buttons.add(btnRemove);
        buttons.add(btnClose);
        buttons.add(btnSave);

        bottomCard.add(buttons, BorderLayout.EAST);

        add(bottomCard, BorderLayout.SOUTH);

        // Enter no form -> add/update
        getRootPane().setDefaultButton(btnAdd);
    }

    private void loadNcms() {
        tableModel.setRowCount(0);
        try {
            List<NcmModel> list = service.findAll();
            for (NcmModel n : list) {
                String raw = n.getCodigo();
                String formatted;
                try {
                    formatted = mask.valueToString(raw);
                } catch (ParseException ex) {
                    formatted = raw;
                }
                tableModel.addRow(new Object[] { formatted, n.getDescricao() });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "Falha ao carregar NCMs:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addOrUpdate() {
        String raw = txtCodigo.getText().replaceAll("\\D", "");
        String desc = txtDescricao.getText().trim();

        if (raw.length() != 8 || desc.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Informe 8 dígitos e descrição.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String formatted;
        try {
            formatted = mask.valueToString(raw);
        } catch (ParseException e) {
            formatted = raw;
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(formatted)) {
                tableModel.setValueAt(desc, i, 1);
                clearInputs();
                return;
            }
        }

        tableModel.addRow(new Object[] { formatted, desc });
        clearInputs();
    }

    private void removeSelected() {
        int sel = table.getSelectedRow();
        if (sel >= 0) {
            tableModel.removeRow(sel);
        }
    }

    private void saveAll() {
        List<NcmModel> list = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String formatted = (String) tableModel.getValueAt(i, 0);
            String raw = formatted.replaceAll("\\D", "");
            String desc = (String) tableModel.getValueAt(i, 1);
            list.add(new NcmModel(raw, desc));
        }

        try {
            service.saveAll(list);
            JOptionPane.showMessageDialog(this,
                    "NCMs salvos com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadNcms();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "Falha ao salvar NCMs:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearInputs() {
        txtCodigo.setValue(null);
        txtDescricao.setText("");
        txtCodigo.requestFocusInWindow();
    }
}
