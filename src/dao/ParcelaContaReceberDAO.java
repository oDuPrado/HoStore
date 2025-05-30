package dao;

import model.ParcelaContaReceberModel;
import util.DB;

import java.sql.*;
import java.util.*;

/**
 * @CR DAO: CRUD para parcelas_contas_receber
 */
public class ParcelaContaReceberDAO {

    /* INSERT */
    public void inserir(ParcelaContaReceberModel p) throws SQLException {
        final String sql = """
            INSERT INTO parcelas_contas_receber
                (titulo_id, numero_parcela, vencimento, valor_nominal,
                 valor_juros, valor_acrescimo, valor_desconto, valor_pago,
                 data_pagamento, data_compensacao, pago_com_desconto,
                 forma_pagamento, status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preencher(ps, p, false);
            ps.executeUpdate();
            ResultSet gen = ps.getGeneratedKeys();
            if (gen.next()) p.setId(gen.getInt(1)); // pega PK
        }
    }

    /* UPDATE */
    public void atualizar(ParcelaContaReceberModel p) throws SQLException {
        final String sql = """
            UPDATE parcelas_contas_receber SET
                titulo_id=?, numero_parcela=?, vencimento=?, valor_nominal=?,
                valor_juros=?, valor_acrescimo=?, valor_desconto=?, valor_pago=?,
                data_pagamento=?, data_compensacao=?, pago_com_desconto=?,
                forma_pagamento=?, status=?
            WHERE id=?
        """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, p, true);
            ps.executeUpdate();
        }
    }

    /* DELETE */
    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM parcelas_contas_receber WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /* LISTAR POR TÍTULO */
    public List<ParcelaContaReceberModel> listarPorTitulo(String tituloId) throws SQLException {
        List<ParcelaContaReceberModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM parcelas_contas_receber WHERE titulo_id=? ORDER BY numero_parcela")) {
            ps.setString(1, tituloId);
            ResultSet r = ps.executeQuery();
            while (r.next()) list.add(mapRow(r));
        }
        return list;
    }

    /* BUSCAR POR ID */
    public ParcelaContaReceberModel buscarPorId(int id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM parcelas_contas_receber WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet r = ps.executeQuery();
            return r.next() ? mapRow(r) : null;
        }
    }

    /* Helper: preenche PreparedStatement */
    private void preencher(PreparedStatement ps, ParcelaContaReceberModel p, boolean includeId) throws SQLException {
        ps.setString(1,  p.getTituloId());
        ps.setInt   (2,  p.getNumeroParcela());
        ps.setString(3,  p.getVencimento());
        ps.setDouble(4,  p.getValorNominal());
        ps.setDouble(5,  p.getValorJuros());
        ps.setDouble(6,  p.getValorAcrescimo());
        ps.setDouble(7,  p.getValorDesconto());
        ps.setDouble(8,  p.getValorPago());
        ps.setString(9,  p.getDataPagamento());
        ps.setString(10, p.getDataCompensacao());
        ps.setInt   (11, p.isPagoComDesconto() ? 1 : 0);
        ps.setString(12, p.getFormaPagamento());
        ps.setString(13, p.getStatus());
        if (includeId) ps.setInt(14, p.getId());
    }

    /* Map Row */
    private ParcelaContaReceberModel mapRow(ResultSet r) throws SQLException {
        ParcelaContaReceberModel p = new ParcelaContaReceberModel();
        p.setId(r.getInt("id"));
        p.setTituloId(r.getString("titulo_id"));
        p.setNumeroParcela(r.getInt("numero_parcela"));
        p.setVencimento(r.getString("vencimento"));
        p.setValorNominal(r.getDouble("valor_nominal"));
        p.setValorJuros(r.getDouble("valor_juros"));
        p.setValorAcrescimo(r.getDouble("valor_acrescimo"));
        p.setValorDesconto(r.getDouble("valor_desconto"));
        p.setValorPago(r.getDouble("valor_pago"));
        p.setDataPagamento(r.getString("data_pagamento"));
        p.setDataCompensacao(r.getString("data_compensacao"));
        p.setPagoComDesconto(r.getInt("pago_com_desconto") == 1);
        p.setFormaPagamento(r.getString("forma_pagamento"));
        p.setStatus(r.getString("status"));
        return p;
    }
}
