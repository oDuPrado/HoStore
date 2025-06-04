package model;

public class VendaPagamentoModel {
    private final int id;
    private final String tipo;
    private final double valorPago;
    private final double valorJaEstornado;

    public VendaPagamentoModel(int id, String tipo, double valorPago, double valorJaEstornado) {
        this.id = id;
        this.tipo = tipo;
        this.valorPago = valorPago;
        this.valorJaEstornado = valorJaEstornado;
    }

    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public double getValorPago() {
        return valorPago;
    }

    public double getValorJaEstornado() {
        return valorJaEstornado;
    }
}
