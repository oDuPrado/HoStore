package model;

/**
 * Representa cada parcela de um título de conta a pagar.
 */
public class ParcelaContaPagarModel {
    private Integer id;
    private String tituloId;
    private int numeroParcela;
    private String vencimento;
    private double valorNominal;
    private double valorJuros;
    private double valorAcrescimo;
    private double valorDesconto;
    private double valorPago;
    private String dataPagamento;
    private String dataCompensacao;
    private boolean pagoComDesconto;
    private String formaPagamento;
    private String status;

    public ParcelaContaPagarModel(Integer id, String tituloId, int numeroParcela, String vencimento,
            double valorNominal, double valorJuros, double valorAcrescimo, double valorDesconto,
            double valorPago, String dataPagamento, String dataCompensacao,
            boolean pagoComDesconto, String formaPagamento, String status) {
        this.id = id;
        this.tituloId = tituloId;
        this.numeroParcela = numeroParcela;
        this.vencimento = vencimento;
        this.valorNominal = valorNominal;
        this.valorJuros = valorJuros;
        this.valorAcrescimo = valorAcrescimo;
        this.valorDesconto = valorDesconto;
        this.valorPago = valorPago;
        this.dataPagamento = dataPagamento;
        this.dataCompensacao = dataCompensacao;
        this.pagoComDesconto = pagoComDesconto;
        this.formaPagamento = formaPagamento;
        this.status = status;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTituloId() { return tituloId; }
    public void setTituloId(String tituloId) { this.tituloId = tituloId; }

    public int getNumeroParcela() { return numeroParcela; }
    public void setNumeroParcela(int numeroParcela) { this.numeroParcela = numeroParcela; }

    public String getVencimento() { return vencimento; }
    public void setVencimento(String vencimento) { this.vencimento = vencimento; }

    public double getValorNominal() { return valorNominal; }
    public void setValorNominal(double valorNominal) { this.valorNominal = valorNominal; }

    public double getValorJuros() { return valorJuros; }
    public void setValorJuros(double valorJuros) { this.valorJuros = valorJuros; }

    public double getValorAcrescimo() { return valorAcrescimo; }
    public void setValorAcrescimo(double valorAcrescimo) { this.valorAcrescimo = valorAcrescimo; }

    public double getValorDesconto() { return valorDesconto; }
    public void setValorDesconto(double valorDesconto) { this.valorDesconto = valorDesconto; }

    public double getValorPago() { return valorPago; }
    public void setValorPago(double valorPago) { this.valorPago = valorPago; }

    public String getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(String dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getDataCompensacao() { return dataCompensacao; }
    public void setDataCompensacao(String dataCompensacao) { this.dataCompensacao = dataCompensacao; }

    public boolean isPagoComDesconto() { return pagoComDesconto; }
    public void setPagoComDesconto(boolean pagoComDesconto) { this.pagoComDesconto = pagoComDesconto; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
