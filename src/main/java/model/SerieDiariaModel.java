package model;

public class SerieDiariaModel {
    public String diaIso; // YYYY-MM-DD
    public int qtdVendas;
    public double total;

    public SerieDiariaModel() {}

    public SerieDiariaModel(String diaIso, int qtdVendas, double total) {
        this.diaIso = diaIso;
        this.qtdVendas = qtdVendas;
        this.total = total;
    }
}
