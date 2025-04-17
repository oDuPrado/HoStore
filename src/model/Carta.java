package model;

public class Carta {
    private String id;
    private String nome;
    private String colecao;
    private String numero;
    private int    qtd;
    private double preco;

    /* --- novos campos --- */
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

    /* construtor mínimo anterior (mantido p/ compat) */
    public Carta(String id, String nome, String colecao, String numero,
                 int qtd, double preco) {
        this.id = id; this.nome = nome; this.colecao = colecao; this.numero = numero;
        this.qtd = qtd; this.preco = preco;
    }

    /* construtor completo (para cadastro novo) */
    public Carta(String id, String nome, String colecao, String numero,
                 int qtd, double preco, double custo, String condicaoId,
                 String linguagemId, boolean consignado, String dono,
                 String tipoId, String subtipoId, String raridadeId,
                 String subRaridadeId, String ilustracaoId) {

        this(id, nome, colecao, numero, qtd, preco);
        this.custo = custo; this.condicaoId = condicaoId; this.linguagemId = linguagemId;
        this.consignado = consignado; this.dono = dono; this.tipoId = tipoId;
        this.subtipoId = subtipoId; this.raridadeId = raridadeId;
        this.subRaridadeId = subRaridadeId; this.ilustracaoId = ilustracaoId;
    }

    /* getters padrão */
    public String getId()            { return id; }
    public String getNome()          { return nome; }
    public String getColecao()       { return colecao; }
    public String getNumero()        { return numero; }
    public int    getQtd()           { return qtd; }
    public double getPreco()         { return preco; }
    public double getCusto()         { return custo; }
    public String getCondicaoId()    { return condicaoId; }
    public String getLinguagemId()   { return linguagemId; }
    public boolean isConsignado()    { return consignado; }
    public String getDono()          { return dono; }
    public String getTipoId()        { return tipoId; }
    public String getSubtipoId()     { return subtipoId; }
    public String getRaridadeId()    { return raridadeId; }
    public String getSubRaridadeId() { return subRaridadeId; }
    public String getIlustracaoId()  { return ilustracaoId; }

    /* setters p/ quantidade e preço (usados na venda) */
    public void setQtd  (int qtd)       { this.qtd = qtd; }
    public void setPreco(double preco)  { this.preco = preco; }
}
