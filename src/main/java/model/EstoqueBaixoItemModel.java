package model;

public class EstoqueBaixoItemModel {
    public String id;
    public String nome;
    public int quantidade;

    public EstoqueBaixoItemModel() {}

    public EstoqueBaixoItemModel(String id, String nome, int quantidade) {
        this.id = id;
        this.nome = nome;
        this.quantidade = quantidade;
    }
}
