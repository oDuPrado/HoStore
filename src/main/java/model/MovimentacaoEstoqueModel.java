// Procure no seu projeto (Ctrl+F) por "estoque_movimentacoes(" e cole isto em src/model/MovimentacaoEstoqueModel.java
package model;

import java.time.LocalDateTime;

public class MovimentacaoEstoqueModel {
    private Integer id;
    private String produtoId;
    private Integer loteId;
    private String tipoMov;      // "entrada", "saida", "ajuste", etc.
    private int quantidade;
    private String motivo;       // ex: "Venda #1234", "Ajuste manual", etc.
    private LocalDateTime data;  // timestamp da movimentação
    private String usuario;      // login ou nome do usuário
    private String eventoId;     // vínculo opcional com evento/torneio

    // Construtor para inserção
    public MovimentacaoEstoqueModel(String produtoId, String tipoMov, int quantidade,
                                    String motivo, String usuario) {
        this.produtoId = produtoId;
        this.tipoMov = tipoMov;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.usuario = usuario;
    }

    public MovimentacaoEstoqueModel(String produtoId, Integer loteId, String tipoMov, int quantidade,
                                    String motivo, String usuario) {
        this.produtoId = produtoId;
        this.loteId = loteId;
        this.tipoMov = tipoMov;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.usuario = usuario;
    }

    // Construtor completo (incluindo id e data)
    public MovimentacaoEstoqueModel(Integer id, String produtoId, String tipoMov,
                                    int quantidade, String motivo,
                                    LocalDateTime data, String usuario) {
        this.id = id;
        this.produtoId = produtoId;
        this.loteId = null;
        this.tipoMov = tipoMov;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.data = data;
        this.usuario = usuario;
    }

    public MovimentacaoEstoqueModel(Integer id, String produtoId, Integer loteId, String tipoMov,
                                    int quantidade, String motivo,
                                    LocalDateTime data, String usuario) {
        this.id = id;
        this.produtoId = produtoId;
        this.loteId = loteId;
        this.tipoMov = tipoMov;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.data = data;
        this.usuario = usuario;
    }

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public Integer getLoteId() { return loteId; }
    public void setLoteId(Integer loteId) { this.loteId = loteId; }

    public String getTipoMov() { return tipoMov; }
    public void setTipoMov(String tipoMov) { this.tipoMov = tipoMov; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }
}
