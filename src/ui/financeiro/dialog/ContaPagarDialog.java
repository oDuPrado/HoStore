package ui.financeiro.dialog;

import com.toedter.calendar.JDateChooser;
import dao.FornecedorDAO;
import dao.PlanoContaDAO;
import dao.ParcelaContaPagarDAO;
import dao.TituloContaPagarDAO;
import model.PlanoContaModel;
import model.ParcelaContaPagarModel;
import service.ContaPagarService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Dialog – cria um novo título a pagar (ou edita parcela),
 * agora vinculando internamente a uma Conta do Plano de Contas.
 */
public class ContaPagarDialog extends JDialog {

    /* ───── Campos UI ─────────────────────────────────────────────────────── */
    private final JComboBox<String> cbFornecedor    = new JComboBox<>();
    private final JFormattedTextField ftValorTotal  =
        new JFormattedTextField(NumberFormat.getNumberInstance());
    private final JComboBox<String> cbTipoPagamento =
        new JComboBox<>(new String[]{"À Vista","A Prazo"});
    private final JSpinner spQtdParcelas            =
        new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));

    private final JComboBox<String> cbPlano         =
        new JComboBox<>(new String[]{"Dias","Intervalos"});
    private final JComboBox<String> cbDias          =
        new JComboBox<>(new String[]{
            "5","7","12","15","25","28","30","45","60",
            "75","90","120","Outros"
        });
    private final JFormattedTextField ftDiasCustom  =
        new JFormattedTextField(NumberFormat.getIntegerInstance());
    private final JComboBox<String> cbIntervalo     =
        new JComboBox<>(new String[]{
            "15,30,45,60",
            "7,15,30,45",
            "15,45,90,120"
        });

    private final JDateChooser dtBase               = new JDateChooser(new Date());
    private final JTextArea taObs                   = new JTextArea(3,20);

    private final JButton btnSelecionarConta        = new JButton("Selecionar Conta Contábil…");
    private PlanoContaModel contaSelecionada        = null;

    /* ───── Serviço / estado ──────────────────────────────────────────────── */
    private final ContaPagarService service         = new ContaPagarService();
    private final ParcelaContaPagarModel parcelaEdit;   // null => novo título
    private final SimpleDateFormat fmtBR            = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat fmtSQL           = new SimpleDateFormat("yyyy-MM-dd");

    /* ───── Construtores ──────────────────────────────────────────────────── */
    public ContaPagarDialog(Frame owner) { this(owner, null); }
    public ContaPagarDialog(Frame owner, ParcelaContaPagarModel parcela) {
        super(owner, parcela==null?"Nova Conta a Pagar":"Editar Parcela", true);
        this.parcelaEdit = parcela;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
        if (parcelaEdit != null) preencherEdicao(parcelaEdit);
    }

    /* ───── Construção visual ─────────────────────────────────────────────── */
    private void buildUI() {
        // popula fornecedores
        cbFornecedor.addItem("Selecione...");
        try {
            new FornecedorDAO().listar(null,null,null,null)
                .forEach(f->cbFornecedor.addItem(f.getNome()));
        } catch(Exception ex){ ex.printStackTrace(); }

        // listeners reativos
        cbTipoPagamento.addActionListener(e->atualizarModoPagamento());
        cbPlano.addActionListener(e->atualizarPlano());
        cbDias.addActionListener(e->ftDiasCustom.setEnabled("Outros".equals(cbDias.getSelectedItem())));

        // botão de seleção de conta
        btnSelecionarConta.addActionListener(e-> {
            try {
                PlanoContaDAO planoDao = new PlanoContaDAO();
                List<PlanoContaModel> contas = planoDao.listarTodos();
                PlanoContaModel sel = (PlanoContaModel) JOptionPane.showInputDialog(
                    this,
                    "Escolha a Conta Contábil:",
                    "Plano de Contas",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    contas.toArray(),
                    contaSelecionada
                );
                if (sel != null) {
                    contaSelecionada = sel;
                    btnSelecionarConta.setText(buildHierarchy(sel));
                }
            } catch(SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Erro ao carregar Plano de Contas:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        // layout
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
        ftDiasCustom.setColumns(4);
        ftDiasCustom.setEnabled(false);

        JButton btnSalvar   = new JButton(parcelaEdit==null?"Salvar":"Atualizar");
        JButton btnCancelar = new JButton("Cancelar");
        btnSalvar.addActionListener(this::onSalvar);
        btnCancelar.addActionListener(e->dispose());

        // HORIZONTAL
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
                        .addComponent(btnSelecionarConta, 250,250,250)
                        .addComponent(spObs,200,200,200)))
                .addGroup(GroupLayout.Alignment.TRAILING,
                    gl.createSequentialGroup()
                        .addComponent(btnCancelar)
                        .addComponent(btnSalvar))
        );

        // VERTICAL
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
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSelecionarConta))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(lObs).addComponent(spObs))
                .addGap(10)
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSalvar).addComponent(btnCancelar))
        );

        setContentPane(root);
        atualizarModoPagamento();
        atualizarPlano();
    }

    /* ───── Construção da hierarquia para exibição no botão ─────────── */
    private String buildHierarchy(PlanoContaModel pc) throws SQLException {
        List<String> names = new ArrayList<>();
        PlanoContaDAO dao = new PlanoContaDAO();
        PlanoContaModel current = pc;
        while (current != null) {
            names.add(current.getDescricao());
            current = current.getParentId()!=null
                ? dao.buscarPorId(current.getParentId())
                : null;
        }
        Collections.reverse(names);
        return String.join(" > ", names);
    }

    /* ───── Handlers de UI ────────────────────────────────────────────────── */
    private void atualizarModoPagamento(){
        boolean aPrazo = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
        spQtdParcelas.setEnabled(aPrazo);
        if (!aPrazo) spQtdParcelas.setValue(1);
        cbPlano.setEnabled(aPrazo);
        atualizarPlano();
    }

    private void atualizarPlano(){
        boolean byDias = "Dias".equals(cbPlano.getSelectedItem());
        cbDias.setEnabled(byDias);
        cbIntervalo.setEnabled(!byDias);
        ftDiasCustom.setEnabled(byDias && "Outros".equals(cbDias.getSelectedItem()));
    }

    /* ───── Salvar / Atualizar ────────────────────────────────────────────── */
    private void onSalvar(ActionEvent evt){
        try {
            if (cbFornecedor.getSelectedIndex()==0) {
                JOptionPane.showMessageDialog(this,"Escolha o fornecedor."); return;
            }
            if (contaSelecionada==null) {
                JOptionPane.showMessageDialog(this,"Selecione a Conta Contábil."); return;
            }

            String fornecedorId = new FornecedorDAO()
                .obterIdPorNome((String)cbFornecedor.getSelectedItem());
            String planoContaId = contaSelecionada.getId();
            double valorTotal   = ((Number)ftValorTotal.getValue()).doubleValue();
            boolean aPrazo      = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
            int numParcelas     = aPrazo ? (Integer)spQtdParcelas.getValue() : 1;
            List<Date> datas    = calcularVencimentos(numParcelas);

            // aqui mantemos a lógica de juros e preview...
            boolean aplicarJuros = false;
            boolean jurosSimples = true;
            double taxa = 0;
            if (numParcelas >= 2) {
                aplicarJuros = JOptionPane.showConfirmDialog(
                    this,"Aplicar juros por parcela?","Juros",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                if (aplicarJuros) {
                    String tipoSel = (String) JOptionPane.showInputDialog(
                        this,"Tipo de Juros:","Juros",
                        JOptionPane.PLAIN_MESSAGE,null,
                        new String[]{"Simples","Composto"},"Simples");
                    if (tipoSel==null) return;
                    jurosSimples = "Simples".equals(tipoSel);
                    String taxaStr = JOptionPane.showInputDialog(
                        this,"Taxa de juros (%) por parcela:");
                    if (taxaStr==null) return;
                    taxa = Double.parseDouble(taxaStr.replace(',','.'));
                }
            }
            boolean preview = JOptionPane.showConfirmDialog(
                this,"Deseja pré-visualizar as parcelas geradas?",
                "Pré-visualização",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

            if (parcelaEdit==null) {
                service.gerarTituloComDatas(
                    fornecedorId,
                    planoContaId,
                    valorTotal,
                    datas,
                    jurosSimples,
                    taxa,
                    preview,
                    this,
                    taObs.getText().trim()
                );
            } else {
                parcelaEdit.setValorNominal(valorTotal);
                parcelaEdit.setVencimento(fmtSQL.format(datas.get(0)));
                new ParcelaContaPagarDAO().atualizar(parcelaEdit);
            }

            JOptionPane.showMessageDialog(this,"Dados salvos com sucesso!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,"Erro: "+ex.getMessage(),
                "Erro",JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ───── Geração de vencimentos ───────────────────────────────────────── */
    private List<Date> calcularVencimentos(int parcelas) throws Exception {
        LocalDate base = dtBase.getDate()
            .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        List<Date> out = new ArrayList<>();
        if (parcelas==1) { out.add(dtBase.getDate()); return out; }
        if ("Dias".equals(cbPlano.getSelectedItem())) {
            int intervalo = "Outros".equals(cbDias.getSelectedItem())
                ? ((Number)ftDiasCustom.getValue()).intValue()
                : Integer.parseInt((String)cbDias.getSelectedItem());
            for(int p=0;p<parcelas;p++)
                out.add(java.sql.Date.valueOf(base.plusDays((long)intervalo*p)));
        } else {
            String[] parts = ((String)cbIntervalo.getSelectedItem()).split(",");
            int[] seq = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            for(int p=0;p<parcelas;p++){
                int offset = p<seq.length
                    ? seq[p]
                    : seq[seq.length-1] + (seq[seq.length-1]-seq[seq.length-2])*(p-seq.length+1);
                out.add(java.sql.Date.valueOf(base.plusDays(offset)));
            }
        }
        return out;
    }

    /* ───── Pré-preenchimento na edição ──────────────────────────────────── */
    private void preencherEdicao(ParcelaContaPagarModel p){
        try {
            String fId = new TituloContaPagarDAO()
                .buscarPorId(p.getTituloId()).getFornecedorId();
            String nome = new FornecedorDAO().buscarPorId(fId).getNome();
            cbFornecedor.setSelectedItem(nome);
        } catch(Exception ignored){}
        ftValorTotal.setValue(p.getValorNominal());
        cbTipoPagamento.setSelectedItem("À Vista");
        spQtdParcelas.setEnabled(false);
        dtBase.setDate(java.sql.Date.valueOf(p.getVencimento()));
        taObs.setText(p.getStatus());
    }
}
