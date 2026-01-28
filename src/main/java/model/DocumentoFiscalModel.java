package model;

import service.FiscalCalcService;

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
    public String xmlPath;
    public String xmlSha256;
    public Integer xmlTamanho;
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

    // Inner class para item com impostos
    public static class ItemComImpostos {
        private String produtoId;
        private String ncm;
        private String descricao;
        private double quantidade;
        private double valorUnit;
        private double desconto;
        private double acrescimo;
        private double totalItem;
        private String cfop;
        private String csosn;
        private String origem;
        private String unidade;
        private FiscalCalcService.ImpostosItem impostos;

        // Getters and Setters
        public String getProdutoId() {
            return produtoId;
        }

        public void setProdutoId(String produtoId) {
            this.produtoId = produtoId;
        }
        public String getNcm() {
            return ncm;
        }

        public void setNcm(String ncm) {
            this.ncm = ncm;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public double getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(double quantidade) {
            this.quantidade = quantidade;
        }

        public double getValorUnit() {
            return valorUnit;
        }

        public void setValorUnit(double valorUnit) {
            this.valorUnit = valorUnit;
        }

        public double getDesconto() {
            return desconto;
        }

        public void setDesconto(double desconto) {
            this.desconto = desconto;
        }

        public double getAcrescimo() {
            return acrescimo;
        }

        public void setAcrescimo(double acrescimo) {
            this.acrescimo = acrescimo;
        }

        public double getTotalItem() {
            return totalItem;
        }

        public void setTotalItem(double totalItem) {
            this.totalItem = totalItem;
        }

        public String getCfop() {
            return cfop;
        }

        public void setCfop(String cfop) {
            this.cfop = cfop;
        }

        public String getCsosn() {
            return csosn;
        }

        public void setCsosn(String csosn) {
            this.csosn = csosn;
        }

        public String getOrigem() {
            return origem;
        }

        public void setOrigem(String origem) {
            this.origem = origem;
        }

        public String getUnidade() {
            return unidade;
        }

        public void setUnidade(String unidade) {
            this.unidade = unidade;
        }

        public FiscalCalcService.ImpostosItem getImpostos() {
            return impostos;
        }

        public void setImpostos(FiscalCalcService.ImpostosItem impostos) {
            this.impostos = impostos;
        }
    }
}
