package service;

import dao.RelatoriosDAO;
import dao.PromocaoAplicacaoDAO;
import model.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RelatoriosService {

    private final RelatoriosDAO dao = new RelatoriosDAO();
    private final PromocaoAplicacaoDAO promoDAO = new PromocaoAplicacaoDAO();

    // Cache simples (evita martelar o SQLite se o usuário ficar clicando em tudo)
    private static final long TTL_MS = 60_000; // 60s
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        Object value;
        long at;
        CacheEntry(Object v, long at) { this.value = v; this.at = at; }
    }

    private <T> T cached(String key, java.util.function.Supplier<T> loader) {
        long now = Instant.now().toEpochMilli();
        CacheEntry e = cache.get(key);
        if (e != null && (now - e.at) <= TTL_MS) return (T) e.value;
        T v = loader.get();
        cache.put(key, new CacheEntry(v, now));
        return v;
    }

    public DashboardHomeModel carregarDashboardHome(PeriodoFiltro periodo, int estoqueThreshold) {
        String key = "home:" + periodo.getInicioIso() + ":" + periodo.getFimIso() + ":thr=" + estoqueThreshold;

        return cached(key, () -> {
            DashboardHomeModel home = new DashboardHomeModel();

            DashboardKpisModel atual = dao.carregarKpisPeriodo(periodo);
            PeriodoFiltro anterior = periodo.periodoAnteriorMesmoTamanho();
            DashboardKpisModel ant = dao.carregarKpisPeriodo(anterior);

            atual.cmpFaturamento = ComparativoModel.of(atual.faturamentoLiquido, ant.faturamentoLiquido);
            atual.cmpLucro = ComparativoModel.of(atual.lucroEstimado, ant.lucroEstimado);

            home.kpis = atual;
            home.estoqueBaixo = dao.listarEstoqueBaixo(estoqueThreshold, 20);
            home.topProdutosQtd = dao.listarTopProdutosPorQtd(periodo, 10);
            home.topProdutosTotal = dao.listarTopProdutosPorTotal(periodo, 10);
            home.vendasPorDia = dao.listarVendasPorDia(periodo);
            home.encalhados = dao.listarEncalhados(periodo, 20);

            return home;
        });
    }

    public java.util.List<PromocaoDesempenhoModel> listarDesempenhoPromocoes(PeriodoFiltro periodo) {
        String key = "promos:" + periodo.getInicioIso() + ":" + periodo.getFimIso();
        return cached(key, () -> {
            try {
                return promoDAO.listarDesempenho(periodo.getInicioIso(), periodo.getFimIso());
            } catch (Exception e) {
                throw new RuntimeException("Falha ao listar promocoes: " + e.getMessage(), e);
            }
        });
    }

    // Drill-down / relatórios específicos
    public java.util.List<ParcelaVencidaModel> listarVencidosReceber(int limit) {
        return cached("vencidosReceber:" + limit, () -> dao.listarVencidosReceber(limit));
    }

    public java.util.List<ParcelaVencidaModel> listarVencidosPagar(int limit) {
        return cached("vencidosPagar:" + limit, () -> dao.listarVencidosPagar(limit));
    }

    public java.util.List<DocFiscalPendenteModel> listarDocsFiscaisPendentes(int limit) {
        return cached("docsPend:" + limit, () -> dao.listarDocsFiscaisPendentes(limit));
    }

    public java.util.List<MovEstoqueModel> listarUltimasMovimentacoesEstoque(int limit) {
        return cached("movEstoque:" + limit, () -> dao.listarUltimasMovimentacoesEstoque(limit));
    }
}
