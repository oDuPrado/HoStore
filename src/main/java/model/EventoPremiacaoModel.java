package model;

public class EventoPremiacaoModel {
    private String id;
    private String eventoId;
    private String participanteId;
    private String tipo;
    private String produtoId;
    private Integer quantidade;
    private Double valorCredito;
    private String status;
    private Integer movimentacaoEstoqueId;
    private String creditoMovId;
    private String entregueEm;
    private String entreguePor;
    private String estornadoEm;
    private String estornadoPor;
    private String observacoes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }

    public String getParticipanteId() { return participanteId; }
    public void setParticipanteId(String participanteId) { this.participanteId = participanteId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Double getValorCredito() { return valorCredito; }
    public void setValorCredito(Double valorCredito) { this.valorCredito = valorCredito; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMovimentacaoEstoqueId() { return movimentacaoEstoqueId; }
    public void setMovimentacaoEstoqueId(Integer movimentacaoEstoqueId) { this.movimentacaoEstoqueId = movimentacaoEstoqueId; }

    public String getCreditoMovId() { return creditoMovId; }
    public void setCreditoMovId(String creditoMovId) { this.creditoMovId = creditoMovId; }

    public String getEntregueEm() { return entregueEm; }
    public void setEntregueEm(String entregueEm) { this.entregueEm = entregueEm; }

    public String getEntreguePor() { return entreguePor; }
    public void setEntreguePor(String entreguePor) { this.entreguePor = entreguePor; }

    public String getEstornadoEm() { return estornadoEm; }
    public void setEstornadoEm(String estornadoEm) { this.estornadoEm = estornadoEm; }

    public String getEstornadoPor() { return estornadoPor; }
    public void setEstornadoPor(String estornadoPor) { this.estornadoPor = estornadoPor; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
