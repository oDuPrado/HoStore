package model;

public class PromocaoAplicacaoModel {
    private String id;
    private String promocaoId;
    private Integer vendaId;
    private Integer vendaItemId;
    private String produtoId;
    private String clienteId;
    private Integer qtd;
    private double precoOriginal;
    private double descontoValor;
    private double precoFinal;
    private String descontoTipo;
    private String dataAplicacao;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPromocaoId() { return promocaoId; }
    public void setPromocaoId(String promocaoId) { this.promocaoId = promocaoId; }
    public Integer getVendaId() { return vendaId; }
    public void setVendaId(Integer vendaId) { this.vendaId = vendaId; }
    public Integer getVendaItemId() { return vendaItemId; }
    public void setVendaItemId(Integer vendaItemId) { this.vendaItemId = vendaItemId; }
    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public Integer getQtd() { return qtd; }
    public void setQtd(Integer qtd) { this.qtd = qtd; }
    public double getPrecoOriginal() { return precoOriginal; }
    public void setPrecoOriginal(double precoOriginal) { this.precoOriginal = precoOriginal; }
    public double getDescontoValor() { return descontoValor; }
    public void setDescontoValor(double descontoValor) { this.descontoValor = descontoValor; }
    public double getPrecoFinal() { return precoFinal; }
    public void setPrecoFinal(double precoFinal) { this.precoFinal = precoFinal; }
    public String getDescontoTipo() { return descontoTipo; }
    public void setDescontoTipo(String descontoTipo) { this.descontoTipo = descontoTipo; }
    public String getDataAplicacao() { return dataAplicacao; }
    public void setDataAplicacao(String dataAplicacao) { this.dataAplicacao = dataAplicacao; }
}
