package ui.dialog;

import model.ClienteModel;
import service.ClienteService;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class ClienteCadastroDialog extends JDialog {

    private JTextField txtNome;
    private JFormattedTextField txtTelefone;
    private JFormattedTextField txtCPF;
    private JFormattedTextField txtDataNasc;
    private JComboBox<String> comboTipo;
    private JTextField txtEndereco;
    private JTextField txtCidade;
    private JComboBox<String> comboEstado;
    private JTextArea txtObservacoes;

    private boolean salvou;
    private ClienteModel clienteModel;

    /* ----------------- CONSTRUTOR ----------------- */
    public ClienteCadastroDialog(Window parent, ClienteModel existente) {
        super(parent, "Cadastro de Cliente", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(500, 520);
        setLocationRelativeTo(parent);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        setBackground(new Color(0, 0, 0, 0));
        JPanel content = new JPanel(null);
        content.setBackground(Color.WHITE);
        add(content);

        /* ----- cria/ carrega model ----- */
        clienteModel = (existente == null) ? novoModelo() : existente;

        /* ----- layout helpers ----- */
        int h = 30, y = 20, dy = 10;
        int lx = 20, cx = 150, lw = 120, cw = 300;

        /* Nome */
        content.add(label("Nome:", lx, y, lw, h));
        txtNome = new JTextField(clienteModel.getNome());
        txtNome.setBounds(cx, y, cw, h);
        content.add(txtNome);
        y += h + dy;

        /* Telefone */
        content.add(label("Telefone:", lx, y, lw, h));
        txtTelefone = campoMascara("(##) #####-####", clienteModel.getTelefone());
        txtTelefone.setBounds(cx, y, cw, h);
        content.add(txtTelefone);
        y += h + dy;

        /* CPF */
        content.add(label("CPF:", lx, y, lw, h));
        txtCPF = campoMascara("###.###.###-##", clienteModel.getCpf());
        txtCPF.setBounds(cx, y, cw, h);
        content.add(txtCPF);
        y += h + dy;

        /* Data nasc */
        content.add(label("Data Nasc.:", lx, y, lw, h));
        txtDataNasc = campoMascara("##/##/####", clienteModel.getDataNasc());
        txtDataNasc.setBounds(cx, y, cw, h);
        content.add(txtDataNasc);
        y += h + dy;

        /* Tipo */
        content.add(label("Tipo:", lx, y, lw, h));
        comboTipo = new JComboBox<>(new String[] { "Colecionador", "Jogador", "Ambos" });
        comboTipo.setSelectedItem(clienteModel.getTipo());
        comboTipo.setBounds(cx, y, cw, h);
        content.add(comboTipo);
        y += h + dy;

        /* Endereço, cidade, estado */
        content.add(label("Endereço:", lx, y, lw, h));
        txtEndereco = new JTextField(clienteModel.getEndereco());
        txtEndereco.setBounds(cx, y, cw, h);
        content.add(txtEndereco);
        y += h + dy;

        content.add(label("Cidade:", lx, y, lw, h));
        txtCidade = new JTextField(clienteModel.getCidade());
        txtCidade.setBounds(cx, y, cw, h);
        content.add(txtCidade);
        y += h + dy;

        content.add(label("Estado:", lx, y, lw, h));
        comboEstado = new JComboBox<>(UF);
        comboEstado.setSelectedItem(clienteModel.getEstado());
        comboEstado.setBounds(cx, y, cw, h);
        content.add(comboEstado);
        y += h + dy;

        /* Observações */
        content.add(label("Observações:", lx, y, lw, h));
        txtObservacoes = new JTextArea(clienteModel.getObservacoes());
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        JScrollPane scObs = new JScrollPane(txtObservacoes);
        scObs.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scObs.setBounds(cx, y, cw, 60);
        content.add(scObs);

        /* --------- Botões --------- */
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBounds(0, 420, 500, 50);
        rodape.setBackground(Color.WHITE);

        rodape.add(btnDark("Salvar", e -> salvar(false)));
        rodape.add(btnDark("Salvar + Novo", e -> salvar(true)));
        rodape.add(btnDark("Cancelar", e -> {
            salvou = false;
            dispose();
        }));

        content.add(rodape);

        /* Fade‑in simples */
        Timer t = new Timer(30, null);
        final float[] op = { 0f };
        t.addActionListener(e -> {
            op[0] += 0.08f;
            if (op[0] >= 1) {
                op[0] = 1;
                t.stop();
            }
            setOpacity(op[0]);
        });
        setOpacity(0);
        t.start();
    }

    /* ----------------- SALVAR ----------------- */
    private void salvar(boolean novoApos) {
        String nome = txtNome.getText().trim();
        String cpf = txtCPF.getText().replaceAll("\\D", ""); // só dígitos

        /* --- validações --- */
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório!");
            return;
        }
        if (cpf.length() != 11) {
            JOptionPane.showMessageDialog(this, "CPF inválido.");
            return;
        }
        /* duplicidade (outro cliente com mesmo CPF) */
        List<ClienteModel> todos = ClienteService.loadAll();
        boolean dup = todos.stream()
                .anyMatch(c -> c.getCpf().replaceAll("\\D", "").equals(cpf) &&
                        !c.getId().equals(clienteModel.getId()));
        if (dup) {
            JOptionPane.showMessageDialog(this, "CPF já cadastrado para outro cliente.");
            return;
        }

        /* --- grava no model --- */
        clienteModel.setNome(nome);
        clienteModel.setCpf(txtCPF.getText());
        clienteModel.setTelefone(txtTelefone.getText());
        clienteModel.setDataNasc(txtDataNasc.getText());
        clienteModel.setTipo((String) comboTipo.getSelectedItem());
        clienteModel.setEndereco(txtEndereco.getText());
        clienteModel.setCidade(txtCidade.getText());
        clienteModel.setEstado((String) comboEstado.getSelectedItem());
        clienteModel.setObservacoes(txtObservacoes.getText());
        clienteModel.setAlteradoEm("2025-04-16 10:10");
        clienteModel.setAlteradoPor("admin");

        ClienteService.upsert(clienteModel);
        salvou = true;

        if (novoApos) {
            dispose();
            new ClienteCadastroDialog(
                    SwingUtilities.getWindowAncestor(getParent()), null).setVisible(true);
        } else {
            dispose();
        }
    }

    /* ----------------- HELPERS UI ----------------- */
    private JLabel label(String t, int x, int y, int w, int h) {
        JLabel l = new JLabel(t);
        l.setBounds(x, y, w, h);
        return l;
    }

    private JFormattedTextField campoMascara(String mask, String val) {
        try {
            MaskFormatter mf = new MaskFormatter(mask);
            mf.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mf);
            f.setText(val == null ? "" : val);
            return f;
        } catch (ParseException e) {
            return new JFormattedTextField(val);
        }
    }

    private JButton btnDark(String txt, java.awt.event.ActionListener ac) {
        JButton b = new JButton(txt);
        b.addActionListener(ac);
        b.setFocusPainted(false);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(new Color(80, 83, 85));
            }

            public void mouseExited(MouseEvent e) {
                b.setBackground(new Color(60, 63, 65));
            }
        });
        return b;
    }

    private ClienteModel novoModelo() {
        ClienteModel m = new ClienteModel();
        m.setId("C-" + UUID.randomUUID().toString().substring(0, 5));
        m.setCriadoEm("2025-04-16 10:00");
        m.setCriadoPor("admin");
        return m;
    }

    public boolean isSalvou() {
        return salvou;
    }

    public ClienteModel getClienteModel() {
        return clienteModel;
    }

    private static final String[] UF = {
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SE", "SP", "TO"
    };
}
