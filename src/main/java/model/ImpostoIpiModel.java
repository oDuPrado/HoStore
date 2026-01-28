package model;

public class ImpostoIpiModel {
    private int id;
    private String ncm;
    private Double aliquota;
    private String cnpjProdutor;
    private boolean ativo = true;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNcm() {
        return ncm;
    }

    public void setNcm(String ncm) {
        this.ncm = ncm;
    }

    public Double getAliquota() {
        return aliquota;
    }

    public void setAliquota(Double aliquota) {
        this.aliquota = aliquota;
    }

    public String getCnpjProdutor() {
        return cnpjProdutor;
    }

    public void setCnpjProdutor(String cnpjProdutor) {
        this.cnpjProdutor = cnpjProdutor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
