package model;

public class RhCargoModel {
    private String id;
    private String nome;
    private String descricao;
    private double salarioBase;
    private int ativo;

    public RhCargoModel() {}

    public RhCargoModel(String id, String nome, String descricao, double salarioBase, int ativo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.salarioBase = salarioBase;
        this.ativo = ativo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }

    public int getAtivo() { return ativo; }
    public void setAtivo(int ativo) { this.ativo = ativo; }
}
