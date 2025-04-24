package model;

public class AlimentoModel extends ProdutoModel {
    private String categoria;
    private String subtipo;
    private String marca;
    private String sabor;
    private String lote;
    private double peso;
    private String unidadePeso;
    private String codigoBarras;
    private String dataValidade;
    private String fornecedorId;
    private String fornecedorNome;

    public AlimentoModel(
        String id,
        String nome,
        int quantidade,
        double precoCompra,
        double precoVenda,
        String fornecedorId,
        String categoria,
        String subtipo,
        String marca,
        String sabor,
        String lote,
        double peso,
        String unidadePeso,
        String codigoBarras,
        String dataValidade
    ) {
        super(id, nome, "Alimento", quantidade, precoCompra, precoVenda);
        this.fornecedorId   = fornecedorId;
        this.categoria      = categoria;
        this.subtipo        = subtipo;
        this.marca          = marca;
        this.sabor          = sabor;
        this.lote           = lote;
        this.peso           = peso;
        this.unidadePeso    = unidadePeso;
        this.codigoBarras   = codigoBarras;
        this.dataValidade   = dataValidade;
    }

    // Getters & Setters
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getSubtipo() { return subtipo; }
    public void setSubtipo(String subtipo) { this.subtipo = subtipo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public String getUnidadePeso() { return unidadePeso; }
    public void setUnidadePeso(String unidadePeso) { this.unidadePeso = unidadePeso; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getDataValidade() { return dataValidade; }
    public void setDataValidade(String dataValidade) { this.dataValidade = dataValidade; }

    public String getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(String fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }
}
