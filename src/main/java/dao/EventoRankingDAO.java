package dao;

import model.EventoRankingModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoRankingDAO {

    public void inserir(EventoRankingModel r, Connection c) throws SQLException {
        String sql = """
                INSERT INTO eventos_ranking
                (id, evento_id, participante_id, pontos, colocacao, observacao)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            ps.executeUpdate();
        }
    }

    public void inserir(EventoRankingModel r) throws SQLException {
        try (Connection c = DB.get()) {
            inserir(r, c);
        }
    }

    public void atualizar(EventoRankingModel r, Connection c) throws SQLException {
        String sql = """
                UPDATE eventos_ranking SET
                    pontos = ?, colocacao = ?, observacao = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getPontos());
            if (r.getColocacao() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, r.getColocacao());
            ps.setString(3, r.getObservacao());
            ps.setString(4, r.getId());
            ps.executeUpdate();
        }
    }

    public void deleteByEvento(String eventoId, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM eventos_ranking WHERE evento_id = ?")) {
            ps.setString(1, eventoId);
            ps.executeUpdate();
        }
    }

    public List<EventoRankingModel> listarPorEvento(String eventoId) throws SQLException {
        List<EventoRankingModel> out = new ArrayList<>();
        String sql = "SELECT * FROM eventos_ranking WHERE evento_id = ? ORDER BY pontos DESC, colocacao ASC";
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

    private void bind(PreparedStatement ps, EventoRankingModel r) throws SQLException {
        ps.setString(1, r.getId());
        ps.setString(2, r.getEventoId());
        ps.setString(3, r.getParticipanteId());
        ps.setInt(4, r.getPontos());
        if (r.getColocacao() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, r.getColocacao());
        ps.setString(6, r.getObservacao());
    }

    private EventoRankingModel map(ResultSet rs) throws SQLException {
        EventoRankingModel r = new EventoRankingModel();
        r.setId(rs.getString("id"));
        r.setEventoId(rs.getString("evento_id"));
        r.setParticipanteId(rs.getString("participante_id"));
        r.setPontos(rs.getInt("pontos"));
        int colocacao = rs.getInt("colocacao");
        r.setColocacao(rs.wasNull() ? null : colocacao);
        r.setObservacao(rs.getString("observacao"));
        return r;
    }
}
