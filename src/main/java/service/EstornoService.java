package service;

import dao.VendaDevolucaoDAO;
import dao.VendaDevolucaoLoteDAO;
import dao.VendaItemLoteDAO;
import model.VendaItemModel;
import model.VendaDevolucaoModel;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Service responsavel por registrar estornos de venda:
 *  1. Registra devolucao com motivo de estorno.
 *  2. Reverte estoque dos itens estornados nos lotes originais.
 */
public class EstornoService {

    private final VendaDevolucaoDAO devolucaoDAO = new VendaDevolucaoDAO();
    private final VendaItemLoteDAO itemLoteDAO = new VendaItemLoteDAO();
    private final VendaDevolucaoLoteDAO devolucaoLoteDAO = new VendaDevolucaoLoteDAO();
    private final ProdutoEstoqueService produtoEstoqueService = new ProdutoEstoqueService();

    /**
     * Registra um estorno parcial de um item da venda.
     */
    public void estornarItem(Connection c, int vendaId, VendaItemModel item, int qtdEstorno) throws Exception {
        if (qtdEstorno <= 0) return;

        VendaDevolucaoModel dev = new VendaDevolucaoModel();
        dev.setVendaId(vendaId);
        dev.setProdutoId(item.getProdutoId());
        dev.setQuantidade(qtdEstorno);
        dev.setValor(item.getPreco());
        dev.setData(LocalDate.now());
        dev.setMotivo("Estorno parcial");

        int devolucaoId = devolucaoDAO.inserir(dev, c);

        int restante = qtdEstorno;
        List<VendaItemLoteDAO.ConsumoLote> consumos = itemLoteDAO.listarConsumoPorLote(
                vendaId,
                item.getProdutoId(),
                c);

        for (VendaItemLoteDAO.ConsumoLote consumo : consumos) {
            if (restante <= 0) break;

            int devolvido = itemLoteDAO.somarDevolvidoNoLote(
                    vendaId,
                    item.getProdutoId(),
                    consumo.loteId,
                    c);
            int disponivel = consumo.qtdConsumida - devolvido;
            if (disponivel <= 0) continue;

            int qtd = Math.min(restante, disponivel);
            produtoEstoqueService.reporNoLote(
                    item.getProdutoId(),
                    consumo.loteId,
                    qtd,
                    "Estorno da venda #" + vendaId,
                    "sistema",
                    c);

            devolucaoLoteDAO.inserir(devolucaoId, consumo.loteId, qtd, consumo.custoUnit, c);
            restante -= qtd;
        }

        if (restante > 0) {
            throw new Exception("Quantidade estornada excede o consumido nos lotes");
        }
    }

    /**
     * Registra estorno total de todos os itens da venda.
     */
    public void estornarVendaCompleta(Connection c, int vendaId, List<VendaItemModel> itens) throws Exception {
        for (VendaItemModel item : itens) {
            estornarItem(c, vendaId, item, item.getQtd());
        }
    }
}
