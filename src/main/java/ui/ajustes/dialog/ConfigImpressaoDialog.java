package ui.ajustes.dialog;

import dao.PrinterConfigDAO;
import model.PrinterConfigModel;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

/**
 * Diálogo para configurar impressoras térmicas e opções de impressão/PDF.
 * - Seleção de impressora padrão
 * - Número de cópias
 * - Largura do cupom (número de colunas)
 * - Habilitar/Desabilitar abrir PDF após gerar
 */
public class ConfigImpressaoDialog extends JDialog {

    private final JComboBox<String> cbPrinters;
    private final JSpinner spCopies;
    private final JSpinner spReceiptWidth;
    private final JCheckBox chkOpenPdf;

    private final PrinterConfigDAO dao = new PrinterConfigDAO();
    private PrinterConfigModel config;

    public ConfigImpressaoDialog(JFrame owner) {
        super(owner, "Configurações de Impressão", true);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        // Carrega configurações existentes (ou padrão)
        config = dao.loadConfig();

        // ───────────── Painel principal de configurações ─────────────
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;

        // --- Linha 1: Impressora padrão ---
        gbc.gridy = y;
        gbc.gridx = 0;
        form.add(new JLabel("Impressora Térmica Padrão:"), gbc);
        gbc.gridx = 1;
        cbPrinters = new JComboBox<>(getAvailablePrinters());
        cbPrinters.setSelectedItem(config.getDefaultPrinterName());
        form.add(cbPrinters, gbc);

        // --- Linha 2: Número de cópias ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        form.add(new JLabel("Cópias por impressão:"), gbc);
        gbc.gridx = 1;
        spCopies = new JSpinner(new SpinnerNumberModel(config.getCopies(), 1, 10, 1));
        form.add(spCopies, gbc);

        // --- Linha 3: Largura do cupom ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        form.add(new JLabel("Largura do Cupom (colunas):"), gbc);
        gbc.gridx = 1;
        spReceiptWidth = new JSpinner(new SpinnerNumberModel(config.getReceiptWidth(), 20, 80, 1));
        form.add(spReceiptWidth, gbc);

        // --- Linha 4: Abrir PDF após gerar ---
        y++;
        gbc.gridy = y;
        gbc.gridx = 0;
        form.add(new JLabel("Abrir PDF após gerar:"), gbc);
        gbc.gridx = 1;
        chkOpenPdf = new JCheckBox("", config.isOpenPdfAfterGenerate());
        form.add(chkOpenPdf, gbc);

        add(form, BorderLayout.CENTER);

        // ───────────── Painel de botões ─────────────
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSalvar = new JButton("Salvar");
        JButton btnCancelar = new JButton("Cancelar");
        buttons.add(btnCancelar);
        buttons.add(btnSalvar);
        add(buttons, BorderLayout.SOUTH);

        // Ações dos botões
        btnSalvar.addActionListener(e -> onSave());
        btnCancelar.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Retorna um array com os nomes das impressoras térmicas/disponíveis no sistema
     * que podem ser usadas para impressão de texto puro (cupom).
     */
    private String[] getAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        ArrayList<String> list = new ArrayList<>();
        list.add(""); // opção vazia = usar padrão do sistema
        for (PrintService ps : services) {
            list.add(ps.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * Ao clicar em "Salvar": atualiza o modelo, persiste e fecha o diálogo.
     */
    private void onSave() {
        String selectedPrinter = (String) cbPrinters.getSelectedItem();
        int copies = (Integer) spCopies.getValue();
        int receiptWidth = (Integer) spReceiptWidth.getValue();
        boolean openPdf = chkOpenPdf.isSelected();

        config.setDefaultPrinterName(selectedPrinter == null ? "" : selectedPrinter);
        config.setCopies(copies);
        config.setReceiptWidth(receiptWidth);
        config.setOpenPdfAfterGenerate(openPdf);

        dao.saveConfig(config);
        JOptionPane.showMessageDialog(this,
                "Configurações de impressão salvas com sucesso.",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
