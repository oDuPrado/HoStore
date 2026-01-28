package service;

import model.ConfiguracaoNfeNfceModel;

/**
 * Wrapper simplificado para ConfiguracaoNfeNfceModel NFCe
 * Mapeia campos privados para propriedades públicas
 */
public class ConfigNfceAdapter {
    private ConfiguracaoNfeNfceModel config;

    public ConfigNfceAdapter(ConfiguracaoNfeNfceModel config) {
        this.config = config;
    }

    // Getters públicos
    public String getCertificadoPath() {
        return config.getCertificadoPathNfce();
    }

    public String getCertificadoSenha() {
        return config.getCertificadoSenhaNfce();
    }

    public String getCnpj() {
        return config.getCnpj();
    }

    public String getNomeEmpresa() {
        return config.getNomeEmpresa();
    }

    public String getNomeFantasia() {
        return config.getNomeFantasia();
    }

    public String getUf() {
        return config.getUf();
    }

    public String getInscricaoEstadual() {
        return config.getInscricaoEstadual();
    }

    public int getSerieNfce() {
        return config.getSerieNfce();
    }

    public int getNumeroInicialNfce() {
        return config.getNumeroInicialNfce();
    }

    public String getAmbiente() {
        return config.getAmbiente();
    }

    public String getCscNfce() {
        return config.getCscNfce();
    }

    public int getIdCscNfce() {
        return config.getIdCscNfce();
    }

    public String getEnderecoLogradouro() {
        return config.getEnderecologradouro();
    }

    public String getEnderecoNumero() {
        return config.getEndereconumero();
    }

    public String getEnderecoComplemento() {
        return config.getEnderecocomplemento();
    }

    public String getEnderecoBairro() {
        return config.getEnderecobairro();
    }

    public String getEnderecoMunicipio() {
        return config.getEnderecomunicipio();
    }

    public String getEnderecoCEP() {
        return config.getEnderecoCEP();
    }
}
