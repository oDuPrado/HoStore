package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    public static class LoteDetalhe {
        public final int id;
        public final String codigoLote;
        public final String fornecedorNome;
        public final String dataEntrada;
        public final String validade;
        public final double custoUnit;
        public final double precoVendaUnit;
        public final int qtdInicial;
        public final int qtdDisponivel;
        public final String status;
        public final String observacoes;

        public LoteDetalhe(int id, String codigoLote, String fornecedorNome, String dataEntrada, String validade,
                double custoUnit, double precoVendaUnit, int qtdInicial, int qtdDisponivel, String status,
                String observacoes) {
            this.id = id;
            this.codigoLote = codigoLote;
            this.fornecedorNome = fornecedorNome;
            this.dataEntrada = dataEntrada;
            this.validade = validade;
            this.custoUnit = custoUnit;
            this.precoVendaUnit = precoVendaUnit;
            this.qtdInicial = qtdInicial;
            this.qtdDisponivel = qtdDisponivel;
            this.status = status;
            this.observacoes = observacoes;
        }
    }

    /** FIFO por id ASC */
    public List<LoteSaldo> listarLotesDisponiveisFIFO(String produtoId, Connection c) throws SQLException {
        String sql = """
                    SELECT id, qtd_disponivel, custo_unit
                      FROM estoque_lotes
                     WHERE produto_id = ?
                       AND qtd_disponivel > 0
                     ORDER BY data_entrada ASC, id ASC
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

    public List<LoteDetalhe> listarLotesDetalhados(String produtoId, Connection c) throws SQLException {
        String sql = """
                    SELECT l.id,
                           l.codigo_lote,
                           l.data_entrada,
                           l.validade,
                           l.custo_unit,
                           l.preco_venda_unit,
                           l.qtd_inicial,
                           l.qtd_disponivel,
                           l.status,
                           l.observacoes,
                           f.nome AS fornecedor_nome
                      FROM estoque_lotes l
                      LEFT JOIN fornecedores f ON f.id = l.fornecedor_id
                     WHERE l.produto_id = ?
                     ORDER BY l.data_entrada ASC, l.id ASC
                """;

        List<LoteDetalhe> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new LoteDetalhe(
                            rs.getInt("id"),
                            rs.getString("codigo_lote"),
                            rs.getString("fornecedor_nome"),
                            rs.getString("data_entrada"),
                            rs.getString("validade"),
                            rs.getDouble("custo_unit"),
                            rs.getDouble("preco_venda_unit"),
                            rs.getInt("qtd_inicial"),
                            rs.getInt("qtd_disponivel"),
                            rs.getString("status"),
                            rs.getString("observacoes")));
                }
            }
        }
        return out;
    }

    public int inserirLote(String produtoId,
            String fornecedorId,
            String codigoLote,
            String dataEntradaIso,
            String validadeIso,
            double custoUnit,
            double precoVendaUnit,
            int qtd,
            String observacoes,
            Connection c) throws SQLException {
        String sql = """
                    INSERT INTO estoque_lotes
                    (produto_id, fornecedor_id, codigo_lote, data_entrada, validade, custo_unit, preco_venda_unit,
                     qtd_inicial, qtd_disponivel, status, observacoes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ativo', ?)
                """;

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, produtoId);
            ps.setString(2, fornecedorId);
            ps.setString(3, codigoLote);
            ps.setString(4, dataEntradaIso);
            ps.setString(5, validadeIso);
            ps.setDouble(6, custoUnit);
            ps.setDouble(7, precoVendaUnit);
            ps.setInt(8, qtd);
            ps.setInt(9, qtd);
            ps.setString(10, observacoes);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        throw new SQLException("Falha ao criar lote para produto " + produtoId);
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
