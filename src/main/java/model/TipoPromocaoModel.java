package model;

public class TipoPromocaoModel {
    private String id;
    private String nome;
    private String descricao;

    public TipoPromocaoModel() {}

    public TipoPromocaoModel(String id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }

    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    @Override
public String toString() {
    return nome; // agora o combobox mostra o nome ao invés do ID
}

}
