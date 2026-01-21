package ui.ajustes.dialog;

import util.UiKit;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.UUID;

public class IdiomaDialog extends JDialog {

    // Lista fixa de idiomas
    private static final String[] IDIOMAS = {
        "Japonês", "Inglês", "Português", "Alemão", "Espanhol"
    };

    private final Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();

    public IdiomaDialog(Frame owner) {
        super(owner, "Configurar Idiomas Disponíveis", true);
        UiKit.applyDialogBase(this);
        initComponents();
        carregarIdiomasDoBanco();
        setSize(350, 250);
        setLocationRelativeTo(owner);
        // 👇 não chamar setVisible aqui
    }

    private void initComponents() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 8));
        for (String idioma : IDIOMAS) {
            JCheckBox cb = new JCheckBox(idioma);
            checkboxes.put(idioma, cb);
            grid.add(cb);
        }
        painel.add(grid, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvarIdiomas());
        botoes.add(btnCancelar);
        botoes.add(btnSalvar);

        painel.add(botoes, BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private void carregarIdiomasDoBanco() {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT nome FROM linguagens");
             ResultSet rs = ps.executeQuery()) {

            Set<String> marcados = new HashSet<>();
            while (rs.next()) {
                marcados.add(rs.getString("nome"));
            }
            for (String idioma : IDIOMAS) {
                JCheckBox cb = checkboxes.get(idioma);
                cb.setSelected(marcados.contains(idioma));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar idiomas:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void salvarIdiomas() {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            // limpa tudo
            try (PreparedStatement del = c.prepareStatement("DELETE FROM linguagens")) {
                del.executeUpdate();
            }

            // insere só os marcados
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO linguagens(id, nome) VALUES (?, ?)")) {
                for (String idioma : IDIOMAS) {
                    if (checkboxes.get(idioma).isSelected()) {
                        ins.setString(1, UUID.randomUUID().toString());
                        ins.setString(2, idioma);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }

            c.commit();
            JOptionPane.showMessageDialog(
                this,
                "Idiomas salvos com sucesso!",
                "Sucesso",
                JOptionPane.INFORMATION_MESSAGE
            );
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar idiomas:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
