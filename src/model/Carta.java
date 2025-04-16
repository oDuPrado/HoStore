package model;

public class Carta {
    private String id;
    private String nome;
    private String colecao;
    private String numero;
    private int qtd;
    private double preco;

    public Carta(String id, String nome, String colecao, String numero, int qtd, double preco) {
        this.id = id;
        this.nome = nome;
        this.colecao = colecao;
        this.numero = numero;
        this.qtd = qtd;
        this.preco = preco;
    }

    public String getId()       { return id; }
    public String getNome()     { return nome; }
    public String getColecao()  { return colecao; }
    public String getNumero()   { return numero; }
    public int getQtd()         { return qtd; }
    public double getPreco()    { return preco; }

    public void setQtd(int qtd)         { this.qtd = qtd; }
    public void setPreco(double preco)  { this.preco = preco; }
}
