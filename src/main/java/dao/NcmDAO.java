// src/main/java/dao/NcmDAO.java
package dao;

import model.NcmModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por operações CRUD na tabela 'ncm'.
 */
public class NcmDAO {

    /**
     * Exclui todos os registros da tabela NCM.
     */
    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM ncm";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Insere um novo NCM.
     */
    public void insert(NcmModel ncm) throws SQLException {
        String sql = "INSERT INTO ncm (codigo, descricao) VALUES (?, ?)";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ncm.getCodigo());
            ps.setString(2, ncm.getDescricao());
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza um NCM existente.
     */
    public void update(NcmModel ncm) throws SQLException {
        String sql = "UPDATE ncm SET descricao = ? WHERE codigo = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ncm.getDescricao());
            ps.setString(2, ncm.getCodigo());
            ps.executeUpdate();
        }
    }

    /**
     * Remove um NCM específico.
     */
    public void delete(String codigo) throws SQLException {
        String sql = "DELETE FROM ncm WHERE codigo = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna todos os registros de NCM.
     */
    public List<NcmModel> findAll() throws SQLException {
        List<NcmModel> list = new ArrayList<>();
        String sql = "SELECT codigo, descricao FROM ncm ORDER BY codigo";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new NcmModel(
                    rs.getString("codigo"),
                    rs.getString("descricao")
                ));
            }
        }
        return list;
    }
}