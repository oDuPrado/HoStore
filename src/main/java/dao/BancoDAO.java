package dao;

import model.BancoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BancoDAO {

    public void inserir(BancoModel b) throws SQLException {
        String sql = "INSERT INTO bancos (id,nome,agencia,conta,observacoes) VALUES (?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getId());
            ps.setString(2, b.getNome());
            ps.setString(3, b.getAgencia());
            ps.setString(4, b.getConta());
            ps.setString(5, b.getObservacoes());
            ps.executeUpdate();
        }
    }

    public void atualizar(BancoModel b) throws SQLException {
        String sql = "UPDATE bancos SET nome=?,agencia=?,conta=?,observacoes=? WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getNome());
            ps.setString(2, b.getAgencia());
            ps.setString(3, b.getConta());
            ps.setString(4, b.getObservacoes());
            ps.setString(5, b.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(String id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM bancos WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public BancoModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM bancos WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new BancoModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("agencia"),
                    rs.getString("conta"),
                    rs.getString("observacoes")
                );
            }
        }
    }

    public List<BancoModel> listarTodos() throws SQLException {
        List<BancoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM bancos ORDER BY nome";
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new BancoModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("agencia"),
                    rs.getString("conta"),
                    rs.getString("observacoes")
                ));
            }
        }
        return out;
    }
}
