package ui.financeiro.dialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dao.*;
import model.ParcelaContaReceberModel;

import java.awt.*;
import java.text.*;
import java.util.List;

/**
 * @CR Dialog: exibe e permite baixar parcelas de um título.
 */
public class ParcelasContaReceberDialog extends JDialog {

    private final ParcelaContaReceberDAO parcelaDAO = new ParcelaContaReceberDAO();
    private final PagamentoContaReceberDAO pgDAO    = new PagamentoContaReceberDAO();

    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00");
    private DefaultTableModel model;
    private JTable tabela;
    private final String tituloId;

    public ParcelasContaReceberDialog(Window owner, String tituloId) {
        super(owner, "Parcelas do Título", ModalityType.APPLICATION_MODAL);
        this.tituloId = tituloId;

        setSize(600,400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(criarTabela(), BorderLayout.CENTER);
        add(criarBotoes(), BorderLayout.SOUTH);
        carregarTabela();
    }

    private JScrollPane criarTabela() {
        String[] cols = { "ID", "Parcela", "Vencimento", "Valor", "Pago", "Status" };
        model = new DefaultTableModel(cols,0){
            public boolean isCellEditable(int r,int c){return false;}
        };
        tabela = new JTable(model);
        tabela.getColumnModel().getColumn(0).setMinWidth(0); // esconde ID
        return new JScrollPane(tabela);
    }

    private JPanel criarBotoes() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btPagar = new JButton("Baixar");
        JButton btFechar = new JButton("Fechar");
        btPagar.addActionListener(e -> baixarParcela());
        btFechar.addActionListener(e -> dispose());
        p.add(btFechar); p.add(btPagar);
        return p;
    }

    private void carregarTabela() {
        model.setRowCount(0);
        try {
            List<ParcelaContaReceberModel> list = parcelaDAO.listarPorTitulo(tituloId);
            java.text.SimpleDateFormat vis = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd");
            for (ParcelaContaReceberModel p : list) {
                model.addRow(new Object[]{
                        p.getId(), p.getNumeroParcela(),
                        vis.format(iso.parse(p.getVencimento())),
                        moneyFmt.format(p.getValorNominal()),
                        moneyFmt.format(p.getValorPago()),
                        p.getStatus()
                });
            }
        } catch (Exception ex){ex.printStackTrace();}
    }

    private void baixarParcela() {
        int sel = tabela.getSelectedRow();
        if (sel < 0) { JOptionPane.showMessageDialog(this,"Selecione uma parcela"); return; }
        int parcelaId = (Integer) model.getValueAt(sel,0);
        new PagamentoReceberDialog(this, parcelaId).setVisible(true);
        carregarTabela();
    }
}
