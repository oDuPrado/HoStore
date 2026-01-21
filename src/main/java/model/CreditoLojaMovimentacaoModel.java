package model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa uma movimentação de crédito de loja:
 * entrada (crédito concedido) ou uso (desconto em venda).
 */
public class CreditoLojaMovimentacaoModel {
    private String id;           // UUID da movimentação
    private String clienteId;    // FK para clientes.id
    private double valor;        // valor movimentado
    private String tipo;         // "entrada" ou "uso"
    private String referencia;   // ex: ID da venda, devolução, motivo
    private String data;         // timestamp ISO
    private String eventoId;     // vínculo opcional com evento/torneio

    public CreditoLojaMovimentacaoModel() {
        this.id = UUID.randomUUID().toString();
        this.data = LocalDateTime.now().toString();
    }

    public CreditoLojaMovimentacaoModel(String clienteId, double valor,
                                        String tipo, String referencia) {
        this();
        this.clienteId = clienteId;
        this.valor = valor;
        this.tipo = tipo;
        this.referencia = referencia;
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }
}
