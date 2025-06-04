// procure no seu projeto: src/model/AcessorioModel.java
package model;

public class AcessorioModel extends ProdutoModel {
    private String categoria;      // ex: "Playmats", "Sleeve", etc.
    private String arte;           // ex: "Pokémon", "Treinador", "Outros" ou "Cor Única"
    private String cor;            // usado apenas se arte="Cor Única"
    private String fornecedorId;
    private String fornecedorNome;

    public AcessorioModel(String id,
                          String nome,
                          int quantidade,
                          double custo,
                          double precoVenda,
                          String fornecedorId,
                          String categoria,
                          String arte,
                          String cor) {
        super(id, nome, "Acessório", quantidade, custo, precoVenda);
        this.fornecedorId = fornecedorId;
        this.categoria = categoria;
        this.arte = arte;
        this.cor = cor;
    }

    // getters e setters
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getArte() { return arte; }
    public void setArte(String arte) { this.arte = arte; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(String fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }
}
