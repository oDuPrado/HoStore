
package service;
import model.Carta;
import util.DB;
import java.sql.*;

public class EstoqueService {
    public boolean possuiEstoque(String cartaId,int qtd) throws SQLException{
        PreparedStatement ps = DB.get().prepareStatement("SELECT qtd FROM cartas WHERE id=?");
        ps.setString(1,cartaId);
        ResultSet rs = ps.executeQuery();
        if(rs.next()){
            return rs.getInt("qtd") >= qtd;
        }
        return false;
    }
    public void baixarEstoque(String cartaId,int qtd) throws SQLException{
        PreparedStatement ps = DB.get().prepareStatement("UPDATE cartas SET qtd=qtd-? WHERE id=?");
        ps.setInt(1,qtd);
        ps.setString(2,cartaId);
        ps.executeUpdate();
    }
}
