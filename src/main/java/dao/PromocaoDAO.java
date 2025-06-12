package dao;

import model.PromocaoModel;
import model.TipoDesconto;
import model.AplicaEm;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Acesso a dados para tabela `promocoes`.
 */
public class PromocaoDAO {

    /**
     * Insere uma nova promoção.
     */
    public void inserir(PromocaoModel p) throws Exception {
        String sql = """
            INSERT INTO promocoes
              (id, nome, desconto, tipo_desconto, aplica_em, tipo_id, data_inicio, data_fim, observacoes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getId());
            ps.setString(2, p.getNome());
            ps.setDouble(3, p.getDesconto());
            ps.setString(4, p.getTipoDesconto().name());
            ps.setString(5, p.getAplicaEm().name());
            ps.setString(6, p.getTipoId());
            ps.setDate(7, new java.sql.Date(p.getDataInicio().getTime()));
            ps.setDate(8, new java.sql.Date(p.getDataFim().getTime()));
            ps.setString(9, p.getObservacoes());
            ps.execute();
        }
    }

    /**
     * Atualiza promoção existente.
     */
    public void atualizar(PromocaoModel p) throws Exception {
        String sql = """
            UPDATE promocoes SET
              nome = ?, desconto = ?, tipo_desconto = ?, aplica_em = ?,
              tipo_id = ?, data_inicio = ?, data_fim = ?, observacoes = ?
            WHERE id = ?
            """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNome());
            ps.setDouble(2, p.getDesconto());
            ps.setString(3, p.getTipoDesconto().name());
            ps.setString(4, p.getAplicaEm().name());
            ps.setString(5, p.getTipoId());
            ps.setDate(6, new java.sql.Date(p.getDataInicio().getTime()));
            ps.setDate(7, new java.sql.Date(p.getDataFim().getTime()));
            ps.setString(8, p.getObservacoes());
            ps.setString(9, p.getId());
            ps.execute();
        }
    }

    /**
     * Remove promoção pelo ID.
     */
    public void excluir(String id) throws Exception {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM promocoes WHERE id = ?")) {
            ps.setString(1, id);
            ps.execute();
        }
    }

    /**
     * Lista todas as promoções, ativas ou não.
     */
    public List<PromocaoModel> listarTodos() throws Exception {
        List<PromocaoModel> lista = new ArrayList<>();
        String sql = "SELECT * FROM promocoes ORDER BY nome";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Busca uma promoção pelo ID.
     */
    public Optional<PromocaoModel> buscarPorId(String id) throws Exception {
        String sql = "SELECT * FROM promocoes WHERE id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lista promoções que estão ativas hoje.
     */
    public List<PromocaoModel> listarAtivas() throws Exception {
        List<PromocaoModel> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM promocoes
            WHERE CURRENT_DATE BETWEEN data_inicio AND data_fim
            ORDER BY nome
            """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Constrói um PromocaoModel a partir do ResultSet.
     */
    private PromocaoModel mapear(ResultSet rs) throws SQLException {
        PromocaoModel p = new PromocaoModel();
        p.setId(rs.getString("id"));
        p.setNome(rs.getString("nome"));
        p.setDesconto(rs.getDouble("desconto"));
        p.setTipoDesconto(TipoDesconto.valueOf(rs.getString("tipo_desconto")));
        p.setAplicaEm(AplicaEm.valueOf(rs.getString("aplica_em")));
        p.setTipoId(rs.getString("tipo_id"));
        p.setDataInicio(rs.getDate("data_inicio"));
        p.setDataFim(rs.getDate("data_fim"));
        p.setObservacoes(rs.getString("observacoes"));
        return p;
    }
}
