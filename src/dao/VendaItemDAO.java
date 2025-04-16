
package dao;
import util.DB;
import model.VendaItemModel;
import java.sql.*;

public class VendaItemDAO {
    public void insert(VendaItemModel it,int vendaId) throws SQLException{
        String sql = "INSERT INTO vendas_itens(venda_id,carta_id,qtd,preco,desconto) VALUES(?,?,?,?,?)";
        PreparedStatement ps = DB.get().prepareStatement(sql);
        ps.setInt(1,vendaId);
        ps.setString(2,it.getCartaId());
        ps.setInt(3,it.getQtd());
        ps.setDouble(4,it.getPreco());
        ps.setDouble(5,it.getDesconto());
        ps.executeUpdate();
    }
}
