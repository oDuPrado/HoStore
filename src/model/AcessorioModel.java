package model;

public class AcessorioModel extends ProdutoModel {

    private String tipoAcessorio; // chaveiro, playmat...

    public AcessorioModel(String id, String nome, int qtd,
                          double custo, double preco,
                          String fornecedor, String tipoAcessorio) {

        super(id, nome, "Acessório", qtd, custo, preco, fornecedor);
        this.tipoAcessorio = tipoAcessorio;
    }

    public String getTipoAcessorio() { return tipoAcessorio; }
    public void   setTipoAcessorio(String s) { tipoAcessorio = s; }
}
