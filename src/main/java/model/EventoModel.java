package model;

public class EventoModel {
    private String id;
    private String nome;
    private String jogoId;
    private String dataInicio;
    private String dataFim;
    private String status;
    private double taxaInscricao;
    private String produtoInscricaoId;
    private String regrasTexto;
    private Integer limiteParticipantes;
    private String observacoes;
    private String criadoEm;
    private String criadoPor;
    private String alteradoEm;
    private String alteradoPor;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getJogoId() { return jogoId; }
    public void setJogoId(String jogoId) { this.jogoId = jogoId; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTaxaInscricao() { return taxaInscricao; }
    public void setTaxaInscricao(double taxaInscricao) { this.taxaInscricao = taxaInscricao; }

    public String getProdutoInscricaoId() { return produtoInscricaoId; }
    public void setProdutoInscricaoId(String produtoInscricaoId) { this.produtoInscricaoId = produtoInscricaoId; }

    public String getRegrasTexto() { return regrasTexto; }
    public void setRegrasTexto(String regrasTexto) { this.regrasTexto = regrasTexto; }

    public Integer getLimiteParticipantes() { return limiteParticipantes; }
    public void setLimiteParticipantes(Integer limiteParticipantes) { this.limiteParticipantes = limiteParticipantes; }

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
