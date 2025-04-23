package model;

public class BoosterModel extends ProdutoModel {

    private String colecao;
    private String set;
    private String tipoBooster;
    private String idioma;
    private String validade;
    private String codigoBarras;

    private String fornecedorId;     // usado para salvar no banco
    private String fornecedorNome;   // exibido na UI

    public BoosterModel(
            String id,
            String nome,
            int quantidade,
            double custo,
            double precoVenda,
            String fornecedorId,
            String colecao,
            String set,
            String tipoBooster,
            String idioma,
            String validade,
            String codigoBarras
    ) {
        super(id, nome, "Booster", quantidade, custo, precoVenda);
        this.fornecedorId   = fornecedorId;
        this.colecao        = colecao;
        this.set            = set;
        this.tipoBooster    = tipoBooster;
        this.idioma         = idioma;
        this.validade       = validade;
        this.codigoBarras   = codigoBarras;
    }

    // Getters específicos
    public String getColecao()        { return colecao; }
    public String getSet()            { return set; }
    public String getTipoBooster()    { return tipoBooster; }
    public String getIdioma()         { return idioma; }
    public String getValidade()       { return validade; }
    public String getCodigoBarras()   { return codigoBarras; }

    public String getFornecedor()     { return fornecedorId; }
    public void setFornecedor(String id) { this.fornecedorId = id; }

    public String getFornecedorNome()     { return fornecedorNome; }
    public void setFornecedorNome(String nome) { this.fornecedorNome = nome; }
}
