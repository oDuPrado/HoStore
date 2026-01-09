package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VendaItemLoteDAO {

    public static class ConsumoLote {
        public final int loteId;
        public final int qtdConsumida;
        public final double custoUnit;

        public ConsumoLote(int loteId, int qtdConsumida, double custoUnit) {
            this.loteId = loteId;
            this.qtdConsumida = qtdConsumida;
            this.custoUnit = custoUnit;
        }
    }

    /** grava consumo item->lote */
    public void inserirConsumo(int vendaItemId, int loteId, int qtd, double custoUnit, Connection c)
            throws SQLException {
        if (vendaItemId <= 0)
            throw new SQLException("vendaItemId inválido: " + vendaItemId);
        if (loteId <= 0)
            throw new SQLException("loteId inválido: " + loteId);
        if (qtd <= 0)
            throw new SQLException("qtd inválida: " + qtd);

        String sql = """
                    INSERT INTO vendas_itens_lotes (venda_item_id, lote_id, qtd, custo_unit)
                    VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendaItemId);
            ps.setInt(2, loteId);
            ps.setInt(3, qtd);
            ps.setDouble(4, custoUnit);
            ps.executeUpdate();
        }
    }

    /**
     * ✅ Soma consumo por lote para uma venda/produto.
     * (junta vendas_itens + vendas_itens_lotes)
     */
    public List<ConsumoLote> listarConsumoPorLote(int vendaId, String produtoId, Connection c) throws SQLException {
        String sql = """
                    SELECT vil.lote_id AS lote_id,
                           SUM(vil.qtd) AS qtd_consumida,
                           COALESCE(MAX(vil.custo_unit), 0) AS custo_unit
                      FROM vendas_itens vi
                      JOIN vendas_itens_lotes vil ON vil.venda_item_id = vi.id
                     WHERE vi.venda_id = ?
                       AND vi.produto_id = ?
                     GROUP BY vil.lote_id
                     ORDER BY vil.lote_id ASC
                """;

        List<ConsumoLote> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            ps.setString(2, produtoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ConsumoLote(
                            rs.getInt("lote_id"),
                            rs.getInt("qtd_consumida"),
                            rs.getDouble("custo_unit")));
                }
            }
        }
        return out;
    }

    /** ✅ Quanto já foi devolvido por lote (pra não devolver duas vezes) */
    public int somarDevolvidoNoLote(int vendaId, String produtoId, int loteId, Connection c) throws SQLException {
        String sql = """
                    SELECT COALESCE(SUM(vdl.qtd), 0) AS devolvido
                      FROM vendas_devolucoes vd
                      JOIN vendas_devolucoes_lotes vdl ON vdl.devolucao_id = vd.id
                     WHERE vd.venda_id = ?
                       AND vd.produto_id = ?
                       AND vdl.lote_id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            ps.setString(2, produtoId);
            ps.setInt(3, loteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("devolvido");
            }
        }
        return 0;
    }
}
