// @TODO: VENDA_ESTORNO_DIALOG
// src/ui/venda/dialog/VendaEstornoDialog.java
package ui.venda.dialog;

import util.UiKit;
import dao.ProdutoDAO;
import dao.VendaDevolucaoDAO;
import dao.VendaEstornoFinanceiroDAO;
import model.ProdutoModel;
import model.VendaDevolucaoModel;
import model.VendaItemModel;
import model.VendaPagamentoModel;
import service.EstornoService;
import util.AlertUtils;
import util.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class VendaEstornoDialog extends JDialog {

    /* ------------------------------------------------------------------
     * 1. Campos principais
     * ------------------------------------------------------------------ */
    private final int vendaId;
    private final List<VendaItemModel> itensDaVenda;
    private final List<VendaPagamentoModel> pagamentosDaVenda = new ArrayList<>();

    private JTable itensTable;
    private JTable pagamentosTable;

    private DefaultTableModel itensModel;
    private DefaultTableModel pagamentosModel;

    /* DAOs / Services */
    private final VendaDevolucaoDAO devolucaoDAO     = new VendaDevolucaoDAO();
    private final VendaEstornoFinanceiroDAO finDAO   = new VendaEstornoFinanceiroDAO();
    private final EstornoService estornoService      = new EstornoService();

    /* ------------------------------------------------------------------
     * 2. Construtor
     * ------------------------------------------------------------------ */
    public VendaEstornoDialog(Window owner, int vendaId, List<VendaItemModel> itensOriginais) {
        super(owner, "Estornar Venda #" + vendaId, ModalityType.APPLICATION_MODAL);
        UiKit.applyDialogBase(this);
        this.vendaId     = vendaId;
        this.itensDaVenda = itensOriginais;   // recebidos do VendaDetalhesDialog

        setSize(820, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        /* Abas */
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Itens",       criaPainelItens());
        tabs.addTab("Pagamentos",  criaPainelPagamentos());

        add(tabs, BorderLayout.CENTER);
        add(criaRodape(), BorderLayout.SOUTH);

        carregarPagamentos();
        setVisible(true);
    }

    /* ------------------------------------------------------------------
     * 3. Painel de Itens
     * ------------------------------------------------------------------ */
    private JScrollPane criaPainelItens() {

        itensModel = new DefaultTableModel(
                new String[]{"Produto","Qtd Vendida","Qtd Já Dev.","Qtd a Estornar","V.Unit"}, 0) {
            @Override public boolean isCellEditable(int r,int c){ return c==3; }
            @Override public Class<?> getColumnClass(int c){
                return (c==1||c==2||c==3) ? Integer.class : String.class;
            }
        };

        itensTable = new JTable(itensModel);
        itensTable.setRowHeight(24);

        // Editor inteiro com limite dinâmico
        itensTable.getColumnModel().getColumn(3)
                  .setCellEditor(new SpinnerIntegerEditor());

        preencheLinhasItens();
        return UiKit.scroll(itensTable);
    }

    private void preencheLinhasItens() {

        itensModel.setRowCount(0);

        for (VendaItemModel it : itensDaVenda) {

            String prodId = it.getProdutoId();
            String nome;
            try {
                ProdutoModel p = new ProdutoDAO().findById(prodId);
                nome = (p != null) ? p.getNome() : prodId;
            } catch (Exception e) { nome = prodId; }

            int qtdVend = it.getQtd();
            int qtdDev  = 0;

            try {
                for (VendaDevolucaoModel d : devolucaoDAO.listarPorVenda(vendaId))
                    if (prodId.equals(d.getProdutoId())) qtdDev += d.getQuantidade();
            } catch (Exception ignored){}

            itensModel.addRow(new Object[]{ nome, qtdVend, qtdDev, 0, it.getPreco() });
        }
    }

    /* ------------------------------------------------------------------
     * 4. Painel de Pagamentos
     * ------------------------------------------------------------------ */
    private JScrollPane criaPainelPagamentos() {

        pagamentosModel = new DefaultTableModel(
                new String[]{"Forma","Valor Pago","Valor Já Est.","Valor a Estornar"},0){
            @Override public boolean isCellEditable(int r,int c){ return c==3; }
            @Override public Class<?> getColumnClass(int c){
                return (c==1||c==2||c==3) ? Double.class : String.class;
            }
        };

        pagamentosTable = new JTable(pagamentosModel);
        pagamentosTable.setRowHeight(24);

        // Editor double com limite dinâmico
        pagamentosTable.getColumnModel().getColumn(3)
                       .setCellEditor(new SpinnerDoubleEditor());

        return UiKit.scroll(pagamentosTable);
    }

    private void carregarPagamentos() {

        pagamentosDaVenda.clear();
        pagamentosModel.setRowCount(0);

        try (Connection c = DB.get();
             PreparedStatement ps =
                     c.prepareStatement("SELECT id,tipo,valor FROM vendas_pagamentos WHERE venda_id=?")){

            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                    int    id   = rs.getInt("id");
                    String tipo = rs.getString("tipo");
                    double val  = rs.getDouble("valor");
                    double jaEst = finDAO.obterTotalEstornadoPorPagamento(id);

                    VendaPagamentoModel vp = new VendaPagamentoModel(id,tipo,val,jaEst);
                    pagamentosDaVenda.add(vp);

                    pagamentosModel.addRow(new Object[]{ tipo, val, jaEst, 0.0 });
                }
            }
        }catch(Exception ex){
            AlertUtils.error("Erro ao carregar pagamentos:\n"+ex.getMessage());
        }
    }

    /* ------------------------------------------------------------------
     * 5. Rodapé
     * ------------------------------------------------------------------ */
    private JPanel criaRodape(){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,4));
        JButton cancelar = new JButton("Cancelar");
        JButton confirmar= new JButton("Confirmar Estorno");

        cancelar.addActionListener(e->dispose());
        confirmar.addActionListener(e->confirmarEstorno());

        p.add(cancelar);
        p.add(confirmar);
        return p;
    }

    /* ------------------------------------------------------------------
     * 6. Confirmação do Estorno
     * ------------------------------------------------------------------ */
    private void confirmarEstorno(){

        /* --- ler seleção de itens --- */
        Map<String,Integer> itensEstorno = new HashMap<>();
        for (int r=0;r<itensModel.getRowCount();r++){
            int qtd = (Integer) itensModel.getValueAt(r,3);
            if (qtd>0){
                String prodId = itensDaVenda.get(r).getProdutoId();
                itensEstorno.put(prodId,qtd);
            }
        }

        /* --- ler seleção de pagamentos --- */
        Map<Integer,Double> pagEstorno = new HashMap<>();
        for (int r=0;r<pagamentosModel.getRowCount();r++){
            double v = (Double) pagamentosModel.getValueAt(r,3);
            if (v>0){
                int pagId = pagamentosDaVenda.get(r).getId();
                pagEstorno.put(pagId,v);
            }
        }

        if (itensEstorno.isEmpty() && pagEstorno.isEmpty()){
            AlertUtils.error("Selecione quantidade ou valor a estornar.");
            return;
        }

        if (JOptionPane.showConfirmDialog(
                this,"Confirmar estorno?","Confirmação",
                JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;

        /* --- transação --- */
        try (Connection c = DB.get()){
            c.setAutoCommit(false);

            // Itens
            for (Map.Entry<String,Integer> e: itensEstorno.entrySet()){
                String prod = e.getKey();
                int qtd     = e.getValue();

                // achar item original
                for (VendaItemModel it: itensDaVenda)
                    if (prod.equals(it.getProdutoId())){
                        estornoService.estornarItem(c, vendaId, it, qtd);
                        break;
                    }
            }

            // Pagamentos
            for (Map.Entry<Integer,Double> e: pagEstorno.entrySet()){
                finDAO.inserirEstorno(e.getKey(), vendaId, e.getValue(),
                        LocalDate.now(),"Estorno manual");
            }

            c.commit();
            AlertUtils.info("Estorno registrado com sucesso.");
            dispose();

        }catch(Exception ex){
            ex.printStackTrace();
            AlertUtils.error("Falha ao estornar:\n"+ex.getMessage());
        }
    }

    /* ==================================================================
     * 7. Editors de célula para JSpinner
     * ================================================================== */
    /** Editor de inteiros */
    private static class SpinnerIntegerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();

        @Override public Object getCellEditorValue(){ return spinner.getValue(); }

        @Override
        public Component getTableCellEditorComponent(JTable tbl,Object val,
                                                     boolean sel,int row,int col){
            int max = (Integer)tbl.getValueAt(row,1) - (Integer)tbl.getValueAt(row,2);
            spinner.setModel(new SpinnerNumberModel(0,0,Math.max(0,max),1));
            return spinner;
        }
    }

    /** Editor de doubles */
    private static class SpinnerDoubleEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();

        @Override public Object getCellEditorValue(){ return spinner.getValue(); }

        @Override
        public Component getTableCellEditorComponent(JTable tbl,Object val,
                                                     boolean sel,int row,int col){
            double pago = (Double)tbl.getValueAt(row,1);
            double jaE  = (Double)tbl.getValueAt(row,2);
            double max  = Math.max(0.0, pago - jaE);
            spinner.setModel(new SpinnerNumberModel(0.0,0.0,max,0.01));
            return spinner;
        }
    }
}
