package dao;

import model.BoosterModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
}
