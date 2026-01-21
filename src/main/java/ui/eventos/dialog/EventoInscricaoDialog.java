package ui.eventos.dialog;

import dao.ClienteDAO;
import model.ClienteModel;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EventoInscricaoDialog extends JDialog {

    private final JComboBox<ClienteModel> cbCliente = new JComboBox<>();
    private final JTextField txtNomeAvulso = new JTextField(24);
    private boolean confirmado;

    public EventoInscricaoDialog(Window owner) {
        super(owner, "Inscrever Participante", ModalityType.APPLICATION_MODAL);

        UiKit.applyDialogBase(this);
        setSize(520, 260);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        carregarClientes();

        add(buildTopCard(), BorderLayout.NORTH);
        add(buildFormCard(), BorderLayout.CENTER);
        add(buildBottomCard(), BorderLayout.SOUTH);
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public ClienteModel getClienteSelecionado() {
        Object val = cbCliente.getSelectedItem();
        return (val instanceof ClienteModel) ? (ClienteModel) val : null;
    }

    public String getNomeAvulso() {
        return txtNomeAvulso.getText().trim();
    }

    private JPanel buildTopCard() {
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));
        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("Inscricao"));
        left.add(UiKit.hint("Selecione cliente cadastrado ou preencha nome avulso"));
        top.add(left, BorderLayout.WEST);
        return top;
    }

    private JPanel buildFormCard() {
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        card.add(new JLabel("Cliente:"), g);

        g.gridx = 1;
        g.weightx = 1;
        cbCliente.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ClienteModel c) {
                    setText(c.getNome());
                }
                return this;
            }
        });
        card.add(cbCliente, g);

        g.gridx = 0;
        g.gridy = 1;
        g.weightx = 0;
        card.add(new JLabel("Nome Avulso:"), g);

        g.gridx = 1;
        g.weightx = 1;
        card.add(txtNomeAvulso, g);

        return card;
    }

    private JPanel buildBottomCard() {
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnOk = UiKit.primary("Inscrever");
        btnOk.addActionListener(e -> confirmar());

        right.add(btnCancelar);
        right.add(btnOk);
        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    private void confirmar() {
        confirmado = true;
        dispose();
    }

    private void carregarClientes() {
        try {
            List<ClienteModel> clientes = new ClienteDAO().findAll();
            DefaultComboBoxModel<ClienteModel> model = new DefaultComboBoxModel<>();
            for (ClienteModel c : clientes) {
                model.addElement(c);
            }
            cbCliente.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
