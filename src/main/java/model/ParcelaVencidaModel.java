package model;

public class ParcelaVencidaModel {
    public String origem; // "RECEBER" ou "PAGAR"
    public String clienteOuFornecedor;
    public String vencimentoIso;
    public double valorAberto;
    public int diasAtraso;

    public ParcelaVencidaModel() {}

    public ParcelaVencidaModel(String origem, String nome, String venc, double aberto, int dias) {
        this.origem = origem;
        this.clienteOuFornecedor = nome;
        this.vencimentoIso = venc;
        this.valorAberto = aberto;
        this.diasAtraso = dias;
    }
}
