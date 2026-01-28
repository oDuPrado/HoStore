package model;

import java.util.Date;

/**
 * Representa uma promoção configurada pelo lojista.
 */
public class PromocaoModel {
    private String id;
    private String nome;
    private Double desconto;
    private TipoDesconto tipoDesconto;
    private AplicaEm aplicaEm;
    private String tipoId;       // FK para TipoPromocaoModel (ex: "Desconto %")
    private String categoria;
    private Date dataInicio;
    private Date dataFim;
    private Integer ativo = 1;
    private Integer prioridade = 0;
    private String observacoes;

    public PromocaoModel() { }

    public PromocaoModel(String id, String nome, Double desconto,
                         TipoDesconto tipoDesconto, AplicaEm aplicaEm,
                         String tipoId, String categoria, Date dataInicio, Date dataFim,
                         Integer ativo, Integer prioridade, String observacoes) {
        this.id = id;
        this.nome = nome;
        this.desconto = desconto;
        this.tipoDesconto = tipoDesconto;
        this.aplicaEm = aplicaEm;
        this.tipoId = tipoId;
        this.categoria = categoria;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.ativo = ativo;
        this.prioridade = prioridade;
        this.observacoes = observacoes;
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getDesconto() { return desconto; }
    public void setDesconto(Double desconto) { this.desconto = desconto; }

    public TipoDesconto getTipoDesconto() { return tipoDesconto; }
    public void setTipoDesconto(TipoDesconto tipoDesconto) { this.tipoDesconto = tipoDesconto; }

    public AplicaEm getAplicaEm() { return aplicaEm; }
    public void setAplicaEm(AplicaEm aplicaEm) { this.aplicaEm = aplicaEm; }

    public String getTipoId() { return tipoId; }
    public void setTipoId(String tipoId) { this.tipoId = tipoId; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Date getDataInicio() { return dataInicio; }
    public void setDataInicio(Date dataInicio) { this.dataInicio = dataInicio; }

    public Date getDataFim() { return dataFim; }
    public void setDataFim(Date dataFim) { this.dataFim = dataFim; }

    public Integer getAtivo() { return ativo; }
    public void setAtivo(Integer ativo) { this.ativo = ativo; }

    public Integer getPrioridade() { return prioridade; }
    public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Override
    public String toString() {
        String suf = (tipoDesconto == TipoDesconto.PORCENTAGEM) ? "%]" : " R$]";
        return nome + " [" + tipoDesconto + " " + desconto + suf;
    }
}
