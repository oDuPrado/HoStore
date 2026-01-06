package model;

public class ProdutoVendaResumoModel {
    public String produtoId;
    public String nome;
    public int quantidade;
    public double total;

    public ProdutoVendaResumoModel() {}

    public ProdutoVendaResumoModel(String produtoId, String nome, int quantidade, double total) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.quantidade = quantidade;
        this.total = total;
    }
}
