package model;

/**
 * Define como o desconto será interpretado:
 * PORCENTAGEM — desconto percentual (ex: 10.0 equivale a 10%)
 * VALOR        — desconto em valor fixo (ex: 5.0 equivale a R$5,00)
 */
public enum TipoDesconto {
    PORCENTAGEM,
    VALOR
}
