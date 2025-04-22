package ui.ajustes.dialog;

import com.toedter.calendar.JDateChooser;
import dao.CadastroGenericoDAO;
import model.TipoPromocaoModel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PromocaoDialog extends JDialog {

    private final JTextField tfNome = new JTextField();
    private final JTextField tfDesconto = new JTextField();
    private final JDateChooser dcInicio = new JDateChooser();
    private final JDateChooser dcFim = new JDateChooser();
    private final JTextArea taObs = new JTextArea(3, 20);
    private final JComboBox<TipoPromocaoModel> cbTipoPromocao = new JComboBox<>();

    private final boolean isEdicao;
    private final String idOriginal;
    private final CadastroGenericoDAO dao;

    public PromocaoDialog(JFrame owner, Map<String, String> dados) {
        super(owner, true);
        this.setTitle(dados == null ? "Nova Promoção" : "Editar Promoção");
        this.setSize(450, 480);
        this.setLocationRelativeTo(owner);
        this.setLayout(new BorderLayout());

        this.isEdicao = dados != null;
        this.idOriginal = isEdicao ? dados.get("id") : UUID.randomUUID().toString();
        this.dao = new CadastroGenericoDAO("promocoes",
            "id", "nome", "desconto", "data_inicio", "data_fim", "observacoes", "tipo_id"
        );

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        carregarTiposPromocao();

        form.add(new JLabel("Nome:"));            form.add(tfNome);
        form.add(new JLabel("Desconto (%):"));    form.add(tfDesconto);
        form.add(new JLabel("Tipo de Promoção:"));form.add(cbTipoPromocao);
        form.add(new JLabel("Data Início:"));     form.add(dcInicio);
        form.add(new JLabel("Data Fim:"));        form.add(dcFim);
        form.add(new JLabel("Observações:"));     form.add(new JScrollPane(taObs));

        if (isEdicao) {
            tfNome.setText(dados.get("nome"));
            tfDesconto.setText(dados.get("desconto"));
            taObs.setText(dados.get("observacoes"));

            try {
                dcInicio.setDate(java.sql.Date.valueOf(dados.get("data_inicio")));
                dcFim.setDate(java.sql.Date.valueOf(dados.get("data_fim")));

                // Corrige seleção do tipo no combo
                String tipoId = dados.get("tipo_id");
                for (int i = 0; i < cbTipoPromocao.getItemCount(); i++) {
                    TipoPromocaoModel tipo = cbTipoPromocao.getItemAt(i);
                    if (tipo.getId().equals(tipoId)) {
                        cbTipoPromocao.setSelectedItem(tipo);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        JButton btnSalvar = new JButton("💾 Salvar");
        btnSalvar.addActionListener(e -> salvar());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnSalvar);

        add(form, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    private void carregarTiposPromocao() {
        try {
            CadastroGenericoDAO tipoDao = new CadastroGenericoDAO("tipos_promocao", "id", "nome", "descricao");
            List<Map<String, String>> tipos = tipoDao.listar();
            cbTipoPromocao.removeAllItems();
            for (Map<String, String> t : tipos) {
                TipoPromocaoModel tipo = new TipoPromocaoModel(
                    t.get("id"),
                    t.get("nome"),
                    t.get("descricao")
                );
                cbTipoPromocao.addItem(tipo);
            }
        } catch (Exception e) {
            cbTipoPromocao.addItem(new TipoPromocaoModel("padrao", "Padrão", ""));
            e.printStackTrace();
        }
    }

    private void salvar() {
        try {
            if (cbTipoPromocao.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Selecione um tipo de promoção.");
                return;
            }

            TipoPromocaoModel tipoSelecionado = (TipoPromocaoModel) cbTipoPromocao.getSelectedItem();

            Map<String, String> dados = new LinkedHashMap<>();
            dados.put("id", idOriginal);
            dados.put("nome", tfNome.getText().trim());
            dados.put("desconto", tfDesconto.getText().trim());
            dados.put("data_inicio", new java.sql.Date(dcInicio.getDate().getTime()).toString());
            dados.put("data_fim", new java.sql.Date(dcFim.getDate().getTime()).toString());
            dados.put("observacoes", taObs.getText().trim());
            dados.put("tipo_id", tipoSelecionado.getId());

            if (isEdicao) {
                dao.atualizar(idOriginal, dados);
            } else {
                dao.inserir(dados);
            }

            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar promoção:\n" + ex.getMessage());
        }
    }
}
