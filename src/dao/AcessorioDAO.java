package dao;

import model.AcessorioModel;
import util.DB;
import java.sql.*;

public class AcessorioDAO extends ProdutoDAO {

    public void insert(AcessorioModel a) throws Exception {
        super.insert(a); // salva na tabela produtos

        try (PreparedStatement ps = DB.get().prepareStatement(
            "INSERT OR REPLACE INTO produtos_detalhes " +
            "(id, tipo_especifico) VALUES (?,?)")) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getTipoAcessorio());
            ps.executeUpdate();
        }
    }
}
