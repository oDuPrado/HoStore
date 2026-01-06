// src/main/java/dao/FiscalCatalogDAO.java
package dao;

import model.CodigoDescricaoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FiscalCatalogDAO {

    public List<CodigoDescricaoModel> findAll(String tableName) throws SQLException {
        assertAllowedTable(tableName);

        List<CodigoDescricaoModel> out = new ArrayList<>();
        String sql = "SELECT codigo, descricao FROM " + tableName + " ORDER BY codigo ASC";

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new CodigoDescricaoModel(rs.getString("codigo"), rs.getString("descricao")));
            }
        }
        return out;
    }

    public void upsertAll(Connection c, String tableName, List<CodigoDescricaoModel> items) throws SQLException {
        assertAllowedTable(tableName);

        String sql = "INSERT OR REPLACE INTO " + tableName + " (codigo, descricao) VALUES (?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (CodigoDescricaoModel it : items) {
                ps.setString(1, it.getCodigo());
                ps.setString(2, it.getDescricao());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public boolean deleteByCodigo(Connection c, String tableName, String codigo) throws SQLException {
        assertAllowedTable(tableName);

        String sql = "DELETE FROM " + tableName + " WHERE codigo = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo);
            int n = ps.executeUpdate();
            return n > 0;
        }
    }

    private void assertAllowedTable(String tableName) throws SQLException {
        // bloqueia qualquer gracinha e evita SQL injection via nome de tabela
        if (!"ncm".equals(tableName)
                && !"cfop".equals(tableName)
                && !"csosn".equals(tableName)
                && !"origem".equals(tableName)
                && !"unidades".equals(tableName)) {
            throw new SQLException("Tabela não permitida no catálogo fiscal: " + tableName);
        }
    }
}
