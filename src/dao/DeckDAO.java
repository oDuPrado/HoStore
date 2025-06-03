package dao;

import model.DeckModel;
import util.DB;

import java.sql.*;

/**
 * DAO para Decks, estendendo as operações padrão de ProdutoDAO
 * e sincronizando detalhes na tabela `decks`, agora com `jogo_id`.
 */
public class DeckDAO extends ProdutoDAO {

    /** Insere novo deck em `produtos` e em `decks`. */
    public void insert(DeckModel d) throws SQLException {
        // 1) insere resumo na tabela produtos
        super.insert(d);

        // 2) insere detalhes na tabela decks, incluindo jogo_id
        String sql = "INSERT INTO decks " +
                     "(id, fornecedor, colecao, tipo_deck, categoria, jogo_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getFornecedor());
            ps.setString(3, d.getColecao());
            ps.setString(4, d.getTipoDeck());
            ps.setString(5, d.getCategoria());
            ps.setString(6, d.getJogoId()); // NOVO
            ps.executeUpdate();
        }
    }

    /** Atualiza resumo e detalhes do deck, incluindo `jogo_id`. */
    public void update(DeckModel d) throws SQLException {
        // 1) atualiza tabela produtos
        super.update(d);

        // 2) atualiza tabela decks
        String sql = "UPDATE decks SET " +
                     "fornecedor = ?, colecao = ?, tipo_deck = ?, categoria = ?, jogo_id = ? " +
                     "WHERE id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, d.getFornecedor());
            ps.setString(2, d.getColecao());
            ps.setString(3, d.getTipoDeck());
            ps.setString(4, d.getCategoria());
            ps.setString(5, d.getJogoId()); // NOVO
            ps.setString(6, d.getId());
            ps.executeUpdate();
        }
    }

    /** Remove deck de ambas as tabelas. */
    public void delete(String id) throws SQLException {
        // 1) remove da tabela produtos
        super.delete(id);

        // 2) remove da tabela decks
        try (PreparedStatement ps = DB.get().prepareStatement(
                "DELETE FROM decks WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /** Busca um DeckModel completo (resumo + detalhes). */
    public DeckModel buscarPorId(String id) throws SQLException {
        String sql = ""
            + "SELECT p.id, p.nome, p.quantidade, p.preco_compra, p.preco_venda, "
            + "       d.fornecedor, d.colecao, d.tipo_deck, d.categoria, d.jogo_id "
            + "FROM produtos p "
            + "JOIN decks d ON p.id = d.id "
            + "WHERE p.id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new DeckModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getInt("quantidade"),
                    rs.getDouble("preco_compra"),
                    rs.getDouble("preco_venda"),
                    rs.getString("fornecedor"),
                    rs.getString("colecao"),
                    rs.getString("tipo_deck"),
                    rs.getString("categoria"),
                    rs.getString("jogo_id") // NOVO
                );
            }
        }
    }
}
