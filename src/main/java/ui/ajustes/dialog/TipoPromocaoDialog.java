package ui.ajustes.dialog;

import dao.TipoPromocaoDAO;
import model.TipoPromocaoModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;

public class TipoPromocaoDialog extends JDialog {
    private final JTextField tfNome = new JTextField();
    private final JTextArea taDescricao = new JTextArea(5, 20);
    private final DefaultTableModel modelo;
    private final Integer linhaSelecionada;

    public TipoPromocaoDialog(Integer linha, DefaultTableModel modelo) {
        super((Frame) null, "Tipo de Promoção", true);
        this.modelo = modelo;
        this.linhaSelecionada = linha;

        setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Nome
        gbc.gridy = 0;
        form.add(new JLabel("Nome:"), gbc);
        gbc.gridy++;
        tfNome.setPreferredSize(new Dimension(250, 28));
        form.add(tfNome, gbc);

        // Descrição
        gbc.gridy++;
        form.add(new JLabel("Descrição:"), gbc);
        gbc.gridy++;
        taDescricao.setLineWrap(true);
        taDescricao.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(taDescricao);
        scroll.setPreferredSize(new Dimension(250, 80));
        form.add(scroll, gbc);

        // Botão salvar
        JButton btnSalvar = new JButton("💾 Salvar");
        btnSalvar.setPreferredSize(new Dimension(100, 36));
        btnSalvar.addActionListener(e -> salvar());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botoes.add(btnSalvar);

        add(form, BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);

        if (linha != null) {
            tfNome.setText(modelo.getValueAt(linha, 1).toString());
            taDescricao.setText(modelo.getValueAt(linha, 2).toString());
        }

        pack();
        setLocationRelativeTo(null);
    }

    private void salvar() {
        String nome = tfNome.getText().trim();
        String desc = taDescricao.getText().trim();

        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome.");
            return;
        }

        TipoPromocaoModel tipo = new TipoPromocaoModel();
        tipo.setNome(nome);
        tipo.setDescricao(desc);

        TipoPromocaoDAO dao = new TipoPromocaoDAO();

        try {
            if (linhaSelecionada == null) {
                tipo.setId(UUID.randomUUID().toString());
                dao.inserir(tipo);
            } else {
                tipo.setId(modelo.getValueAt(linhaSelecionada, 0).toString());
                dao.atualizar(tipo);
            }
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage());
        }
    }
}
