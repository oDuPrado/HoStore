package ui.financeiro.painel;

import javax.swing.*;
import javax.swing.table.*;
import com.toedter.calendar.JDateChooser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import dao.*;
import model.*;
import service.ContaPagarService;
import ui.financeiro.dialog.*;

public class PainelFinanceiro extends JPanel {

    /* ────── services/daos ────── */
    private final ContaPagarService contaPagarService = new ContaPagarService();
    private final TituloContaPagarDAO tituloDAO = new TituloContaPagarDAO();
    private final ParcelaContaPagarDAO parcelaDAO = new ParcelaContaPagarDAO();

    /* ────── componentes que precisamos em vários pontos ────── */
    private DefaultTableModel modelPagar;
    private JTable tabelaPagar;
    private JComboBox<String> cbFornecedor, cbStatusPagar;
    private JDateChooser dtInicioPagar, dtFimPagar;

    private final SimpleDateFormat sqlFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00");

    public PainelFinanceiro() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Contas a Pagar", criarPainelContasPagar());
        tabs.addTab("Contas a Receber", criarPainelContasReceber()); // ainda enxuto
        tabs.addTab("Resumo Financeiro", criarPainelResumo());
        add(tabs, BorderLayout.CENTER);
    }

    /* ───────────────────────────────────────────────────────────── Contas a PAGAR ── */
    private JPanel criarPainelContasPagar() {
        JPanel painel = new JPanel(new BorderLayout());

        /* Filtros */
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        filtros.add(new JLabel("Fornecedor:"));
        cbFornecedor = new JComboBox<>();
        cbFornecedor.addItem("Todos");
        try {
            new FornecedorDAO().listar(null, null, null, null)
                               .forEach(f -> cbFornecedor.addItem(f.getNome()));
        } catch (Exception ex) { ex.printStackTrace(); }
        filtros.add(cbFornecedor);

        filtros.add(new JLabel("Status:"));
        cbStatusPagar = new JComboBox<>(new String[]{"Todos","Aberto","Pago","Vencido"});
        filtros.add(cbStatusPagar);

        filtros.add(new JLabel("De:"));
        dtInicioPagar = criarDateChooser();
        filtros.add(dtInicioPagar);

        filtros.add(new JLabel("Até:"));
        dtFimPagar = criarDateChooser();
        filtros.add(dtFimPagar);

        JButton btnFiltrar = new JButton("Filtrar");
        filtros.add(btnFiltrar);

        painel.add(filtros, BorderLayout.NORTH);

        /* Tabela */
        String[] cols = {"ID","Fornecedor","Vencimento","Pagamento",
                         "Nominal","Pago","Em Aberto","Status"};
        modelPagar = new DefaultTableModel(cols,0){ public boolean isCellEditable(int r,int c){return false;}};
        tabelaPagar = new JTable(modelPagar);
        esconderColunaID(tabelaPagar);
        aplicarRenderers(tabelaPagar);
        painel.add(new JScrollPane(tabelaPagar), BorderLayout.CENTER);

        /* Botões */
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,5));
        JButton btNovo   = new JButton("Novo");
        JButton btEditar = new JButton("Editar");
        JButton btPagar  = new JButton("Marcar Pago");
        JButton btDel    = new JButton("Excluir");
        JButton btRefresh= new JButton("Atualizar");
        Arrays.asList(btNovo,btEditar,btPagar,btDel,btRefresh).forEach(acoes::add);
        painel.add(acoes, BorderLayout.SOUTH);

        /* Ações */

        btRefresh.addActionListener(e->carregarTabela());
        btnFiltrar.addActionListener(e->carregarTabela());

        btNovo.addActionListener(e->{
            new ContaPagarDialog((Frame)SwingUtilities.getWindowAncestor(this)).setVisible(true);
            carregarTabela();
        });

        btEditar.addActionListener(e->{
            Integer idSel = idSelecionado();
            if(idSel==null)return;
            try{
                ParcelaContaPagarModel p = parcelaDAO.buscarPorId(idSel);
                new ContaPagarDialog((Frame)SwingUtilities.getWindowAncestor(this), p).setVisible(true);
                carregarTabela();
            }catch(Exception ex){ex.printStackTrace();}
        });

        btPagar.addActionListener(e->{
            Integer idSel = idSelecionado();
            if(idSel==null)return;
            try{
                ParcelaContaPagarModel p = parcelaDAO.buscarPorId(idSel);
                new PagamentoContaPagarDialog(SwingUtilities.getWindowAncestor(this), p).setVisible(true);
                carregarTabela();
            }catch(Exception ex){ex.printStackTrace();}
        });

        btDel.addActionListener(e->{
            Integer idSel = idSelecionado();
            if(idSel==null)return;
            if(JOptionPane.showConfirmDialog(this,"Excluir essa parcela?")==JOptionPane.YES_OPTION){
                try{ parcelaDAO.excluir(idSel); }catch(Exception ex){ex.printStackTrace(); }
                carregarTabela();
            }
        });

        carregarTabela(); // inicial
        return painel;
    }

    /* carrega / filtra */
    private void carregarTabela(){
        modelPagar.setRowCount(0);
        try{
            String fornecedorFiltro = (String)cbFornecedor.getSelectedItem();
            String statusFiltro     = (String)cbStatusPagar.getSelectedItem();
            Date dIni = dtInicioPagar.getDate();
            Date dFim = dtFimPagar.getDate();

            List<TituloContaPagarModel> titulos = tituloDAO.listarTodos();
            for(TituloContaPagarModel t : titulos){
                String fornNome = new FornecedorDAO()
                        .buscarPorId(t.getFornecedorId()).getNome();
                List<ParcelaContaPagarModel> parcelas = parcelaDAO.listarPorTitulo(t.getId());

                for(ParcelaContaPagarModel p: parcelas){
                    /* filtro fornecedor */
                    if(!"Todos".equals(fornecedorFiltro) && !fornNome.equals(fornecedorFiltro)) continue;
                    /* filtro status */
                    if(!"Todos".equals(statusFiltro) && !p.getStatus().equalsIgnoreCase(statusFiltro)) continue;
                    /* filtro data */
                    Date dataComparar = "pago".equalsIgnoreCase(p.getStatus()) && !"Aberto".equalsIgnoreCase(p.getStatus())
                                      ? optDate(p.getDataPagamento()) : optDate(p.getVencimento());
                    if(dataComparar!=null && (dataComparar.before(dIni)||dataComparar.after(dFim))) continue;

                    modelPagar.addRow(new Object[]{
                        p.getId(),
                        fornNome,
                        visFmt.format(optDate(p.getVencimento())),
                        p.getDataPagamento()==null?"":visFmt.format(optDate(p.getDataPagamento())),
                        moneyFmt.format(p.getValorNominal()),
                        moneyFmt.format(p.getValorPago()),
                        moneyFmt.format(p.getValorNominal()-p.getValorPago()),
                        p.getStatus()
                    });
                }
            }
        }catch(Exception ex){ex.printStackTrace();}
    }

    /* helpers */
    private Date optDate(String iso){ try{return iso==null?null:sqlFmt.parse(iso);}catch(Exception e){return null;} }
    private JDateChooser criarDateChooser(){
        JDateChooser dc = new JDateChooser(new Date());
        dc.setPreferredSize(new Dimension(120,25));
        dc.setDateFormatString("dd/MM/yyyy");
        return dc;
    }
    private void esconderColunaID(JTable t){
        t.getColumnModel().getColumn(0).setMinWidth(0);
        t.getColumnModel().getColumn(0).setMaxWidth(0);
        t.getColumnModel().getColumn(0).setPreferredWidth(0);
    }
    private void aplicarRenderers(JTable t){
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        for(int c=2;c<t.getColumnCount();c++)
            t.getColumnModel().getColumn(c).setCellRenderer(center);
    }
    private Integer idSelecionado(){
        int sel = tabelaPagar.getSelectedRow();
        if(sel<0){ JOptionPane.showMessageDialog(this,"Selecione uma linha"); return null; }
        return (Integer)modelPagar.getValueAt(sel,0);
    }

    /* ──────────────────────────────── Contas a Receber (layout ajustado, sem lógica) */
    private JPanel criarPainelContasReceber(){
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("Contas a Receber – em implementação",SwingConstants.CENTER), BorderLayout.CENTER);
        return p;
    }

    private JPanel criarPainelResumo(){
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("<html><h2>Resumo Financeiro em desenvolvimento...</h2></html>",SwingConstants.CENTER),
              BorderLayout.CENTER);
        return p;
    }
}
