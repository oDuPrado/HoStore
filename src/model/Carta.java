package model;

public class Carta {
    private String id;
    private String nome;
    private String setId;
    private String colecao;
    private String numero;
    private int    qtd;

    private double precoLoja;
    private double precoConsignado;
    private double percentualLoja;
    private double valorLoja;

    private double custo;
    private String condicaoId;
    private String linguagemId;
    private boolean consignado;
    private String dono;

    private String tipoId;
    private String subtipoId;
    private String raridadeId;
    private String subRaridadeId;
    private String ilustracaoId;
    private String fornecedorId;

    /* construtor mínimo anterior (mantido p/ compat) */
    public Carta(String id, String nome, String colecao, String numero,
                 int qtd, double precoLoja) {
        this.id = id;
        this.nome = nome;
        this.colecao = colecao;
        this.numero = numero;
        this.qtd = qtd;
        this.precoLoja = precoLoja;
    }

    /* construtor completo */
    public Carta(String id, String nome, String setId, String colecao, String numero,
                 int qtd, double precoLoja, double precoConsignado, double percentualLoja,
                 double valorLoja, double custo, String condicaoId, String linguagemId,
                 boolean consignado, String dono, String tipoId, String subtipoId,
                 String raridadeId, String subRaridadeId, String ilustracaoId,
                 String fornecedorId) {

        this.id = id;
        this.nome = nome;
        this.setId = setId;
        this.colecao = colecao;
        this.numero = numero;
        this.qtd = qtd;
        this.precoLoja = precoLoja;
        this.precoConsignado = precoConsignado;
        this.percentualLoja = percentualLoja;
        this.valorLoja = valorLoja;
        this.custo = custo;
        this.condicaoId = condicaoId;
        this.linguagemId = linguagemId;
        this.consignado = consignado;
        this.dono = dono;
        this.tipoId = tipoId;
        this.subtipoId = subtipoId;
        this.raridadeId = raridadeId;
        this.subRaridadeId = subRaridadeId;
        this.ilustracaoId = ilustracaoId;
        this.fornecedorId = fornecedorId;
    }

    // Getters

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getSetId() { return setId; }
    public String getColecao() { return colecao; }
    public String getNumero() { return numero; }
    public int getQtd() { return qtd; }
    public double getPrecoLoja() { return precoLoja; }
    public double getPrecoConsignado() { return precoConsignado; }
    public double getPercentualLoja() { return percentualLoja; }
    public double getValorLoja() { return valorLoja; }
    public double getCusto() { return custo; }
    public String getCondicaoId() { return condicaoId; }
    public String getLinguagemId() { return linguagemId; }
    public boolean isConsignado() { return consignado; }
    public String getDono() { return dono; }
    public String getTipoId() { return tipoId; }
    public String getSubtipoId() { return subtipoId; }
    public String getRaridadeId() { return raridadeId; }
    public String getSubRaridadeId() { return subRaridadeId; }
    public String getIlustracaoId() { return ilustracaoId; }
    public String getFornecedorId() { return fornecedorId; }

    // Setters usados em venda

    public void setQtd(int qtd) { this.qtd = qtd; }
    public void setPrecoLoja(double precoLoja) { this.precoLoja = precoLoja; }
}
