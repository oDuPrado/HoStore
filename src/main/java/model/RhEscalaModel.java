package model;

public class RhEscalaModel {
    private int id;
    private String funcionarioId;
    private String data;
    private String inicio;
    private String fim;
    private String observacoes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(String funcionarioId) { this.funcionarioId = funcionarioId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getInicio() { return inicio; }
    public void setInicio(String inicio) { this.inicio = inicio; }

    public String getFim() { return fim; }
    public void setFim(String fim) { this.fim = fim; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
