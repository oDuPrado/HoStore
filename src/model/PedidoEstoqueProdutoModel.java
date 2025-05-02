package model;

public class PedidoEstoqueProdutoModel {
    private String id;
    private String pedidoId;
    private String produtoId;
    private int quantidadePedida;
    private int quantidadeRecebida;
    private String status; // pendente, parcial, completo

    public PedidoEstoqueProdutoModel(String id,
                                     String pedidoId,
                                     String produtoId,
                                     int quantidadePedida,
                                     int quantidadeRecebida,
                                     String status) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.produtoId = produtoId;
        this.quantidadePedida = quantidadePedida;
        this.quantidadeRecebida = quantidadeRecebida;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public int getQuantidadePedida() { return quantidadePedida; }
    public void setQuantidadePedida(int quantidadePedida) { this.quantidadePedida = quantidadePedida; }

    public int getQuantidadeRecebida() { return quantidadeRecebida; }
    public void setQuantidadeRecebida(int quantidadeRecebida) { this.quantidadeRecebida = quantidadeRecebida; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
