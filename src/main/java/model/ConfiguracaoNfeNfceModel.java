package model;

public class ConfiguracaoNfeNfceModel {
    public static final String ID_UNICO = "CONFIG_PADRAO";

    private String id = ID_UNICO;
    private boolean emitirNfe = false;
    private String certificadoPathNfe;
    private String certificadoSenhaNfe;
    private int serieNfe = 1;
    private int numeroInicialNfe = 1;
    private boolean emitirNfce = true;
    private String cscNfce;
    private int idCscNfce;
    private String certificadoPathNfce;
    private String certificadoSenhaNfce;
    private int serieNfce = 1;
    private int numeroInicialNfce = 1;
    private String ambiente = "homologacao";
    private String regimeTributario;
    private String nomeEmpresa;
    private String cnpj;
    private String inscricaoEstadual;
    private String uf;
    private String nomeFantasia;
    private String enderecologradouro;
    private String endereconumero;
    private String enderecocomplemento;
    private String enderecobairro;
    private String enderecomunicipio;
    private String enderecoCEP;

    // Getters and Setters

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

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public void setInscricaoEstadual(String inscricaoEstadual) {
        this.inscricaoEstadual = inscricaoEstadual;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    }

    public String getEnderecologradouro() {
        return enderecologradouro;
    }

    public void setEnderecologradouro(String enderecologradouro) {
        this.enderecologradouro = enderecologradouro;
    }

    public String getEndereconumero() {
        return endereconumero;
    }

    public void setEndereconumero(String endereconumero) {
        this.endereconumero = endereconumero;
    }

    public String getEnderecocomplemento() {
        return enderecocomplemento;
    }

    public void setEnderecocomplemento(String enderecocomplemento) {
        this.enderecocomplemento = enderecocomplemento;
    }

    public String getEnderecobairro() {
        return enderecobairro;
    }

    public void setEnderecobairro(String enderecobairro) {
        this.enderecobairro = enderecobairro;
    }

    public String getEnderecomunicipio() {
        return enderecomunicipio;
    }

    public void setEnderecomunicipio(String enderecomunicipio) {
        this.enderecomunicipio = enderecomunicipio;
    }

    public String getEnderecoCEP() {
        return enderecoCEP;
    }

    public void setEnderecoCEP(String enderecoCEP) {
        this.enderecoCEP = enderecoCEP;
    }
}
