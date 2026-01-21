package dao;

import model.EventoPremiacaoRegraModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoPremiacaoRegraDAO {

    public void inserir(EventoPremiacaoRegraModel r, Connection c) throws SQLException {
        String sql = """
                INSERT INTO eventos_premiacao_regras
                (id, evento_id, colocacao_inicio, colocacao_fim, tipo, produto_id, quantidade, valor_credito, observacoes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            ps.executeUpdate();
        }
    }

    public void inserir(EventoPremiacaoRegraModel r) throws SQLException {
        try (Connection c = DB.get()) {
            inserir(r, c);
        }
    }

    public void deleteByEvento(String eventoId, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM eventos_premiacao_regras WHERE evento_id = ?")) {
            ps.setString(1, eventoId);
            ps.executeUpdate();
        }
    }

    public List<EventoPremiacaoRegraModel> listarPorEvento(String eventoId) throws SQLException {
        List<EventoPremiacaoRegraModel> out = new ArrayList<>();
        String sql = "SELECT * FROM eventos_premiacao_regras WHERE evento_id = ? ORDER BY colocacao_inicio ASC";
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

    private void bind(PreparedStatement ps, EventoPremiacaoRegraModel r) throws SQLException {
        ps.setString(1, r.getId());
        ps.setString(2, r.getEventoId());
        if (r.getColocacaoInicio() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, r.getColocacaoInicio());
        if (r.getColocacaoFim() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, r.getColocacaoFim());
        ps.setString(5, r.getTipo());
        ps.setString(6, r.getProdutoId());
        if (r.getQuantidade() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, r.getQuantidade());
        if (r.getValorCredito() == null) ps.setNull(8, Types.REAL); else ps.setDouble(8, r.getValorCredito());
        ps.setString(9, r.getObservacoes());
    }

    private EventoPremiacaoRegraModel map(ResultSet rs) throws SQLException {
        EventoPremiacaoRegraModel r = new EventoPremiacaoRegraModel();
        r.setId(rs.getString("id"));
        r.setEventoId(rs.getString("evento_id"));
        int ini = rs.getInt("colocacao_inicio");
        r.setColocacaoInicio(rs.wasNull() ? null : ini);
        int fim = rs.getInt("colocacao_fim");
        r.setColocacaoFim(rs.wasNull() ? null : fim);
        r.setTipo(rs.getString("tipo"));
        r.setProdutoId(rs.getString("produto_id"));
        int qtd = rs.getInt("quantidade");
        r.setQuantidade(rs.wasNull() ? null : qtd);
        double vc = rs.getDouble("valor_credito");
        r.setValorCredito(rs.wasNull() ? null : vc);
        r.setObservacoes(rs.getString("observacoes"));
        return r;
    }
}
