package model;

public class VendaItemModel {
    private int id;
    private int vendaId;
    private String cartaId;
    private int qtd;
    private double preco;
    private double desconto;
    private double totalItem; // novo campo

    public VendaItemModel(String cartaId, int qtd, double preco, double desconto) {
        this.cartaId = cartaId;
        this.qtd = qtd;
        this.preco = preco;
        this.desconto = desconto;
        calcularTotal(); // já calcula no construtor
    }

    private void calcularTotal() {
        double bruto = qtd * preco;
        this.totalItem = bruto * (1 - desconto / 100.0);
    }

    // setters e getters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVendaId() { return vendaId; }
    public void setVendaId(int id) { this.vendaId = id; }

    public String getCartaId() { return cartaId; }

    public int getQtd() { return qtd; }
    public void setQtd(int qtd) {
        this.qtd = qtd;
        calcularTotal();
    }

    public double getPreco() { return preco; }
    public void setPreco(double preco) {
        this.preco = preco;
        calcularTotal();
    }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) {
        this.desconto = desconto;
        calcularTotal();
    }

    public double getTotalItem() { return totalItem; }
    public void setTotalItem(double totalItem) { this.totalItem = totalItem; }
}
