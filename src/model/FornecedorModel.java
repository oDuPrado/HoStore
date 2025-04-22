// src/model/FornecedorModel.java
package model;

public class FornecedorModel {
    private String id;
    private String nome;
    private String telefone;
    private String email;
    private String cnpj;
    private String contato;
    private String endereco;
    private String cidade;
    private String estado;
    private String observacoes;
    private String pagamentoTipo; // "À Vista" ou "A Prazo"
    private Integer prazo;        // dias (7,15,...)
    private String criadoEm;
    private String alteradoEm;

    public FornecedorModel() {}

    public FornecedorModel(String id, String nome, String telefone, String email,
                           String cnpj, String contato, String endereco,
                           String cidade, String estado, String observacoes,
                           String pagamentoTipo, Integer prazo,
                           String criadoEm, String alteradoEm) {
        this.id = id; this.nome = nome; this.telefone = telefone; this.email = email;
        this.cnpj = cnpj; this.contato = contato; this.endereco = endereco;
        this.cidade = cidade; this.estado = estado; this.observacoes = observacoes;
        this.pagamentoTipo = pagamentoTipo; this.prazo = prazo;
        this.criadoEm = criadoEm; this.alteradoEm = alteradoEm;
    }

    // getters & setters para todos os campos...
    // (omiti por brevidade, mas gere todos)
}
