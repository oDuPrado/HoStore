package ui.ajustes.painel;

import util.UiKit;
import dao.FornecedorDAO;
import model.FornecedorModel;
import ui.ajustes.dialog.FornecedorDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.util.List;

public class FornecedorPainel extends JPanel {

    private final JTextField tfFiltroNome = new JTextField(15);
    private final JFormattedTextField ftfFiltroCnpj;
    private final JComboBox<String> cbFiltroTipo   = new JComboBox<>(new String[]{"", "À Vista","A Prazo"});
    private final JComboBox<Integer> cbFiltroPrazo = new JComboBox<>(new Integer[]{null,7,15,30,45,60,90,180});

    private final JButton btnBuscar = new JButton("🔍 Buscar");
    private final JButton btnAdd    = new JButton("➕ Adicionar");
    private final JButton btnEdit   = new JButton("✏️ Editar");
    private final JButton btnDel    = new JButton("🗑️ Excluir");

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final FornecedorDAO dao = new FornecedorDAO();

    public FornecedorPainel() {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(8,8));

        // cria máscara de CNPJ sem propagar exceção
        JFormattedTextField tmp;
        try {
            MaskFormatter mf = new MaskFormatter("##.###.###/####-##");
            mf.setPlaceholderCharacter('_');
            tmp = new JFormattedTextField(mf);
        } catch (Exception e) {
            tmp = new JFormattedTextField();
        }
        ftfFiltroCnpj = tmp;

        // painel de filtros
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
        filtro.add(new JLabel("Nome:"));  filtro.add(tfFiltroNome);
        filtro.add(new JLabel("CNPJ:"));  filtro.add(ftfFiltroCnpj);
        filtro.add(new JLabel("Tipo:"));  filtro.add(cbFiltroTipo);
        filtro.add(new JLabel("Prazo:")); filtro.add(cbFiltroPrazo);
        filtro.add(btnBuscar);
        btnBuscar.addActionListener(e -> carregarTabela());

        // tabela de resultados
        modelo = new DefaultTableModel(
            new String[]{"Nome","CNPJ","Tipo","Prazo"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // botões CRUD
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
        botoes.add(btnAdd); botoes.add(btnEdit); botoes.add(btnDel);
        btnAdd .addActionListener(e -> onAdicionar());
        btnEdit.addActionListener(e -> onEditar());
        btnDel .addActionListener(e -> onRemover());

        add(filtro, BorderLayout.NORTH);
        add(UiKit.scroll(tabela), BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);

        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<FornecedorModel> lista = dao.listar(
                tfFiltroNome.getText().trim(),
                ftfFiltroCnpj.getText().trim(),
                (String) cbFiltroTipo.getSelectedItem(),
                (Integer) cbFiltroPrazo.getSelectedItem()
            );
            for (FornecedorModel f : lista) {
                modelo.addRow(new Object[]{
                    f.getId(),
                    f.getNome(),
                    f.getCnpj(),
                    f.getPagamentoTipo(),
                    f.getPrazo()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao buscar fornecedores.");
        }
    }

    private void onAdicionar() {
        Window w = SwingUtilities.getWindowAncestor(this);
        Frame owner = (w instanceof Frame ? (Frame)w : null);
        new FornecedorDialog(owner, null).setVisible(true);
        carregarTabela();
    }

    private void onEditar() {
        int row = tabela.getSelectedRow();
        if (row == -1) return;
        String id = modelo.getValueAt(row, 0).toString();
        try {
            FornecedorModel f = dao.buscarPorId(id);
            if (f != null) {
                Window w = SwingUtilities.getWindowAncestor(this);
                Frame owner = (w instanceof Frame ? (Frame)w : null);
                new FornecedorDialog(owner, f).setVisible(true);
                carregarTabela();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao editar.");
        }
    }

    private void onRemover() {
        int row = tabela.getSelectedRow();
        if (row == -1) return;
        String id = modelo.getValueAt(row, 0).toString();
        if (JOptionPane.showConfirmDialog(
                this,
                "Excluir este fornecedor?",
                "Confirmação",
                JOptionPane.YES_NO_OPTION
            ) == JOptionPane.YES_OPTION) {
            try {
                dao.excluir(id);
                carregarTabela();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao excluir.");
            }
        }
    }

    public void abrir() {
        // este painel pode ser chamado de um diálogo de owner qualquer
        Window w = SwingUtilities.getWindowAncestor(this);
        Frame  owner = (w instanceof Frame ? (Frame)w : null);

        JDialog d = new JDialog(owner, "Fornecedores", true);
        d.setContentPane(this);
        d.setSize(700, 500);
        d.setLocationRelativeTo(owner);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setVisible(true);
    }
}
