package ui.ajustes.dialog;

import model.PlanoContaModel;
import service.PlanoContaService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.UUID;

public class PlanoContaDialog extends JDialog {

    private final JTextField tfDescricao = new JTextField(20);
    private final JComboBox<String> cbTipo = new JComboBox<>(
        new String[]{ "Ativo", "Passivo", "Receita", "Custo" }
    );
    // substituímos o combo de pai por um botão + campo texto
    private final JTextField tfPai    = new JTextField(20);
    private final JButton btnBuscarPai = new JButton("...");
    private PlanoContaModel contaPaiSelecionada;
    private final JTextArea taObs = new JTextArea(3,20);

    private final PlanoContaService service = new PlanoContaService();
    private final PlanoContaModel editing;

    public PlanoContaDialog(Window owner, PlanoContaModel p) {
        super(owner, p==null? "Novo Plano de Contas" : "Editar Plano de Contas",
              ModalityType.APPLICATION_MODAL);
        this.editing = p;
        initComponents();
        pack();
        setLocationRelativeTo(owner);

        // se for editar, carrega dados e pai
        if (editing != null) preencherCampos(editing);
    }

    private void initComponents() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Detalhes da Conta",
            TitledBorder.LEFT, TitledBorder.TOP
        ));
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lDesc   = new JLabel("Descrição:");
        JLabel lTipo   = new JLabel("Tipo:");
        JLabel lParent = new JLabel("Conta Pai:");
        JLabel lObs    = new JLabel("Observações:");

        // configurações
        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);
        JScrollPane spObs = new JScrollPane(taObs,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        tfPai.setEditable(false);

        // ação do botão "..."
        btnBuscarPai.addActionListener(e -> {
            SelecionarPlanoContaDialog dlg = 
                new SelecionarPlanoContaDialog(SwingUtilities.getWindowAncestor(this));
            dlg.setVisible(true);
            PlanoContaModel sel = dlg.getSelecionado();
            if (sel != null) {
                contaPaiSelecionada = sel;
                tfPai.setText(sel.getDescricao());
            }
        });

        // botões
        JButton btnSalvar   = new JButton(editing==null? "Salvar" : "Atualizar");
        JButton btnCancelar = new JButton("Cancelar");
        btnSalvar.addActionListener(this::onSave);
        btnCancelar.addActionListener(e -> dispose());

        // layout horizontal
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(lDesc)
                    .addComponent(lTipo)
                    .addComponent(lParent)
                    .addComponent(lObs))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(tfDescricao)
                    .addComponent(cbTipo)
                    .addGroup(gl.createSequentialGroup()
                        .addComponent(tfPai)
                        .addComponent(btnBuscarPai))
                    .addComponent(spObs)))
            .addGroup(GroupLayout.Alignment.TRAILING, gl.createSequentialGroup()
                .addComponent(btnCancelar)
                .addComponent(btnSalvar))
        );

        // layout vertical
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lDesc).addComponent(tfDescricao))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lTipo).addComponent(cbTipo))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lParent).addComponent(tfPai).addComponent(btnBuscarPai))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lObs).addComponent(spObs))
            .addGap(10)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(btnCancelar)
                .addComponent(btnSalvar))
        );

        setContentPane(panel);
    }

    private void onSave(ActionEvent e) {
        String descricao = tfDescricao.getText().trim();
        if (descricao.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Descrição não pode ficar em branco.",
                "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // id e codigo serão gerados no Service
        PlanoContaModel m;
        if (editing == null) {
            m = new PlanoContaModel(
                null, // id
                null, // codigo
                descricao,
                (String) cbTipo.getSelectedItem(),
                contaPaiSelecionada != null ? contaPaiSelecionada.getId() : null,
                taObs.getText().trim()
            );
        } else {
            m = editing;
            m.setDescricao(descricao);
            m.setTipo((String) cbTipo.getSelectedItem());
            m.setParentId(contaPaiSelecionada != null ? contaPaiSelecionada.getId() : null);
            m.setObservacoes(taObs.getText().trim());
        }

        try {
            service.salvar(m);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao salvar: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void preencherCampos(PlanoContaModel p) {
        tfDescricao.setText(p.getDescricao());
        cbTipo.setSelectedItem(p.getTipo());
        taObs.setText(p.getObservacoes());
        // carrega pai, se existir
        if (p.getParentId() != null) {
            try {
                PlanoContaModel pai = service.buscarPorId(p.getParentId());
                if (pai != null) {
                    contaPaiSelecionada = pai;
                    tfPai.setText(pai.getDescricao());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
