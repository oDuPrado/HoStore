package model;

import java.time.LocalDateTime;

public class ProdutoModel {

    private String id;              // SKU ou código interno
    private String nome;
    private String categoria;       // “Booster”, “Deck”, “Acessório”, etc.
    private int    quantidade;
    private double precoCompra;
    private double precoVenda;
    private String fornecedor;
    private LocalDateTime criadoEm;
    private LocalDateTime alteradoEm;

    /* ==================== CONSTRUTORES ==================== */

    public ProdutoModel(String id, String nome, String categoria,
                        int quantidade, double precoCompra,
                        double precoVenda, String fornecedor) {

        this.id          = id;
        this.nome        = nome;
        this.categoria   = categoria;
        this.quantidade  = quantidade;
        this.precoCompra = precoCompra;
        this.precoVenda  = precoVenda;
        this.fornecedor  = fornecedor;
        this.criadoEm    = LocalDateTime.now();
        this.alteradoEm  = LocalDateTime.now();
    }

    /* ==================== GETTERS / SETTERS ==================== */

    public String getId()                { return id; }
    public String getNome()              { return nome; }
    public void   setNome(String nome)   { this.nome = nome; }

    public String getCategoria()               { return categoria; }
    public void   setCategoria(String cat)     { this.categoria = cat; }

    public int  getQuantidade()                { return quantidade; }
    public void setQuantidade(int qtd)         { this.quantidade = qtd; }

    public double getPrecoCompra()             { return precoCompra; }
    public void   setPrecoCompra(double pc)    { this.precoCompra = pc; }

    public double getPrecoVenda()              { return precoVenda; }
    public void   setPrecoVenda(double pv)     { this.precoVenda = pv; }

    public String getFornecedor()              { return fornecedor; }
    public void   setFornecedor(String f)      { this.fornecedor = f; }

    public LocalDateTime getCriadoEm()         { return criadoEm; }
    public LocalDateTime getAlteradoEm()       { return alteradoEm; }
    public void          setAlteradoEmNow()    { this.alteradoEm = LocalDateTime.now(); }
}
