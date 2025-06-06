package model;

/**
 * Representa um CSOSN (ou CST, se preferir) com descrição.
 */
public class CsosnModel {
    private String codigo;    // ex: "102"
    private String descricao; // ex: "Isento de ICMS (Simples Nacional – faixa até X)"

    public CsosnModel(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    // Getters / Setters
    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
