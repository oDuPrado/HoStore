package model;

/**
 * Representa uma entrada de NCM (8 dígitos) com sua descrição oficial.
 */
public class NcmModel {
    private String codigo;    // ex: "95044000"
    private String descricao; // ex: "Jogos de cartas, para jogos de salão ou de tabuleiro"

    public NcmModel(String codigo, String descricao) {
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
