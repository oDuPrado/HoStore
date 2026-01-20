package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import dao.ClienteDAO;
import model.ClienteModel;
import service.ContaReceberService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class ContaReceberDialog extends JDialog {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ContaReceberService crSvc = new ContaReceberService();

    private JComboBox<String> cbCliente;

    private final JFormattedTextField ftTotal = new JFormattedTextField(NumberFormat.getNumberInstance());

    private final JSpinner spParcelas = new JSpinner(new SpinnerNumberModel(1, 1, 120, 1));

    private final JSpinner spIntervaloDias = new JSpinner(new SpinnerNumberModel(30, 1, 3650, 1));

    private final JDateChooser dtPrimeiroVenc = new JDateChooser(hoje());

    private final JTextArea taObs = new JTextArea(4, 28);

    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");

    public ContaReceberDialog(Frame owner) {
        super(owner, "Novo Título (Receber)", true);

        UiKit.applyDialogBase(this);
        buildUI();

        pack();
        setMinimumSize(new Dimension(680, 420));
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title("Novo Título a Receber"));
        header.add(UiKit.hint("Crie um título manualmente com parcelas. Por padrão: 1 parcela e vencimento hoje."));
        root.add(header, BorderLayout.NORTH);

        // Card do formulário
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Cliente
        cbCliente = new JComboBox<>();
        cbCliente.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        carregarClientes();

        addField(card, gc, 0, "Cliente", cbCliente);

        // Total
        ftTotal.setColumns(12);
        ftTotal.setValue(0);
        ftTotal.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
        addField(card, gc, 1, "Valor Total (R$)", ftTotal);

        // Parcelas
        ((JComponent) spParcelas.getEditor()).putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        addField(card, gc, 2, "Parcelas", spParcelas);

        // Intervalo
        ((JComponent) spIntervaloDias.getEditor()).putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        addField(card, gc, 3, "Intervalo (dias)", spIntervaloDias);

        // Primeiro venc.
        dtPrimeiroVenc.setDateFormatString("dd/MM/yyyy");
        prepararDateChooser(dtPrimeiroVenc);
        addField(card, gc, 4, "Primeiro vencimento", dtPrimeiroVenc);

        // Observações
        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);
        taObs.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Observações (opcional)...");
        JScrollPane spObs = UiKit.scroll(taObs);
        spObs.setPreferredSize(new Dimension(520, 110));

        GridBagConstraints gl = (GridBagConstraints) gc.clone();
        gl.gridx = 0;
        gl.gridy = 5;
        gl.weightx = 0;
        gl.anchor = GridBagConstraints.WEST;
        gl.fill = GridBagConstraints.NONE;
        card.add(new JLabel("Observações"), gl);

        GridBagConstraints gf = (GridBagConstraints) gc.clone();
        gf.gridx = 1;
        gf.gridy = 5;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.BOTH;
        gf.weighty = 1;
        card.add(spObs, gf);

        root.add(card, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btCancelar = UiKit.ghost("Cancelar");
        JButton btSalvar = UiKit.primary("Salvar");

        btCancelar.addActionListener(e -> dispose());
        btSalvar.addActionListener(e -> onSalvar());

        footer.add(btCancelar);
        footer.add(btSalvar);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void carregarClientes() {
        cbCliente.removeAllItems();
        cbCliente.addItem("Selecione...");
        try {
            clienteDAO.findAll().forEach(c -> cbCliente.addItem(c.getNome()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onSalvar() {
        try {
            if (cbCliente.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(this, "Escolha um cliente.");
                return;
            }

            String cliNome = (String) cbCliente.getSelectedItem();
            ClienteModel cli = clienteDAO.buscarPorNome(cliNome);
            if (cli == null) {
                JOptionPane.showMessageDialog(this, "Cliente inválido.");
                return;
            }

            Number nTotal = (Number) ftTotal.getValue();
            double total = (nTotal == null) ? 0.0 : nTotal.doubleValue();
            if (total <= 0) {
                JOptionPane.showMessageDialog(this, "Informe um valor total maior que zero.");
                return;
            }

            int parcelas = (Integer) spParcelas.getValue();
            int intervalo = (Integer) spIntervaloDias.getValue();

            Date dV = dtPrimeiroVenc.getDate();
            if (dV == null) {
                JOptionPane.showMessageDialog(this, "Informe o primeiro vencimento.");
                return;
            }

            String venc = iso.format(dV);

            crSvc.criarTituloParcelado(
                    cli.getId(),
                    total,
                    parcelas,
                    venc,
                    intervalo,
                    taObs.getText().trim());

            JOptionPane.showMessageDialog(this, "Título criado!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ─────────────────────────── UI Helpers ─────────────────────────── */

    private void addField(JPanel parent, GridBagConstraints base, int row, String label, Component field) {
        GridBagConstraints gl = (GridBagConstraints) base.clone();
        gl.gridx = 0;
        gl.gridy = row;
        gl.weightx = 0;
        gl.anchor = GridBagConstraints.WEST;
        gl.fill = GridBagConstraints.NONE;
        parent.add(new JLabel(label), gl);

        GridBagConstraints gf = (GridBagConstraints) base.clone();
        gf.gridx = 1;
        gf.gridy = row;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.HORIZONTAL;
        parent.add(field, gf);
    }

    private void prepararDateChooser(JDateChooser dc) {
        dc.setPreferredSize(new Dimension(170, 30));

        if (dc.getDateEditor() != null) {
            JComponent editor = dc.getDateEditor().getUiComponent();
            if (editor instanceof JComponent) {
                editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
            }
        }

        JButton calBtn = dc.getCalendarButton();
        if (calBtn != null) {
            calBtn.setText("📅");
            calBtn.setFocusPainted(false);
            calBtn.setMargin(new Insets(2, 8, 2, 8));
            calBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0; font: +1;");
            calBtn.setToolTipText("Selecionar data");
        }
    }

    private static Date hoje() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
