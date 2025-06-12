package model;

/**
 * Define em qual alvo a promoção incidirá:
 * PRODUTO      — apenas produto específico
 * CATEGORIA    — todos os produtos de uma categoria
 * CLIENTE_VIP  — apenas para clientes marcados como VIP
 */
public enum AplicaEm {
    PRODUTO,
    CATEGORIA,
    CLIENTE_VIP
}
