package dao;

import model.EventoPremiacaoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoPremiacaoDAO {

    public void inserir(EventoPremiacaoModel p, Connection c) throws SQLException {
        String sql = """
                INSERT INTO eventos_premiacoes
                (id, evento_id, participante_id, tipo, produto_id, quantidade, valor_credito, status,
                 movimentacao_estoque_id, credito_mov_id, entregue_em, entregue_por, estornado_em, estornado_por, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            ps.executeUpdate();
        }
    }

    public void inserir(EventoPremiacaoModel p) throws SQLException {
        try (Connection c = DB.get()) {
            inserir(p, c);
        }
    }

    public void atualizar(EventoPremiacaoModel p, Connection c) throws SQLException {
        String sql = """
                UPDATE eventos_premiacoes SET
                    status = ?, movimentacao_estoque_id = ?, credito_mov_id = ?, entregue_em = ?, entregue_por = ?,
                    estornado_em = ?, estornado_por = ?, observacoes = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getStatus());
            if (p.getMovimentacaoEstoqueId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, p.getMovimentacaoEstoqueId());
            ps.setString(3, p.getCreditoMovId());
            ps.setString(4, p.getEntregueEm());
            ps.setString(5, p.getEntreguePor());
            ps.setString(6, p.getEstornadoEm());
            ps.setString(7, p.getEstornadoPor());
            ps.setString(8, p.getObservacoes());
            ps.setString(9, p.getId());
            ps.executeUpdate();
        }
    }

    public List<EventoPremiacaoModel> listarPorEvento(String eventoId) throws SQLException {
        List<EventoPremiacaoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM eventos_premiacoes WHERE evento_id = ? ORDER BY status, id";
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

    public EventoPremiacaoModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM eventos_premiacoes WHERE id = ?";
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

    public void deleteByEvento(String eventoId, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM eventos_premiacoes WHERE evento_id = ?")) {
            ps.setString(1, eventoId);
            ps.executeUpdate();
        }
    }

    public void deleteById(String id, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM eventos_premiacoes WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public void atualizarDadosBasicos(EventoPremiacaoModel p, Connection c) throws SQLException {
        String sql = """
                UPDATE eventos_premiacoes SET
                    tipo = ?, produto_id = ?, quantidade = ?, valor_credito = ?, observacoes = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getTipo());
            ps.setString(2, p.getProdutoId());
            if (p.getQuantidade() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, p.getQuantidade());
            if (p.getValorCredito() == null) ps.setNull(4, Types.REAL); else ps.setDouble(4, p.getValorCredito());
            ps.setString(5, p.getObservacoes());
            ps.setString(6, p.getId());
            ps.executeUpdate();
        }
    }

    public void deletePendentesByEvento(String eventoId, Connection c) throws SQLException {
        String sql = "DELETE FROM eventos_premiacoes WHERE evento_id = ? AND status <> 'entregue'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, eventoId);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, EventoPremiacaoModel p) throws SQLException {
        ps.setString(1, p.getId());
        ps.setString(2, p.getEventoId());
        ps.setString(3, p.getParticipanteId());
        ps.setString(4, p.getTipo());
        ps.setString(5, p.getProdutoId());
        if (p.getQuantidade() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, p.getQuantidade());
        if (p.getValorCredito() == null) ps.setNull(7, Types.REAL); else ps.setDouble(7, p.getValorCredito());
        ps.setString(8, p.getStatus());
        if (p.getMovimentacaoEstoqueId() == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, p.getMovimentacaoEstoqueId());
        ps.setString(10, p.getCreditoMovId());
        ps.setString(11, p.getEntregueEm());
        ps.setString(12, p.getEntreguePor());
        ps.setString(13, p.getEstornadoEm());
        ps.setString(14, p.getEstornadoPor());
        ps.setString(15, p.getObservacoes());
    }

    private EventoPremiacaoModel map(ResultSet rs) throws SQLException {
        EventoPremiacaoModel p = new EventoPremiacaoModel();
        p.setId(rs.getString("id"));
        p.setEventoId(rs.getString("evento_id"));
        p.setParticipanteId(rs.getString("participante_id"));
        p.setTipo(rs.getString("tipo"));
        p.setProdutoId(rs.getString("produto_id"));
        int qtd = rs.getInt("quantidade");
        p.setQuantidade(rs.wasNull() ? null : qtd);
        double vc = rs.getDouble("valor_credito");
        p.setValorCredito(rs.wasNull() ? null : vc);
        p.setStatus(rs.getString("status"));
        int movId = rs.getInt("movimentacao_estoque_id");
        p.setMovimentacaoEstoqueId(rs.wasNull() ? null : movId);
        p.setCreditoMovId(rs.getString("credito_mov_id"));
        p.setEntregueEm(rs.getString("entregue_em"));
        p.setEntreguePor(rs.getString("entregue_por"));
        p.setEstornadoEm(rs.getString("estornado_em"));
        p.setEstornadoPor(rs.getString("estornado_por"));
        p.setObservacoes(rs.getString("observacoes"));
        return p;
    }
}
