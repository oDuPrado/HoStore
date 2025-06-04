package dao;

import model.ParcelaContaPagarModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para parcelas_contas_pagar
 */
public class ParcelaContaPagarDAO {

    public void inserir(ParcelaContaPagarModel p) throws SQLException {
        String sql = "INSERT INTO parcelas_contas_pagar("
            + "titulo_id, numero_parcela, vencimento, valor_nominal, valor_juros, valor_acrescimo,"
            + "valor_desconto, valor_pago, data_pagamento, data_compensacao, pago_com_desconto,"
            + "forma_pagamento, status"
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getTituloId());
            ps.setInt(2, p.getNumeroParcela());
            ps.setString(3, p.getVencimento());
            ps.setDouble(4, p.getValorNominal());
            ps.setDouble(5, p.getValorJuros());
            ps.setDouble(6, p.getValorAcrescimo());
            ps.setDouble(7, p.getValorDesconto());
            ps.setDouble(8, p.getValorPago());
            ps.setString(9, p.getDataPagamento());
            ps.setString(10, p.getDataCompensacao());
            ps.setInt(11, p.isPagoComDesconto() ? 1 : 0);
            ps.setString(12, p.getFormaPagamento());
            ps.setString(13, p.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        }
    }

    public void atualizar(ParcelaContaPagarModel p) throws SQLException {
        String sql = "UPDATE parcelas_contas_pagar SET "
            + "vencimento=?, valor_nominal=?, valor_juros=?, valor_acrescimo=?, "
            + "valor_desconto=?, valor_pago=?, data_pagamento=?, data_compensacao=?, "
            + "pago_com_desconto=?, forma_pagamento=?, status=? "
            + "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getVencimento());
            ps.setDouble(2, p.getValorNominal());
            ps.setDouble(3, p.getValorJuros());
            ps.setDouble(4, p.getValorAcrescimo());
            ps.setDouble(5, p.getValorDesconto());
            ps.setDouble(6, p.getValorPago());
            ps.setString(7, p.getDataPagamento());
            ps.setString(8, p.getDataCompensacao());
            ps.setInt(9, p.isPagoComDesconto() ? 1 : 0);
            ps.setString(10, p.getFormaPagamento());
            ps.setString(11, p.getStatus());
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM parcelas_contas_pagar WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public ParcelaContaPagarModel buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM parcelas_contas_pagar WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<ParcelaContaPagarModel> listarPorTitulo(String tituloId) throws SQLException {
        List<ParcelaContaPagarModel> out = new ArrayList<>();
        String sql = "SELECT * FROM parcelas_contas_pagar WHERE titulo_id=? ORDER BY numero_parcela";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tituloId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private ParcelaContaPagarModel map(ResultSet rs) throws SQLException {
        return new ParcelaContaPagarModel(
            rs.getInt("id"),
            rs.getString("titulo_id"),
            rs.getInt("numero_parcela"),
            rs.getString("vencimento"),
            rs.getDouble("valor_nominal"),
            rs.getDouble("valor_juros"),
            rs.getDouble("valor_acrescimo"),
            rs.getDouble("valor_desconto"),
            rs.getDouble("valor_pago"),
            rs.getString("data_pagamento"),
            rs.getString("data_compensacao"),
            rs.getInt("pago_com_desconto")==1,
            rs.getString("forma_pagamento"),
            rs.getString("status")
        );
    }
}
