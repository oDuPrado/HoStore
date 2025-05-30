package dao;

import model.TituloContaReceberModel;
import util.DB;

import java.sql.*;
import java.util.*;

/**
 * @CR DAO: CRUD para titulos_contas_receber
 */
public class TituloContaReceberDAO {

    /* ── INSERT ───────────────────────────────────────────────────────── */
    public void inserir(TituloContaReceberModel t) throws SQLException {
        final String sql = """
            INSERT INTO titulos_contas_receber
                (id, cliente_id, codigo_selecao, data_geracao, valor_total, status, observacoes)
            VALUES (?,?,?,?,?,?,?)
        """;

        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, t.getId());
            p.setString(2, t.getClienteId());
            p.setString(3, t.getCodigoSelecao());
            p.setString(4, t.getDataGeracao());
            p.setDouble(5, t.getValorTotal());
            p.setString(6, t.getStatus());
            p.setString(7, t.getObservacoes());
            p.executeUpdate();
        }
    }

    /* ── UPDATE ───────────────────────────────────────────────────────── */
    public void atualizar(TituloContaReceberModel t) throws SQLException {
        final String sql = """
            UPDATE titulos_contas_receber SET
                cliente_id=?, codigo_selecao=?, data_geracao=?,
                valor_total=?, status=?, observacoes=?
            WHERE id=?
        """;

        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, t.getClienteId());
            p.setString(2, t.getCodigoSelecao());
            p.setString(3, t.getDataGeracao());
            p.setDouble(4, t.getValorTotal());
            p.setString(5, t.getStatus());
            p.setString(6, t.getObservacoes());
            p.setString(7, t.getId());
            p.executeUpdate();
        }
    }

    /* ── DELETE ───────────────────────────────────────────────────────── */
    public void excluir(String id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(
                 "DELETE FROM titulos_contas_receber WHERE id=?")) {
            p.setString(1, id);
            p.executeUpdate();
        }
    }

    /* ── SELECT SINGLE ────────────────────────────────────────────────── */
    public TituloContaReceberModel buscarPorId(String id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(
                 "SELECT * FROM titulos_contas_receber WHERE id=?")) {
            p.setString(1, id);
            ResultSet r = p.executeQuery();
            return r.next() ? mapRow(r) : null;
        }
    }

    /* ── SELECT LIST (filtros simples) ────────────────────────────────── */
    public List<TituloContaReceberModel> listarTodos() throws SQLException {
        List<TituloContaReceberModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet r = st.executeQuery("SELECT * FROM titulos_contas_receber")) {
            while (r.next()) list.add(mapRow(r));
        }
        return list;
    }

    /* ── Mapeamento ResultSet -> Model ────────────────────────────────── */
    private TituloContaReceberModel mapRow(ResultSet r) throws SQLException {
        TituloContaReceberModel t = new TituloContaReceberModel();
        t.setId(r.getString("id"));
        t.setClienteId(r.getString("cliente_id"));
        t.setCodigoSelecao(r.getString("codigo_selecao"));
        t.setDataGeracao(r.getString("data_geracao"));
        t.setValorTotal(r.getDouble("valor_total"));
        t.setStatus(r.getString("status"));
        t.setObservacoes(r.getString("observacoes"));
        return t;
    }
}
