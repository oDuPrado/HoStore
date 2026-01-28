package model;

public class ImpostoIcmsModel {
    private int id;
    private String estado;
    private String estadoDestino;
    private String ncm;
    private Double aliquotaConsumidor;
    private Double aliquotaContribuinte;
    private double reducaoBase = 0;
    private double mvaBc = 0;
    private boolean ativo = true;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstadoDestino() {
        return estadoDestino;
    }

    public void setEstadoDestino(String estadoDestino) {
        this.estadoDestino = estadoDestino;
    }

    public String getNcm() {
        return ncm;
    }

    public void setNcm(String ncm) {
        this.ncm = ncm;
    }

    public Double getAliquotaConsumidor() {
        return aliquotaConsumidor;
    }

    public void setAliquotaConsumidor(Double aliquotaConsumidor) {
        this.aliquotaConsumidor = aliquotaConsumidor;
    }

    public Double getAliquotaContribuinte() {
        return aliquotaContribuinte;
    }

    public void setAliquotaContribuinte(Double aliquotaContribuinte) {
        this.aliquotaContribuinte = aliquotaContribuinte;
    }

    public double getReducaoBase() {
        return reducaoBase;
    }

    public void setReducaoBase(double reducaoBase) {
        this.reducaoBase = reducaoBase;
    }

    public double getMvaBc() {
        return mvaBc;
    }

    public void setMvaBc(double mvaBc) {
        this.mvaBc = mvaBc;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
