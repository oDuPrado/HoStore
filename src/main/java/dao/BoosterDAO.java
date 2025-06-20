package dao;

import model.BoosterModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DAO para boosters. Lida com inserção, atualização e leitura de boosters,
 * integrando dados entre a tabela genérica de produtos e a tabela específica boosters.
 */
public class BoosterDAO extends ProdutoDAO {

    /**
     * Insere ou atualiza um booster:
     *  - tabela produtos (via super.insert/super.update)
     *  - tabela boosters (com jogo_id incluído)
     */
    public void insert(BoosterModel b) throws Exception {
        // Verifica se já existe produto com mesmo ID
        if (super.findById(b.getId()) == null) {
            super.insert(b);
        } else {
            super.update(b);
        }

        // Insere ou atualiza os dados específicos do booster
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR REPLACE INTO boosters (" +
                             "id, nome, serie, colecao, tipo, idioma, codigo_barras, quantidade, custo, preco_venda, fornecedor_id, jogo_id" +
                             ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
             )) {

            ps.setString(1, b.getId());
            ps.setString(2, b.getNome());
            ps.setString(3, b.getSet());            // série
            ps.setString(4, b.getColecao());
            ps.setString(5, b.getTipoBooster());
            ps.setString(6, b.getIdioma());
            ps.setString(7, b.getCodigoBarras());
            ps.setInt(8, b.getQuantidade());
            ps.setDouble(9, b.getPrecoCompra());
            ps.setDouble(10, b.getPrecoVenda());
            ps.setString(11, b.getFornecedor());     // id do fornecedor
            ps.setString(12, b.getJogoId());         // NOVO: id do jogo

            ps.executeUpdate();
        }
    }

    /**
     * Busca um booster pelo ID.
     * @param id ID do produto
     * @return BoosterModel populado ou null se não encontrado
     */
    public BoosterModel buscarPorId(String id) {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT p.id, p.nome, p.quantidade, p.preco_compra, p.preco_venda, " +
                             "b.serie, b.colecao, b.tipo, b.idioma, b.data_lancamento, " +
                             "b.codigo_barras, b.fornecedor_id, b.jogo_id " +
                             "FROM produtos p " +
                             "LEFT JOIN boosters b ON p.id = b.id " +
                             "WHERE p.id = ?"
             )) {

            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new BoosterModel(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getInt("quantidade"),
                        rs.getDouble("preco_compra"),
                        rs.getDouble("preco_venda"),
                        rs.getString("fornecedor_id"),
                        rs.getString("colecao"),
                        rs.getString("serie"),
                        rs.getString("tipo"),
                        rs.getString("idioma"),
                        rs.getString("data_lancamento"),
                        rs.getString("codigo_barras"),
                        rs.getString("jogo_id") // NOVO
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
