package model;

import java.util.List;

/**
 * Cabeçalho de venda.
 * Mantido igual, mas com setter opcional de itens.
 */
public class VendaModel {

    private int id;
    private String dataVenda;
    private String clienteId;
    private double totalBruto;
    private double desconto;
    private double acrescimo;
    private double totalLiquido;
    private String formaPagamento;
    private int parcelas;
    private String dataPrimeiroVencimento;
    private int intervaloDias;
    private double juros;

    private String status;

    /* --- Novo: itens da venda (útil para gerar PDF) --- */
    private transient List<VendaItemModel> itens;

    /* ---------- Construtores ---------- */
    public VendaModel(int id, String data, String cliente,
            double totalBruto, double desconto, double totalLiquido,
            String forma, int parcelas, String status) {

        this.id = id;
        this.dataVenda = data;
        this.clienteId = cliente;
        this.totalBruto = totalBruto;
        this.desconto = desconto;
        this.totalLiquido = totalLiquido;
        this.formaPagamento = forma;
        this.parcelas = parcelas;
        this.status = status;
    }

    public VendaModel(String data, String cliente,
            double totalBruto, double desconto, double totalLiquido,
            String forma, int parcelas, String status) {
        this(-1, data, cliente, totalBruto, desconto, totalLiquido, forma, parcelas, status);
    }

    public boolean isDevolucaoParcial(List<VendaDevolucaoModel> devolucoes) {
        if (devolucoes == null || devolucoes.isEmpty() || itens == null)
            return false;

        int totalDevolvido = devolucoes.stream()
                .mapToInt(VendaDevolucaoModel::getQuantidade)
                .sum();

        int totalVendido = this.getItens().stream()
                .mapToInt(VendaItemModel::getQtd)
                .sum();

        return totalDevolvido > 0 && totalDevolvido < totalVendido;
    }

    private String usuario;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    /* ---------- Getters / Setters ---------- */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDataVenda() {
        return dataVenda;
    }

    public String getClienteId() {
        return clienteId;
    }

    public double getTotalBruto() {
        return totalBruto;
    }

    public void setTotalBruto(double v) {
        this.totalBruto = v;
    }

    public double getDesconto() {
        return desconto;
    }

    public void setDesconto(double v) {
        this.desconto = v;
    }

    public double getAcrescimo() {
        return acrescimo;
    }

    public void setAcrescimo(double acrescimo) {
        this.acrescimo = acrescimo;
    }

    public double getTotalLiquido() {
        return totalLiquido;
    }

    public void setTotalLiquido(double v) {
        this.totalLiquido = v;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public int getParcelas() {
        return parcelas;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String s) {
        this.status = s;
    }

    public List<VendaItemModel> getItens() {
        return itens;
    }

    public void setItens(List<VendaItemModel> itens) {
        this.itens = itens;
    }

    public String getDataPrimeiroVencimento() {
        return dataPrimeiroVencimento;
    }

    public void setDataPrimeiroVencimento(String data) {
        this.dataPrimeiroVencimento = data;
    }

    public int getIntervaloDias() {
        return intervaloDias;
    }

    public void setIntervaloDias(int dias) {
        this.intervaloDias = dias;
    }

    public double getJuros() {
        return juros;
    }

    public void setJuros(double juros) {
        this.juros = juros;
    }

}
