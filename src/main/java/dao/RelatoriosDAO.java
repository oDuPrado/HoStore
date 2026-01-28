package dao;

import model.*;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fonte única da verdade.
 * Dashboard e Relatórios usam esse DAO pra não virar dois sistemas com números divergentes.
 */
public class RelatoriosDAO {

    public DashboardKpisModel carregarKpisPeriodo(PeriodoFiltro periodo) {
        DashboardKpisModel k = new DashboardKpisModel();
        k.periodo = periodo;

        try (Connection c = DB.get()) {

            // Faturamento bruto, qtd vendas, descontos, acréscimos, cancelamentos
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT
                  COALESCE(SUM(CASE WHEN status <> 'cancelada' THEN total_liquido ELSE 0 END), 0) AS faturamento_bruto,
                  COALESCE(SUM(CASE WHEN status <> 'cancelada' THEN total_bruto ELSE 0 END), 0) AS bruto,
                  COALESCE(SUM(CASE WHEN status <> 'cancelada' THEN desconto ELSE 0 END), 0) AS desconto_total,
                  COALESCE(SUM(CASE WHEN status <> 'cancelada' THEN acrescimo ELSE 0 END), 0) AS acrescimo_total,
                  SUM(CASE WHEN status='cancelada' THEN 1 ELSE 0 END) AS cancelamentos_qtd,
                  SUM(CASE WHEN status <> 'cancelada' THEN 1 ELSE 0 END) AS vendas_qtd
                FROM vendas
                WHERE date(data_venda) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        k.faturamentoBruto = rs.getDouble("faturamento_bruto");
                        k.qtdVendas = rs.getInt("vendas_qtd");
                        k.descontoTotal = rs.getDouble("desconto_total");
                        k.acrescimoTotal = rs.getDouble("acrescimo_total");
                        k.cancelamentosQtd = rs.getInt("cancelamentos_qtd");
                    }
                }
            }

            // Itens vendidos líquidos (vendas - devoluções no período)
            int itensVendas = 0;
            int itensDevolvidos = 0;
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
                    if (rs.next()) itensVendas = rs.getInt("itens");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(d.qtd), 0) AS itens
                FROM vendas_devolucoes d
                JOIN vendas v ON v.id = d.venda_id
                WHERE v.status <> 'cancelada'
                  AND date(COALESCE(d.data, v.data_venda)) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) itensDevolvidos = rs.getInt("itens");
                }
            }
            k.itensVendidos = Math.max(0, itensVendas - itensDevolvidos);

            // Lucro estimado líquido (vendas - devoluções no período)
            double lucroBruto = 0.0;
            double lucroDevolucao = 0.0;
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
                    if (rs.next()) lucroBruto = rs.getDouble("lucro");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM((COALESCE(d.valor_unit,0) * d.qtd) - COALESCE(c.custo_total, d.qtd * COALESCE(p.preco_compra,0))), 0) AS lucro
                FROM vendas_devolucoes d
                JOIN vendas v ON v.id = d.venda_id
                LEFT JOIN (
                    SELECT devolucao_id, SUM(qtd * COALESCE(custo_unit,0)) AS custo_total
                    FROM vendas_devolucoes_lotes
                    GROUP BY devolucao_id
                ) c ON c.devolucao_id = d.id
                LEFT JOIN produtos p ON p.id = d.produto_id
                WHERE v.status <> 'cancelada'
                  AND date(COALESCE(d.data, v.data_venda)) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) lucroDevolucao = rs.getDouble("lucro");
                }
            }
            k.lucroEstimado = lucroBruto - lucroDevolucao;

            // Devoluções (qtd e valor)
            // OBS: vendas_devolucoes tem valor_unit. Se estiver 0, você já sabe que ainda tem bug em algum fluxo.
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT
                  COALESCE(SUM(d.qtd), 0) AS qtd,
                  COALESCE(SUM(d.qtd * COALESCE(d.valor_unit, 0)), 0) AS valor
                FROM vendas_devolucoes d
                JOIN vendas v ON v.id = d.venda_id
                WHERE v.status <> 'cancelada'
                  AND date(COALESCE(d.data, v.data_venda)) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        k.devolucoesQtd = rs.getInt("qtd");
                        k.devolucoesValor = rs.getDouble("valor");
                    }
                }
            }

            // Estornos (valor)
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT COALESCE(SUM(e.valor_estornado), 0) AS valor
                FROM vendas_estornos_pagamentos e
                JOIN vendas v ON v.id = e.venda_id
                WHERE v.status <> 'cancelada'
                  AND date(COALESCE(e.data, v.data_venda)) BETWEEN date(?) AND date(?)
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) k.estornosValor = rs.getDouble("valor");
                }
            }

            k.faturamentoLiquido = k.faturamentoBruto - k.devolucoesValor - k.estornosValor;
            k.ticketMedio = (k.qtdVendas <= 0) ? 0 : (k.faturamentoLiquido / k.qtdVendas);
            k.margemPct = (Math.abs(k.faturamentoLiquido) < 0.0000001) ? 0 : (k.lucroEstimado / k.faturamentoLiquido);

            // Mix de pagamentos (vendas_pagamentos)
            double totalPag = queryDouble(c, """
                SELECT COALESCE(SUM(vp.valor),0)
                FROM vendas_pagamentos vp
                JOIN vendas v ON v.id = vp.venda_id
                WHERE v.status <> 'cancelada'
                  AND date(v.data_venda) BETWEEN date(?) AND date(?)
            """, periodo.getInicioIso(), periodo.getFimIso());

            try (PreparedStatement ps = c.prepareStatement("""
                SELECT vp.tipo AS tipo, COALESCE(SUM(vp.valor),0) AS total
                FROM vendas_pagamentos vp
                JOIN vendas v ON v.id = vp.venda_id
                WHERE v.status <> 'cancelada'
                  AND date(v.data_venda) BETWEEN date(?) AND date(?)
                GROUP BY vp.tipo
                ORDER BY total DESC
            """)) {
                ps.setString(1, periodo.getInicioIso());
                ps.setString(2, periodo.getFimIso());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String tipo = rs.getString("tipo");
                        double valor = rs.getDouble("total");
                        double pct = (Math.abs(totalPag) < 0.0000001) ? 0 : (valor / totalPag);
                        k.mixPagamentos.add(new PagamentoMixItemModel(tipo, valor, pct));
                    }
                }
            }

            // Taxa de cartão estimada (simplificada):
            // - Se tiver tabela taxas_cartao, isso depende de bandeira/tipo/parcelas/mes.
            // - Como vendas_pagamentos não guarda bandeira/parcelas por pagamento, estimamos pela coluna forma_pagamento + parcelas na venda.
            // - Se você quiser precisão, precisa modelar pagamento com metadados (bandeira, tipo, parcelas).
            k.taxaCartaoEstimada = estimarTaxaCartaoSimplificada(c, periodo);

            // Alertas gerais (fiscal + vencidos)
            k.docsFiscaisPendentes = (int) queryDouble(c, """
                SELECT COUNT(*)
                FROM documentos_fiscais
                WHERE status NOT IN ('AUTORIZADO','CANCELADO')
            """);

            k.receberVencido = queryDouble(c, """
                SELECT COALESCE(SUM(valor_nominal - valor_pago),0)
                FROM parcelas_contas_receber
                WHERE status='aberto' AND date(vencimento) < date('now')
            """);

            k.pagarVencido = queryDouble(c, """
                SELECT COALESCE(SUM(valor_nominal - valor_pago),0)
                FROM parcelas_contas_pagar
                WHERE status='aberto' AND date(vencimento) < date('now')
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Falha ao carregar KPIs: " + e.getMessage(), e);
        }

        return k;
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
                    list.add(new EstoqueBaixoItemModel(rs.getString("id"), rs.getString("nome"), rs.getInt("quantidade")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar estoque baixo: " + e.getMessage(), e);
        }
        return list;
    }

    public List<EstoqueBaixoItemModel> listarEncalhados(PeriodoFiltro periodo, int limit) {
        // Produtos com estoque > 0 e ZERO vendas no período
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
                WHERE COALESCE(s.qtd,0) > 0
                  AND p.id NOT IN (
                    SELECT DISTINCT vi.produto_id
                    FROM vendas_itens vi
                    JOIN vendas v ON v.id = vi.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(v.data_venda) BETWEEN date(?) AND date(?)
                  )
                ORDER BY quantidade DESC, p.nome ASC
                LIMIT ?
             """)) {
            ps.setString(1, periodo.getInicioIso());
            ps.setString(2, periodo.getFimIso());
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new EstoqueBaixoItemModel(rs.getString("id"), rs.getString("nome"), rs.getInt("quantidade")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar encalhados: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ProdutoVendaResumoModel> listarTopProdutosPorQtd(PeriodoFiltro periodo, int limit) {
        List<ProdutoVendaResumoModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                WITH vendas_cte AS (
                    SELECT vi.produto_id AS produto_id,
                           COALESCE(SUM(vi.qtd),0) AS qtd,
                           COALESCE(SUM(vi.total_item),0) AS total
                    FROM vendas_itens vi
                    JOIN vendas v ON v.id = vi.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(v.data_venda) BETWEEN date(?) AND date(?)
                    GROUP BY vi.produto_id
                ),
                dev AS (
                    SELECT d.produto_id AS produto_id,
                           COALESCE(SUM(d.qtd),0) AS qtd,
                           COALESCE(SUM(d.qtd * COALESCE(d.valor_unit,0)),0) AS total
                    FROM vendas_devolucoes d
                    JOIN vendas v ON v.id = d.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(COALESCE(d.data, v.data_venda)) BETWEEN date(?) AND date(?)
                    GROUP BY d.produto_id
                ),
                ids AS (
                    SELECT produto_id FROM vendas_cte
                    UNION
                    SELECT produto_id FROM dev
                )
                SELECT
                  i.produto_id AS produto_id,
                  COALESCE(p.nome, '[Produto removido]') AS nome,
                  (COALESCE(v.qtd,0) - COALESCE(d.qtd,0)) AS qtd,
                  (COALESCE(v.total,0) - COALESCE(d.total,0)) AS total
                FROM ids i
                LEFT JOIN vendas_cte v ON v.produto_id = i.produto_id
                LEFT JOIN dev d ON d.produto_id = i.produto_id
                LEFT JOIN produtos p ON p.id = i.produto_id
                WHERE (COALESCE(v.qtd,0) - COALESCE(d.qtd,0)) > 0
                   OR (COALESCE(v.total,0) - COALESCE(d.total,0)) > 0
                ORDER BY qtd DESC, total DESC
                LIMIT ?
             """)) {
            ps.setString(1, periodo.getInicioIso());
            ps.setString(2, periodo.getFimIso());
            ps.setString(3, periodo.getInicioIso());
            ps.setString(4, periodo.getFimIso());
            ps.setInt(5, limit);
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
            throw new RuntimeException("Falha ao listar top produtos (qtd): " + e.getMessage(), e);
        }
        return list;
    }

    public List<ProdutoVendaResumoModel> listarTopProdutosPorTotal(PeriodoFiltro periodo, int limit) {
        List<ProdutoVendaResumoModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                WITH vendas_cte AS (
                    SELECT vi.produto_id AS produto_id,
                           COALESCE(SUM(vi.qtd),0) AS qtd,
                           COALESCE(SUM(vi.total_item),0) AS total
                    FROM vendas_itens vi
                    JOIN vendas v ON v.id = vi.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(v.data_venda) BETWEEN date(?) AND date(?)
                    GROUP BY vi.produto_id
                ),
                dev AS (
                    SELECT d.produto_id AS produto_id,
                           COALESCE(SUM(d.qtd),0) AS qtd,
                           COALESCE(SUM(d.qtd * COALESCE(d.valor_unit,0)),0) AS total
                    FROM vendas_devolucoes d
                    JOIN vendas v ON v.id = d.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(COALESCE(d.data, v.data_venda)) BETWEEN date(?) AND date(?)
                    GROUP BY d.produto_id
                ),
                ids AS (
                    SELECT produto_id FROM vendas_cte
                    UNION
                    SELECT produto_id FROM dev
                )
                SELECT
                  i.produto_id AS produto_id,
                  COALESCE(p.nome, '[Produto removido]') AS nome,
                  (COALESCE(v.qtd,0) - COALESCE(d.qtd,0)) AS qtd,
                  (COALESCE(v.total,0) - COALESCE(d.total,0)) AS total
                FROM ids i
                LEFT JOIN vendas_cte v ON v.produto_id = i.produto_id
                LEFT JOIN dev d ON d.produto_id = i.produto_id
                LEFT JOIN produtos p ON p.id = i.produto_id
                WHERE (COALESCE(v.qtd,0) - COALESCE(d.qtd,0)) > 0
                   OR (COALESCE(v.total,0) - COALESCE(d.total,0)) > 0
                ORDER BY total DESC, qtd DESC
                LIMIT ?
             """)) {
            ps.setString(1, periodo.getInicioIso());
            ps.setString(2, periodo.getFimIso());
            ps.setString(3, periodo.getInicioIso());
            ps.setString(4, periodo.getFimIso());
            ps.setInt(5, limit);
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
            throw new RuntimeException("Falha ao listar top produtos (total): " + e.getMessage(), e);
        }
        return list;
    }

    public List<SerieDiariaModel> listarVendasPorDia(PeriodoFiltro periodo) {
        List<SerieDiariaModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                WITH vendas_cte AS (
                    SELECT date(v.data_venda) AS dia,
                           COUNT(*) AS qtd_vendas,
                           COALESCE(SUM(v.total_liquido),0) AS total
                    FROM vendas v
                    WHERE v.status <> 'cancelada'
                      AND date(v.data_venda) BETWEEN date(?) AND date(?)
                    GROUP BY date(v.data_venda)
                ),
                dev AS (
                    SELECT date(COALESCE(d.data, v.data_venda)) AS dia,
                           COALESCE(SUM(d.qtd * COALESCE(d.valor_unit,0)),0) AS total
                    FROM vendas_devolucoes d
                    JOIN vendas v ON v.id = d.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(COALESCE(d.data, v.data_venda)) BETWEEN date(?) AND date(?)
                    GROUP BY date(COALESCE(d.data, v.data_venda))
                ),
                est AS (
                    SELECT date(COALESCE(e.data, v.data_venda)) AS dia,
                           COALESCE(SUM(e.valor_estornado),0) AS total
                    FROM vendas_estornos_pagamentos e
                    JOIN vendas v ON v.id = e.venda_id
                    WHERE v.status <> 'cancelada'
                      AND date(COALESCE(e.data, v.data_venda)) BETWEEN date(?) AND date(?)
                    GROUP BY date(COALESCE(e.data, v.data_venda))
                )
                SELECT v.dia AS dia,
                       v.qtd_vendas AS qtd_vendas,
                       (COALESCE(v.total,0) - COALESCE(d.total,0) - COALESCE(e.total,0)) AS total
                FROM vendas_cte v
                LEFT JOIN dev d ON d.dia = v.dia
                LEFT JOIN est e ON e.dia = v.dia
                ORDER BY v.dia ASC
             """)) {
            ps.setString(1, periodo.getInicioIso());
            ps.setString(2, periodo.getFimIso());
            ps.setString(3, periodo.getInicioIso());
            ps.setString(4, periodo.getFimIso());
            ps.setString(5, periodo.getInicioIso());
            ps.setString(6, periodo.getFimIso());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SerieDiariaModel(rs.getString("dia"), rs.getInt("qtd_vendas"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar série diária: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ParcelaVencidaModel> listarVencidosReceber(int limit) {
        List<ParcelaVencidaModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                SELECT
                  COALESCE(cli.nome, '[Sem cliente]') AS nome,
                  date(p.vencimento) AS venc,
                  (p.valor_nominal - p.valor_pago) AS aberto,
                  CAST((julianday('now') - julianday(date(p.vencimento))) AS INTEGER) AS dias
                FROM parcelas_contas_receber p
                LEFT JOIN titulos_contas_receber t ON t.id = p.titulo_id
                LEFT JOIN clientes cli ON cli.id = t.cliente_id
                WHERE p.status='aberto'
                  AND date(p.vencimento) < date('now')
                  AND (p.valor_nominal - p.valor_pago) > 0
                ORDER BY date(p.vencimento) ASC
                LIMIT ?
             """)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ParcelaVencidaModel("RECEBER", rs.getString("nome"),
                            rs.getString("venc"), rs.getDouble("aberto"), rs.getInt("dias")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar vencidos (receber): " + e.getMessage(), e);
        }
        return list;
    }

    public List<ParcelaVencidaModel> listarVencidosPagar(int limit) {
        List<ParcelaVencidaModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                SELECT
                  COALESCE(f.nome, '[Sem fornecedor]') AS nome,
                  date(p.vencimento) AS venc,
                  (p.valor_nominal - p.valor_pago) AS aberto,
                  CAST((julianday('now') - julianday(date(p.vencimento))) AS INTEGER) AS dias
                FROM parcelas_contas_pagar p
                LEFT JOIN titulos_contas_pagar t ON t.id = p.titulo_id
                LEFT JOIN fornecedores f ON f.id = t.fornecedor_id
                WHERE p.status='aberto'
                  AND date(p.vencimento) < date('now')
                  AND (p.valor_nominal - p.valor_pago) > 0
                ORDER BY date(p.vencimento) ASC
                LIMIT ?
             """)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ParcelaVencidaModel("PAGAR", rs.getString("nome"),
                            rs.getString("venc"), rs.getDouble("aberto"), rs.getInt("dias")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar vencidos (pagar): " + e.getMessage(), e);
        }
        return list;
    }

    public List<DocFiscalPendenteModel> listarDocsFiscaisPendentes(int limit) {
        List<DocFiscalPendenteModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                SELECT id, venda_id, modelo, serie, numero, ambiente, status, erro
                FROM documentos_fiscais
                WHERE status NOT IN ('AUTORIZADO','CANCELADO')
                ORDER BY criado_em DESC
                LIMIT ?
             """)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocFiscalPendenteModel d = new DocFiscalPendenteModel();
                    d.id = rs.getString("id");
                    d.vendaId = rs.getInt("venda_id");
                    d.modelo = rs.getString("modelo");
                    d.serie = rs.getInt("serie");
                    d.numero = rs.getInt("numero");
                    d.ambiente = rs.getString("ambiente");
                    d.status = rs.getString("status");
                    d.erro = rs.getString("erro");
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar docs fiscais pendentes: " + e.getMessage(), e);
        }
        return list;
    }

    public List<MovEstoqueModel> listarUltimasMovimentacoesEstoque(int limit) {
        List<MovEstoqueModel> list = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("""
                SELECT
                  em.id,
                  em.produto_id,
                  COALESCE(p.nome, '[Produto removido]') AS produto_nome,
                  em.tipo_mov,
                  COALESCE(em.quantidade, 0) AS quantidade,
                  em.motivo,
                  em.data,
                  em.usuario
                FROM estoque_movimentacoes em
                LEFT JOIN produtos p ON p.id = em.produto_id
                ORDER BY em.id DESC
                LIMIT ?
             """)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovEstoqueModel m = new MovEstoqueModel();
                    m.id = rs.getInt("id");
                    m.produtoId = rs.getString("produto_id");
                    m.produtoNome = rs.getString("produto_nome");
                    m.tipoMov = rs.getString("tipo_mov");
                    m.quantidade = rs.getInt("quantidade");
                    m.motivo = rs.getString("motivo");
                    m.dataIso = rs.getString("data");
                    m.usuario = rs.getString("usuario");
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar movimentações estoque: " + e.getMessage(), e);
        }
        return list;
    }

    // -------------------- Helpers --------------------

    private double queryDouble(Connection c, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    private double queryDouble(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private double estimarTaxaCartaoSimplificada(Connection c, PeriodoFiltro periodo) throws SQLException {
        // Estratégia simples:
        // Se a venda forma_pagamento contém "CARTAO" então aplica taxa média ponderada do mês vigente (taxas_cartao),
        // senão zero. (É estimativa, mas já dá sinal de “taxa comendo margem”.)
        //
        // Se quiser 100% real: tem que guardar bandeira/tipo no pagamento, e aí calcular corretamente.
        double taxaMediaMes = queryDouble(c, """
            SELECT COALESCE(AVG(taxa_pct),0) / 100.0
            FROM taxas_cartao
            WHERE mes_vigencia = strftime('%Y-%m','now')
        """);

        double totalCartao = queryDouble(c, """
            SELECT COALESCE(SUM(total_liquido),0)
            FROM vendas
            WHERE status <> 'cancelada'
              AND UPPER(forma_pagamento) LIKE '%CARTAO%'
              AND date(data_venda) BETWEEN date(?) AND date(?)
        """, periodo.getInicioIso(), periodo.getFimIso());

        return totalCartao * taxaMediaMes;
    }
}
