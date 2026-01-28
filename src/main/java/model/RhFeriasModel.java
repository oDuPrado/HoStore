package model;

public class RhFeriasModel {
    private int id;
    private String funcionarioId;
    private String dataInicio;
    private String dataFim;
    private int abono;
    private String status;
    private String observacoes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(String funcionarioId) { this.funcionarioId = funcionarioId; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public int getAbono() { return abono; }
    public void setAbono(int abono) { this.abono = abono; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
