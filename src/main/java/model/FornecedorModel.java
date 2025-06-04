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
    private String pagamentoTipo;
    private Integer prazo;
    private String criadoEm;
    private String alteradoEm;

    public FornecedorModel() {}

    public FornecedorModel(String id, String nome, String telefone, String email,
                           String cnpj, String contato, String endereco,
                           String cidade, String estado, String observacoes,
                           String pagamentoTipo, Integer prazo,
                           String criadoEm, String alteradoEm) {
        this.id = id;
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.cnpj = cnpj;
        this.contato = contato;
        this.endereco = endereco;
        this.cidade = cidade;
        this.estado = estado;
        this.observacoes = observacoes;
        this.pagamentoTipo = pagamentoTipo;
        this.prazo = prazo;
        this.criadoEm = criadoEm;
        this.alteradoEm = alteradoEm;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getCnpj() { return cnpj; }
    public String getContato() { return contato; }
    public String getEndereco() { return endereco; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
    public String getObservacoes() { return observacoes; }
    public String getPagamentoTipo() { return pagamentoTipo; }
    public Integer getPrazo() { return prazo; }
    public String getCriadoEm() { return criadoEm; }
    public String getAlteradoEm() { return alteradoEm; }

    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEmail(String email) { this.email = email; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public void setContato(String contato) { this.contato = contato; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public void setPagamentoTipo(String pagamentoTipo) { this.pagamentoTipo = pagamentoTipo; }
    public void setPrazo(Integer prazo) { this.prazo = prazo; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }
    public void setAlteradoEm(String alteradoEm) { this.alteradoEm = alteradoEm; }
}
