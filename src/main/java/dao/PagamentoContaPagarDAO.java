package dao;

import model.PagamentoContaPagarModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para pagamentos_contas_pagar
 */
public class PagamentoContaPagarDAO {

    public void inserir(PagamentoContaPagarModel p) throws SQLException {
        String sql = "INSERT INTO pagamentos_contas_pagar("
            + "parcela_id, forma_pagamento, valor_pago, data_pagamento"
            + ") VALUES (?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getParcelaId());
            ps.setString(2, p.getFormaPagamento());
            ps.setDouble(3, p.getValorPago());
            ps.setString(4, p.getDataPagamento());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
    }

    public List<PagamentoContaPagarModel> listarPorParcela(int parcelaId) throws SQLException {
        List<PagamentoContaPagarModel> out = new ArrayList<>();
        String sql = "SELECT * FROM pagamentos_contas_pagar WHERE parcela_id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, parcelaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PagamentoContaPagarModel(
                        rs.getInt("id"),
                        rs.getInt("parcela_id"),
                        rs.getString("forma_pagamento"),
                        rs.getDouble("valor_pago"),
                        rs.getString("data_pagamento")
                    ));
                }
            }
        }
        return out;
    }
}
