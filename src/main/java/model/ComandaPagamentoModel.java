package model;

import java.time.LocalDateTime;

public class ComandaPagamentoModel {
    private Integer id;
    private Integer comandaId;

    private String tipo;
    private double valor;

    private LocalDateTime data;
    private String usuario;

    public ComandaPagamentoModel() {}

    // Getters/Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getComandaId() { return comandaId; }
    public void setComandaId(Integer comandaId) { this.comandaId = comandaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
