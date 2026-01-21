package ui.ajustes.dialog;

import util.UiKit;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.UUID;

public class CategoriaProdutoDialog extends JDialog {

    // Lista fixa de categorias
    private static final String[] CATEGORIAS = {
        "Carta", "Booster", "Deck", "Acessório",
        "ETB", "Promo", "Comida/Bebida", "Outro"
    };

    // Map para manter checkbox por categoria
    private final Map<String, JCheckBox> checkboxes = new LinkedHashMap<>();

    public CategoriaProdutoDialog(Frame owner) {
        super(owner, "Configurar Categorias de Produtos", true);
        UiKit.applyDialogBase(this);
        initComponents();
        carregarCategoriasDoBanco();
        setSize(400, 300);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Painel principal
        JPanel painel = new JPanel(new BorderLayout(10,10));
        painel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Grid de checkboxes (4 linhas x 2 colunas)
        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 8));
        for (String cat : CATEGORIAS) {
            JCheckBox cb = new JCheckBox(cat);
            checkboxes.put(cat, cb);
            grid.add(cb);
        }
        painel.add(grid, BorderLayout.CENTER);

        // Botões Salvar / Cancelar
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvarCategorias());
        botoes.add(btnCancelar);
        botoes.add(btnSalvar);

        painel.add(botoes, BorderLayout.SOUTH);
        setContentPane(painel);
    }

    private void carregarCategoriasDoBanco() {
        // Busca nomes salvos e marca os checkboxes correspondentes
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT nome FROM categorias_produtos"
             );
             ResultSet rs = ps.executeQuery()
        ) {
            Set<String> marcados = new HashSet<>();
            while (rs.next()) {
                marcados.add(rs.getString("nome"));
            }
            for (String cat : CATEGORIAS) {
                JCheckBox cb = checkboxes.get(cat);
                if (marcados.contains(cat)) {
                    cb.setSelected(true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar categorias:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void salvarCategorias() {
        // Persiste apenas as categorias marcadas
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            // Limpa tudo
            try (PreparedStatement del = c.prepareStatement(
                 "DELETE FROM categorias_produtos"
            )) {
                del.executeUpdate();
            }

            // Insere selecionadas
            try (PreparedStatement ins = c.prepareStatement(
                 "INSERT INTO categorias_produtos(id, nome, descricao) VALUES (?, ?, ?)"
            )) {
                for (String cat : CATEGORIAS) {
                    if (checkboxes.get(cat).isSelected()) {
                        ins.setString(1, UUID.randomUUID().toString());
                        ins.setString(2, cat);
                        ins.setString(3, "");  // sem descrição
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }

            c.commit();
            JOptionPane.showMessageDialog(
                this,
                "Categorias salvas com sucesso!",
                "Sucesso",
                JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao salvar categorias:\n" + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
