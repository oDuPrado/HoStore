// src/model/ConfigLojaModel.java
package model;

import java.util.UUID;

/**
 * Representa as configurações da loja: dados básicos e parâmetros de notas fiscais.
 */
public class ConfigLojaModel {
    private final String id;               // UUID único
    private String nome;                   // Nome da loja
    private String cnpj;                   // CNPJ
    private String telefone;               // Telefone
    private String socios;                 // Sócios / observações
    private String modeloNota;             // Ex.: “Nota Avulsa”, “Cupom SAT”
    private String serieNota;              // Ex.: “001”
    private int    numeroInicialNota;      // Ex.: 1, 1001, etc.
    private String nomeImpressora;         // Ex.: “HP_LaserJet”
    private String textoRodapeNota;        // Ex.: “Obrigado pela preferência!”

    /**
     * Construtor completo. Use este ao buscar do banco ou instanciar um objeto pronto para salvar.
     */
    public ConfigLojaModel(
            String id,
            String nome,
            String cnpj,
            String telefone,
            String socios,
            String modeloNota,
            String serieNota,
            int numeroInicialNota,
            String nomeImpressora,
            String textoRodapeNota
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.nome = nome;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.socios = socios;
        this.modeloNota = modeloNota;
        this.serieNota = serieNota;
        this.numeroInicialNota = numeroInicialNota;
        this.nomeImpressora = nomeImpressora;
        this.textoRodapeNota = textoRodapeNota;
    }

    /**
     * Construtor que gera um novo UUID automaticamente.
     * Pode ser usado ao criar pela primeira vez.
     */
    public ConfigLojaModel(
            String nome,
            String cnpj,
            String telefone,
            String socios,
            String modeloNota,
            String serieNota,
            int numeroInicialNota,
            String nomeImpressora,
            String textoRodapeNota
    ) {
        this(UUID.randomUUID().toString(),
             nome, cnpj, telefone, socios,
             modeloNota, serieNota, numeroInicialNota,
             nomeImpressora, textoRodapeNota);
    }

    // Getters e setters (exceto setId, pois id é final)

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnpj() {
        return cnpj;
    }
    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getTelefone() {
        return telefone;
    }
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getSocios() {
        return socios;
    }
    public void setSocios(String socios) {
        this.socios = socios;
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
}
