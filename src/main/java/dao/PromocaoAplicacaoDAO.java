package dao;

import model.PromocaoAplicacaoModel;
import model.PromocaoDesempenhoModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PromocaoAplicacaoDAO {

    public void inserir(PromocaoAplicacaoModel m, Connection c) throws Exception {
        String sql = """
            INSERT INTO promocoes_aplicacoes
            (id, promocao_id, venda_id, venda_item_id, produto_id, cliente_id, qtd,
             preco_original, desconto_valor, preco_final, desconto_tipo, data_aplicacao)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getPromocaoId());
            if (m.getVendaId() == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, m.getVendaId());
            if (m.getVendaItemId() == null) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, m.getVendaItemId());
            ps.setString(5, m.getProdutoId());
            ps.setString(6, m.getClienteId());
            if (m.getQtd() == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, m.getQtd());
            ps.setDouble(8, m.getPrecoOriginal());
            ps.setDouble(9, m.getDescontoValor());
            ps.setDouble(10, m.getPrecoFinal());
            ps.setString(11, m.getDescontoTipo());
            ps.setString(12, m.getDataAplicacao());
            ps.executeUpdate();
        }
    }

    public List<PromocaoDesempenhoModel> listarDesempenho(String dataIni, String dataFim) throws Exception {
        List<PromocaoDesempenhoModel> out = new ArrayList<>();
        String sql = """
            SELECT p.id AS promocao_id, p.nome AS nome,
                   COALESCE(SUM(a.qtd),0) AS qtd_itens,
                   COALESCE(SUM(a.desconto_valor),0) AS desconto_total,
                   COALESCE(SUM(a.preco_final),0) AS faturamento,
                   COALESCE(AVG(a.preco_final),0) AS preco_medio
            FROM promocoes_aplicacoes a
            LEFT JOIN promocoes p ON p.id = a.promocao_id
            WHERE date(a.data_aplicacao) BETWEEN date(?) AND date(?)
            GROUP BY p.id, p.nome
            ORDER BY desconto_total DESC
            """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dataIni);
            ps.setString(2, dataFim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PromocaoDesempenhoModel m = new PromocaoDesempenhoModel();
                    m.promocaoId = rs.getString("promocao_id");
                    m.nome = rs.getString("nome");
                    m.qtdItens = rs.getInt("qtd_itens");
                    m.descontoTotal = rs.getDouble("desconto_total");
                    m.faturamentoGerado = rs.getDouble("faturamento");
                    m.precoMedio = rs.getDouble("preco_medio");
                    out.add(m);
                }
            }
        }
        return out;
    }

    public List<PromocaoAplicacaoModel> listarPorPromocao(String promocaoId) throws Exception {
        List<PromocaoAplicacaoModel> out = new ArrayList<>();
        String sql = """
            SELECT * FROM promocoes_aplicacoes
            WHERE promocao_id = ?
            ORDER BY date(data_aplicacao) DESC
            """;
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, promocaoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PromocaoAplicacaoModel m = new PromocaoAplicacaoModel();
                    m.setId(rs.getString("id"));
                    m.setPromocaoId(rs.getString("promocao_id"));
                    m.setVendaId(rs.getInt("venda_id"));
                    m.setVendaItemId(rs.getInt("venda_item_id"));
                    m.setProdutoId(rs.getString("produto_id"));
                    m.setClienteId(rs.getString("cliente_id"));
                    m.setQtd(rs.getInt("qtd"));
                    m.setPrecoOriginal(rs.getDouble("preco_original"));
                    m.setDescontoValor(rs.getDouble("desconto_valor"));
                    m.setPrecoFinal(rs.getDouble("preco_final"));
                    m.setDescontoTipo(rs.getString("desconto_tipo"));
                    m.setDataAplicacao(rs.getString("data_aplicacao"));
                    out.add(m);
                }
            }
        }
        return out;
    }
}
