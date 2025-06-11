package model;

import java.util.UUID;

/**
 * Representa o saldo de crédito de loja de um cliente.
 */
public class CreditoLojaModel {
    private String id;         // UUID da linha
    private String clienteId;  // FK para clientes.id
    private double valor;      // saldo atual

    public CreditoLojaModel() {
        // Gera ID único automaticamente
        this.id = UUID.randomUUID().toString();
        this.valor = 0.0;
    }

    public CreditoLojaModel(String clienteId, double valor) {
        this();  // cria UUID
        this.clienteId = clienteId;
        this.valor = valor;
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}
