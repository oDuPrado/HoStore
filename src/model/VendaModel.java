package model;

public class VendaModel {
    private int id;
    private String dataVenda;
    private String clienteId;

    private double totalBruto;     // valor total antes do desconto
    private double desconto;       // valor em reais (não %)
    private double totalLiquido;   // valor final (bruto - desconto)

    private String formaPagamento;
    private int parcelas;
    private String status;

    // ==== Construtores ====
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

    // ==== Getters e Setters ====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDataVenda() { return dataVenda; }

    public String getClienteId() { return clienteId; }

    public double getTotalBruto() { return totalBruto; }
    public void setTotalBruto(double totalBruto) { this.totalBruto = totalBruto; }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; }

    public double getTotalLiquido() { return totalLiquido; }
    public void setTotalLiquido(double totalLiquido) { this.totalLiquido = totalLiquido; }

    public String getFormaPagamento() { return formaPagamento; }

    public int getParcelas() { return parcelas; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
