package dao;

import model.BoosterModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BoosterDAO extends ProdutoDAO {

    /**
     * Insere ou atualiza um booster:
     *  - tabela produtos (super.insert)
     *  - tabela produtos_detalhes (colecao, set, tipo, idioma, validade, codigo_barras)
     */
    public void insert(BoosterModel b) throws Exception {
        // salva campos genéricos na tabela produtos
        super.insert(b);

        // depois, insere/atualiza detalhes específicos
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
               "INSERT OR REPLACE INTO produtos_detalhes " +
               "(id, colecao, subtipo, set_especifico, idioma, validade, codigo_barras) " +
               "VALUES (?,?,?,?,?,?,?)"
             )) {
            ps.setString(1, b.getId());
            ps.setString(2, b.getColecao());
            ps.setString(3, b.getTipoBooster());
            ps.setString(4, b.getSet());
            ps.setString(5, b.getIdioma());
            ps.setString(6, b.getValidade());
            ps.setString(7, b.getCodigoBarras());
            ps.executeUpdate();
        }
    }
    public BoosterModel buscarPorId(String id) {
    try (Connection c = DB.get();
         PreparedStatement ps = c.prepareStatement(
             "SELECT p.id, p.nome, p.quantidade, p.preco_compra, p.preco_venda, p.fornecedor, " +
             "d.colecao, d.set_especifico, d.subtipo, d.idioma, d.validade, d.codigo_barras " +
             "FROM produtos p " +
             "LEFT JOIN produtos_detalhes d ON p.id = d.id " +
             "WHERE p.id = ?")) {

        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new BoosterModel(
                rs.getString("id"),
                rs.getString("nome"),
                rs.getInt("quantidade"),
                rs.getDouble("preco_compra"),
                rs.getDouble("preco_venda"),
                rs.getString("fornecedor"),
                rs.getString("colecao"),
                rs.getString("set_especifico"),
                rs.getString("subtipo"),
                rs.getString("idioma"),
                rs.getString("validade"),
                rs.getString("codigo_barras")
            );
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}

}
