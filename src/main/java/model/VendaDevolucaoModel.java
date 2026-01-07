package model;

import java.time.LocalDate;

public class VendaDevolucaoModel {

    private int id;
    private int vendaId;
    private String produtoId;
    private int quantidade;
    private double valor;     // valor unitário
    private LocalDate data;
    private String motivo;

    // Opcional (não precisa existir no banco)
    private String usuario;

    public VendaDevolucaoModel() {}

    public VendaDevolucaoModel(int vendaId, String produtoId, int quantidade, double valor,
                               LocalDate data, String motivo, String usuario) {
        this.vendaId = vendaId;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.valor = valor;
        this.data = data;
        this.motivo = motivo;
        this.usuario = usuario;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVendaId() { return vendaId; }
    public void setVendaId(int vendaId) { this.vendaId = vendaId; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
