package ui.ajustes.dialog;

// Ctrl+F: para localizar rapidamente, busque 'public class NcmDialog'
import model.NcmModel;
import service.NcmService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.awt.Dimension;

/**
 * Diálogo para configuração de NCMs.
 *  
 * Funcionalidades:
 *  - Listagem (findAll → dao.buscarTodos)
 *  - Adição/edição na tabela (addOrUpdate)
 *  - Remoção (removeSelected)
 *  - Salvamento em lote (service.saveAll → dao.sincronizarComApi) e recarga de UI
 *  
 * Dica de navegação: use Ctrl+F por “initComponents” para ir à definição do layout,
 * ou “btnSave.addActionListener” para pular direto ao fluxo de salvamento.
 */
public class NcmDialog extends JDialog {

    // Service singleton para operações de negócio
    private final NcmService service = NcmService.getInstance();

    // Modelo da tabela: colunas "Código" e "Descrição"; células não editáveis
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Código", "Descrição"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    // Máscara para formatação de NCM no formato “####.##.##”
    private MaskFormatter mask;
    private final JFormattedTextField txtCodigo;
    private final JTextField txtDescricao = new JTextField(25);

    public NcmDialog(Window owner) {
        super(owner, "Configuração de NCM", ModalityType.APPLICATION_MODAL);

        // Inicializa a máscara (8 dígitos)
        try {
            mask = new MaskFormatter("####.##.##");
            mask.setPlaceholderCharacter('_');
            mask.setValueContainsLiteralCharacters(false);
        } catch (ParseException e) {
            throw new RuntimeException("Erro ao configurar máscara de NCM", e);
        }
        txtCodigo = new JFormattedTextField(mask);
        txtCodigo.setColumns(10);

        // Constrói a UI e carrega dados iniciais
        initComponents();
        loadNcms();

        // Definir tamanho preferencial maior para melhor usabilidade
        setPreferredSize(new Dimension(900, 500));
        pack();
        setMinimumSize(getSize()); // impede redimensionar abaixo do inicial
        setLocationRelativeTo(owner);
    }

    /**
     * Ctrl+F “initComponents” para ir direto aqui.
     * Monta o layout: inputPanel (add), scroll (tabela) e buttons (remoção, salvar, fechar).
     */
    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- Input Section: adicionar/atualizar NCM ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Novo / Editar NCM",
            TitledBorder.LEFT, TitledBorder.TOP));

        JButton btnAdd = new JButton("Adicionar / Atualizar");
        btnAdd.addActionListener(e -> addOrUpdate());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0, Col 0: Código label
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inputPanel.add(new JLabel("Código (8 dígitos):"), gbc);

        // Row 0, Col 1: Código field (ajustado)
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(txtCodigo, gbc);
        // Restaurar preenchimento horizontal para próximos componentes
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0, Col 2: Descrição label
        gbc.gridx = 2; gbc.weightx = 0;
        inputPanel.add(new JLabel("Descrição:"), gbc);

        // Row 0, Col 3: Descrição field
        gbc.gridx = 3; gbc.weightx = 1.0;
        inputPanel.add(txtDescricao, gbc);

        // Row 0, Col 4: Botão Adicionar/Atualizar
        gbc.gridx = 4; gbc.weightx = 0;
        inputPanel.add(btnAdd, gbc);

        // --- Table Section: exibe lista de NCMs ---
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // Duplo-clique carrega dados de volta aos campos para edição
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int row = table.getSelectedRow();
                    String formatted = (String) tableModel.getValueAt(row, 0);
                    txtCodigo.setText(formatted);
                    txtDescricao.setText((String) tableModel.getValueAt(row, 1));
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Lista de NCMs",
            TitledBorder.LEFT, TitledBorder.TOP));

        // --- Buttons Section: remover, salvar, fechar ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnRemove = new JButton("Remover");
        btnRemove.addActionListener(e -> removeSelected());
        JButton btnSave = new JButton("Salvar");
        // Ctrl+F “btnSave.addActionListener” para localizar lógica de persistência
        btnSave.addActionListener(e -> saveAll());
        JButton btnClose = new JButton("Fechar");
        btnClose.addActionListener(e -> dispose());

        buttons.add(btnRemove);
        buttons.add(btnSave);
        buttons.add(btnClose);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    /**
     * Carrega todos os registros do banco e popula a tabela.
     * Usa service.findAll() que chama dao.buscarTodos().
     */
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
                tableModel.addRow(new Object[]{formatted, n.getDescricao()});
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                "Falha ao carregar NCMs:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Adiciona ou atualiza o NCM na tabela.
     * Valida 8 dígitos e descrição não vazia.
     */
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

        // Atualiza se o código já existir
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0).equals(formatted)) {
                tableModel.setValueAt(desc, i, 1);
                clearInputs();
                return;
            }
        }
        // Caso contrário, adiciona nova linha
        tableModel.addRow(new Object[]{formatted, desc});
        clearInputs();
    }

    /**
     * Remove a linha selecionada da tabela.
     */
    private void removeSelected() {
        int sel = table.getSelectedRow();
        if (sel >= 0) {
            tableModel.removeRow(sel);
        }
    }

    /**
     * Salva todos os NCMs listados: 
     * 1) Constrói List<NcmModel>  
     * 2) Chama service.saveAll(lista)  
     * 3) Recarrega UI com loadNcms()  
     * 4) Exibe feedback ao usuário  
     */
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

    /**
     * Limpa campos de entrada e foca no txtCodigo.
     */
    private void clearInputs() {
        txtCodigo.setValue(null);
        txtDescricao.setText("");
        txtCodigo.requestFocus();
    }
}