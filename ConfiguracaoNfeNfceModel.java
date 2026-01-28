package model;

public class ConfiguracaoNfeNfceModel {

    private String id;
    private boolean emitirNfe;
    private String certificadoPathNfe;
    private String certificadoSenhaNfe;
    private int serieNfe;
    private int numeroInicialNfe;
    private boolean emitirNfce;
    private String cscNfce;
    private int idCscNfce;
    private String certificadoPathNfce;
    private String certificadoSenhaNfce;
    private int serieNfce;
    private int numeroInicialNfce;
    private String ambiente;
    private String regimeTributario;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEmitirNfe() {
        return emitirNfe;
    }

    public void setEmitirNfe(boolean emitirNfe) {
        this.emitirNfe = emitirNfe;
    }

    public String getCertificadoPathNfe() {
        return certificadoPathNfe;
    }

    public void setCertificadoPathNfe(String certificadoPathNfe) {
        this.certificadoPathNfe = certificadoPathNfe;
    }

    public String getCertificadoSenhaNfe() {
        return certificadoSenhaNfe;
    }

    public void setCertificadoSenhaNfe(String certificadoSenhaNfe) {
        this.certificadoSenhaNfe = certificadoSenhaNfe;
    }

    public int getSerieNfe() {
        return serieNfe;
    }

    public void setSerieNfe(int serieNfe) {
        this.serieNfe = serieNfe;
    }

    public int getNumeroInicialNfe() {
        return numeroInicialNfe;
    }

    public void setNumeroInicialNfe(int numeroInicialNfe) {
        this.numeroInicialNfe = numeroInicialNfe;
    }

    public boolean isEmitirNfce() {
        return emitirNfce;
    }

    public void setEmitirNfce(boolean emitirNfce) {
        this.emitirNfce = emitirNfce;
    }

    public String getCscNfce() {
        return cscNfce;
    }

    public void setCscNfce(String cscNfce) {
        this.cscNfce = cscNfce;
    }

    public int getIdCscNfce() {
        return idCscNfce;
    }

    public void setIdCscNfce(int idCscNfce) {
        this.idCscNfce = idCscNfce;
    }

    public String getCertificadoPathNfce() {
        return certificadoPathNfce;
    }

    public void setCertificadoPathNfce(String certificadoPathNfce) {
        this.certificadoPathNfce = certificadoPathNfce;
    }

    public String getCertificadoSenhaNfce() {
        return certificadoSenhaNfce;
    }

    public void setCertificadoSenhaNfce(String certificadoSenhaNfce) {
        this.certificadoSenhaNfce = certificadoSenhaNfce;
    }

    public int getSerieNfce() {
        return serieNfce;
    }

    public void setSerieNfce(int serieNfce) {
        this.serieNfce = serieNfce;
    }

    public int getNumeroInicialNfce() {
        return numeroInicialNfce;
    }

    public void setNumeroInicialNfce(int numeroInicialNfce) {
        this.numeroInicialNfce = numeroInicialNfce;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getRegimeTributario() {
        return regimeTributario;
    }

    public void setRegimeTributario(String regimeTributario) {
        this.regimeTributario = regimeTributario;
    }
}