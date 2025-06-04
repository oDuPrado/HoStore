package model;

/**
 * @CR: Cada parcela vinculada a um título de contas a receber.
 */
public class ParcelaContaReceberModel {

    private int    id;                // PK (autoincrement)
    private String tituloId;          // FK -> titulos_contas_receber.id
    private int    numeroParcela;
    private String vencimento;        // ISO yyyy-MM-dd
    private double valorNominal;
    private double valorJuros;
    private double valorAcrescimo;
    private double valorDesconto;
    private double valorPago;
    private String dataPagamento;     // ISO
    private String dataCompensacao;   // ISO
    private boolean pagoComDesconto;
    private String formaPagamento;    // dinheiro, pix, cartão…
    private String status;            // aberto, pago, vencido, cancelado

    // ── Construtor padrão ──
    public ParcelaContaReceberModel() {
        this.status = "aberto";
    }

    // ── Getters/Setters (IDE) ──
    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }
    public String getTituloId()                  { return tituloId; }
    public void setTituloId(String tituloId)     { this.tituloId = tituloId; }
    public int getNumeroParcela()                { return numeroParcela; }
    public void setNumeroParcela(int n)          { this.numeroParcela = n; }
    public String getVencimento()                { return vencimento; }
    public void setVencimento(String v)          { this.vencimento = v; }
    public double getValorNominal()              { return valorNominal; }
    public void setValorNominal(double v)        { this.valorNominal = v; }
    public double getValorJuros()                { return valorJuros; }
    public void setValorJuros(double v)          { this.valorJuros = v; }
    public double getValorAcrescimo()            { return valorAcrescimo; }
    public void setValorAcrescimo(double v)      { this.valorAcrescimo = v; }
    public double getValorDesconto()             { return valorDesconto; }
    public void setValorDesconto(double v)       { this.valorDesconto = v; }
    public double getValorPago()                 { return valorPago; }
    public void setValorPago(double v)           { this.valorPago = v; }
    public String getDataPagamento()             { return dataPagamento; }
    public void setDataPagamento(String d)       { this.dataPagamento = d; }
    public String getDataCompensacao()           { return dataCompensacao; }
    public void setDataCompensacao(String d)     { this.dataCompensacao = d; }
    public boolean isPagoComDesconto()           { return pagoComDesconto; }
    public void setPagoComDesconto(boolean b)    { this.pagoComDesconto = b; }
    public String getFormaPagamento()            { return formaPagamento; }
    public void setFormaPagamento(String f)      { this.formaPagamento = f; }
    public String getStatus()                    { return status; }
    public void setStatus(String s)              { this.status = s; }
}
