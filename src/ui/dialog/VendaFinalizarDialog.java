package ui.dialog;

import controller.VendaController;
import dao.ClienteDAO;
import model.VendaItemModel;
import ui.PainelVendas;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

/** Diálogo de confirmação final da venda – versão completa */
public class VendaFinalizarDialog extends JDialog {

    // ==== Campos principais ====
    private final VendaController controller;
    private final PainelVendas painelPai;
    private final ClienteDAO clienteDAO = new ClienteDAO();

    private final JComboBox<String> formaPG = new JComboBox<>(new String[]{"DINHEIRO", "CARTAO", "PIX"});
    private final JTextField parcelasField  = new JTextField("1", 3);
    private final JTextField taxaMaquininha = new JTextField("2.10", 4);  // % que a maquininha cobra
    private final JLabel lblBruto  = new JLabel();
    private final JLabel lblDesc   = new JLabel();
    private final JLabel lblLiqui  = new JLabel();
    private final JLabel lblRecebe = new JLabel();
    private final JLabel lblPrimeiraParcela = new JLabel();
    private ParcelamentoConfig config = new ParcelamentoConfig();          // default = vista

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public VendaFinalizarDialog(JDialog owner, VendaController controller,
                                String clienteId, PainelVendas painelPai) {
        super(owner, "Finalizar Venda", true);
        this.controller = controller;
        this.painelPai  = painelPai;

        setSize(500, 380);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10,10));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

        String clienteNome = clienteDAO.obterNomePorId(clienteId);

        // ==== Painel Resumo ====
        JPanel resumo = new JPanel(new GridLayout(0,2,4,4));

        double bruto = 0, descVal = 0;
        for (VendaItemModel it : controller.getCarrinho()) {
            double itemBruto = it.getQtd() * it.getPreco();
            bruto += itemBruto;
            descVal += itemBruto * it.getDesconto()/100.0;
        }
        double liquido = bruto - descVal;

        resumo.add(labelNegrito("Cliente:"));         resumo.add(new JLabel(clienteNome));
        resumo.add(labelNegrito("Itens:"));           resumo.add(new JLabel(String.valueOf(controller.getCarrinho().size())));

        resumo.add(labelNegrito("Valor Bruto:"));     resumo.add(lblBruto);
        resumo.add(labelNegrito("Desconto:"));        resumo.add(lblDesc);
        resumo.add(labelNegrito("Total Líquido:"));   resumo.add(lblLiqui);

        lblBruto.setText("R$ "+df.format(bruto));
        lblDesc.setText(df.format((descVal/bruto)*100)+" %  (R$ "+df.format(descVal)+")");
        lblLiqui.setText("R$ "+df.format(liquido));

        // ==== Painel Pagamento ====
        JPanel pg = new JPanel(new FlowLayout(FlowLayout.LEFT,8,2));
        pg.setBorder(BorderFactory.createTitledBorder("Pagamento"));

        pg.add(new JLabel("Forma:"));
        pg.add(formaPG);

        pg.add(new JLabel("Parcelas:"));
        pg.add(parcelasField);

        JButton btnCfgParc = criarBotao("Configurar...");
        pg.add(btnCfgParc);

        pg.add(new JLabel("Taxa % maquina:"));
        pg.add(taxaMaquininha);

        pg.add(new JLabel("1ª Parcela:"));
        pg.add(lblPrimeiraParcela);

        lblPrimeiraParcela.setText(formatData(config.dataPrimeira));

        // listeners
        formaPG.addActionListener(e -> atualizarRecebimento(liquido));
        parcelasField.addFocusListener(selAllOnFocus());
        taxaMaquininha.addFocusListener(selAllOnFocus());

        btnCfgParc.addActionListener(e -> {
            ParcelasDialog dlg = new ParcelasDialog(this, config);
            dlg.setVisible(true);
            if (dlg.isOk()) {
                config = dlg.getConfig();
                parcelasField.setText(String.valueOf(config.parcelas));
                lblPrimeiraParcela.setText(formatData(config.dataPrimeira));
                atualizarRecebimento(liquido);
            }
        });

        // ==== Rodapé Botões ====
        JPanel rodape = new JPanel(new BorderLayout());
        JPanel btns   = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,2));

        lblRecebe.setFont(lblRecebe.getFont().deriveFont(Font.BOLD,14f));
        atualizarRecebimento(liquido);   // seta inicialmente

        JButton confirmar = criarBotao("Confirmar");
        confirmar.addActionListener(e -> confirmarVenda(clienteId, liquido));
        JButton cancelar  = criarBotao("Cancelar");
        cancelar.addActionListener(e -> dispose());

        btns.add(confirmar); btns.add(cancelar);
        rodape.add(lblRecebe, BorderLayout.WEST);
        rodape.add(btns,      BorderLayout.EAST);

        // ==== Monta tudo na tela ====
        add(resumo, BorderLayout.NORTH);
        add(pg,     BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    // ------------------------------- lógica -------------------------------

    private void atualizarRecebimento(double totalLiquido) {
        try {
            int    parcelas = Integer.parseInt(parcelasField.getText().trim());
            double taxa     = Double.parseDouble(taxaMaquininha.getText().replace(",","."));
            double liquido  = totalLiquido;

            double recebido;
            if ("CARTAO".equals(formaPG.getSelectedItem().toString())) {
                // taxa da maquininha (sempre desconta)
                recebido = liquido * (1 - taxa/100.0);

                // juros das parcelas (se >1) – assume que o lojista repassa ao cliente
                if (parcelas > 1) {
                    recebido = recebido / Math.pow(1+config.juros/100.0, parcelas-1);
                }
            } else {
                recebido = liquido; // sem taxas/juros
            }
            lblRecebe.setText("Loja recebe: R$ "+df.format(recebido));
        } catch (Exception ex) {
            lblRecebe.setText("Loja recebe: –");
        }
    }

    private void confirmarVenda(String clienteId, double totalLiquido) {
        try {
            int parcelas = Integer.parseInt(parcelasField.getText().trim());

            int idVenda = controller.finalizar(
                    clienteId,
                    formaPG.getSelectedItem().toString(),
                    parcelas
            );
            AlertUtils.info("Venda #"+idVenda+" concluída com sucesso!");
            painelPai.carregarVendas(null);
            dispose();        // fecha este
            getOwner().dispose(); // fecha o diálogo anterior
        } catch (Exception ex) {
            AlertUtils.error("Erro ao finalizar venda:\n"+ex.getMessage());
        }
    }

    // --------------------------- helpers UI -------------------------------

    private JLabel labelNegrito(String txt){
        JLabel l = new JLabel(txt);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }
    private FocusAdapter selAllOnFocus(){
        return new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                JTextField f = (JTextField)e.getComponent();
                SwingUtilities.invokeLater(f::selectAll);
            }
        };
    }
    private JButton criarBotao(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(60,63,65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
    private String formatData(Date d){
        return new SimpleDateFormat("dd/MM/yyyy").format(d);
    }

    // ---------------------- Classe auxiliar de config ---------------------

    private static class ParcelamentoConfig {
        int    parcelas      = 1;
        double juros         = 0;          // % a.m.
        Date   dataPrimeira  = Date.from(LocalDate.now().plusMonths(1)
                                         .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ---------------------- Diálogo de configuração -----------------------

    private static class ParcelasDialog extends JDialog {
        private boolean ok = false;
        private final JSpinner spParcelas;
        private final JFormattedTextField jurosField;
        private final JSpinner spData;
        private ParcelamentoConfig cfg;

        ParcelasDialog(Window owner, ParcelamentoConfig atual) {
            super(owner, "Configurar Parcelamento", ModalityType.APPLICATION_MODAL);        
            this.cfg = atual;
            setSize(350,200);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8,8));
            ((JComponent)getContentPane()).setBorder(new EmptyBorder(10,10,10,10));

            // controles
            spParcelas = new JSpinner(new SpinnerNumberModel(atual.parcelas,1,36,1));
            jurosField = new JFormattedTextField(new NumberFormatter(new DecimalFormat("#0.00")));
            jurosField.setValue(atual.juros);
            SpinnerDateModel dm = new SpinnerDateModel(atual.dataPrimeira, null, null, java.util.Calendar.DAY_OF_MONTH);
            spData = new JSpinner(dm);
            spData.setEditor(new JSpinner.DateEditor(spData,"dd/MM/yyyy"));

            JPanel form = new JPanel(new GridLayout(0,2,4,4));
            form.add(new JLabel("Parcelas:")); form.add(spParcelas);
            form.add(new JLabel("Juros % a.m.:")); form.add(jurosField);
            form.add(new JLabel("1ª Parcela:")); form.add(spData);
            add(form,BorderLayout.CENTER);

            // botões
            JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okBtn=new JButton("OK"), cancel=new JButton("Cancelar");
            btns.add(okBtn); btns.add(cancel); add(btns,BorderLayout.SOUTH);

            okBtn.addActionListener(e->{
                try{
                    cfg.parcelas = (Integer)spParcelas.getValue();
                    cfg.juros    = Double.parseDouble(jurosField.getText().replace(",","."));
                    cfg.dataPrimeira = (Date) spData.getValue();
                    ok=true;
                    dispose();
                }catch(Exception ex){ JOptionPane.showMessageDialog(this,"Valores inválidos"); }
            });
            cancel.addActionListener(e->dispose());
        }
        boolean isOk(){ return ok; }
        ParcelamentoConfig getConfig(){ return cfg; }
    }
}
