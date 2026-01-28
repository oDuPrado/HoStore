package model;

public class RhPontoModel {
    private int id;
    private String funcionarioId;
    private String data;
    private String entrada;
    private String saida;
    private String intervaloInicio;
    private String intervaloFim;
    private double horasTrabalhadas;
    private String origem;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(String funcionarioId) { this.funcionarioId = funcionarioId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getEntrada() { return entrada; }
    public void setEntrada(String entrada) { this.entrada = entrada; }

    public String getSaida() { return saida; }
    public void setSaida(String saida) { this.saida = saida; }

    public String getIntervaloInicio() { return intervaloInicio; }
    public void setIntervaloInicio(String intervaloInicio) { this.intervaloInicio = intervaloInicio; }

    public String getIntervaloFim() { return intervaloFim; }
    public void setIntervaloFim(String intervaloFim) { this.intervaloFim = intervaloFim; }

    public double getHorasTrabalhadas() { return horasTrabalhadas; }
    public void setHorasTrabalhadas(double horasTrabalhadas) { this.horasTrabalhadas = horasTrabalhadas; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
