package model;

public class DocumentoFiscalModel {
    public String id;
    public Integer vendaId;

    public String modelo;        // "NFCe"
    public int codigoModelo;     // 65
    public int serie;
    public int numero;

    public String ambiente;      // OFF/HOMOLOG/PRODUCAO
    public String status;        // PENDENTE/AUTORIZADA/...

    public String chaveAcesso;
    public String protocolo;
    public String recibo;
    public String xml;
    public String erro;

    public Double totalProdutos;
    public Double totalDesconto;
    public Double totalAcrescimo;
    public Double totalFinal;

    public String criadoEm;
    public String criadoPor;
    public String atualizadoEm;
    public String canceladoEm;
    public String canceladoPor;
}
