package model;

public class DeckModel extends ProdutoModel {

    private String fornecedor;
    private String colecao;
    private String tipoDeck;
    private String categoria;

    /**
     * @param id           ID único do deck (mesmo usado na tabela produtos)
     * @param nome         Nome do deck
     * @param quantidade   Quantidade em estoque
     * @param precoCompra  Preço de custo
     * @param precoVenda   Preço de venda
     * @param fornecedor   ID do fornecedor (ou nome, conforme seu fluxo)
     * @param colecao      Nome da coleção
     * @param tipoDeck     Tipo (Pré-montado, Liga)
     * @param categoria    Categoria extra (Estrela, 2 Estrelas, …)
     */
    public DeckModel(String id,
                     String nome,
                     int quantidade,
                     double precoCompra,
                     double precoVenda,
                     String fornecedor,
                     String colecao,
                     String tipoDeck,
                     String categoria) {
        super(id, nome, "Deck", quantidade, precoCompra, precoVenda);
        this.fornecedor = fornecedor;
        this.colecao    = colecao;
        this.tipoDeck   = tipoDeck;
        this.categoria  = categoria;
    }

    // — Getters —  
    public String getFornecedor() { return fornecedor; }
    public String getColecao()    { return colecao; }
    public String getTipoDeck()   { return tipoDeck; }
    public String getCategoria()  { return categoria; }

    // — Setters — (opcionais, caso precise editar em memória)
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }
    public void setColecao(String colecao)       { this.colecao = colecao; }
    public void setTipoDeck(String tipoDeck)     { this.tipoDeck = tipoDeck; }
    public void setCategoria(String categoria)   { this.categoria = categoria; }
}
