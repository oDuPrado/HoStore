// src/controller/VendaController.java
package controller;

import service.VendaService;
import model.VendaItemModel;
import model.VendaModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de venda – mantém o carrinho em memória
 * e delega o fechamento à camada de serviço.
 */
public class VendaController {

    private final VendaService vendaService = new VendaService();
    private final List<VendaItemModel> carrinho = new ArrayList<>();

    // =========================
    // Carrinho (produtos selecionados)
    // =========================
    public void adicionarItem(VendaItemModel item) {
        carrinho.add(item);
    }

    public void limparCarrinho() {
        carrinho.clear();
    }

    public List<VendaItemModel> getCarrinho() {
        return carrinho;
    }

    // =========================
    // Resumo financeiro (usado na UI)
    // =========================
    public double getTotalBruto() {
        return carrinho.stream()
                .mapToDouble(it -> it.getQtd() * it.getPreco())
                .sum();
    }

    public double getTotalDesconto() {
        return carrinho.stream()
                .mapToDouble(it -> it.getQtd() * it.getPreco() * it.getDesconto() / 100.0)
                .sum();
    }

    public double getTotalLiquido() {
        return getTotalBruto() - getTotalDesconto();
    }

    // =========================
    // Finaliza e grava a venda
    // =========================
    public int finalizar(String clienteId, String forma, int parcelas,
            int intervaloDias, String dataPrimeiroVencimento) throws Exception {

        double totalBruto = getTotalBruto();
        double totalDesconto = getTotalDesconto();
        double totalLiquido = getTotalLiquido();

        VendaModel venda = factory.VendaFactory.criarVenda(
                clienteId,
                totalBruto,
                totalDesconto,
                forma,
                parcelas);

        return vendaService.finalizarVenda(venda, carrinho);
    }
}
