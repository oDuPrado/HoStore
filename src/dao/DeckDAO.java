package dao;

import model.DeckModel;
import util.DB;
import java.sql.*;

public class DeckDAO extends ProdutoDAO {

    public void insert(DeckModel d) throws Exception {
        super.insert(d);

        try (PreparedStatement ps = DB.get().prepareStatement(
            "INSERT OR REPLACE INTO produtos_detalhes " +
            "(id, colecao, tipo_especifico, categoria_extra) VALUES (?,?,?,?)")) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getColecao());
            ps.setString(3, d.getTipoDeck());
            ps.setString(4, d.getCategoria());
            ps.executeUpdate();
        }
    }
}
