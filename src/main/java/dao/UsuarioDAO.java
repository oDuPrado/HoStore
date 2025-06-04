package dao;

import model.UsuarioModel;
import util.DB;

import java.sql.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public void inserir(UsuarioModel u) throws SQLException {
        String sql = "INSERT INTO usuarios (id,nome,usuario,senha,tipo,ativo) VALUES (?,?,?,?,?,?)";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, u.getId());
            p.setString(2, u.getNome());
            p.setString(3, u.getUsuario());
            p.setString(4, hashSenha(u.getSenha()));
            p.setString(5, u.getTipo());
            p.setInt(6, u.isAtivo() ? 1 : 0);
            p.executeUpdate();
        }
    }

    public void atualizar(UsuarioModel u) throws SQLException {
        String sql = "UPDATE usuarios SET nome=?, usuario=?, senha=?, tipo=?, ativo=? WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, u.getNome());
            p.setString(2, u.getUsuario());
            p.setString(3, hashSenha(u.getSenha()));
            p.setString(4, u.getTipo());
            p.setInt(5, u.isAtivo() ? 1 : 0);
            p.setString(6, u.getId());
            p.executeUpdate();
        }
    }

    public UsuarioModel buscarPorUsuarioESenha(String usuario, String senha) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE usuario=? AND senha=? AND ativo=1";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, usuario);
            p.setString(2, hashSenha(senha));
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioModel(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("usuario"),
                        rs.getString("senha"),
                        rs.getString("tipo"),
                        rs.getInt("ativo") == 1
                    );
                }
            }
        }
        return null;
    }

    public List<UsuarioModel> listar() throws SQLException {
        String sql = "SELECT * FROM usuarios";
        List<UsuarioModel> lista = new ArrayList<>();
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                lista.add(new UsuarioModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("usuario"),
                    rs.getString("senha"),
                    rs.getString("tipo"),
                    rs.getInt("ativo") == 1
                ));
            }
        }
        return lista;
    }

    public void excluir(String id) throws SQLException {
        String sql = "UPDATE usuarios SET ativo=0 WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            p.executeUpdate();
        }
    }

    // utilitário de hash SHA-256
    private String hashSenha(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
