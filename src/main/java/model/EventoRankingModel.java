package model;

public class EventoRankingModel {
    private String id;
    private String eventoId;
    private String participanteId;
    private int pontos;
    private Integer colocacao;
    private String observacao;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }

    public String getParticipanteId() { return participanteId; }
    public void setParticipanteId(String participanteId) { this.participanteId = participanteId; }

    public int getPontos() { return pontos; }
    public void setPontos(int pontos) { this.pontos = pontos; }

    public Integer getColocacao() { return colocacao; }
    public void setColocacao(Integer colocacao) { this.colocacao = colocacao; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
