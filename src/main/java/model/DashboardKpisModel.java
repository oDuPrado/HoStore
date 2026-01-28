package model;

import java.util.ArrayList;
import java.util.List;

public class DashboardKpisModel {
    // período selecionado
    public PeriodoFiltro periodo;

    // KPIs principais (período)
    public double faturamentoLiquido;
    public double faturamentoBruto;
    public double lucroEstimado;
    public double margemPct;        // lucro/faturamento liquido
    public int qtdVendas;
    public double ticketMedio;
    public int itensVendidos;

    // descontos / acrescimos
    public double descontoTotal;
    public double acrescimoTotal;

    // devoluções / estornos / cancelamentos
    public int devolucoesQtd;
    public double devolucoesValor;
    public double estornosValor;
    public int cancelamentosQtd;

    // mix de pagamento
    public List<PagamentoMixItemModel> mixPagamentos = new ArrayList<>();
    public double taxaCartaoEstimada; // simplificado
    public double taxaCartaoLojista;
    public double taxaCartaoCliente;

    // alertas (gerais)
    public int docsFiscaisPendentes;
    public double receberVencido;
    public double pagarVencido;

    // comparativos
    public ComparativoModel cmpFaturamento;
    public ComparativoModel cmpLucro;
}
