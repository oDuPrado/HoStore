// src/ui/venda/dialog/VendaFinalizarDialog.java
package ui.venda.dialog;

import controller.VendaController;
import dao.ProdutoDAO;
import util.DB;
import util.AlertUtils;
import util.PDFGenerator;
import model.VendaItemModel;
import model.VendaModel;
import ui.venda.painel.PainelVendas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Dialog de finalização de venda com:
 *  - múltiplas formas de pagamento
 *  - parcelamento (cartão) e juros
 *  - baixa estoque e geração de comprovante/PDF
 */
public class VendaFinalizarDialog extends JDialog {

    /* ======= Dependências ======= */
    private final VendaController controller;
    private final PainelVendas    painelPai;
    private final String          clienteId;
    private final List<VendaItemModel> itens;

    /* ======= Resumo (labels) ======= */
    private final JLabel lblBruto   = new JLabel();
    private final JLabel lblDesc    = new JLabel();
    private final JLabel lblLiquido = new JLabel();

    /* ======= Pagamentos (UI) ======= */
    private JTable            pagamentosTable;
    private DefaultTableModel pagamentosModel;
    private JComboBox<String> cboForma;
    private JFormattedTextField txtValor;

    /* ---- Campos de cartão ---- */
    private final JTextField txtParcelas = new JTextField("1", 4);
    private final JFormattedTextField txtJuros;
    private final JComboBox<String> cboPeriodo =
        new JComboBox<>(new String[]{"15 dias","30 dias"});

    /* ---- Rodapé ---- */
    private final JLabel lblPago  = new JLabel();
    private final JLabel lblTroco = new JLabel();

    public VendaFinalizarDialog(Dialog owner,
                                VendaController controller,
                                String clienteId,
                                PainelVendas painelPai) {
        super(owner, "Finalizar Venda", true);
        this.controller = controller;
        this.painelPai  = painelPai;
        this.clienteId  = clienteId;
        this.itens      = new ArrayList<>(controller.getCarrinho());

        /* === formatter para juros % === */
        NumberFormatter pctFmt = new NumberFormatter(
            NumberFormat.getNumberInstance(new Locale("pt","BR")));
        pctFmt.setValueClass(Double.class);
        pctFmt.setMinimum(0.0); pctFmt.setMaximum(999.0);
        pctFmt.setAllowsInvalid(false);
        txtJuros = new JFormattedTextField(pctFmt);
        txtJuros.setColumns(4);
        txtJuros.setValue(0.0);

        setSize(650, 540);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10,10));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        add(createResumoPanel(),     BorderLayout.NORTH);
        add(createPagamentosPanel(), BorderLayout.CENTER);
        add(createFooterPanel(),     BorderLayout.SOUTH);

        atualizarValores();
    }

    /* ======= Painel Resumo ======= */
    private JPanel createResumoPanel() {
        JPanel p = new JPanel(new GridLayout(0,2,4,4));
        p.setBorder(BorderFactory.createTitledBorder("Resumo da Venda"));

        double bruto=0, descV=0;
        for (VendaItemModel it : itens) {
            double b = it.getQtd()*it.getPreco();
            bruto += b;
            descV += b*it.getDesconto()/100.0;
        }
        double liquido = bruto-descV;
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

        p.add(new JLabel("Itens:"));         p.add(new JLabel(String.valueOf(itens.size())));
        p.add(new JLabel("Valor Bruto:"));   p.add(lblBruto);
        p.add(new JLabel("Desconto:"));      p.add(lblDesc);
        p.add(new JLabel("Total Líquido:")); p.add(lblLiquido);

        lblBruto.setText(nf.format(bruto));
        lblDesc .setText(String.format("%,.2f%%  (%s)", descV/bruto*100, nf.format(descV)));
        lblLiquido.setText(nf.format(liquido));
        return p;
    }

    /* ======= Painel Pagamentos ======= */
    private JPanel createPagamentosPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createTitledBorder("Pagamentos"));

        /* ---- Linha de entrada ---- */
        JPanel entrada = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        cboForma = new JComboBox<>(new String[]{"DINHEIRO","PIX","CARTAO","VALE-PRESENTE","OUTROS"});
        entrada.add(new JLabel("Forma:")); entrada.add(cboForma);

        NumberFormatter currencyFmt =
            new NumberFormatter(NumberFormat.getCurrencyInstance(new Locale("pt","BR")));
        currencyFmt.setValueClass(Double.class);
        currencyFmt.setMinimum(0.0); currencyFmt.setAllowsInvalid(false);

        txtValor = new JFormattedTextField(currencyFmt);
        txtValor.setColumns(10);
        entrada.add(new JLabel("Valor:")); entrada.add(txtValor);

        /* ---- Campos cartão (parcelas / juros / período) ---- */
        entrada.add(new JLabel("Parcelas:"));
        entrada.add(txtParcelas);
        entrada.add(new JLabel("Juros%:"));
        entrada.add(txtJuros);
        entrada.add(new JLabel("Período:"));
        entrada.add(cboPeriodo);

        // inicialmente invisíveis (só cartão)
        txtParcelas.setVisible(false);
        txtJuros.setVisible(false);
        cboPeriodo.setVisible(false);

        /* ---- Listener para alternar visibilidade ---- */
        cboForma.addActionListener(e -> {
            boolean card = "CARTAO".equalsIgnoreCase((String)cboForma.getSelectedItem());
            txtParcelas.setVisible(card);
            txtJuros.setVisible(card);
            cboPeriodo.setVisible(card);
            entrada.revalidate();
            entrada.repaint();
        });

        JButton btnAdd = criarBotao("Adicionar");
        btnAdd.addActionListener(e -> onAddPagamento());
        entrada.add(btnAdd);
        p.add(entrada, BorderLayout.NORTH);

        /* ---- Tabela pagamentos ---- */
        pagamentosModel = new DefaultTableModel(new String[]{"Forma","Valor",""},0){
            @Override public boolean isCellEditable(int r,int c){ return c==2; }
            @Override public Class<?> getColumnClass(int c){ return c==1?Double.class:String.class; }
        };
        pagamentosTable = new JTable(pagamentosModel);
        pagamentosTable.getColumnModel().getColumn(2)
            .setCellRenderer(new ButtonRenderer("🗑"));
        pagamentosTable.getColumnModel().getColumn(2)
            .setCellEditor(new ButtonEditor(evt -> {
                int r = pagamentosTable.getSelectedRow();
                if(r>=0) pagamentosModel.removeRow(r);
                atualizarValores();
            }));
        p.add(new JScrollPane(pagamentosTable), BorderLayout.CENTER);
        return p;
    }

    /* ======= Rodapé ======= */
    private JPanel createFooterPanel() {
        JPanel rod = new JPanel(new BorderLayout());

        lblPago .setFont(lblPago.getFont().deriveFont(Font.BOLD,14f));
        lblTroco.setFont(lblTroco.getFont().deriveFont(Font.BOLD,14f));
        JPanel val = new JPanel(new GridLayout(1,2,4,4));
        val.add(lblPago); val.add(lblTroco);
        rod.add(val, BorderLayout.WEST);

        JButton btnConf = criarBotao("Confirmar");
        btnConf.addActionListener(e -> onConfirm());
        JButton btnCan = criarBotao("Cancelar");
        btnCan.addActionListener(e -> dispose());
        JPanel bts = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,4));
        bts.add(btnCan); bts.add(btnConf);
        rod.add(bts, BorderLayout.EAST);
        return rod;
    }

    /* ======= Ações ======= */
    private void onAddPagamento() {
        Double valor = ((Number)txtValor.getValue()).doubleValue();
        if(valor==null || valor<=0){ AlertUtils.error("Valor inválido!"); return; }
        pagamentosModel.addRow(new Object[]{cboForma.getSelectedItem(), valor, ""});
        txtValor.setValue(null);
        atualizarValores();
    }

    private void atualizarValores() {
        double liquido = NumberFormat.getCurrencyInstance(new Locale("pt","BR"))
                         .parse(lblLiquido.getText(), new java.text.ParsePosition(0))
                         .doubleValue();
        double pago = 0;
        for(int i=0;i<pagamentosModel.getRowCount();i++)
            pago += (Double)pagamentosModel.getValueAt(i,1);

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
        lblPago .setText("Pago:  " + nf.format(pago));
        lblTroco.setText("Troco: " + nf.format(pago-liquido));
    }

    private void onConfirm() {
        try {
            /* --- formaPagamento + parcelas --- */
            String formaFinal;
            int parcelas = 1;
            if (pagamentosModel.getRowCount()==1) {
                formaFinal = (String)pagamentosModel.getValueAt(0,0);
                if ("CARTAO".equalsIgnoreCase(formaFinal)) {
                    try { parcelas = Integer.parseInt(txtParcelas.getText().trim()); }catch(NumberFormatException ignored){}
                }
            } else {
                formaFinal = "MULTI";
                // se houver cartão entre as formas, usa parcelas nele p/ cabeçalho
                for(int i=0;i<pagamentosModel.getRowCount();i++)
                    if("CARTAO".equalsIgnoreCase((String)pagamentosModel.getValueAt(i,0))){
                        try { parcelas = Integer.parseInt(txtParcelas.getText().trim()); }catch(NumberFormatException ignored){}
                        break;
                    }
            }

            /* 1) Grava venda */
            int vendaId = controller.finalizar(clienteId, formaFinal, parcelas);

            /* 2) Grava pagamentos */
            try (Connection c = DB.get()) {
                for (int i = 0; i < pagamentosModel.getRowCount(); i++) {
                    String forma = (String) pagamentosModel.getValueAt(i,0);
                    Double valor = (Double) pagamentosModel.getValueAt(i,1);
                    try (PreparedStatement ps = c.prepareStatement(
                       "INSERT INTO vendas_pagamentos(venda_id,tipo,valor) VALUES (?,?,?)")) {
                        ps.setInt   (1, vendaId);
                        ps.setString(2, forma);
                        ps.setDouble(3, valor);
                        ps.executeUpdate();
                    }
                }
            }

            /* 3) Atualiza UI principal */
            painelPai.carregarVendas(null, null, "Todos", "Todos");
            dispose();

            /* 4) Comprovante */
            double juros = ((Number)txtJuros.getValue()).doubleValue();
            String periodo = (String)cboPeriodo.getSelectedItem();
            new ComprovanteDialog((Dialog)getOwner(), vendaId, itens,
                                  formaFinal, parcelas, juros, periodo,
                                  pagamentosModel).setVisible(true);

        } catch (Exception ex) {
            AlertUtils.error("Erro ao finalizar venda:\n"+ex.getMessage());
        }
    }

    private JButton criarBotao(String txt){
        JButton b = new JButton(txt);
        b.setBackground(new Color(60,63,65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    /* ─────────────────── Comprovante ─────────────────── */
    public static class ComprovanteDialog extends JDialog {
        public ComprovanteDialog(Dialog owner,
                                 int vendaId,
                                 List<VendaItemModel> itens,
                                 String formaFinal,
                                 int parcelas,
                                 double juros,
                                 String periodo,
                                 TableModel pagamentos) {

            super(owner, "Comprovante Venda #" + vendaId, true);
            setSize(480, 650);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8,8));
            ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

            JTextArea ta = new JTextArea();
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
            StringBuilder sb = new StringBuilder();

            /* Cabeçalho Loja */
            sb.append("      HoStore - Sistema de Vendas\n")
              .append("    CNPJ: 12.345.678/0001-99\n")
              .append("    Rua Exemplo, 123 - Centro\n\n");

            sb.append(String.format("Venda #: %-5d  Data: %s\n",
               vendaId,
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))))
              .append("\n");

            /* Itens */
            sb.append(String.format("%-20s %4s %10s %6s %10s\n",
                "Produto","Qtd","V.Unit","Desc%","Total"));
            sb.append("-----------------------------------------------------------\n");

            ProdutoDAO pdao = new ProdutoDAO();
            double totBruto=0, totDesc=0;
            final double[] totLiquido={0};

            for(VendaItemModel it: itens){
                String nome;
                try{ nome = pdao.findById(it.getProdutoId()).getNome();
                }catch(Exception e){ nome = it.getProdutoId(); }
                int    qtd   = it.getQtd();
                double unit  = it.getPreco();
                double descV = unit*qtd*it.getDesconto()/100.0;
                double linha = unit*qtd-descV;

                sb.append(String.format("%-20.20s %4d %10s %5.0f%% %10s\n",
                    nome,qtd,cf.format(unit),it.getDesconto(),cf.format(linha)));

                totBruto  += unit*qtd;
                totDesc   += descV;
                totLiquido[0]+= linha;
            }
            sb.append("-----------------------------------------------------------\n");
            sb.append(String.format("%-36s %10s\n","Total bruto:",   cf.format(totBruto)));
            sb.append(String.format("%-36s %10s\n","Desconto:",      cf.format(totDesc)));
            sb.append(String.format("%-36s %10s\n","Total líquido:", cf.format(totLiquido[0])))
              .append("\n");

            /* Pagamentos */
            sb.append("Pagamentos:\n");
            for(int i=0;i<pagamentos.getRowCount();i++){
                sb.append(String.format("  %-12s %10s\n",
                    pagamentos.getValueAt(i,0),
                    cf.format((Double)pagamentos.getValueAt(i,1))));
            }
            sb.append("\n");

            /* Parcelas cartão (se houver) */
            if(parcelas>1 && pagamentos.getRowCount()>0){
                for(int i=0;i<pagamentos.getRowCount();i++){
                    if("CARTAO".equalsIgnoreCase((String)pagamentos.getValueAt(i,0))){
                        double valorCartao = (Double)pagamentos.getValueAt(i,1);
                        double valorParc   = valorCartao/parcelas;
                        int dias = "15 dias".equals(periodo)?15:30;
                        sb.append("Parcelamento cartão: ")
                          .append(parcelas).append("x de ").append(cf.format(valorParc))
                          .append(juros>0?("  Juros: "+juros+"%\n"):"\n");
                        LocalDate data = LocalDate.now().plusDays(dias);
                        for(int p=1;p<=parcelas;p++){
                            sb.append(String.format("  %2d/%d  %s  %s\n",
                                p,parcelas,
                                data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                cf.format(valorParc)));
                            data = data.plusDays(dias);
                        }
                        sb.append("\n");
                        break;
                    }
                }
            }

            sb.append("Forma de pagamento: ").append(formaFinal).append("\n");
            sb.append("Obrigado pela preferência!\nVolte sempre à HoStore.\n");

            ta.setText(sb.toString());
            add(new JScrollPane(ta), BorderLayout.CENTER);

            /* Botões */
            JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPdf = botao("Imprimir PDF");
            btnPdf.addActionListener(ev->{
                try{
                    VendaModel vm = new VendaModel(
                        String.valueOf(vendaId), null, 0, 0, totLiquido[0], null, parcelas, null);
                    vm.setItens(itens);
                    PDFGenerator.gerarComprovanteVenda(vm, itens);
                }catch(Exception ex){ ex.printStackTrace(); }
            });
            JButton btnClose = botao("Fechar");
            btnClose.addActionListener(ev->dispose());
            b.add(btnPdf); b.add(btnClose);
            add(b,BorderLayout.SOUTH);
        }
        private JButton botao(String t){
            JButton b=new JButton(t);
            b.setBackground(new Color(60,63,65));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false); return b;
        }
    }

    /* ─────────── Button Renderer/Editor (remover linha) ─────────── */
    class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer(String txt){ setText(txt); setOpaque(true); setFocusPainted(false); }
        public Component getTableCellRendererComponent(JTable t,Object v,
            boolean s,boolean f,int r,int c){ return this; }
    }
    class ButtonEditor extends DefaultCellEditor{
        private final JButton btn=new JButton("🗑"); private final java.util.function.Consumer<Void> action;
        private boolean clicked;
        ButtonEditor(java.util.function.Consumer<Void> action){
            super(new JCheckBox()); this.action = action;
            btn.setFocusPainted(false);
            btn.addActionListener(e->{ clicked=true; fireEditingStopped(); });
        }
        public Component getTableCellEditorComponent(JTable tbl,Object v,boolean s,int r,int c){
            clicked=false; return btn; }
        public Object getCellEditorValue(){
            if(clicked && action!=null) action.accept(null); return ""; }
    }
}
