package model;

public class SetModel {
    private String id;
    private String nome;
    private String series;
    private String colecao_id;
    private String dataLancamento;

    public SetModel() {}

    public SetModel(String id, String nome, String series, String colecao_id, String dataLancamento) {
        this.id = id;
        this.nome = nome;
        this.series = series;
        this.colecao_id = colecao_id;
        this.dataLancamento = dataLancamento;
    }    

    // Getters
    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getSeries() { return series; }
    public String getColecaoId() { return colecao_id; }
    public String getDataLancamento() { return dataLancamento; }

    // Setters (caso precise setar depois)
    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setSeries(String series) { this.series = series; }
    public void setColecaoId(String colecao_id) { this.colecao_id = colecao_id; }
    public void setDataLancamento(String dataLancamento) { this.dataLancamento = dataLancamento; }

    @Override
    public String toString() {
        return series;
    }
}
