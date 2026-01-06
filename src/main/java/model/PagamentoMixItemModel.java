package model;

public class PagamentoMixItemModel {
    public String tipo;
    public double valor;
    public double pct; // 0..1

    public PagamentoMixItemModel() {}

    public PagamentoMixItemModel(String tipo, double valor, double pct) {
        this.tipo = tipo;
        this.valor = valor;
        this.pct = pct;
    }
}
