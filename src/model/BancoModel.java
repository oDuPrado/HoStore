package model;

public class BancoModel {
    private String id;
    private String nome;
    private String agencia;
    private String conta;
    private String observacoes;

    public BancoModel(String id, String nome, String agencia, String conta, String observacoes) {
        this.id = id;
        this.nome = nome;
        this.agencia = agencia;
        this.conta = conta;
        this.observacoes = observacoes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
