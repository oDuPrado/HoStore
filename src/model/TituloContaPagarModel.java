package model;

/**
 * Representa o título de uma conta a pagar (grupo de parcelas).
 */
public class TituloContaPagarModel {
    private String id;
    private String fornecedorId;
    private String codigoSelecao;
    private String dataGeracao;
    private double valorTotal;
    private String status;
    private String observacoes;

    public TituloContaPagarModel(String id, String fornecedorId, String codigoSelecao,
                                 String dataGeracao, double valorTotal,
                                 String status, String observacoes) {
        this.id = id;
        this.fornecedorId = fornecedorId;
        this.codigoSelecao = codigoSelecao;
        this.dataGeracao = dataGeracao;
        this.valorTotal = valorTotal;
        this.status = status;
        this.observacoes = observacoes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(String fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getCodigoSelecao() { return codigoSelecao; }
    public void setCodigoSelecao(String codigoSelecao) { this.codigoSelecao = codigoSelecao; }

    public String getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(String dataGeracao) { this.dataGeracao = dataGeracao; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
