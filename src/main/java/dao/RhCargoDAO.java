package dao;

import model.RhCargoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RhCargoDAO {

    public String inserir(RhCargoModel m) throws SQLException {
        if (m.getId() == null || m.getId().isBlank()) {
            m.setId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO rh_cargos(id, nome, descricao, salario_base, ativo, criado_em) VALUES (?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getNome());
            ps.setString(3, m.getDescricao());
            ps.setDouble(4, m.getSalarioBase());
            ps.setInt(5, m.getAtivo());
            ps.executeUpdate();
        }
        return m.getId();
    }

    public void atualizar(RhCargoModel m) throws SQLException {
        String sql = "UPDATE rh_cargos SET nome=?, descricao=?, salario_base=?, ativo=?, alterado_em=datetime('now') WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getNome());
            ps.setString(2, m.getDescricao());
            ps.setDouble(3, m.getSalarioBase());
            ps.setInt(4, m.getAtivo());
            ps.setString(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(String id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_cargos WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public RhCargoModel buscarPorId(String id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM rh_cargos WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                RhCargoModel m = new RhCargoModel();
                m.setId(rs.getString("id"));
                m.setNome(rs.getString("nome"));
                m.setDescricao(rs.getString("descricao"));
                m.setSalarioBase(rs.getDouble("salario_base"));
                m.setAtivo(rs.getInt("ativo"));
                return m;
            }
        }
    }

    public List<RhCargoModel> listar() throws SQLException {
        List<RhCargoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM rh_cargos ORDER BY nome";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RhCargoModel m = new RhCargoModel();
                m.setId(rs.getString("id"));
                m.setNome(rs.getString("nome"));
                m.setDescricao(rs.getString("descricao"));
                m.setSalarioBase(rs.getDouble("salario_base"));
                m.setAtivo(rs.getInt("ativo"));
                out.add(m);
            }
        }
        return out;
    }
}
