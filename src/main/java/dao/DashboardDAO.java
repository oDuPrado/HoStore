package dao;

import model.*;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO central de métricas.
 * Reutilizável para Dashboard e Relatórios (mesma fonte, mesmos números, mesma verdade).
 */
public class DashboardDAO {

    public DashboardResumoModel carregarResumo(PeriodoFiltro periodo) {
        DashboardResumoModel r = new DashboardResumoModel();

        try (Connection c = DB.get()) {
            // Faturamento / qtd vendas / ticket
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT
                  COALESCE(SUM(total_liquido), 0) AS faturamento,
                  COUNT(*) AS qtd_vendas,
                  COALESCE(AVG(total_liquido), 0) AS ticket_medio
                FROM vendas
                WHERE status <> 'cancelada'
                  AND date(data_venda) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        r.faturamento = rs.getDouble("faturamento");
                        r.qtdVendas = rs.getInt("qtd_vendas");
                        r.ticketMedio = rs.getDouble("ticket_medio");
                    }
                }
            }

            // Itens vendidos
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(vi.qtd), 0) AS itens
                FROM vendas_itens vi
                JOIN vendas v ON v.id = vi.venda_id
                WHERE v.status <> 'cancelada'
                  AND date(v.data_venda) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) r.itensVendidos = rs.getInt("itens");
                }
            }

            // Lucro estimado (preço real do item - custo do lote; fallback para custo do produto)
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM((vi.preco * vi.qtd) - COALESCE(c.custo_total, vi.qtd * COALESCE(p.preco_compra,0))), 0) AS lucro
                FROM vendas_itens vi
                JOIN vendas v ON v.id = vi.venda_id
                LEFT JOIN (
                    SELECT venda_item_id, SUM(qtd * COALESCE(custo_unit,0)) AS custo_total
                    FROM vendas_itens_lotes
                    GROUP BY venda_item_id
                ) c ON c.venda_item_id = vi.id
                LEFT JOIN produtos p ON p.id = vi.produto_id
                WHERE v.status <> 'cancelada'
                  AND date(v.data_venda) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) r.lucroEstimado = rs.getDouble("lucro");
                }
            }

            // Pendências fiscais (geral, não depende do período)
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COUNT(*) AS pendentes
                FROM documentos_fiscais
                WHERE status NOT IN ('AUTORIZADO','CANCELADO')
            """);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) r.docsFiscaisPendentes = rs.getInt("pendentes");
            }

            // Receber vencido (geral)
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(valor_nominal - valor_pago), 0) AS vencido
                FROM parcelas_contas_receber
                WHERE status = 'aberto'
                  AND date(vencimento) < date('now')
            """);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) r.receberVencido = rs.getDouble("vencido");
            }

            // Pagar vencido (geral)
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(valor_nominal - valor_pago), 0) AS vencido
                FROM parcelas_contas_pagar
                WHERE status = 'aberto'
                  AND date(vencimento) < date('now')
            """);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) r.pagarVencido = rs.getDouble("vencido");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Falha ao carregar resumo: " + e.getMessage(), e);
        }

        return r;
    }

    public List<EstoqueBaixoItemModel> listarEstoqueBaixo(int threshold, int limit) {
        List<EstoqueBaixoItemModel> list = new ArrayList<>();

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                WITH saldo AS (
                    SELECT produto_id, COALESCE(SUM(qtd_disponivel), 0) AS qtd
                    FROM estoque_lotes
                    GROUP BY produto_id
                )
                SELECT p.id, p.nome, COALESCE(s.qtd,0) AS quantidade
                FROM produtos p
                LEFT JOIN saldo s ON s.produto_id = p.id
                WHERE COALESCE(s.qtd,0) <= ?
                ORDER BY quantidade ASC, p.nome ASC
                LIMIT ?
             """)) {
            ps.setInt(1, threshold);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new EstoqueBaixoItemModel(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getInt("quantidade")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar estoque baixo: " + e.getMessage(), e);
        }

        return list;
    }

    public List<ProdutoVendaResumoModel> listarTopProdutos(PeriodoFiltro periodo, int limit) {
        List<ProdutoVendaResumoModel> list = new ArrayList<>();

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                SELECT
                  vi.produto_id AS produto_id,
                  COALESCE(p.nome, '[Produto removido]') AS nome,
                  COALESCE(SUM(vi.qtd), 0) AS qtd,
                  COALESCE(SUM(vi.total_item), 0) AS total
                FROM vendas_itens vi
                JOIN vendas v ON v.id = vi.venda_id
                LEFT JOIN produtos p ON p.id = vi.produto_id
                WHERE v.status <> 'cancelada'
                  AND date(v.data_venda) BETWEEN date(?) AND date(?)
                GROUP BY vi.produto_id
                ORDER BY qtd DESC, total DESC
                LIMIT ?
             """)) {
            ps.setString(1, periodo.getInicioIso());
            ps.setString(2, periodo.getFimIso());
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProdutoVendaResumoModel(
                            rs.getString("produto_id"),
                            rs.getString("nome"),
                            rs.getInt("qtd"),
                            rs.getDouble("total")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar top produtos: " + e.getMessage(), e);
        }

        return list;
    }

    public List<SerieDiariaModel> listarVendasPorDia(PeriodoFiltro periodo) {
        List<SerieDiariaModel> list = new ArrayList<>();

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                SELECT
                  date(v.data_venda) AS dia,
                  COUNT(*) AS qtd_vendas,
                  COALESCE(SUM(v.total_liquido), 0) AS total
                FROM vendas v
                WHERE v.status <> 'cancelada'
                  AND date(v.data_venda) BETWEEN date(?) AND date(?)
                GROUP BY date(v.data_venda)
                ORDER BY date(v.data_venda) ASC
             """)) {
            ps.setString(1, periodo.getInicioIso());
            ps.setString(2, periodo.getFimIso());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SerieDiariaModel(
                            rs.getString("dia"),
                            rs.getInt("qtd_vendas"),
                            rs.getDouble("total")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar vendas por dia: " + e.getMessage(), e);
        }

        return list;
    }
}
