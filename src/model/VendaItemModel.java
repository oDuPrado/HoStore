// src/model/VendaItemModel.java
package model;

/**
 * Item de venda.
 *
 * Agora usa o campo generificado <produtoId>. Mantive os antigos
 * métodos getCartaId() apenas como *alias* para não quebrar
 * referências soltas no código legado.
 */
public class VendaItemModel {

    /* ---------- Campos ---------- */
    private int    id;
    private int    vendaId;
    private String produtoId;   // <- AGORA É genérico!
    private int    qtd;
    private double preco;
    private double desconto;    // % (0-100)
    private double totalItem;   // calculado

    /* ---------- Construtores ---------- */
public VendaItemModel() {
    // necessário para instanciar vazio e preencher com setters
}
    public VendaItemModel(String produtoId, int qtd,
                          double preco, double desconto) {
        this.produtoId = produtoId;
        this.qtd       = qtd;
        this.preco     = preco;
        this.desconto  = desconto;
        calcularTotal();
    }

    /* ---------- Cálculo ---------- */
    private void calcularTotal() {
        double bruto = qtd * preco;
        this.totalItem = bruto * (1 - desconto / 100.0);
    }

    /* ---------- Getters / Setters ---------- */
    public int    getId()            { return id; }
    public void   setId(int id)      { this.id = id; }

    public int    getVendaId()       { return vendaId; }
    public void   setVendaId(int id) { this.vendaId = id; }

    /** NOVO campo principal */
    public String getProdutoId()     { return produtoId; }

    /** Alias p/ legado (não use em código novo) */
    public String getCartaId()       { return produtoId; }

    public int    getQtd()           { return qtd; }
    public void   setQtd(int q)      { this.qtd = q; calcularTotal(); }

    public double getPreco()         { return preco; }
    public void   setPreco(double p) { this.preco = p; calcularTotal(); }

    public double getDesconto()         { return desconto; }
    public void   setDesconto(double d) { this.desconto = d; calcularTotal(); }

    public double getTotalItem()        { return totalItem; }
    public void   setTotalItem(double t){ this.totalItem = t; }

    public void setProdutoId(String produtoId) {
    this.produtoId = produtoId;
}
}
