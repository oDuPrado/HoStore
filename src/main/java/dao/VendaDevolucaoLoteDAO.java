package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VendaDevolucaoLoteDAO {

    public void inserir(int devolucaoId, int loteId, int qtd, double custoUnit, Connection c) throws SQLException {
        if (devolucaoId <= 0)
            throw new SQLException("devolucaoId inválido: " + devolucaoId);
        if (loteId <= 0)
            throw new SQLException("loteId inválido: " + loteId);
        if (qtd <= 0)
            throw new SQLException("qtd inválida: " + qtd);

        String sql = """
                    INSERT INTO vendas_devolucoes_lotes (devolucao_id, lote_id, qtd, custo_unit)
                    VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, devolucaoId);
            ps.setInt(2, loteId);
            ps.setInt(3, qtd);
            ps.setDouble(4, custoUnit);
            ps.executeUpdate();
        }
    }
}
