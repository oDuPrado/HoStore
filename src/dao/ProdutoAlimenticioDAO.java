package dao;

import model.ProdutoAlimenticioModel;
import util.DB;
import java.sql.*;

public class ProdutoAlimenticioDAO extends ProdutoDAO {

    public void insert(ProdutoAlimenticioModel p) throws Exception {
        super.insert(p);

        try (PreparedStatement ps = DB.get().prepareStatement(
            "INSERT OR REPLACE INTO produtos_detalhes " +
            "(id, validade) VALUES (?,?)")) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getValidade());
            ps.executeUpdate();
        }
    }
}
