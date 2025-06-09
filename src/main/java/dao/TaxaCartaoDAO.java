package dao;

import model.TaxaCartaoModel;
import util.DB;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso a dados para a tabela taxas_cartao, agora com bandeira e observações.
 */
public class TaxaCartaoDAO {

    /**
     * Insere (se id==null) ou atualiza uma taxa de cartão.
     */
    public void save(TaxaCartaoModel m) throws SQLException {
        String sql = m.getId() == null
                ? "INSERT INTO taxas_cartao(bandeira,tipo,min_parcelas,max_parcelas,mes_vigencia,taxa_pct,observacoes) "
                        + "VALUES(?,?,?,?,?,?,?)"
                : "UPDATE taxas_cartao SET bandeira=?,tipo=?,min_parcelas=?,max_parcelas=?,"
                        + "mes_vigencia=?,taxa_pct=?,observacoes=? WHERE id=?";
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, m.getBandeira());
            ps.setString(2, m.getTipo());
            ps.setInt(3, m.getMinParcelas());
            ps.setInt(4, m.getMaxParcelas());
            ps.setString(5, m.getMesVigencia());
            ps.setDouble(6, m.getTaxaPct());
            ps.setString(7, m.getObservacoes());

            if (m.getId() != null) {
                ps.setInt(8, m.getId());
            }

            ps.executeUpdate();
            if (m.getId() == null) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next())
                        m.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Exclui o registro com o id informado.
     */
    public void delete(int id) throws SQLException {
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM taxas_cartao WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna todas as taxas, ordenadas por vigência, bandeira e tipo.
     */
    public List<TaxaCartaoModel> findAll() throws SQLException {
        String sql = "SELECT id,bandeira,tipo,min_parcelas,max_parcelas,mes_vigencia,taxa_pct,observacoes "
                + "FROM taxas_cartao ORDER BY mes_vigencia DESC,bandeira,tipo";
        List<TaxaCartaoModel> list = new ArrayList<>();
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TaxaCartaoModel(
                        rs.getInt("id"),
                        rs.getString("bandeira"),
                        rs.getString("tipo"),
                        rs.getInt("min_parcelas"),
                        rs.getInt("max_parcelas"),
                        rs.getString("mes_vigencia"),
                        rs.getDouble("taxa_pct"),
                        rs.getString("observacoes")));
            }
        }
        return list;
    }

    /**
     * Busca a taxa (%) aplicável para o tipo, parcelas e mês de referência.
     * Considera somente o primeiro registro que casar.
     */
    public Optional<Double> buscarTaxa(String bandeira, String tipo,
            int parcelas, YearMonth mesRef) throws SQLException {
        String sql = "SELECT taxa_pct FROM taxas_cartao "
                + "WHERE bandeira = ? AND tipo = ? "
                + "  AND ? BETWEEN min_parcelas AND max_parcelas "
                + "  AND mes_vigencia = ? LIMIT 1";
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, bandeira);
            ps.setString(2, tipo);
            ps.setInt(3, parcelas);
            ps.setString(4, mesRef.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getDouble("taxa_pct"));
                }
            }
        }
        return Optional.empty();
    }
}
