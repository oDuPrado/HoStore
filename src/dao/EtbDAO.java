package dao;

import model.EtbModel;
import util.DB;
import java.sql.*;

public class EtbDAO extends ProdutoDAO {

    public void insert(EtbModel e) throws Exception {
        super.insert(e);

        try (PreparedStatement ps = DB.get().prepareStatement(
            "INSERT OR REPLACE INTO produtos_detalhes " +
            "(id, colecao, subtipo, versao) VALUES (?,?,?,?)")) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getColecao());
            ps.setString(3, e.getTipoEtb());
            ps.setString(4, e.getVersao());
            ps.executeUpdate();
        }
    }
}
