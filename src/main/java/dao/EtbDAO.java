package dao;

import model.EtbModel;
import util.DB;

import java.sql.*;

/**
 * DAO para ETB, estendendo ProdutoDAO e sincronizando detalhes em `etbs`, com suporte a jogo_id
 * e código de barras (armazenado na tabela `produtos`).
 */
public class EtbDAO extends ProdutoDAO {

    /**
     * Insere resumo em produtos (incluindo código de barras) + detalhes em etbs.
     */
    public void insert(EtbModel e) throws SQLException {
        // 1) resumo em produtos (ProdutoDAO.insert já cuida de gravar o código de barras)
        super.insert(e);

        // 2) detalhes em etbs
        String sql = "INSERT INTO etbs " +
                     "(id, fornecedor, serie, colecao, tipo, versao, jogo_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getFornecedor());
            ps.setString(3, e.getSerie());
            ps.setString(4, e.getColecao());
            ps.setString(5, e.getTipo());
            ps.setString(6, e.getVersao());
            ps.setString(7, e.getJogoId());
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza produtos (incluindo código de barras) + detalhes em etbs.
     */
    public void update(EtbModel e) throws SQLException {
        // 1) atualiza tabela produtos (ProdutoDAO.update já cuida de atualizar o código de barras)
        super.update(e);

        // 2) atualiza tabela etbs
        String sql = "UPDATE etbs SET " +
                     "fornecedor = ?, serie = ?, colecao = ?, tipo = ?, versao = ?, jogo_id = ? " +
                     "WHERE id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, e.getFornecedor());
            ps.setString(2, e.getSerie());
            ps.setString(3, e.getColecao());
            ps.setString(4, e.getTipo());
            ps.setString(5, e.getVersao());
            ps.setString(6, e.getJogoId());
            ps.setString(7, e.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Remove ETB de produtos + etbs.
     */
    public void delete(String id) throws SQLException {
        // 1) remove da tabela produtos
        super.delete(id);

        // 2) remove da tabela etbs
        try (PreparedStatement ps = DB.get().prepareStatement(
                "DELETE FROM etbs WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Busca um EtbModel completo (produtos + detalhes).
     * Inclui o campo código_barras vindo de `produtos`.
     */
    public EtbModel buscarPorId(String id) throws SQLException {
        String sql = ""
            + "SELECT p.id, p.nome, p.quantidade, p.preco_compra, p.preco_venda, p.codigo_barras, "
            + "       e.fornecedor, e.serie, e.colecao, e.tipo, e.versao, e.jogo_id "
            + "FROM produtos p "
            + "JOIN etbs e ON p.id = e.id "
            + "WHERE p.id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                EtbModel e = new EtbModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getInt("quantidade"),
                    rs.getDouble("preco_compra"),
                    rs.getDouble("preco_venda"),
                    rs.getString("fornecedor"),
                    rs.getString("serie"),
                    rs.getString("colecao"),
                    rs.getString("tipo"),
                    rs.getString("versao"),
                    rs.getString("jogo_id")
                );
                e.setCodigoBarras(rs.getString("codigo_barras"));
                return e;
            }
        }
    }
}
