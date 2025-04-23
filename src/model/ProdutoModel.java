package model;

import java.time.LocalDateTime;

public class ProdutoModel {

    private String id;              // mesmo id da entidade detalhada (ex: carta.id)
    private String nome;
    private String tipo;           // "Carta", "Booster", etc.
    private int quantidade;
    private double precoCompra;
    private double precoVenda;
    private LocalDateTime criadoEm;
    private LocalDateTime alteradoEm;

    /* ==================== CONSTRUTORES ==================== */

    public ProdutoModel(String id, String nome, String tipo,
                        int quantidade, double precoCompra, double precoVenda) {

        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoCompra = precoCompra;
        this.precoVenda = precoVenda;
        this.criadoEm = LocalDateTime.now();
        this.alteradoEm = LocalDateTime.now();
    }

    /* ==================== GETTERS / SETTERS ==================== */

    public String getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getPrecoCompra() { return precoCompra; }
    public void setPrecoCompra(double precoCompra) { this.precoCompra = precoCompra; }

    public double getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(double precoVenda) { this.precoVenda = precoVenda; }

    public double getLucro() { return precoVenda - precoCompra; }

    public LocalDateTime getCriadoEm() { return criadoEm; }

    public LocalDateTime getAlteradoEm() { return alteradoEm; }
    public void setAlteradoEmNow() { this.alteradoEm = LocalDateTime.now(); }
}
