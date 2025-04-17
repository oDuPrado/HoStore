package dao;

import model.VendaModel;
import java.sql.*;
import util.DateUtils;

public class VendaDAO {
    /** Agora recebe a conexão já aberta e faz seu trabalho nela */
    public int insert(VendaModel v, Connection c) throws SQLException {
        String sql = "INSERT INTO vendas(data_venda, cliente_id, total_bruto, total_liquido, desconto,"
                   + " forma_pagamento, parcelas, status, criado_em, criado_por) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getDataVenda());
            ps.setString(2, v.getClienteId());
            ps.setDouble(3, v.getTotalBruto());
            ps.setDouble(4, v.getTotalLiquido());
            ps.setDouble(5, v.getDesconto());
            ps.setString(6, v.getFormaPagamento());
            ps.setInt(7, v.getParcelas());
            ps.setString(8, v.getStatus());
            ps.setString(9, DateUtils.now());
            ps.setString(10, "admin");

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
