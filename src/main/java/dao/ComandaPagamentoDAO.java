package dao;

import model.ComandaPagamentoModel;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ComandaPagamentoDAO {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int inserir(ComandaPagamentoModel pg, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO comandas_pagamentos (comanda_id, tipo, valor, data, usuario)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pg.getComandaId());
            ps.setString(2, pg.getTipo());
            ps.setDouble(3, pg.getValor());
            ps.setString(4, pg.getData().format(FMT));
            ps.setString(5, pg.getUsuario());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Falha ao gerar ID do pagamento da comanda.");
    }

    public List<ComandaPagamentoModel> listarPorComanda(int comandaId, Connection conn) throws SQLException {
        List<ComandaPagamentoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM comandas_pagamentos WHERE comanda_id=? ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, comandaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private ComandaPagamentoModel map(ResultSet rs) throws SQLException {
        ComandaPagamentoModel p = new ComandaPagamentoModel();
        p.setId(rs.getInt("id"));
        p.setComandaId(rs.getInt("comanda_id"));
        p.setTipo(rs.getString("tipo"));
        p.setValor(rs.getDouble("valor"));
        p.setUsuario(rs.getString("usuario"));

        String dt = rs.getString("data");
        p.setData(dt == null ? null : LocalDateTime.parse(dt, FMT));
        return p;
    }
}
