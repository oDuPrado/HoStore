package ui.financeiro.dialog;

import com.toedter.calendar.JDateChooser;
import dao.FornecedorDAO;
import dao.ParcelaContaPagarDAO;
import dao.TituloContaPagarDAO;
import model.FornecedorModel;
import model.ParcelaContaPagarModel;
import service.ContaPagarService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Dialog – cria um novo título a pagar (ou edita parcela) com:
 * • pagamento à vista ou a prazo
 * • planos de parcelamento por D I A S  ou por  I N T E R V A L O S
 * • juros simples / composto opcionais
 * • pré-visualização de valores + datas
 */
public class ContaPagarDialog extends JDialog {

    /* ───── Campos UI ─────────────────────────────────────────────────────── */
    private final JComboBox<String> cbFornecedor      = new JComboBox<>();
    private final JFormattedTextField ftValorTotal    =
        new JFormattedTextField(NumberFormat.getNumberInstance());
    private final JComboBox<String> cbTipoPagamento   =
        new JComboBox<>(new String[]{"À Vista","A Prazo"});
    private final JSpinner spQtdParcelas             =
        new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));

    /* plano de parcelamento */
    private final JComboBox<String> cbPlano           =
        new JComboBox<>(new String[]{"Dias","Intervalos"});
    private final JComboBox<String> cbDias            =
        new JComboBox<>(new String[]{
            "5","7","12","15","25","28","30","45","60",
            "75","90","120","Outros"
        });
    private final JFormattedTextField ftDiasCustom    =
        new JFormattedTextField(NumberFormat.getIntegerInstance());
    private final JComboBox<String> cbIntervalo       =
        new JComboBox<>(new String[]{
            "15,30,45,60",
            "7,15,30,45",
            "15,45,90,120"
        });

    private final JDateChooser dtBase                 = new JDateChooser(new Date());
    private final JTextArea taObs                     = new JTextArea(3,20);

    /* ───── Serviço / estado ──────────────────────────────────────────────── */
    private final ContaPagarService service           = new ContaPagarService();
    private final ParcelaContaPagarModel parcelaEdit;       // null => novo título
    private final SimpleDateFormat fmtBR              = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat fmtSQL             = new SimpleDateFormat("yyyy-MM-dd");

    /* ───── Construtores ──────────────────────────────────────────────────── */
    public ContaPagarDialog(Frame owner){ this(owner,null); }

    public ContaPagarDialog(Frame owner, ParcelaContaPagarModel parcela){
        super(owner, parcela==null?"Nova Conta a Pagar":"Editar Parcela", true);
        this.parcelaEdit = parcela;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
        if (parcelaEdit!=null) preencherEdicao(parcelaEdit);
    }

    /* ───── Construção visual ─────────────────────────────────────────────── */
    private void buildUI(){
        /* fornecedores */
        cbFornecedor.addItem("Selecione...");
        try{
            for (FornecedorModel f: new FornecedorDAO().listar(null,null,null,null))
                cbFornecedor.addItem(f.getNome());
        }catch(Exception ex){ex.printStackTrace();}

        /* listeners reativos */
        cbTipoPagamento.addActionListener(e -> atualizarModoPagamento());
        cbPlano.addActionListener(e -> atualizarPlano());
        cbDias.addActionListener(e -> ftDiasCustom.setEnabled("Outros".equals(cbDias.getSelectedItem())));

        /* layout principal */
        JPanel root = new JPanel();
        GroupLayout gl = new GroupLayout(root);
        root.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lForn  = new JLabel("Fornecedor:");
        JLabel lValor = new JLabel("Valor Total (R$):");
        JLabel lTipo  = new JLabel("Tipo Pagamento:");
        JLabel lQtd   = new JLabel("Qtd. Parcelas:");
        JLabel lPlano = new JLabel("Plano Parcelas:");
        JLabel lBase  = new JLabel("Vencimento Base:");
        JLabel lObs   = new JLabel("Observações:");

        JScrollPane spObs = new JScrollPane(taObs);
        dtBase.setDateFormatString("dd/MM/yyyy");
        ftDiasCustom.setColumns(4); ftDiasCustom.setEnabled(false);

        JButton btnSalvar   = new JButton(parcelaEdit==null?"Salvar":"Atualizar");
        JButton btnCancelar = new JButton("Cancelar");
        btnSalvar.addActionListener(this::onSalvar);
        btnCancelar.addActionListener(e->dispose());

        /* HORIZONTAL */
        gl.setHorizontalGroup(
            gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(gl.createSequentialGroup()
                    .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(lForn).addComponent(lValor).addComponent(lTipo)
                        .addComponent(lQtd).addComponent(lPlano).addComponent(lBase)
                        .addComponent(lObs))
                    .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(cbFornecedor,200,200,200)
                        .addComponent(ftValorTotal,200,200,200)
                        .addComponent(cbTipoPagamento,200,200,200)
                        .addComponent(spQtdParcelas,200,200,200)
                        .addGroup(gl.createSequentialGroup()
                            .addComponent(cbPlano,100,100,100)
                            .addComponent(cbDias,80,80,80)
                            .addComponent(ftDiasCustom,60,60,60)
                            .addComponent(cbIntervalo,150,150,150))
                        .addComponent(dtBase,200,200,200)
                        .addComponent(spObs,200,200,200)))
                .addGroup(GroupLayout.Alignment.TRAILING,
                    gl.createSequentialGroup()
                        .addComponent(btnCancelar)
                        .addComponent(btnSalvar))
        );

        /* VERTICAL */
        gl.setVerticalGroup(
            gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lForn).addComponent(cbFornecedor))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lValor).addComponent(ftValorTotal))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lTipo).addComponent(cbTipoPagamento))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lQtd).addComponent(spQtdParcelas))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lPlano)
                    .addComponent(cbPlano)
                    .addComponent(cbDias)
                    .addComponent(ftDiasCustom)
                    .addComponent(cbIntervalo))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lBase).addComponent(dtBase))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(lObs).addComponent(spObs))
                .addGap(10)
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSalvar).addComponent(btnCancelar))
        );

        setContentPane(root);
        atualizarModoPagamento();      // inicializa estado
        atualizarPlano();              // idem
    }

    /* ───── Handlers de UI ────────────────────────────────────────────────── */
    private void atualizarModoPagamento(){
        boolean aPrazo = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
        spQtdParcelas.setEnabled(aPrazo);
        if (!aPrazo){
            spQtdParcelas.setValue(1);
        }else if ((Integer)spQtdParcelas.getValue()==1){
            spQtdParcelas.setValue(2);
        }
        cbPlano.setEnabled(aPrazo);
        cbDias.setEnabled(aPrazo && "Dias".equals(cbPlano.getSelectedItem()));
        cbIntervalo.setEnabled(aPrazo && "Intervalos".equals(cbPlano.getSelectedItem()));
        ftDiasCustom.setEnabled(false);
    }

    private void atualizarPlano(){
        boolean byDias = "Dias".equals(cbPlano.getSelectedItem());
        cbDias.setEnabled(byDias);
        cbIntervalo.setEnabled(!byDias);
        ftDiasCustom.setEnabled(byDias && "Outros".equals(cbDias.getSelectedItem()));
    }

    /* ───── Salvar / Atualizar ────────────────────────────────────────────── */
    private void onSalvar(ActionEvent evt){
        try{
            /* 1) validações básicas */
            if (cbFornecedor.getSelectedIndex()==0){
                JOptionPane.showMessageDialog(this,"Escolha o fornecedor."); return;
            }
            String fornecedorId =
                new FornecedorDAO().obterIdPorNome((String)cbFornecedor.getSelectedItem());
            double valorTotal =
                ((Number)ftValorTotal.getValue()).doubleValue();

            /* 2) modo de pagamento */
            boolean aPrazo = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
            int numParcelas = aPrazo ? (Integer)spQtdParcelas.getValue() : 1;

            /* 3) gera lista de vencimentos */
            List<Date> datas = calcularVencimentos(numParcelas);

            /* 4) juros? */
            boolean aplicarJuros = false;
            boolean jurosSimples = true;
            double taxa = 0;

            if (numParcelas>=2){
                aplicarJuros =
                    JOptionPane.showConfirmDialog(
                        this,"Aplicar juros por parcela?","Juros",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                if (aplicarJuros){
                    String[] tipos={"Simples","Composto"};
                    String tipoSel =(String)JOptionPane.showInputDialog(
                        this,"Tipo de Juros:","Juros",
                        JOptionPane.PLAIN_MESSAGE,null,tipos,tipos[0]);
                    if (tipoSel==null) return;
                    jurosSimples = "Simples".equals(tipoSel);
                    String taxaStr = JOptionPane.showInputDialog(
                        this,"Taxa de juros (%) por parcela:");
                    if (taxaStr==null) return;
                    taxa = Double.parseDouble(taxaStr.replace(',','.'));
                }
            }

            /* 5) preview sempre */
            boolean preview =
                JOptionPane.showConfirmDialog(
                    this,"Deseja pré-visualizar as parcelas geradas?",
                    "Pré-visualização",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

            /* 6) grava ou atualiza */
            if (parcelaEdit==null){
                if (aplicarJuros){
                    service.gerarTituloComDatas(
                        fornecedorId,valorTotal,datas,
                        jurosSimples,taxa,preview,this,
                        taObs.getText().trim());
                }else{
                    service.gerarTituloComDatas(
                        fornecedorId,valorTotal,datas,
                        false,0,preview,this,
                        taObs.getText().trim());
                }
            }else{
                // edição de parcela existente (mantém regra anterior)
                parcelaEdit.setValorNominal(valorTotal);
                parcelaEdit.setVencimento(
                    new SimpleDateFormat("yyyy-MM-dd")
                        .format(datas.get(0)));
                new ParcelaContaPagarDAO().atualizar(parcelaEdit);
            }

            JOptionPane.showMessageDialog(this,"Dados salvos com sucesso!");
            dispose();
        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,"Erro: "+ex.getMessage(),
                "Erro",JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ───── Geração de vencimentos ───────────────────────────────────────── */
    private List<Date> calcularVencimentos(int parcelas) throws Exception{
        LocalDate base = dtBase.getDate()
            .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        List<Date> out = new ArrayList<>();
        if (parcelas==1){ out.add(dtBase.getDate()); return out; }

        if ("Dias".equals(cbPlano.getSelectedItem())){
            int intervalo;
            if ("Outros".equals(cbDias.getSelectedItem())){
                intervalo = ((Number)ftDiasCustom.getValue()).intValue();
                if (intervalo<=0) throw new Exception("Dias inválidos.");
            }else{
                intervalo=Integer.parseInt((String)cbDias.getSelectedItem());
            }
            for(int p=0;p<parcelas;p++){
                LocalDate d = base.plusDays((long)intervalo*p);
                out.add(java.sql.Date.valueOf(d));
            }
        }else{ // Intervalos
            String[] parts=((String)cbIntervalo.getSelectedItem()).split(",");
            int[] seq = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            for(int p=0;p<parcelas;p++){
                int offset;
                if (p<seq.length) offset = seq[p];
                else{ // continua progressão com último delta
                    int diff = seq[seq.length-1]-seq[seq.length-2];
                    offset = seq[seq.length-1] + diff*(p-(seq.length-1));
                }
                LocalDate d = base.plusDays(offset);
                out.add(java.sql.Date.valueOf(d));
            }
        }
        return out;
    }

    /* ───── Pré-preenchimento na edição ──────────────────────────────────── */
    private void preencherEdicao(ParcelaContaPagarModel p){
        try{
            String fId = new TituloContaPagarDAO()
                .buscarPorId(p.getTituloId()).getFornecedorId();
            String nome = new FornecedorDAO().buscarPorId(fId).getNome();
            cbFornecedor.setSelectedItem(nome);
        }catch(Exception ignored){}

        ftValorTotal.setValue(p.getValorNominal());
        cbTipoPagamento.setSelectedItem("À Vista");
        spQtdParcelas.setEnabled(false);
        dtBase.setDate(java.sql.Date.valueOf(p.getVencimento()));
        taObs.setText(p.getStatus());
    }
}
