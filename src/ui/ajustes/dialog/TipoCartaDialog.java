package ui.ajustes.dialog;

import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.UUID;

public class TipoCartaDialog extends JDialog {

    private static final String[] OPCOES = {
        "Regular",
        "Foil",
        "Reverse",
        "Foil Reverse",
        "Full Art",
        "Secreta",
        "Promo"
    };

    private final Map<String,JCheckBox> checkboxes = new LinkedHashMap<>();

    public TipoCartaDialog(Frame owner) {
        super(owner, "Configurar Tipos de Carta", true);
        initComponents();
        carregarDoBanco();
        setSize(350, 300);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel painel = new JPanel(new BorderLayout(10,10));
        painel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        for (String opc : OPCOES) {
            JCheckBox cb = new JCheckBox(opc);
            checkboxes.put(opc, cb);
            grid.add(cb);
        }
        painel.add(grid, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvar());
        botoes.add(btnCancelar);
        botoes.add(btnSalvar);

        painel.add(botoes, BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private void carregarDoBanco() {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT nome FROM tipo_cartas");
             ResultSet rs = ps.executeQuery()) {
            Set<String> marc = new HashSet<>();
            while (rs.next()) marc.add(rs.getString("nome"));
            for (String opc : OPCOES) {
                checkboxes.get(opc).setSelected(marc.contains(opc));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar tipos:\n"+ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void salvar() {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement("DELETE FROM tipo_cartas")) {
                del.executeUpdate();
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO tipo_cartas(id,nome) VALUES(?,?)")) {
                for (String opc : OPCOES) {
                    if (checkboxes.get(opc).isSelected()) {
                        ins.setString(1, UUID.randomUUID().toString());
                        ins.setString(2, opc);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
            c.commit();
            JOptionPane.showMessageDialog(this, "Tipos salvos!");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar:\n"+ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
