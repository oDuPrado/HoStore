package model;

/**
 * Representa um pagamento de uma parcela de conta a pagar.
 */
public class PagamentoContaPagarModel {
    private Integer id;
    private int parcelaId;
    private String formaPagamento;
    private double valorPago;
    private String dataPagamento;

    public PagamentoContaPagarModel(Integer id, int parcelaId, String formaPagamento,
                                    double valorPago, String dataPagamento) {
        this.id = id;
        this.parcelaId = parcelaId;
        this.formaPagamento = formaPagamento;
        this.valorPago = valorPago;
        this.dataPagamento = dataPagamento;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getParcelaId() { return parcelaId; }
    public void setParcelaId(int parcelaId) { this.parcelaId = parcelaId; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public double getValorPago() { return valorPago; }
    public void setValorPago(double valorPago) { this.valorPago = valorPago; }

    public String getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(String dataPagamento) { this.dataPagamento = dataPagamento; }
}
