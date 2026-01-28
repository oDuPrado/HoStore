package model;

public class RhSalarioModel {
    private int id;
    private String funcionarioId;
    private String cargoId;
    private double salarioBase;
    private String dataInicio;
    private String dataFim;
    private String motivo;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(String funcionarioId) { this.funcionarioId = funcionarioId; }

    public String getCargoId() { return cargoId; }
    public void setCargoId(String cargoId) { this.cargoId = cargoId; }

    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
