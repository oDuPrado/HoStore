package model;

public class PromocaoProdutoModel {
    private String id;
    private String promocaoId;
    private String produtoId;

    public PromocaoProdutoModel() {}

    public PromocaoProdutoModel(String id, String promocaoId, String produtoId) {
        this.id = id;
        this.promocaoId = promocaoId;
        this.produtoId = produtoId;
    }

    public String getId() { return id; }
    public String getPromocaoId() { return promocaoId; }
    public String getProdutoId() { return produtoId; }

    public void setId(String id) { this.id = id; }
    public void setPromocaoId(String promocaoId) { this.promocaoId = promocaoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }
}
