// src/model/ConfigLojaModel.java
package model;

import java.util.UUID;

/**
 * Representa as configurações da loja, necessárias para emissão de cupom fiscal (NFC-e).
 * Inclui dados cadastrais, endereço, parâmetros de NFC-e e configuração de impressão.
 */
public class ConfigLojaModel {
    // ========== Identificação ==========
    private final String id;                   // UUID único (sempre presente)
    private String nome;                       // Razão Social
    private String nomeFantasia;               // Nome Fantasia
    private String cnpj;                       // CNPJ (somente números, 14 dígitos)
    private String inscricaoEstadual;          // Inscrição Estadual
    private String regimeTributario;           // Simples, Lucro Presumido, Lucro Real...
    private String cnae;                       // CNAE principal (código)


    // ========== Endereço ==========
    private String enderecoLogradouro;         // Ex.: "Rua Exemplo"
    private String enderecoNumero;             // Ex.: "123"
    private String enderecoComplemento;        // Ex.: "Sala 1"
    private String enderecoBairro;             // Ex.: "Centro"
    private String enderecoMunicipio;          // Ex.: "São Paulo"
    private String enderecoUf;                 // Ex.: "SP"
    private String enderecoCep;                // Ex.: "01234-567"

    // ========== Contato ==========
    private String telefone;                   // Ex.: "(11) 98765-4321"
    private String email;                      // Ex.: "contato@minhaloja.com.br"

    // ========== NFC-e / Documentos Fiscais ==========
    private String modeloNota;                 // Ex.: "65" para NFC-e
    private String serieNota;                  // Ex.: "001"
    private int    numeroInicialNota;          // Ex.: 1, 1001...
    private String ambienteNfce;               // "HOMOLOGACAO" ou "PRODUCAO"
    private String csc;                        // Código de Segurança do Contribuinte
    private String tokenCsc;                   // Token do CSC (quando aplicável)
    private String certificadoPath;            // Caminho para arquivo .pfx do Certificado Digital A1
    private String certificadoSenha;           // Senha do certificado digital

    // ========== Impressão ==========
    private String nomeImpressora;             // Ex.: "EPSON_TM-T20"
    private String textoRodapeNota;            // Texto de rodapé no cupom (ex.: "Volte sempre!")

    // ========== URL / WebService (opcional) ==========
    private String urlWebServiceNfce;          // Ex.: "https://nfe.fazenda.sp.gov.br"
    private String proxyHost;                  // Ex.: "proxy.meudominio.com"
    private int    proxyPort;                  // Ex.: 3128
    private String proxyUsuario;               // Se necessário
    private String proxySenha;                 // Se necessário

    /**
     * Construtor completo, usado ao ler do banco de dados.
     */
    public ConfigLojaModel(
            String id,
            String nome,
            String nomeFantasia,
            String cnpj,
            String inscricaoEstadual,
            String regimeTributario,
            String cnae,
            String enderecoLogradouro,
            String enderecoNumero,
            String enderecoComplemento,
            String enderecoBairro,
            String enderecoMunicipio,
            String enderecoUf,
            String enderecoCep,
            String telefone,
            String email,
            String modeloNota,
            String serieNota,
            int numeroInicialNota,
            String ambienteNfce,
            String csc,
            String tokenCsc,
            String certificadoPath,
            String certificadoSenha,
            String nomeImpressora,
            String textoRodapeNota,
            String urlWebServiceNfce,
            String proxyHost,
            int proxyPort,
            String proxyUsuario,
            String proxySenha
    ) {
        this.id                    = (id != null) ? id : UUID.randomUUID().toString();
        this.nome                  = nome;
        this.nomeFantasia          = nomeFantasia;
        this.cnpj                  = cnpj;
        this.inscricaoEstadual     = inscricaoEstadual;
        this.regimeTributario      = regimeTributario;
        this.cnae                  = cnae;
        this.enderecoLogradouro    = enderecoLogradouro;
        this.enderecoNumero        = enderecoNumero;
        this.enderecoComplemento   = enderecoComplemento;
        this.enderecoBairro        = enderecoBairro;
        this.enderecoMunicipio     = enderecoMunicipio;
        this.enderecoUf            = enderecoUf;
        this.enderecoCep           = enderecoCep;
        this.telefone              = telefone;
        this.email                 = email;
        this.modeloNota            = modeloNota;
        this.serieNota             = serieNota;
        this.numeroInicialNota     = numeroInicialNota;
        this.ambienteNfce          = ambienteNfce;
        this.csc                   = csc;
        this.tokenCsc              = tokenCsc;
        this.certificadoPath       = certificadoPath;
        this.certificadoSenha      = certificadoSenha;
        this.nomeImpressora        = nomeImpressora;
        this.textoRodapeNota       = textoRodapeNota;
        this.urlWebServiceNfce     = urlWebServiceNfce;
        this.proxyHost             = proxyHost;
        this.proxyPort             = proxyPort;
        this.proxyUsuario          = proxyUsuario;
        this.proxySenha            = proxySenha;
    }

    /**
     * Construtor que gera um novo UUID automaticamente.
     * Pode ser usado quando o lojista ainda não tiver nenhuma configuração salva.
     */
    public ConfigLojaModel(
            String nome,
            String nomeFantasia,
            String cnpj,
            String inscricaoEstadual,
            String regimeTributario,
            String cnae,
            String enderecoLogradouro,
            String enderecoNumero,
            String enderecoComplemento,
            String enderecoBairro,
            String enderecoMunicipio,
            String enderecoUf,
            String enderecoCep,
            String telefone,
            String email,
            String modeloNota,
            String serieNota,
            int numeroInicialNota,
            String ambienteNfce,
            String csc,
            String tokenCsc,
            String certificadoPath,
            String certificadoSenha,
            String nomeImpressora,
            String textoRodapeNota,
            String urlWebServiceNfce,
            String proxyHost,
            int proxyPort,
            String proxyUsuario,
            String proxySenha
    ) {
        this(UUID.randomUUID().toString(),
             nome,
             nomeFantasia,
             cnpj,
             inscricaoEstadual,
             regimeTributario,
             cnae,
             enderecoLogradouro,
             enderecoNumero,
             enderecoComplemento,
             enderecoBairro,
             enderecoMunicipio,
             enderecoUf,
             enderecoCep,
             telefone,
             email,
             modeloNota,
             serieNota,
             numeroInicialNota,
             ambienteNfce,
             csc,
             tokenCsc,
             certificadoPath,
             certificadoSenha,
             nomeImpressora,
             textoRodapeNota,
             urlWebServiceNfce,
             proxyHost,
             proxyPort,
             proxyUsuario,
             proxySenha);
    }

    // ========= Getters e Setters =========

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }
    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
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

    public String getRegimeTributario() {
        return regimeTributario;
    }
    public void setRegimeTributario(String regimeTributario) {
        this.regimeTributario = regimeTributario;
    }

    public String getCnae() {
        return cnae;
    }
    public void setCnae(String cnae) {
        this.cnae = cnae;
    }

    public String getEnderecoLogradouro() {
        return enderecoLogradouro;
    }
    public void setEnderecoLogradouro(String enderecoLogradouro) {
        this.enderecoLogradouro = enderecoLogradouro;
    }

    public String getEnderecoNumero() {
        return enderecoNumero;
    }
    public void setEnderecoNumero(String enderecoNumero) {
        this.enderecoNumero = enderecoNumero;
    }

    public String getEnderecoComplemento() {
        return enderecoComplemento;
    }
    public void setEnderecoComplemento(String enderecoComplemento) {
        this.enderecoComplemento = enderecoComplemento;
    }

    public String getEnderecoBairro() {
        return enderecoBairro;
    }
    public void setEnderecoBairro(String enderecoBairro) {
        this.enderecoBairro = enderecoBairro;
    }

    public String getEnderecoMunicipio() {
        return enderecoMunicipio;
    }
    public void setEnderecoMunicipio(String enderecoMunicipio) {
        this.enderecoMunicipio = enderecoMunicipio;
    }

    public String getEnderecoUf() {
        return enderecoUf;
    }
    public void setEnderecoUf(String enderecoUf) {
        this.enderecoUf = enderecoUf;
    }

    public String getEnderecoCep() {
        return enderecoCep;
    }
    public void setEnderecoCep(String enderecoCep) {
        this.enderecoCep = enderecoCep;
    }

    public String getTelefone() {
        return telefone;
    }
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getModeloNota() {
        return modeloNota;
    }
    public void setModeloNota(String modeloNota) {
        this.modeloNota = modeloNota;
    }

    public String getSerieNota() {
        return serieNota;
    }
    public void setSerieNota(String serieNota) {
        this.serieNota = serieNota;
    }

    public int getNumeroInicialNota() {
        return numeroInicialNota;
    }
    public void setNumeroInicialNota(int numeroInicialNota) {
        this.numeroInicialNota = numeroInicialNota;
    }

    public String getAmbienteNfce() {
        return ambienteNfce;
    }
    public void setAmbienteNfce(String ambienteNfce) {
        this.ambienteNfce = ambienteNfce;
    }

    public String getCsc() {
        return csc;
    }
    public void setCsc(String csc) {
        this.csc = csc;
    }

    public String getTokenCsc() {
        return tokenCsc;
    }
    public void setTokenCsc(String tokenCsc) {
        this.tokenCsc = tokenCsc;
    }

    public String getCertificadoPath() {
        return certificadoPath;
    }
    public void setCertificadoPath(String certificadoPath) {
        this.certificadoPath = certificadoPath;
    }

    public String getCertificadoSenha() {
        return certificadoSenha;
    }
    public void setCertificadoSenha(String certificadoSenha) {
        this.certificadoSenha = certificadoSenha;
    }

    public String getNomeImpressora() {
        return nomeImpressora;
    }
    public void setNomeImpressora(String nomeImpressora) {
        this.nomeImpressora = nomeImpressora;
    }

    public String getTextoRodapeNota() {
        return textoRodapeNota;
    }
    public void setTextoRodapeNota(String textoRodapeNota) {
        this.textoRodapeNota = textoRodapeNota;
    }

    public String getUrlWebServiceNfce() {
        return urlWebServiceNfce;
    }
    public void setUrlWebServiceNfce(String urlWebServiceNfce) {
        this.urlWebServiceNfce = urlWebServiceNfce;
    }

    public String getProxyHost() {
        return proxyHost;
    }
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsuario() {
        return proxyUsuario;
    }
    public void setProxyUsuario(String proxyUsuario) {
        this.proxyUsuario = proxyUsuario;
    }

    public String getProxySenha() {
        return proxySenha;
    }
    public void setProxySenha(String proxySenha) {
        this.proxySenha = proxySenha;
    }
}
