
package dao;
import util.DB;
import model.VendaModel;
import java.sql.*;
import util.DateUtils;

public class VendaDAO {
    public int insert(VendaModel v) throws SQLException{
        String sql = "INSERT INTO vendas(data_venda,cliente_id,total,desconto,forma_pagamento,parcelas,status,criado_em,criado_por) VALUES(?,?,?,?,?,?,?, ?, ?)";
        PreparedStatement ps = DB.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1,v.getDataVenda());
        ps.setString(2,v.getClienteId());
        ps.setDouble(3,v.getTotal());
        ps.setDouble(4,v.getDesconto());
        ps.setString(5,v.getFormaPagamento());
        ps.setInt(6,v.getParcelas());
        ps.setString(7,"fechada");
        ps.setString(8, DateUtils.now());
        ps.setString(9,"admin");
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if(keys.next()) v.setId(keys.getInt(1));
        return v.getId();
    }
}
