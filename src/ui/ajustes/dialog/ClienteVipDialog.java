package ui.ajustes.dialog;

import util.DB;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ClienteVipDialog extends JDialog {

    // Map: cliente_id → checkbox
    private final Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();

    private final JTextField tfBusca = new JTextField();
    private final JPanel panelChecks = new JPanel();

    public ClienteVipDialog(Frame owner) {
        super(owner, "Gerenciar Clientes VIP", true);

        setLayout(new BorderLayout(10,10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 1) Top: busca
        JPanel top = new JPanel(new BorderLayout(5,5));
        top.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
        top.add(new JLabel("Buscar cliente:"), BorderLayout.WEST);
        top.add(tfBusca, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // 2) Center: scroll de checkboxes
        panelChecks.setLayout(new BoxLayout(panelChecks, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(panelChecks);
        sp.setBorder(BorderFactory.createTitledBorder("Marque para VIP"));
        add(sp, BorderLayout.CENTER);

        // 3) Buttons
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JButton btnSalvar    = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvarVIP());
        south.add(btnCancelar);
        south.add(btnSalvar);
        add(south, BorderLayout.SOUTH);

        // Carrega checkboxes e adiciona listener de busca
        carregarClientesECheckboxes();
        tfBusca.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){ filtrar(); }
            public void removeUpdate(DocumentEvent e){ filtrar(); }
            public void changedUpdate(DocumentEvent e){ filtrar(); }
        });

        setSize(400, 500);
        setLocationRelativeTo(owner);
    }

    private void carregarClientesECheckboxes() {
        checkboxes.clear();
        panelChecks.removeAll();
        try (Connection c = DB.get();
             PreparedStatement psAll = c.prepareStatement(
                 "SELECT id, nome FROM clientes ORDER BY nome");
             ResultSet rsAll = psAll.executeQuery();
             PreparedStatement psVip = c.prepareStatement(
                 "SELECT id FROM clientes_vip");
             ResultSet rsVip = psVip.executeQuery()
        ) {
            // quem já é VIP?
            Map<String, Boolean> isVip = new LinkedHashMap<>();
            while (rsVip.next()) {
                isVip.put(rsVip.getString("id"), true);
            }
            // popula checkboxes
            while (rsAll.next()) {
                String id   = rsAll.getString("id");
                String nome = rsAll.getString("nome");
                JCheckBox cb = new JCheckBox(nome);
                cb.setSelected(isVip.getOrDefault(id, false));
                checkboxes.put(id, cb);
                panelChecks.add(cb);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar clientes:\n"+ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
        panelChecks.revalidate();
        panelChecks.repaint();
    }

    private void filtrar() {
        String termo = tfBusca.getText().trim().toLowerCase();
        for (Map.Entry<String,JCheckBox> e : checkboxes.entrySet()) {
            JCheckBox cb = e.getValue();
            cb.setVisible(cb.getText().toLowerCase().contains(termo));
        }
        panelChecks.revalidate();
        panelChecks.repaint();
    }

    private void salvarVIP() {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            // limpa VIPs antigos
            try (PreparedStatement del = c.prepareStatement(
                "DELETE FROM clientes_vip")) {
                del.executeUpdate();
            }
            // prepara insert
            String sql = "INSERT INTO clientes_vip("
                       + "id, nome, cpf, telefone, categoria, criado_em, observacoes"
                       + ") VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement ins = c.prepareStatement(sql)) {
                for (Map.Entry<String,JCheckBox> e : checkboxes.entrySet()) {
                    if (!e.getValue().isSelected()) continue;
                    String clienteId = e.getKey();
                    // traz dados do cliente
                    try (PreparedStatement psC = c.prepareStatement(
                         "SELECT nome, cpf, telefone FROM clientes WHERE id=?")) {
                        psC.setString(1, clienteId);
                        try (ResultSet rs = psC.executeQuery()) {
                            if (!rs.next()) continue;
                            ins.setString(1, clienteId);
                            ins.setString(2, rs.getString("nome"));
                            ins.setString(3, rs.getString("cpf"));
                            ins.setString(4, rs.getString("telefone"));
                            ins.setString(5, ""); // categoria VIP (vazio)
                            ins.setString(6, LocalDate.now().toString());
                            ins.setString(7, ""); // observações
                            ins.addBatch();
                        }
                    }
                }
                ins.executeBatch();
            }
            c.commit();
            JOptionPane.showMessageDialog(this,
                "Clientes VIP atualizados!",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar VIP:\n"+ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
