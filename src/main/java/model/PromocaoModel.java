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
    private Date dataInicio;
    private Date dataFim;
    private String observacoes;

    public PromocaoModel() { }

    public PromocaoModel(String id, String nome, Double desconto,
                         TipoDesconto tipoDesconto, AplicaEm aplicaEm,
                         String tipoId, Date dataInicio, Date dataFim,
                         String observacoes) {
        this.id = id;
        this.nome = nome;
        this.desconto = desconto;
        this.tipoDesconto = tipoDesconto;
        this.aplicaEm = aplicaEm;
        this.tipoId = tipoId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
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

    public Date getDataInicio() { return dataInicio; }
    public void setDataInicio(Date dataInicio) { this.dataInicio = dataInicio; }

    public Date getDataFim() { return dataFim; }
    public void setDataFim(Date dataFim) { this.dataFim = dataFim; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Override
    public String toString() {
        return nome + " [" + tipoDesconto + " " + desconto + (tipoDesconto == TipoDesconto.PORCENTAGEM ? "%]" : " R$]");
    }
}
