package ui;

import controller.ProdutoEstoqueController;
import model.ProdutoModel;
import ui.dialog.CadastroAcessorioDialog;
import ui.dialog.CadastroBoosterDialog;
import ui.dialog.CadastroDeckDialog;
import ui.dialog.CadastroEtbDialog;
import ui.dialog.CadastroProdutoAlimenticioDialog;
import ui.dialog.ProdutoCadastroDialog;
import ui.dialog.SelecionarCategoriaDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PainelEstoque extends JPanel {

    private final ProdutoEstoqueController ctrl = new ProdutoEstoqueController();
    private final DefaultTableModel model;
    private final JTable tabela;
    private final JTextField tfBusca = new JTextField();

    public PainelEstoque() {
        setLayout(new BorderLayout(10,10));

        /* ---------- TOPO ---------- */
        JPanel topo = new JPanel(new BorderLayout(5,5));
        topo.add(new JLabel("Buscar:"), BorderLayout.WEST);
        topo.add(tfBusca, BorderLayout.CENTER);

        JButton btBuscar  = new JButton("OK");
        JButton btNovo    = new JButton("Novo");
        JButton btEditar  = new JButton("Editar");
        JButton btDelete  = new JButton("Excluir");
        JButton btRefresh = new JButton("⟳");

        btBuscar.addActionListener(e -> listar());
        btRefresh.addActionListener(e -> { tfBusca.setText(""); listar(); });
        btNovo.addActionListener(e -> abrirNovo());
        btEditar.addActionListener(e -> abrirEditar());
        btDelete.addActionListener(e -> deletarSelecionado());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(btRefresh); botoes.add(btNovo);
        botoes.add(btEditar);  botoes.add(btDelete);
        topo.add(botoes, BorderLayout.EAST);
        add(topo, BorderLayout.NORTH);

        /* ---------- TABELA ---------- */
        model  = new DefaultTableModel(new String[]{
            "ID","Nome","Categoria","Qtd","R$ Compra","R$ Venda","Fornecedor"},0) {
                @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tabela = new JTable(model);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        listar();
    }

    private void listar() {
        model.setRowCount(0);
        List<ProdutoModel> data = ctrl.listar(tfBusca.getText());
        for (ProdutoModel p : data) {
            model.addRow(new Object[]{
                p.getId(), p.getNome(), p.getCategoria(),
                p.getQuantidade(), p.getPrecoCompra(),
                p.getPrecoVenda(), p.getFornecedor()
            });
        }
    }

    private void abrirNovo() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        SelecionarCategoriaDialog sel = new SelecionarCategoriaDialog(owner);
        sel.setVisible(true);
        String cat = sel.getCategoriaSelecionada();
        if (cat == null) return;  // cancelou

        switch (cat) {
            case "Booster":
                new CadastroBoosterDialog((JFrame)owner).setVisible(true);
                break;
            case "Deck":
                new CadastroDeckDialog((JFrame)owner).setVisible(true);
                break;
            case "ETB":
                new CadastroEtbDialog((JFrame)owner).setVisible(true);
                break;
            case "Acessório":
                new CadastroAcessorioDialog((JFrame)owner).setVisible(true);
                break;
            case "Alimento":
                new CadastroProdutoAlimenticioDialog((JFrame)owner).setVisible(true);
                break;
            default:
                new ProdutoCadastroDialog((JFrame)owner, null).setVisible(true);
        }

        listar();
    }

    private void abrirEditar() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,"Selecione um item.");
            return;
        }
        String id = (String) model.getValueAt(row,0);
        ProdutoModel p = ctrl.listar("").stream()
                              .filter(prod -> prod.getId().equals(id))
                              .findFirst().orElse(null);
        if (p == null) return;

        // usa genérico para edição de todos os tipos
        ProdutoCadastroDialog dlg = new ProdutoCadastroDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), p);
        dlg.setVisible(true);
        listar();
    }

    private void deletarSelecionado() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,"Selecione um item.");
            return;
        }
        String id = (String) model.getValueAt(row,0);
        if (JOptionPane.showConfirmDialog(this,
                "Excluir o produto "+id+"?") == JOptionPane.OK_OPTION) {
            ctrl.remover(id);
            listar();
        }
    }
}
