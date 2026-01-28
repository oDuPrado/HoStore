package model;

import java.time.LocalDateTime;

public class ProdutoModel {

    private String id; // mesmo id da entidade detalhada (ex: carta.id)
    private String nome;
    private String tipo; // "Carta", "Booster", etc.
    private String categoria; // categoria opcional
    private String jogoId;
    private String codigoBarras;
    private String fornecedorId; // ID do fornecedor associado a este produto
    private String fornecedorNome; // Nome do fornecedor para exibição na UI
    private int quantidade;
    private double precoCompra;
    private double precoVenda;
    private LocalDateTime criadoEm;
    private LocalDateTime alteradoEm;

    // Dados fiscais obrigatórios
    private String ncm; // Código NCM (8 dígitos)
    private String cfop; // Código CFOP (4 dígitos)
    private String csosn; // Código CSOSN (ou CST)
    private String origem; // Código de Origem (0–8)
    private String unidade; // Unidade de comercialização, ex: "UN", "CX", "KG"

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

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPrecoCompra() {
        return precoCompra;
    }

    public void setPrecoCompra(double precoCompra) {
        this.precoCompra = precoCompra;
    }

    public double getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(double precoVenda) {
        this.precoVenda = precoVenda;
    }

    public double getLucro() {
        return precoVenda - precoCompra;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAlteradoEm() {
        return alteradoEm;
    }

    public void setAlteradoEmNow() {
        this.alteradoEm = LocalDateTime.now();
    }

    // Getter
    public String getJogoId() {
        return jogoId;
    }

    // Setter
    public void setJogoId(String jogoId) {
        this.jogoId = jogoId;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    // Getters e Setters fiscais
    public String getNcm() {
        return ncm;
    }

    public void setNcm(String ncm) {
        this.ncm = ncm;
    }

    public String getCfop() {
        return cfop;
    }

    public void setCfop(String cfop) {
        this.cfop = cfop;
    }

    public String getCsosn() {
        return csosn;
    }

    public void setCsosn(String csosn) {
        this.csosn = csosn;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    /**
     * ID do fornecedor associado a este produto.
     */
    public String getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(String fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    /**
     * Nome do fornecedor para exibição na UI.
     */
    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public void setFornecedorNome(String fornecedorNome) {
        this.fornecedorNome = fornecedorNome;
    }

    @Override
    public String toString() {
        return String.format(
                "%s [%s] | Estoque: %d | R$ %.2f",
                nome,
                tipo,
                quantidade,
                precoVenda);
    }

}
