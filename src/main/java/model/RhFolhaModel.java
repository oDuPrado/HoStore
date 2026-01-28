package model;

public class RhFolhaModel {
    private int id;
    private String competencia;
    private String funcionarioId;
    private double salarioBase;
    private double horasTrabalhadas;
    private double horasExtras;
    private double descontos;
    private double comissao;
    private double totalBruto;
    private double totalLiquido;
    private String status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompetencia() { return competencia; }
    public void setCompetencia(String competencia) { this.competencia = competencia; }

    public String getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(String funcionarioId) { this.funcionarioId = funcionarioId; }

    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }

    public double getHorasTrabalhadas() { return horasTrabalhadas; }
    public void setHorasTrabalhadas(double horasTrabalhadas) { this.horasTrabalhadas = horasTrabalhadas; }

    public double getHorasExtras() { return horasExtras; }
    public void setHorasExtras(double horasExtras) { this.horasExtras = horasExtras; }

    public double getDescontos() { return descontos; }
    public void setDescontos(double descontos) { this.descontos = descontos; }

    public double getComissao() { return comissao; }
    public void setComissao(double comissao) { this.comissao = comissao; }

    public double getTotalBruto() { return totalBruto; }
    public void setTotalBruto(double totalBruto) { this.totalBruto = totalBruto; }

    public double getTotalLiquido() { return totalLiquido; }
    public void setTotalLiquido(double totalLiquido) { this.totalLiquido = totalLiquido; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
