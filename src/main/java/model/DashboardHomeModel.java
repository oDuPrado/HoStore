package model;

import java.util.ArrayList;
import java.util.List;

public class DashboardHomeModel {
    public DashboardKpisModel kpis;

    public List<EstoqueBaixoItemModel> estoqueBaixo = new ArrayList<>();
    public List<ProdutoVendaResumoModel> topProdutosQtd = new ArrayList<>();
    public List<ProdutoVendaResumoModel> topProdutosTotal = new ArrayList<>();
    public List<SerieDiariaModel> vendasPorDia = new ArrayList<>();

    // “dead stock” (zero vendas no período)
    public List<EstoqueBaixoItemModel> encalhados = new ArrayList<>();
}
