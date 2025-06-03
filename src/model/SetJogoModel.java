package model;

public class SetJogoModel {

    private String setId;           // ID do set (ex: "WCS", "ced", "BT01")
    private String nome;            // Nome do set (ex: "World Championship 2004")
    private String jogoId;          // Ex: "YUGIOH", "MAGIC", "DIGIMON", etc.
    private String dataLancamento;  // Formato ISO (yyyy-MM-dd)
    private Integer qtdCartas;      // Número de cartas no set (pode ser nulo)
    private String codigoExterno;   // Código opcional adicional (ex: ID da API externa)

    // Getters e setters

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getJogoId() {
        return jogoId;
    }

    public void setJogoId(String jogoId) {
        this.jogoId = jogoId;
    }

    public String getDataLancamento() {
        return dataLancamento;
    }

    public void setDataLancamento(String dataLancamento) {
        this.dataLancamento = dataLancamento;
    }

    public Integer getQtdCartas() {
        return qtdCartas;
    }

    public void setQtdCartas(Integer qtdCartas) {
        this.qtdCartas = qtdCartas;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }
}
