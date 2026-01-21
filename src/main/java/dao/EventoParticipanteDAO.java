package dao;

import model.EventoParticipanteModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoParticipanteDAO {

    public void inserir(EventoParticipanteModel p, Connection c) throws SQLException {
        String sql = """
                INSERT INTO eventos_participantes
                (id, evento_id, cliente_id, nome_avulso, status, venda_id, comanda_id, comanda_item_id,
                 data_checkin, criado_em, criado_por, alterado_em, alterado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            ps.executeUpdate();
        }
    }

    public void inserir(EventoParticipanteModel p) throws SQLException {
        try (Connection c = DB.get()) {
            inserir(p, c);
        }
    }

    public void atualizar(EventoParticipanteModel p, Connection c) throws SQLException {
        String sql = """
                UPDATE eventos_participantes SET
                    cliente_id = ?, nome_avulso = ?, status = ?, venda_id = ?, comanda_id = ?, comanda_item_id = ?,
                    data_checkin = ?, alterado_em = ?, alterado_por = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getClienteId());
            ps.setString(2, p.getNomeAvulso());
            ps.setString(3, p.getStatus());
            if (p.getVendaId() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, p.getVendaId());
            if (p.getComandaId() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, p.getComandaId());
            if (p.getComandaItemId() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, p.getComandaItemId());
            ps.setString(7, p.getDataCheckin());
            ps.setString(8, p.getAlteradoEm());
            ps.setString(9, p.getAlteradoPor());
            ps.setString(10, p.getId());
            ps.executeUpdate();
        }
    }

    public void atualizarStatus(String participanteId, String status, String alteradoEm, String alteradoPor)
            throws SQLException {
        String sql = "UPDATE eventos_participantes SET status = ?, alterado_em = ?, alterado_por = ? WHERE id = ?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, alteradoEm);
            ps.setString(3, alteradoPor);
            ps.setString(4, participanteId);
            ps.executeUpdate();
        }
    }

    public void vincularVenda(String participanteId, int vendaId, String status, String alteradoEm, String alteradoPor)
            throws SQLException {
        String sql = """
                UPDATE eventos_participantes
                   SET venda_id = ?, status = ?, alterado_em = ?, alterado_por = ?
                 WHERE id = ?
                """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            ps.setString(2, status);
            ps.setString(3, alteradoEm);
            ps.setString(4, alteradoPor);
            ps.setString(5, participanteId);
            ps.executeUpdate();
        }
    }

    public void vincularVendaPorComanda(int comandaId, int vendaId, Connection c) throws SQLException {
        String sql = """
                UPDATE eventos_participantes
                   SET venda_id = ?, status = 'pago'
                 WHERE comanda_id = ?
                   AND (venda_id IS NULL OR venda_id = 0)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            ps.setInt(2, comandaId);
            ps.executeUpdate();
        }
    }

    public List<EventoParticipanteModel> listarPorEvento(String eventoId) throws SQLException {
        List<EventoParticipanteModel> out = new ArrayList<>();
        String sql = "SELECT * FROM eventos_participantes WHERE evento_id = ? ORDER BY criado_em DESC";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    public int contarPorEvento(String eventoId) throws SQLException {
        String sql = "SELECT COUNT(1) AS total FROM eventos_participantes WHERE evento_id = ?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    public EventoParticipanteModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM eventos_participantes WHERE id = ?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    private void bind(PreparedStatement ps, EventoParticipanteModel p) throws SQLException {
        ps.setString(1, p.getId());
        ps.setString(2, p.getEventoId());
        ps.setString(3, p.getClienteId());
        ps.setString(4, p.getNomeAvulso());
        ps.setString(5, p.getStatus());
        if (p.getVendaId() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, p.getVendaId());
        if (p.getComandaId() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, p.getComandaId());
        if (p.getComandaItemId() == null) ps.setNull(8, Types.INTEGER); else ps.setInt(8, p.getComandaItemId());
        ps.setString(9, p.getDataCheckin());
        ps.setString(10, p.getCriadoEm());
        ps.setString(11, p.getCriadoPor());
        ps.setString(12, p.getAlteradoEm());
        ps.setString(13, p.getAlteradoPor());
    }

    private EventoParticipanteModel map(ResultSet rs) throws SQLException {
        EventoParticipanteModel p = new EventoParticipanteModel();
        p.setId(rs.getString("id"));
        p.setEventoId(rs.getString("evento_id"));
        p.setClienteId(rs.getString("cliente_id"));
        p.setNomeAvulso(rs.getString("nome_avulso"));
        p.setStatus(rs.getString("status"));
        int vendaId = rs.getInt("venda_id");
        p.setVendaId(rs.wasNull() ? null : vendaId);
        int comandaId = rs.getInt("comanda_id");
        p.setComandaId(rs.wasNull() ? null : comandaId);
        int comandaItemId = rs.getInt("comanda_item_id");
        p.setComandaItemId(rs.wasNull() ? null : comandaItemId);
        p.setDataCheckin(rs.getString("data_checkin"));
        p.setCriadoEm(rs.getString("criado_em"));
        p.setCriadoPor(rs.getString("criado_por"));
        p.setAlteradoEm(rs.getString("alterado_em"));
        p.setAlteradoPor(rs.getString("alterado_por"));
        return p;
    }
}
