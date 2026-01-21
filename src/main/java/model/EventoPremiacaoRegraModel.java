package model;

public class EventoPremiacaoRegraModel {
    private String id;
    private String eventoId;
    private Integer colocacaoInicio;
    private Integer colocacaoFim;
    private String tipo;
    private String produtoId;
    private Integer quantidade;
    private Double valorCredito;
    private String observacoes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }

    public Integer getColocacaoInicio() { return colocacaoInicio; }
    public void setColocacaoInicio(Integer colocacaoInicio) { this.colocacaoInicio = colocacaoInicio; }

    public Integer getColocacaoFim() { return colocacaoFim; }
    public void setColocacaoFim(Integer colocacaoFim) { this.colocacaoFim = colocacaoFim; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Double getValorCredito() { return valorCredito; }
    public void setValorCredito(Double valorCredito) { this.valorCredito = valorCredito; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
