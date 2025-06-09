package model;

/**
 * Representa uma configuração de taxa de cartão para uma maquininha.
 */
public class TaxaCartaoModel {
    private Integer id;
    private String bandeira; // ex: "Cielo", "Stone", "Rede"
    private String tipo; // "CREDITO" ou "DEBITO"
    private int minParcelas; // parcela mínima para esta taxa
    private int maxParcelas; // parcela máxima para esta taxa
    private String mesVigencia; // formato "YYYY-MM", ex "2025-06"
    private double taxaPct; // percentual, ex 3.49
    private String observacoes; // texto livre / JSON para configs extras

    public TaxaCartaoModel() {
    }

    public TaxaCartaoModel(Integer id, String bandeira, String tipo,
            int minParcelas, int maxParcelas,
            String mesVigencia, double taxaPct,
            String observacoes) {
        this.id = id;
        this.bandeira = bandeira;
        this.tipo = tipo;
        this.minParcelas = minParcelas;
        this.maxParcelas = maxParcelas;
        this.mesVigencia = mesVigencia;
        this.taxaPct = taxaPct;
        this.observacoes = observacoes;
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getMinParcelas() {
        return minParcelas;
    }

    public void setMinParcelas(int minParcelas) {
        this.minParcelas = minParcelas;
    }

    public int getMaxParcelas() {
        return maxParcelas;
    }

    public void setMaxParcelas(int maxParcelas) {
        this.maxParcelas = maxParcelas;
    }

    public String getMesVigencia() {
        return mesVigencia;
    }

    public void setMesVigencia(String mesVigencia) {
        this.mesVigencia = mesVigencia;
    }

    public double getTaxaPct() {
        return taxaPct;
    }

    public void setTaxaPct(double taxaPct) {
        this.taxaPct = taxaPct;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
