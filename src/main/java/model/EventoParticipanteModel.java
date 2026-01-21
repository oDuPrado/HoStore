package model;

public class EventoParticipanteModel {
    private String id;
    private String eventoId;
    private String clienteId;
    private String nomeAvulso;
    private String status;
    private Integer vendaId;
    private Integer comandaId;
    private Integer comandaItemId;
    private String dataCheckin;
    private String criadoEm;
    private String criadoPor;
    private String alteradoEm;
    private String alteradoPor;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getNomeAvulso() { return nomeAvulso; }
    public void setNomeAvulso(String nomeAvulso) { this.nomeAvulso = nomeAvulso; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getVendaId() { return vendaId; }
    public void setVendaId(Integer vendaId) { this.vendaId = vendaId; }

    public Integer getComandaId() { return comandaId; }
    public void setComandaId(Integer comandaId) { this.comandaId = comandaId; }

    public Integer getComandaItemId() { return comandaItemId; }
    public void setComandaItemId(Integer comandaItemId) { this.comandaItemId = comandaItemId; }

    public String getDataCheckin() { return dataCheckin; }
    public void setDataCheckin(String dataCheckin) { this.dataCheckin = dataCheckin; }

    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }

    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }

    public String getAlteradoEm() { return alteradoEm; }
    public void setAlteradoEm(String alteradoEm) { this.alteradoEm = alteradoEm; }

    public String getAlteradoPor() { return alteradoPor; }
    public void setAlteradoPor(String alteradoPor) { this.alteradoPor = alteradoPor; }
}
