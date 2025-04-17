package dao;

import model.VendaItemModel;
import java.sql.*;

public class VendaItemDAO {
    /** Também usa a conexão da transação */
    public void insert(VendaItemModel it, int vendaId, Connection c) throws SQLException {
        String sql = "INSERT INTO vendas_itens(venda_id, carta_id, qtd, preco, desconto, total_item)"
                   + " VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            ps.setString(2, it.getCartaId());
            ps.setInt(3, it.getQtd());
            ps.setDouble(4, it.getPreco());
            ps.setDouble(5, it.getDesconto());
            ps.setDouble(6, it.getTotalItem());
            ps.executeUpdate();
        }
    }
}
