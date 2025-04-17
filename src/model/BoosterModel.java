package model;

public class BoosterModel extends ProdutoModel {

    private String colecao;
    private String set;
    private String tipoBooster;
    private String idioma;
    private String validade;
    private String codigoBarras;

    public BoosterModel(
            String id,
            String nome,
            int quantidade,
            double custo,
            double precoVenda,
            String fornecedor,
            String colecao,
            String set,
            String tipoBooster,
            String idioma,
            String validade,
            String codigoBarras
    ) {
        super(id, nome, "Booster", quantidade, custo, precoVenda, fornecedor);
        this.colecao       = colecao;
        this.set           = set;
        this.tipoBooster   = tipoBooster;
        this.idioma        = idioma;
        this.validade      = validade;
        this.codigoBarras  = codigoBarras;
    }

    public String getColecao()      { return colecao; }
    public String getSet()          { return set; }
    public String getTipoBooster()  { return tipoBooster; }
    public String getIdioma()       { return idioma; }
    public String getValidade()     { return validade; }
    public String getCodigoBarras() { return codigoBarras; }
}
