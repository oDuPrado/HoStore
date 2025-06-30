package dao;

import model.ProdutoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    /* ==================== CRUD BÁSICO ==================== */

    /**
     * Insere um novo ProdutoModel em "produtos", incluindo o campo `codigo_barras`.
     */
    public void insert(ProdutoModel p) throws SQLException {
        String sql = "INSERT INTO produtos " +
    "(id, nome, jogo_id, tipo, quantidade, preco_compra, preco_venda, codigo_barras, " +
    "ncm, cfop, csosn, origem, unidade, criado_em, alterado_em) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            bindInsert(ps, p);
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza o ProdutoModel em "produtos", incluindo o campo `codigo_barras`.
     */
    public void update(ProdutoModel p) throws SQLException {
        String sql = "UPDATE produtos SET " +
    "nome = ?, jogo_id = ?, tipo = ?, quantidade = ?, preco_compra = ?, preco_venda = ?, " +
    "codigo_barras = ?, ncm=?, cfop=?, csosn=?, origem=?, unidade=?, alterado_em = ? " +
    "WHERE id = ?";

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
           ps.setString(1, p.getNome());
ps.setString(2, p.getJogoId());
ps.setString(3, p.getTipo());
ps.setInt(4, p.getQuantidade());
ps.setDouble(5, p.getPrecoCompra());
ps.setDouble(6, p.getPrecoVenda());
ps.setString(7, p.getCodigoBarras());
ps.setString(8, p.getNcm());
ps.setString(9, p.getCfop());
ps.setString(10, p.getCsosn());
ps.setString(11, p.getOrigem());
ps.setString(12, p.getUnidade());
ps.setString(13, p.getAlteradoEm().toString());
ps.setString(14, p.getId());
        }
    }

    /**
     * Atualiza o ProdutoModel em "produtos" usando uma Connection externa,
     * incluindo o campo `codigo_barras`.
     */
    public void update(ProdutoModel p, Connection c) throws SQLException {
        String sql = "UPDATE produtos SET " +
    "nome = ?, jogo_id = ?, tipo = ?, quantidade = ?, preco_compra = ?, preco_venda = ?, " +
    "codigo_barras = ?, ncm=?, cfop=?, csosn=?, origem=?, unidade=?, alterado_em = ? " +
    "WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
ps.setString(2, p.getJogoId());
ps.setString(3, p.getTipo());
ps.setInt(4, p.getQuantidade());
ps.setDouble(5, p.getPrecoCompra());
ps.setDouble(6, p.getPrecoVenda());
ps.setString(7, p.getCodigoBarras());
ps.setString(8, p.getNcm());
ps.setString(9, p.getCfop());
ps.setString(10, p.getCsosn());
ps.setString(11, p.getOrigem());
ps.setString(12, p.getUnidade());
ps.setString(13, p.getAlteradoEm().toString());
ps.setString(14, p.getId());
        }
    }

    /**
     * Remove o ProdutoModel de "produtos".
     */
    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = DB.get().prepareStatement(
                "DELETE FROM produtos WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Busca um ProdutoModel completo por ID. Inclui `codigo_barras`.
     */
    public ProdutoModel findById(String id) {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lista todos os produtos ordenados por nome. Inclui `codigo_barras`.
     */
    public List<ProdutoModel> listAll() {
        List<ProdutoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM produtos ORDER BY nome";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /* ==================== AJUDANTES ==================== */

    /**
     * Constrói um ProdutoModel a partir de um ResultSet.
     * Lê o campo `codigo_barras` junto com os demais.
     */
    private ProdutoModel map(ResultSet rs) throws SQLException {
        ProdutoModel p = new ProdutoModel(
                rs.getString("id"),
                rs.getString("nome"),
                rs.getString("tipo"),
                rs.getInt("quantidade"),
                rs.getDouble("preco_compra"),
                rs.getDouble("preco_venda"));
        p.setJogoId(rs.getString("jogo_id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNcm(rs.getString("ncm"));
p.setCfop(rs.getString("cfop"));
p.setCsosn(rs.getString("csosn"));
p.setOrigem(rs.getString("origem"));
p.setUnidade(rs.getString("unidade"));
        return p;
    }

    /**
     * Prepara o PreparedStatement para inserir um ProdutoModel,
     * incluindo `codigo_barras`, `criado_em` e `alterado_em`.
     */
    private void bindInsert(PreparedStatement ps, ProdutoModel p) throws SQLException {
    ps.setString(1, p.getId());
    ps.setString(2, p.getNome());
    ps.setString(3, p.getJogoId());
    ps.setString(4, p.getTipo());
    ps.setInt(5, p.getQuantidade());
    ps.setDouble(6, p.getPrecoCompra());
    ps.setDouble(7, p.getPrecoVenda());
    ps.setString(8, p.getCodigoBarras());
    ps.setString(9, p.getNcm());
    ps.setString(10, p.getCfop());
    ps.setString(11, p.getCsosn());
    ps.setString(12, p.getOrigem());
    ps.setString(13, p.getUnidade());
    ps.setString(14, p.getCriadoEm().toString());
    ps.setString(15, p.getAlteradoEm().toString());
}
}
