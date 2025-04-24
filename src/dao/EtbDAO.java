package dao;

import model.EtbModel;
import util.DB;

import java.sql.*;

/**
 * DAO para ETB, estendendo ProdutoDAO e sincronizando detalhes em `etbs`.
 */
public class EtbDAO extends ProdutoDAO {

    /** Insere resumo em produtos + detalhes em etbs. */
    public void insert(EtbModel e) throws SQLException {
        // 1) resumo
        super.insert(e);

        // 2) detalhes
        String sql = "INSERT INTO etbs "
                   + "(id, fornecedor, serie, colecao, tipo, versao) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getFornecedor());
            ps.setString(3, e.getSerie());
            ps.setString(4, e.getColecao());
            ps.setString(5, e.getTipo());
            ps.setString(6, e.getVersao());
            ps.executeUpdate();
        }
    }

    /** Atualiza produtos + detalhes em etbs. */
    public void update(EtbModel e) throws SQLException {
        super.update(e);

        String sql = "UPDATE etbs SET "
                   + "fornecedor = ?, serie = ?, colecao = ?, tipo = ?, versao = ? "
                   + "WHERE id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, e.getFornecedor());
            ps.setString(2, e.getSerie());
            ps.setString(3, e.getColecao());
            ps.setString(4, e.getTipo());
            ps.setString(5, e.getVersao());
            ps.setString(6, e.getId());
            ps.executeUpdate();
        }
    }

    /** Remove de produtos + etbs. */
    public void delete(String id) throws SQLException {
        super.delete(id);
        try (PreparedStatement ps = DB.get().prepareStatement(
                "DELETE FROM etbs WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /** Busca um ETB completo (produtos + detalhes). */
    public EtbModel buscarPorId(String id) throws SQLException {
        String sql = ""
            + "SELECT p.id, p.nome, p.quantidade, p.preco_compra, p.preco_venda, "
            + "       e.fornecedor, e.serie, e.colecao, e.tipo, e.versao "
            + "FROM produtos p "
            + "JOIN etbs e ON p.id = e.id "
            + "WHERE p.id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new EtbModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getInt("quantidade"),
                    rs.getDouble("preco_compra"),
                    rs.getDouble("preco_venda"),
                    rs.getString("fornecedor"),
                    rs.getString("serie"),
                    rs.getString("colecao"),
                    rs.getString("tipo"),
                    rs.getString("versao")
                );
            }
        }
    }
}
