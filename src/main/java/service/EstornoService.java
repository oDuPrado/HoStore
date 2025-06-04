package service;

import dao.VendaDevolucaoDAO;
import dao.ProdutoDAO;
import model.VendaItemModel;
import model.VendaDevolucaoModel;
import model.ProdutoModel;
import model.MovimentacaoEstoqueModel;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsável por registrar estornos de venda:
 *  1. Registra devoluções com motivo "Estorno".
 *  2. Reverte estoque dos itens estornados.
 *  3. Registra movimentações no histórico com motivo "Estorno da Venda #".
 */
public class EstornoService {

    private final VendaDevolucaoDAO devolucaoDAO = new VendaDevolucaoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final MovimentacaoEstoqueService movService = new MovimentacaoEstoqueService();

    /**
     * Registra um estorno parcial de um item da venda.
     */
    public void estornarItem(Connection c, int vendaId, VendaItemModel item, int qtdEstorno) throws Exception {
        if (qtdEstorno <= 0) return;

        // 1. Registra no vendas_devolucoes com motivo específico
        VendaDevolucaoModel dev = new VendaDevolucaoModel();
        dev.setVendaId(vendaId);
        dev.setProdutoId(item.getProdutoId());
        dev.setQuantidade(qtdEstorno);
        dev.setValor(item.getPreco());
        dev.setData(LocalDate.now());
        dev.setMotivo("Estorno parcial");

        devolucaoDAO.inserir(dev);

        // 2. Repor no estoque
        ProdutoModel produto = produtoDAO.findById(item.getProdutoId());
        if (produto != null) {
            produto.setQuantidade(produto.getQuantidade() + qtdEstorno);
            produtoDAO.update(produto);

            // 3. Registra entrada no histórico com motivo "Estorno da venda #"
            MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                    item.getProdutoId(),
                    "entrada",
                    qtdEstorno,
                    "Estorno da venda #" + vendaId,
                    "sistema"
            );
            mov.setData(LocalDateTime.now());
            movService.registrar(mov);
        } else {
            System.err.println("[ERRO] Produto não encontrado para estorno: " + item.getProdutoId());
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
