package model;

public class EtbModel extends ProdutoModel {
    private String fornecedor;
    private String serie;
    private String colecao;
    private String tipo;
    private String versao;

    /**
     * @param id          ID único (mesmo da tabela produtos)
     * @param nome        Nome do ETB
     * @param quantidade  Quantidade em estoque
     * @param precoCompra Preço de custo
     * @param precoVenda  Preço de venda
     * @param fornecedor  ID ou nome do fornecedor
     * @param serie       Série (set) do produto
     * @param colecao     Coleção relacionada à série
     * @param tipo        Tipo (Booster Box, Pokémon Center, ETB)
     * @param versao      Versão (Nacional, Americana)
     */
    public EtbModel(String id,
                    String nome,
                    int quantidade,
                    double precoCompra,
                    double precoVenda,
                    String fornecedor,
                    String serie,
                    String colecao,
                    String tipo,
                    String versao) {
        super(id, nome, "ETB", quantidade, precoCompra, precoVenda);
        this.fornecedor = fornecedor;
        this.serie      = serie;
        this.colecao    = colecao;
        this.tipo       = tipo;
        this.versao     = versao;
    }

    // — Getters —
    public String getFornecedor() { return fornecedor; }
    public String getSerie()      { return serie; }
    public String getColecao()    { return colecao; }
    public String getTipo()       { return tipo; }
    public String getVersao()     { return versao; }

    // — Setters (opcionais) —
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }
    public void setSerie(String serie)           { this.serie = serie; }
    public void setColecao(String colecao)       { this.colecao = colecao; }
    public void setTipo(String tipo)             { this.tipo = tipo; }
    public void setVersao(String versao)         { this.versao = versao; }
}
