package model;

public class ClienteModel {
    private String id;
    private String nome;
    private String telefone;
    private String cpf;
    private String dataNasc;
    private String tipo;  // Colecionador, Jogador, Ambos
    private String endereco;
    private String cidade;
    private String estado;
    private String observacoes;

    private String criadoEm;
    private String criadoPor;
    private String alteradoEm;
    private String alteradoPor;

    // Getters e setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getDataNasc() { return dataNasc; }
    public void setDataNasc(String dataNasc) { this.dataNasc = dataNasc; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    public String getAlteradoEm() { return alteradoEm; }
    public void setAlteradoEm(String alteradoEm) { this.alteradoEm = alteradoEm; }
    public String getAlteradoPor() { return alteradoPor; }
    public void setAlteradoPor(String alteradoPor) { this.alteradoPor = alteradoPor; }
}
