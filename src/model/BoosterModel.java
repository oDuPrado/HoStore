package model;

public class BoosterModel extends ProdutoModel {

    private String colecao;
    private String set;
    private String tipoBooster; // unitário, quadri, triple, especial

    public BoosterModel(String id, String nome, int qtd,
                        double custo, double preco, String fornecedor,
                        String colecao, String set, String tipo) {

        super(id, nome, "Booster", qtd, custo, preco, fornecedor);
        this.colecao = colecao;
        this.set     = set;
        this.tipoBooster = tipo;
    }

    public String getColecao()    { return colecao; }
    public String getSet()        { return set; }
    public String getTipoBooster(){ return tipoBooster; }
}
