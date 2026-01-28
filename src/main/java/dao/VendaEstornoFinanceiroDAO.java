package dao;

import util.DB;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class VendaEstornoFinanceiroDAO {

    /**
     * Insere um registro de estorno financeiro
     * @param pagamentoId  ID da linha em vendas_pagamentos
     * @param vendaId      ID da venda original
     * @param valor        Valor estornado (deve ser ? valorPago - j? estornado)
     * @param data         LocalDate do estorno
     * @param observacao   Texto livre (ex: "Estorno manual")
     */
    public void inserirEstorno(int pagamentoId, int vendaId, double valor, LocalDate data, String observacao,
                               String tipoEstorno, String taxaQuem) throws SQLException {
        String sql = "INSERT INTO vendas_estornos_pagamentos " +
                "(pagamento_id, venda_id, tipo_pagamento, valor_estornado, data, observacao, criado_em, criado_por, tipo_estorno, taxa_quem) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Inicialmente, vamos buscar o tipo de pagamento na pr?pria tabela vendas_pagamentos
        String tipo = "";
        try (Connection c = DB.get();
             PreparedStatement psTipo = c.prepareStatement(
                     "SELECT tipo FROM vendas_pagamentos WHERE id = ?")) {
            psTipo.setInt(1, pagamentoId);
            try (ResultSet rs = psTipo.executeQuery()) {
                if (rs.next()) {
                    tipo = rs.getString("tipo");
                }
            }
        }

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pagamentoId);
            ps.setInt(2, vendaId);
            ps.setString(3, tipo);
            ps.setDouble(4, valor);
            ps.setString(5, data.toString());
            ps.setString(6, observacao);
            ps.setString(7, LocalDateTime.now().toString());
            ps.setString(8, "sistema"); // ou usu?rio logado
            ps.setString(9, tipoEstorno);
            ps.setString(10, taxaQuem);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna a soma de todos os estornos j? feitos para um dado pagamento (vendas_pagamentos.id).
     */
    public double obterTotalEstornadoPorPagamento(int pagamentoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor_estornado), 0) AS total FROM vendas_estornos_pagamentos " +
                     "WHERE pagamento_id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pagamentoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    public double obterTotalEstornadoPorPagamentoTipo(int pagamentoId, String tipoEstorno) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor_estornado), 0) AS total FROM vendas_estornos_pagamentos " +
                     "WHERE pagamento_id = ? AND COALESCE(tipo_estorno,'LIQUIDO') = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pagamentoId);
            ps.setString(2, tipoEstorno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }
}
