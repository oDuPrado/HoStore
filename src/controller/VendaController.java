package controller;

import service.VendaService;
import model.*;

import java.util.*;

public class VendaController {
    private VendaService vendaService = new VendaService();
    private List<VendaItemModel> carrinho = new ArrayList<>();

    public void adicionarItem(VendaItemModel it) {
        carrinho.add(it);
    }

    public void limparCarrinho() {
        carrinho.clear();
    }

    public List<VendaItemModel> getCarrinho() {
        return carrinho;
    }

    public int finalizar(String clienteId, String forma, int parcelas) throws Exception {
        double totalBruto = 0;
        double totalDesconto = 0;

        for (VendaItemModel item : carrinho) {
            double itemBruto = item.getQtd() * item.getPreco();
            totalBruto += itemBruto;
            totalDesconto += itemBruto * item.getDesconto() / 100.0;
        }

        double totalLiquido = totalBruto - totalDesconto;

        VendaModel venda = factory.VendaFactory.criarVenda(
            clienteId,
            totalBruto,
            totalDesconto,
            forma,
            parcelas
        );

        return vendaService.finalizarVenda(venda, carrinho);
    }
}
