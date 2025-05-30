package model;

/**
 * @CR: Registro granular de cada pagamento efetuado numa parcela.
 */
public class PagamentoContaReceberModel {

    private int    id;              // PK
    private int    parcelaId;       // FK -> parcelas_contas_receber.id
    private String formaPagamento;  // pix, dinheiro, cartão…
    private double valorPago;
    private String dataPagamento;   // ISO

    // Getters/Setters
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public int getParcelaId()                 { return parcelaId; }
    public void setParcelaId(int id)          { this.parcelaId = id; }
    public String getFormaPagamento()         { return formaPagamento; }
    public void setFormaPagamento(String f)   { this.formaPagamento = f; }
    public double getValorPago()              { return valorPago; }
    public void setValorPago(double v)        { this.valorPago = v; }
    public String getDataPagamento()          { return dataPagamento; }
    public void setDataPagamento(String d)    { this.dataPagamento = d; }
}
