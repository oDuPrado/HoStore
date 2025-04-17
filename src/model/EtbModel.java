package model;

public class EtbModel extends ProdutoModel {

    private String colecao;
    private String set;
    private String tipoEtb; // booster box, pokemon center, ETB
    private String versao;  // nacional, americana

    public EtbModel(String id, String nome, int qtd, double custo,
                    double preco, String fornecedor, String colecao,
                    String set, String tipo, String versao) {

        super(id, nome, "ETB", qtd, custo, preco, fornecedor);
        this.colecao = colecao; this.set = set;
        this.tipoEtb = tipo; this.versao = versao;
    }

    public String getColecao() { return colecao; }
    public String getSet()     { return set; }
    public String getTipoEtb() { return tipoEtb; }
    public String getVersao()  { return versao; }
}
