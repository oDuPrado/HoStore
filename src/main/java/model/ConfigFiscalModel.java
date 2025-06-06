package model;

import java.util.UUID;

/**
 * Representa a configuração fiscal associada a um cliente (CNPJ/ID).
 * Contém padrão de regime tributário, CFOP, CSOSN, Origem, NCM e Unidade.
 */
public class ConfigFiscalModel {

    private final String id;           // UUID único
    private String clienteId;          // FK para clientes(id)

    private String regimeTributario;   // "Simples Nacional", "Lucro Presumido", etc.
    private String cfopPadrao;         // ex: "5102"
    private String csosnPadrao;        // ex: "102"
    private String origemPadrao;       // ex: "0"
    private String ncmPadrao;          // ex: "95044000"
    private String unidadePadrao;      // ex: "UN", "CX", "KG"

    /** Construtor completo, já com ID (usado ao ler do banco). */
    public ConfigFiscalModel(
            String id,
            String clienteId,
            String regimeTributario,
            String cfopPadrao,
            String csosnPadrao,
            String origemPadrao,
            String ncmPadrao,
            String unidadePadrao
    ) {
        this.id = (id != null) ? id : UUID.randomUUID().toString();
        this.clienteId = clienteId;
        this.regimeTributario = regimeTributario;
        this.cfopPadrao = cfopPadrao;
        this.csosnPadrao = csosnPadrao;
        this.origemPadrao = origemPadrao;
        this.ncmPadrao = ncmPadrao;
        this.unidadePadrao = unidadePadrao;
    }

    /** Construtor para criar nova configuração (gera UUID automaticamente). */
    public ConfigFiscalModel(
            String clienteId,
            String regimeTributario,
            String cfopPadrao,
            String csosnPadrao,
            String origemPadrao,
            String ncmPadrao,
            String unidadePadrao
    ) {
        this(UUID.randomUUID().toString(),
             clienteId,
             regimeTributario,
             cfopPadrao,
             csosnPadrao,
             origemPadrao,
             ncmPadrao,
             unidadePadrao);
    }

    // ========= Getters / Setters =========

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }
    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getRegimeTributario() {
        return regimeTributario;
    }
    public void setRegimeTributario(String regimeTributario) {
        this.regimeTributario = regimeTributario;
    }

    public String getCfopPadrao() {
        return cfopPadrao;
    }
    public void setCfopPadrao(String cfopPadrao) {
        this.cfopPadrao = cfopPadrao;
    }

    public String getCsosnPadrao() {
        return csosnPadrao;
    }
    public void setCsosnPadrao(String csosnPadrao) {
        this.csosnPadrao = csosnPadrao;
    }

    public String getOrigemPadrao() {
        return origemPadrao;
    }
    public void setOrigemPadrao(String origemPadrao) {
        this.origemPadrao = origemPadrao;
    }

    public String getNcmPadrao() {
        return ncmPadrao;
    }
    public void setNcmPadrao(String ncmPadrao) {
        this.ncmPadrao = ncmPadrao;
    }

    public String getUnidadePadrao() {
        return unidadePadrao;
    }
    public void setUnidadePadrao(String unidadePadrao) {
        this.unidadePadrao = unidadePadrao;
    }
}
