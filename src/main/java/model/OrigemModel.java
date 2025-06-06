package model;

/**
 * Representa uma Origem de produto (0–8) e sua descrição.
 */
public class OrigemModel {
    private String codigo;    // ex: "0"
    private String descricao; // ex: "0 – Nacional"

    public OrigemModel(String codigo, String descricao) {
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
