package dao;

import model.ProdutoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    /* ==================== CRUD BÁSICO ==================== */

    public void insert(ProdutoModel p) throws SQLException {
        String sql = "INSERT INTO produtos " +
                     "(id, nome, categoria, quantidade, preco_compra, preco_venda, fornecedor, criado_em, alterado_em) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            bind(ps, p);
            ps.executeUpdate();
        }
    }

    public void update(ProdutoModel p) throws SQLException {
        String sql = "UPDATE produtos SET nome=?, categoria=?, quantidade=?, preco_compra=?, " +
                     "preco_venda=?, fornecedor=?, alterado_em=? WHERE id=?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getCategoria());
            ps.setInt   (3, p.getQuantidade());
            ps.setDouble(4, p.getPrecoCompra());
            ps.setDouble(5, p.getPrecoVenda());
            ps.setString(6, p.getFornecedor());
            ps.setString(7, p.getAlteradoEm().toString());
            ps.setString(8, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = DB.get()
                 .prepareStatement("DELETE FROM produtos WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public ProdutoModel findById(String id) {
        String sql = "SELECT * FROM produtos WHERE id=?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<ProdutoModel> listAll() {
        List<ProdutoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM produtos ORDER BY nome";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(map(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    /* ==================== AJUDANTES ==================== */

    private ProdutoModel map(ResultSet rs) throws SQLException {
        ProdutoModel p = new ProdutoModel(
            rs.getString("id"),
            rs.getString("nome"),
            rs.getString("categoria"),
            rs.getInt   ("quantidade"),
            rs.getDouble("preco_compra"),
            rs.getDouble("preco_venda"),
            rs.getString("fornecedor")
        );
        return p;
    }

    private void bind(PreparedStatement ps, ProdutoModel p) throws SQLException {
        ps.setString(1, p.getId());
        ps.setString(2, p.getNome());
        ps.setString(3, p.getCategoria());
        ps.setInt   (4, p.getQuantidade());
        ps.setDouble(5, p.getPrecoCompra());
        ps.setDouble(6, p.getPrecoVenda());
        ps.setString(7, p.getFornecedor());
        ps.setString(8, p.getCriadoEm().toString());
        ps.setString(9, p.getAlteradoEm().toString());
    }
}
