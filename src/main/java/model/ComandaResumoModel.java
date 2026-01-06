package model;

public class ComandaResumoModel {
    private int id;
    private String cliente;
    private String mesa;
    private String status;
    private String criadoEm;

    private double totalLiquido;
    private double totalPago;
    private double saldo;

    public ComandaResumoModel() {}

    // Getters/Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getMesa() { return mesa; }
    public void setMesa(String mesa) { this.mesa = mesa; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }

    public double getTotalLiquido() { return totalLiquido; }
    public void setTotalLiquido(double totalLiquido) { this.totalLiquido = totalLiquido; }

    public double getTotalPago() { return totalPago; }
    public void setTotalPago(double totalPago) { this.totalPago = totalPago; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
}
