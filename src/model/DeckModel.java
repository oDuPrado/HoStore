package model;

public class DeckModel extends ProdutoModel {

    private String colecao;
    private String tipoDeck;   // pré‑montado, liga
    private String categoria;  // estrela, junior...

    public DeckModel(String id, String nome, int qtd,
                     double custo, double preco, String fornecedor,
                     String colecao, String tipoDeck, String categoria) {

        super(id, nome, "Deck", qtd, custo, preco, fornecedor);
        this.colecao = colecao;
        this.tipoDeck = tipoDeck;
        this.categoria = categoria;
    }

    public String getColecao()  { return colecao; }
    public String getTipoDeck() { return tipoDeck; }
    public String getCategoria(){ return categoria; }
}
