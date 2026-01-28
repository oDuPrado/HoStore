package model;

public class RhFuncionarioModel {
    private String id;
    private String nome;
    private String tipoContrato;
    private String cpf;
    private String cnpj;
    private String rg;
    private String pis;
    private String dataAdmissao;
    private String dataDemissao;
    private String cargoId;
    private double salarioBase;
    private double comissaoPct;
    private String usuarioId;
    private String email;
    private String telefone;
    private String endereco;
    private int ativo;
    private String observacoes;

    public RhFuncionarioModel() {}

    public RhFuncionarioModel(String id, String nome, String tipoContrato) {
        this.id = id;
        this.nome = nome;
        this.tipoContrato = tipoContrato;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(String tipoContrato) { this.tipoContrato = tipoContrato; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }

    public String getPis() { return pis; }
    public void setPis(String pis) { this.pis = pis; }

    public String getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(String dataAdmissao) { this.dataAdmissao = dataAdmissao; }

    public String getDataDemissao() { return dataDemissao; }
    public void setDataDemissao(String dataDemissao) { this.dataDemissao = dataDemissao; }

    public String getCargoId() { return cargoId; }
    public void setCargoId(String cargoId) { this.cargoId = cargoId; }

    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }

    public double getComissaoPct() { return comissaoPct; }
    public void setComissaoPct(double comissaoPct) { this.comissaoPct = comissaoPct; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public int getAtivo() { return ativo; }
    public void setAtivo(int ativo) { this.ativo = ativo; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
