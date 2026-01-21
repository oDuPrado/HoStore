package ui.ajustes.dialog;

import util.UiKit;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.UUID;

public class CondicaoDialog extends JDialog {

    // Opções fixas conforme solicitado
    private static final String[] OPCOES = {
        "Neat Mint (NM)",
        "SP (slayed playd)",
        "High Playes (HP)",
        "Damage (D)",
        "Lacrada"
    };

    private final Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();

    public CondicaoDialog(Frame owner) {
        super(owner, "Configurar Condições de Produto", true);
        UiKit.applyDialogBase(this);
        initComponents();
        carregarCondicoesDoBanco();
        setSize(350, 250);
        setLocationRelativeTo(owner);
        // NÃO chame setVisible aqui
    }

    private void initComponents() {
        JPanel painel = new JPanel(new BorderLayout(10,10));
        painel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // grid 3x2 (5 itens)
        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 8));
        for (String opcao : OPCOES) {
            JCheckBox cb = new JCheckBox(opcao);
            checkboxes.put(opcao, cb);
            grid.add(cb);
        }
        painel.add(grid, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvarCondicoes());
        botoes.add(btnCancelar);
        botoes.add(btnSalvar);

        painel.add(botoes, BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private void carregarCondicoesDoBanco() {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT nome FROM condicoes");
             ResultSet rs = ps.executeQuery()) {

            Set<String> marcadas = new HashSet<>();
            while (rs.next()) {
                marcadas.add(rs.getString("nome"));
            }
            for (String opcao : OPCOES) {
                JCheckBox cb = checkboxes.get(opcao);
                cb.setSelected(marcadas.contains(opcao));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar condições:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void salvarCondicoes() {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            // limpa tudo
            try (PreparedStatement del = c.prepareStatement("DELETE FROM condicoes")) {
                del.executeUpdate();
            }

            // insere apenas as marcadas
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO condicoes(id, nome) VALUES (?, ?)")) {
                for (String opcao : OPCOES) {
                    if (checkboxes.get(opcao).isSelected()) {
                        ins.setString(1, UUID.randomUUID().toString());
                        ins.setString(2, opcao);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }

            c.commit();
            JOptionPane.showMessageDialog(
                this,
                "Condições salvas com sucesso!",
                "Sucesso",
                JOptionPane.INFORMATION_MESSAGE
            );
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar condições:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
