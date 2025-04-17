package model;

public class ProdutoAlimenticioModel extends ProdutoModel {

    private String validade; // DD/MM/AAAA

    public ProdutoAlimenticioModel(String id, String nome, int qtd,
                                   double custo, double preco,
                                   String fornecedor, String validade) {

        super(id, nome, "Alimento", qtd, custo, preco, fornecedor);
        this.validade = validade;
    }

    public String getValidade() { return validade; }
}
