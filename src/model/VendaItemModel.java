package model;

public class VendaItemModel {
    private int id;
    private int vendaId;
    private String cartaId;
    private int qtd;
    private double preco;
    private double desconto;

    public VendaItemModel(String cartaId, int qtd, double preco, double desconto) {
        this.cartaId = cartaId;
        this.qtd = qtd;
        this.preco = preco;
        this.desconto = desconto;
    }

    public void setVendaId(int id) { this.vendaId = id; }
    public String getCartaId() { return cartaId; }
    public int getQtd() { return qtd; }
    public double getPreco() { return preco; }
    public double getDesconto() { return desconto; }
    public void setQtd(int qtd) { this.qtd = qtd; }
    public void setPreco(double preco) { this.preco = preco; }
    public void setDesconto(double desconto) { this.desconto = desconto; }

}
