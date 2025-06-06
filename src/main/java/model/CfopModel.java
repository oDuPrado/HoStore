package model;

/**
 * Representa um CFOP (4 dígitos) e sua descrição.
 */
public class CfopModel {
    private String codigo;    // ex: "5102"
    private String descricao; // ex: "Venda de produção do estabelecimento"

    public CfopModel(String codigo, String descricao) {
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
