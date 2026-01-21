package ui.eventos.dialog;

import dao.JogoDAO;
import model.EventoModel;
import model.JogoModel;
import service.EventoService;
import service.SessaoService;
import util.UiKit;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public class EventoFormDialog extends JDialog {

    private final EventoService service = new EventoService();
    private final JComboBox<JogoModel> cbJogo = new JComboBox<>();
    private final JTextField txtNome = new JTextField(30);
    private final JFormattedTextField txtInicio = criarCampoData();
    private final JFormattedTextField txtFim = criarCampoData();
    private final JComboBox<String> cbStatus = new JComboBox<>(
            new String[] { "rascunho", "aberto", "fechado", "cancelado" });
    private final JFormattedTextField txtTaxa = criarCampoMoeda();
    private final JSpinner spLimite = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
    private final JTextArea taRegras = new JTextArea(6, 40);
    private final JTextArea taObs = new JTextArea(4, 40);

    private EventoModel evento;
    private boolean salvo;

    public EventoFormDialog(Window owner, String eventoId) {
        super(owner, eventoId == null ? "Novo Evento" : "Editar Evento", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);
        setSize(760, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        carregarJogos();
        carregarEvento(eventoId);

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildFormCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);
    }

    public boolean isSalvo() {
        return salvo;
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Evento / Liga"));
        left.add(UiKit.hint("Campos basicos do evento, taxa e regras"));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JPanel buildFormCard() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        addRow(form, g, y++, "Nome:", txtNome, true);
        addRow2(form, g, y++, "Jogo:", cbJogo, "Status:", cbStatus);
        addRow2(form, g, y++, "Inicio:", txtInicio, "Fim:", txtFim);
        addRow2(form, g, y++, "Taxa:", txtTaxa, "Limite (0=ilimitado):", spLimite);

        g.gridy = y++;
        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Regras:"), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 3;
        g.fill = GridBagConstraints.BOTH;
        taRegras.setLineWrap(true);
        taRegras.setWrapStyleWord(true);
        form.add(UiKit.scroll(taRegras), g);

        g.gridy = y;
        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Obs:"), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 3;
        g.fill = GridBagConstraints.BOTH;
        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);
        form.add(UiKit.scroll(taObs), g);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomCard() {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnSalvar = UiKit.primary("Salvar");
        btnSalvar.addActionListener(e -> salvar());

        right.add(btnCancelar);
        right.add(btnSalvar);
        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void carregarJogos() {
        try {
            List<JogoModel> jogos = new JogoDAO().listarTodos();
            DefaultComboBoxModel<JogoModel> model = new DefaultComboBoxModel<>();
            for (JogoModel j : jogos) {
                model.addElement(j);
            }
            cbJogo.setModel(model);
            cbJogo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof JogoModel j) {
                        setText(j.getNome());
                    }
                    return this;
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarEvento(String eventoId) {
        if (eventoId == null) {
            evento = new EventoModel();
            return;
        }
        evento = service.buscarEvento(eventoId);
        if (evento == null) {
            evento = new EventoModel();
            return;
        }

        txtNome.setText(evento.getNome());
        txtInicio.setText(formatarDataBr(evento.getDataInicio()));
        txtFim.setText(formatarDataBr(evento.getDataFim()));
        cbStatus.setSelectedItem(evento.getStatus());
        txtTaxa.setValue(BigDecimal.valueOf(evento.getTaxaInscricao()));
        spLimite.setValue(evento.getLimiteParticipantes() == null ? 0 : evento.getLimiteParticipantes());
        taRegras.setText(evento.getRegrasTexto());
        taObs.setText(evento.getObservacoes());

        for (int i = 0; i < cbJogo.getItemCount(); i++) {
            JogoModel j = cbJogo.getItemAt(i);
            if (j != null && j.getId().equals(evento.getJogoId())) {
                cbJogo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void salvar() {
        try {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) {
                throw new RuntimeException("Informe o nome do evento.");
            }

            evento.setNome(nome);
            JogoModel j = (JogoModel) cbJogo.getSelectedItem();
            evento.setJogoId(j != null ? j.getId() : null);
            evento.setStatus((String) cbStatus.getSelectedItem());
            evento.setDataInicio(parseDataIso(txtInicio));
            evento.setDataFim(parseDataIso(txtFim));
            evento.setTaxaInscricao(parseMoney(txtTaxa));
            int limite = ((Number) spLimite.getValue()).intValue();
            evento.setLimiteParticipantes(limite <= 0 ? null : limite);
            evento.setRegrasTexto(taRegras.getText());
            evento.setObservacoes(taObs.getText());

            String usuario = (SessaoService.get() != null) ? SessaoService.get().getNome() : "sistema";
            service.salvarEvento(evento, usuario);
            salvo = true;
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field, boolean full) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = full ? 3 : 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, g);
    }

    private void addRow2(JPanel p, GridBagConstraints g, int row,
            String l1, JComponent f1,
            String l2, JComponent f2) {
        g.gridy = row;

        g.gridx = 0;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l1), g);

        g.gridx = 1;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f1, g);

        g.gridx = 2;
        g.weightx = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        p.add(new JLabel(l2), g);

        g.gridx = 3;
        g.weightx = 1;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(f2, g);
    }

    private static JFormattedTextField criarCampoData() {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField();
            f.setFormatterFactory(new DefaultFormatterFactory(mask));
            f.setColumns(10);
            return f;
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    private static JFormattedTextField criarCampoMoeda() {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        NumberFormatter nf = new NumberFormatter(fmt);
        nf.setValueClass(BigDecimal.class);
        nf.setAllowsInvalid(true);
        nf.setMinimum(BigDecimal.ZERO);
        JFormattedTextField f = new JFormattedTextField(nf);
        f.setColumns(10);
        f.setValue(BigDecimal.ZERO);
        return f;
    }

    private static String parseDataIso(JFormattedTextField field) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        if (raw.isBlank() || raw.contains("_")) {
            return null;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                .withResolverStyle(ResolverStyle.STRICT);
        LocalDate d = LocalDate.parse(raw, fmt);
        return d.toString();
    }

    private static String formatarDataBr(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            LocalDate d = LocalDate.parse(iso);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        } catch (Exception e) {
            return iso;
        }
    }

    private static double parseMoney(JFormattedTextField field) {
        Object v = field.getValue();
        if (v instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        String raw = field.getText();
        if (raw == null || raw.isBlank()) {
            return 0.0;
        }
        try {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            Number n = fmt.parse(raw);
            return n.doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Valor monetario invalido.");
        }
    }
}
