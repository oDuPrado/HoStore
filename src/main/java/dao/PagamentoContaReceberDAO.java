package dao;

import model.PagamentoContaReceberModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @CR DAO: CRUD para pagamentos_contas_receber
 */
public class PagamentoContaReceberDAO {

    /* INSERT */
    public void inserir(PagamentoContaReceberModel p) throws SQLException {
        final String sql = """
            INSERT INTO pagamentos_contas_receber
                (parcela_id, forma_pagamento, valor_pago, data_pagamento)
            VALUES (?,?,?,?)
        """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, p.getParcelaId());
            ps.setString(2, p.getFormaPagamento());
            ps.setDouble(3, p.getValorPago());
            ps.setString(4, p.getDataPagamento());
            ps.executeUpdate();
            ResultSet gen = ps.getGeneratedKeys();
            if (gen.next()) p.setId(gen.getInt(1));
        }
    }

    /* LISTAR POR PARCELA */
    public List<PagamentoContaReceberModel> listarPorParcela(int parcelaId) throws SQLException {
        List<PagamentoContaReceberModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM pagamentos_contas_receber WHERE parcela_id=?")) {
            ps.setInt(1, parcelaId);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(mapRow(r));
        }
        return list;
    }

    /* Map Row */
    private PagamentoContaReceberModel mapRow(ResultSet r) throws SQLException {
        PagamentoContaReceberModel p = new PagamentoContaReceberModel();
        p.setId(r.getInt("id"));
        p.setParcelaId(r.getInt("parcela_id"));
        p.setFormaPagamento(r.getString("forma_pagamento"));
        p.setValorPago(r.getDouble("valor_pago"));
        p.setDataPagamento(r.getString("data_pagamento"));
        return p;
    }
}
