package model;

import java.math.BigDecimal;

/**
 * Representa dados mínimos para exibir um produto na tela de venda.
 */
public class ProdutoEstoqueDTO {
    private int id;
    private String tipoDisplay;
    private String nome;
    private int quantidade;
    private BigDecimal precoVenda;
    private String ncm;
    private String cfop;
    private String csosn;
    private String origem;
    private String unidade;

    // Getters e setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTipoDisplay() {
        return tipoDisplay;
    }

    public void setTipoDisplay(String tipoDisplay) {
        this.tipoDisplay = tipoDisplay;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

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

}
