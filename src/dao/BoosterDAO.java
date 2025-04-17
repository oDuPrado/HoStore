package dao;

import model.BoosterModel;
import util.DB;
import java.sql.*;

public class BoosterDAO extends ProdutoDAO {

    public void insert(BoosterModel b) throws Exception {
        super.insert(b);

        try (PreparedStatement ps = DB.get().prepareStatement(
            "INSERT OR REPLACE INTO produtos_detalhes " +
            "(id, colecao, subtipo) VALUES (?,?,?)")) {
            ps.setString(1, b.getId());
            ps.setString(2, b.getColecao());
            ps.setString(3, b.getTipoBooster());
            ps.executeUpdate();
        }
    }
}
