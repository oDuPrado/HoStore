package dao;

import model.VendaItemModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de itens da venda – usa a conexão da transação.
 */
public class VendaItemDAO {

    public int insert(VendaItemModel it, int vendaId, Connection c) throws SQLException {
        String sql = "INSERT INTO vendas_itens " +
                "(venda_id, produto_id, qtd, preco, desconto, total_item) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, vendaId);
            ps.setString(2, it.getProdutoId());
            ps.setInt(3, it.getQtd());
            ps.setDouble(4, it.getPreco());
            ps.setDouble(5, it.getDesconto());
            ps.setDouble(6, it.getTotalItem());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    it.setId(id);
                    it.setVendaId(vendaId);
                    return id;
                }
            }
        }
        throw new SQLException("Falha ao inserir item da venda " + vendaId);
    }

    public List<VendaItemModel> listarPorVenda(int vendaId) throws SQLException {
    List<VendaItemModel> itens = new ArrayList<>();
    String sql = "SELECT * FROM vendas_itens WHERE venda_id = ?";

    try (Connection c = util.DB.get();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setInt(1, vendaId);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                VendaItemModel item = new VendaItemModel();
                item.setProdutoId(rs.getString("produto_id"));
                item.setQtd(rs.getInt("qtd"));
                item.setPreco(rs.getDouble("preco"));
                item.setDesconto(rs.getDouble("desconto"));
                item.setTotalItem(rs.getDouble("total_item"));

                itens.add(item);
            }
        }
    }

    return itens;
}

}
