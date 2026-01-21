package dao;

import model.EventoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoDAO {

    public void inserir(EventoModel e) throws SQLException {
        String sql = """
                INSERT INTO eventos
                (id, nome, jogo_id, data_inicio, data_fim, status, taxa_inscricao, produto_inscricao_id,
                 regras_texto, limite_participantes, observacoes, criado_em, criado_por, alterado_em, alterado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.executeUpdate();
        }
    }

    public void atualizar(EventoModel e) throws SQLException {
        String sql = """
                UPDATE eventos SET
                    nome = ?, jogo_id = ?, data_inicio = ?, data_fim = ?, status = ?, taxa_inscricao = ?,
                    produto_inscricao_id = ?, regras_texto = ?, limite_participantes = ?, observacoes = ?,
                    alterado_em = ?, alterado_por = ?
                WHERE id = ?
                """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getNome());
            ps.setString(2, e.getJogoId());
            ps.setString(3, e.getDataInicio());
            ps.setString(4, e.getDataFim());
            ps.setString(5, e.getStatus());
            ps.setDouble(6, e.getTaxaInscricao());
            ps.setString(7, e.getProdutoInscricaoId());
            ps.setString(8, e.getRegrasTexto());
            if (e.getLimiteParticipantes() == null) {
                ps.setNull(9, Types.INTEGER);
            } else {
                ps.setInt(9, e.getLimiteParticipantes());
            }
            ps.setString(10, e.getObservacoes());
            ps.setString(11, e.getAlteradoEm());
            ps.setString(12, e.getAlteradoPor());
            ps.setString(13, e.getId());
            ps.executeUpdate();
        }
    }

    public EventoModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM eventos WHERE id = ?";
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

    public List<EventoModel> listarTodos() throws SQLException {
        List<EventoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM eventos ORDER BY data_inicio DESC, nome";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    private void bind(PreparedStatement ps, EventoModel e) throws SQLException {
        ps.setString(1, e.getId());
        ps.setString(2, e.getNome());
        ps.setString(3, e.getJogoId());
        ps.setString(4, e.getDataInicio());
        ps.setString(5, e.getDataFim());
        ps.setString(6, e.getStatus());
        ps.setDouble(7, e.getTaxaInscricao());
        ps.setString(8, e.getProdutoInscricaoId());
        ps.setString(9, e.getRegrasTexto());
        if (e.getLimiteParticipantes() == null) {
            ps.setNull(10, Types.INTEGER);
        } else {
            ps.setInt(10, e.getLimiteParticipantes());
        }
        ps.setString(11, e.getObservacoes());
        ps.setString(12, e.getCriadoEm());
        ps.setString(13, e.getCriadoPor());
        ps.setString(14, e.getAlteradoEm());
        ps.setString(15, e.getAlteradoPor());
    }

    private EventoModel map(ResultSet rs) throws SQLException {
        EventoModel e = new EventoModel();
        e.setId(rs.getString("id"));
        e.setNome(rs.getString("nome"));
        e.setJogoId(rs.getString("jogo_id"));
        e.setDataInicio(rs.getString("data_inicio"));
        e.setDataFim(rs.getString("data_fim"));
        e.setStatus(rs.getString("status"));
        e.setTaxaInscricao(rs.getDouble("taxa_inscricao"));
        e.setProdutoInscricaoId(rs.getString("produto_inscricao_id"));
        e.setRegrasTexto(rs.getString("regras_texto"));
        int limite = rs.getInt("limite_participantes");
        e.setLimiteParticipantes(rs.wasNull() ? null : limite);
        e.setObservacoes(rs.getString("observacoes"));
        e.setCriadoEm(rs.getString("criado_em"));
        e.setCriadoPor(rs.getString("criado_por"));
        e.setAlteradoEm(rs.getString("alterado_em"));
        e.setAlteradoPor(rs.getString("alterado_por"));
        return e;
    }
}
