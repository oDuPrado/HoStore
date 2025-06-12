package dao;

import model.TipoPromocaoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TipoPromocaoDAO {

    public void inserir(TipoPromocaoModel tipo) throws Exception {
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO tipos_promocao (id, nome, descricao) VALUES (?, ?, ?)")) {
            ps.setString(1, tipo.getId());
            ps.setString(2, tipo.getNome());
            ps.setString(3, tipo.getDescricao());
            ps.execute();
        }
    }

    public void atualizar(TipoPromocaoModel tipo) throws Exception {
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "UPDATE tipos_promocao SET nome = ?, descricao = ? WHERE id = ?")) {
            ps.setString(1, tipo.getNome());
            ps.setString(2, tipo.getDescricao());
            ps.setString(3, tipo.getId());
            ps.execute();
        }
    }

    public void excluir(String id) throws Exception {
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM tipos_promocao WHERE id = ?")) {
            ps.setString(1, id);
            ps.execute();
        }
    }

    public List<TipoPromocaoModel> listarTodos() throws Exception {
        List<TipoPromocaoModel> lista = new ArrayList<>();
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM tipos_promocao ORDER BY nome");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TipoPromocaoModel tipo = new TipoPromocaoModel();
                tipo.setId(rs.getString("id"));
                tipo.setNome(rs.getString("nome"));
                tipo.setDescricao(rs.getString("descricao"));
                lista.add(tipo);
            }
        }
        return lista;
    }

    public TipoPromocaoModel buscarPorId(String id) throws Exception {
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM tipos_promocao WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TipoPromocaoModel tipo = new TipoPromocaoModel();
                    tipo.setId(rs.getString("id"));
                    tipo.setNome(rs.getString("nome"));
                    tipo.setDescricao(rs.getString("descricao"));
                    return tipo;
                }
            }
        }
        return null;
    }

}
