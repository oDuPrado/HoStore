package model;

import java.time.LocalDateTime;

public class ComandaItemModel {
    private Integer id;
    private Integer comandaId;
    private String produtoId;

    private int qtd;
    private double preco;
    private double desconto;
    private double acrescimo;
    private double totalItem;

    private String observacoes;
    private LocalDateTime criadoEm;
    private String criadoPor;

    public ComandaItemModel() {}

    public void recalcularTotal() {
        double bruto = qtd * preco;
        totalItem = Math.max(0.0, bruto - desconto + acrescimo);
    }

    // Getters/Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getComandaId() { return comandaId; }
    public void setComandaId(Integer comandaId) { this.comandaId = comandaId; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public int getQtd() { return qtd; }
    public void setQtd(int qtd) { this.qtd = qtd; recalcularTotal(); }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; recalcularTotal(); }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; recalcularTotal(); }

    public double getAcrescimo() { return acrescimo; }
    public void setAcrescimo(double acrescimo) { this.acrescimo = acrescimo; recalcularTotal(); }

    public double getTotalItem() { return totalItem; }
    public void setTotalItem(double totalItem) { this.totalItem = totalItem; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
}
