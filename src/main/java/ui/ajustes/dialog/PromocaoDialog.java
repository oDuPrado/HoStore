package ui.ajustes.dialog;

import util.UiKit;
import com.toedter.calendar.JDateChooser;
import dao.PromocaoDAO;
import dao.TipoPromocaoDAO;
import model.PromocaoModel;
import model.TipoDesconto;
import model.AplicaEm;
import model.TipoPromocaoModel;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @TODO: AJUSTAR_PROMOCAO_DIALOG
 *        Dialog para criar/editar uma promoção usando o novo PromocaoModel e
 *        DAO.
 */
public class PromocaoDialog extends JDialog {

    // campos do formulário
    private final JTextField tfNome = new JTextField();
    private final JComboBox<TipoDesconto> cbTipoDesconto = new JComboBox<>();
    private final JTextField tfDesconto = new JTextField();
    private final JComboBox<AplicaEm> cbAplicaEm = new JComboBox<>();
    private final JComboBox<TipoPromocaoModel> cbTipoDef = new JComboBox<>();
    private final JDateChooser dcInicio = new JDateChooser();
    private final JDateChooser dcFim = new JDateChooser();
    private final JTextArea taObs = new JTextArea(3, 20);

    private final boolean isEdicao;
    private final String idOriginal;
    private final PromocaoDAO dao = new PromocaoDAO();
    private final TipoPromocaoDAO tipoDao = new TipoPromocaoDAO();

    public PromocaoDialog(JFrame owner, String promocaoId) throws Exception {
        super(owner, true);
        UiKit.applyDialogBase(this);
        this.isEdicao = promocaoId != null;
        this.idOriginal = isEdicao ? promocaoId : UUID.randomUUID().toString();

        setTitle(isEdicao ? "Editar Promoção" : "Nova Promoção");
        setSize(480, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // === prepara painel de formulário com 2 colunas
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // carrega combos
        for (TipoDesconto td : TipoDesconto.values())
            cbTipoDesconto.addItem(td);
        for (AplicaEm ae : AplicaEm.values())
            cbAplicaEm.addItem(ae);
        List<TipoPromocaoModel> tipos = tipoDao.listarTodos();
        tipos.forEach(cbTipoDef::addItem);

        // ajustes visuais dos campos
        tfNome.setPreferredSize(new Dimension(200, 28));
        tfDesconto.setPreferredSize(new Dimension(120, 28));
        cbTipoDesconto.setPreferredSize(new Dimension(150, 28));
        cbAplicaEm.setPreferredSize(new Dimension(150, 28));
        cbTipoDef.setPreferredSize(new Dimension(150, 28));
        dcInicio.setPreferredSize(new Dimension(140, 28));
        dcFim.setPreferredSize(new Dimension(140, 28));
        taObs.setRows(3);

        // adiciona campos
        GridBagConstraints gbcc = new GridBagConstraints();
        gbcc.insets = new Insets(10, 10, 4, 10);
        gbcc.anchor = GridBagConstraints.WEST;
        gbcc.fill = GridBagConstraints.HORIZONTAL;
        gbcc.weightx = 1.0;

        // Nome
        gbcc.gridy = 0;
        gbcc.gridx = 0;
        form.add(new JLabel("Nome:"), gbcc);
        gbcc.gridx = 1;
        form.add(tfNome, gbcc);

        // Tipo de Desconto
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Tipo de Desconto:"), gbcc);
        gbcc.gridx = 1;
        form.add(cbTipoDesconto, gbcc);

        // Valor do Desconto
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Valor do Desconto:"), gbcc);
        gbcc.gridx = 1;
        form.add(tfDesconto, gbcc);

        // Aplica em
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Aplica em:"), gbcc);
        gbcc.gridx = 1;
        form.add(cbAplicaEm, gbcc);

        // Tipo de Promoção
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Tipo de Promoção:"), gbcc);
        gbcc.gridx = 1;
        form.add(cbTipoDef, gbcc);

        // Data Início
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Data Início:"), gbcc);
        gbcc.gridx = 1;
        form.add(dcInicio, gbcc);

        // Data Fim
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Data Fim:"), gbcc);
        gbcc.gridx = 1;
        form.add(dcFim, gbcc);

        // Observações
        gbcc.gridy++;
        gbcc.gridx = 0;
        form.add(new JLabel("Observações:"), gbcc);
        gbcc.gridx = 1;
        form.add(UiKit.scroll(taObs), gbcc);

        // se for edição, carrega campos do modelo
        if (isEdicao) {
            PromocaoModel p = dao.buscarPorId(idOriginal)
                    .orElseThrow(() -> new IllegalStateException("Promoção não encontrada"));
            tfNome.setText(p.getNome());
            cbTipoDesconto.setSelectedItem(p.getTipoDesconto());
            tfDesconto.setText(p.getDesconto().toString());
            cbAplicaEm.setSelectedItem(p.getAplicaEm());
            // seleciona o TipoPromocaoModel correto
            for (int i = 0; i < cbTipoDef.getItemCount(); i++) {
                if (cbTipoDef.getItemAt(i).getId().equals(p.getTipoId())) {
                    cbTipoDef.setSelectedIndex(i);
                    break;
                }
            }
            dcInicio.setDate(p.getDataInicio());
            dcFim.setDate(p.getDataFim());
            taObs.setText(p.getObservacoes());
        }

        // botão Salvar
        JButton btnSalvar = new JButton("💾 Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                salvar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar:\n" + ex.getMessage());
            }
        });

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnSalvar);

        add(form, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    /**
     * @TODO: AJUSTAR_PROMOCAO_DIALOG
     *        Constrói o PromocaoModel a partir dos campos e chama DAO
     *        inserir/atualizar.
     */
    private void salvar() throws Exception {
        PromocaoModel p = new PromocaoModel();
        p.setId(idOriginal);
        p.setNome(tfNome.getText().trim());
        p.setTipoDesconto((TipoDesconto) cbTipoDesconto.getSelectedItem());
        p.setDesconto(Double.parseDouble(tfDesconto.getText().trim()));
        p.setAplicaEm((AplicaEm) cbAplicaEm.getSelectedItem());
        p.setTipoId(((TipoPromocaoModel) cbTipoDef.getSelectedItem()).getId());
        p.setDataInicio(dcInicio.getDate());
        p.setDataFim(dcFim.getDate());
        p.setObservacoes(taObs.getText().trim());

        if (isEdicao) {
            dao.atualizar(p);
        } else {
            dao.inserir(p);
        }
        dispose();
    }
}
