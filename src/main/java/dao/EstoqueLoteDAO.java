package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EstoqueLoteDAO {

    public static class LoteSaldo {
        public final int loteId;
        public final int qtdDisponivel;
        public final double custoUnit;

        public LoteSaldo(int loteId, int qtdDisponivel, double custoUnit) {
            this.loteId = loteId;
            this.qtdDisponivel = qtdDisponivel;
            this.custoUnit = custoUnit;
        }
    }

    /** FIFO por id ASC */
    public List<LoteSaldo> listarLotesDisponiveisFIFO(String produtoId, Connection c) throws SQLException {
        String sql = """
                    SELECT id, qtd_disponivel, custo_unit
                      FROM estoque_lotes
                     WHERE produto_id = ?
                       AND qtd_disponivel > 0
                     ORDER BY id ASC
                """;

        List<LoteSaldo> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new LoteSaldo(
                            rs.getInt("id"),
                            rs.getInt("qtd_disponivel"),
                            rs.getDouble("custo_unit")));
                }
            }
        }
        return out;
    }

    public int somarSaldoProduto(String produtoId, Connection c) throws SQLException {
        String sql = "SELECT COALESCE(SUM(qtd_disponivel), 0) AS total FROM estoque_lotes WHERE produto_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        }
        return 0;
    }

    /** Consome do lote com trava: só consome se tiver saldo */
    public void consumirDoLote(int loteId, int qtd, Connection c) throws SQLException {
        if (qtd <= 0)
            throw new SQLException("qtd inválida: " + qtd);

        String sql = """
                    UPDATE estoque_lotes
                       SET qtd_disponivel = qtd_disponivel - ?
                     WHERE id = ?
                       AND qtd_disponivel >= ?
                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, qtd);
            ps.setInt(2, loteId);
            ps.setInt(3, qtd);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException(
                        "Falha ao consumir lote " + loteId + ": saldo insuficiente ou lote inexistente.");
            }
        }
    }

    /** ✅ Reposição em lote (devolução) */
    public void reporNoLote(int loteId, int qtd, Connection c) throws SQLException {
        if (qtd <= 0)
            throw new SQLException("qtd inválida: " + qtd);

        String sql = """
                    UPDATE estoque_lotes
                       SET qtd_disponivel = qtd_disponivel + ?
                     WHERE id = ?
                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, qtd);
            ps.setInt(2, loteId);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Falha ao repor no lote " + loteId + ": lote inexistente.");
            }
        }
    }
}
