// procure no seu projeto: src/dao/AcessorioDAO.java
package dao;

import util.DB;
import model.AcessorioModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AcessorioDAO {

    /** Lista todos os acessórios (usa LEFT JOIN para trazer o nome do fornecedor) */
    public List<AcessorioModel> listar() throws SQLException {
        List<AcessorioModel> lista = new ArrayList<>();
        String sql = "SELECT a.*, f.nome AS fornecedor_nome " +
                     "FROM acessorios a " +
                     "LEFT JOIN fornecedores f ON a.fornecedor_id = f.id";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AcessorioModel a = mapRow(rs);
                lista.add(a);
            }
        }
        return lista;
    }

    /** Busca um acessório pelo ID */
    public AcessorioModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT a.*, f.nome AS fornecedor_nome " +
                     "FROM acessorios a " +
                     "LEFT JOIN fornecedores f ON a.fornecedor_id = f.id " +
                     "WHERE a.id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /** Insere um novo acessório */
    public void salvar(AcessorioModel a) throws SQLException {
        String sql = "INSERT INTO acessorios " +
                     "(id, nome, tipo, arte, cor, quantidade, custo, preco_venda, fornecedor_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getNome());
            ps.setString(3, a.getCategoria());
            ps.setString(4, a.getArte());
            ps.setString(5, a.getCor());
            ps.setInt(6, a.getQuantidade());
            ps.setDouble(7, a.getPrecoCompra());
            ps.setDouble(8, a.getPrecoVenda());
            ps.setString(9, a.getFornecedorId());
            ps.executeUpdate();
        }
    }

    /** Atualiza um acessório existente */
    public void atualizar(AcessorioModel a) throws SQLException {
        String sql = "UPDATE acessorios SET " +
                     "nome=?, tipo=?, arte=?, cor=?, quantidade=?, custo=?, preco_venda=?, fornecedor_id=? " +
                     "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getNome());
            ps.setString(2, a.getCategoria());
            ps.setString(3, a.getArte());
            ps.setString(4, a.getCor());
            ps.setInt(5, a.getQuantidade());
            ps.setDouble(6, a.getPrecoCompra());
            ps.setDouble(7, a.getPrecoVenda());
            ps.setString(8, a.getFornecedorId());
            ps.setString(9, a.getId());
            ps.executeUpdate();
        }
    }

    /** Remove um acessório */
    public void remover(String id) throws SQLException {
        String sql = "DELETE FROM acessorios WHERE id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /** Mapeia um ResultSet para AcessorioModel */
    private AcessorioModel mapRow(ResultSet rs) throws SQLException {
        AcessorioModel a = new AcessorioModel(
            rs.getString("id"),
            rs.getString("nome"),
            rs.getInt("quantidade"),
            rs.getDouble("custo"),
            rs.getDouble("preco_venda"),
            rs.getString("fornecedor_id"),
            rs.getString("tipo"),
            rs.getString("arte"),
            rs.getString("cor")
        );
        a.setFornecedorNome(rs.getString("fornecedor_nome"));
        return a;
    }
}
