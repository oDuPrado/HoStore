package dao;

import model.VendaModel;
import util.DateUtils;

import java.sql.*;

public class VendaDAO {

    /**
     * Insere a venda usando a conexão da transação já aberta.
     * Retorna o ID gerado.
     */
    public int insert(VendaModel v, Connection c) throws SQLException {
        String sql = "INSERT INTO vendas(data_venda, cliente_id, total_bruto, total_liquido," +
                     " desconto, acrescimo, forma_pagamento, parcelas, status, criado_em, criado_por, juros, intervalo_dias) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getDataVenda());
            ps.setString(2, v.getClienteId());
            ps.setDouble(3, v.getTotalBruto());
            ps.setDouble(4, v.getTotalLiquido());
            ps.setDouble(5, v.getDesconto());
            ps.setDouble(6, v.getAcrescimo());
            ps.setString(7, v.getFormaPagamento());
            ps.setInt(8, v.getParcelas());
            ps.setString(9, v.getStatus());
            ps.setString(10, DateUtils.now());
            String usuario = (v.getUsuario() != null && !v.getUsuario().isBlank()) ? v.getUsuario() : "admin";
            ps.setString(11, usuario);
            ps.setDouble(12, v.getJuros());
            ps.setInt(13, v.getIntervaloDias());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    v.setId(keys.getInt(1));
                }
            }
        }
        return v.getId();
    }
}
