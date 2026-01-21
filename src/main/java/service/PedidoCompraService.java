package service;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import util.LogService;

import java.util.List;
import java.util.Map;

/**
 * Service responsável por tratar regras de negócio dos pedidos de compra:
 * - Ao receber um pedido, atualiza o estoque de cada item incrementalmente.
 * - Registra a movimentação de entrada ou saída no histórico.
 * - Atualiza o status do pedido automaticamente.
 */
public class PedidoCompraService {

    // DAO para operações de pedido (status, etc.)
    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();

    // DAO para itens vinculados a um pedido de compra
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();

    // DAO para acessar e atualizar o estoque dos produtos
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    // Service de estoque (não criar dentro do loop)
    private final ProdutoEstoqueService estoqueService = new ProdutoEstoqueService();

    /**
     * Recebe todos os itens de um pedido de compra e ajusta o estoque
     * incrementalmente.
     *
     * Fluxo:
     * 1. Para cada itemId presente em mapaRecebimento:
     * a) Busca o item antigo no banco para saber a quantidade recebida anterior.
     * b) Calcula delta = qtdRecebidaNova - qtdRecebidaAnterior.
     * c) Se delta > 0, registra entrada (delta).
     * Se delta < 0, registra saída (abs(delta)) (correção de recebimento).
     * Se delta == 0, não altera o estoque.
     * d) Atualiza status do item ("pendente", "parcial" ou "completo") com base em
     * qtdRecebidaNova e qtdPedida.
     * e) Persiste no banco nova quantidadeRecebida e status.
     * 2. Ao final, chama pedidoDAO.recalcularStatus(pedidoId) para recalcular o
     * status geral do pedido.
     *
     * @param pedidoId        ID do pedido de compra a ser recebido.
     * @param mapaRecebimento mapa contendo (itemId → qtdRecebidaNova) para cada
     *                        linha da UI.
     * @param usuario         identificador de quem está registrando (ex:
     *                        "sistema").
     * @throws Exception se ocorrer falha no banco de dados.
     */
    public void receberPedido(String pedidoId, Map<String, Integer> mapaRecebimento, String usuario) throws Exception {
        LogService.audit("PEDIDO_RECEBIMENTO_INICIO", "pedido", pedidoId, "itens=" + mapaRecebimento.size());

        // (Opcional) carrega todos os itens só pra validação/contagem
        List<PedidoEstoqueProdutoModel> todosItens = itemDAO.listarPorPedido(pedidoId);
        if (todosItens == null || todosItens.isEmpty()) {
            LogService.audit("PEDIDO_SEM_ITENS", "pedido", pedidoId, "nenhum item encontrado");
            // ainda assim recalcula status pra manter consistência
            pedidoDAO.recalcularStatus(pedidoId);
            return;
        }

        // 2. Percorre cada entrada do mapa (itemId + qtdRecebidaNova)
        for (Map.Entry<String, Integer> entry : mapaRecebimento.entrySet()) {
            String itemId = entry.getKey();
            int qtdNovaRecebida = (entry.getValue() == null) ? 0 : entry.getValue();
            if (qtdNovaRecebida < 0)
                qtdNovaRecebida = 0;

            // 2.1) Lê do banco o "itemAntigo" para saber a quantidade recebida até agora
            PedidoEstoqueProdutoModel itemAntigo = itemDAO.buscarPorId(itemId);
            if (itemAntigo == null) {
                LogService.audit("PEDIDO_ITEM_INEXISTENTE", "pedido_item", itemId, "pedido=" + pedidoId);
                continue;
            }

            int qtdRecebidaAnterior = itemAntigo.getQuantidadeRecebida();
            int qtdPedida = itemAntigo.getQuantidadePedida();

            // (Opcional) trava pra não exceder o pedido (se você quiser permitir excedente,
            // remova)
            if (qtdNovaRecebida > qtdPedida)
                qtdNovaRecebida = qtdPedida;

            int delta = qtdNovaRecebida - qtdRecebidaAnterior;

            LogService.audit("PEDIDO_ITEM_PROCESSO", "pedido_item", itemId, "pedido=" + pedidoId);
            LogService.info("pedido item anterior qtd=" + qtdRecebidaAnterior);
            LogService.info("pedido item novo qtd=" + qtdNovaRecebida);
            LogService.info("pedido item delta=" + delta);

            // 2.2) Ajusta o estoque do produto somente se delta != 0
            if (delta != 0) {
                String produtoId = itemAntigo.getProdutoId();
                ProdutoModel produto = produtoDAO.findById(produtoId);

                if (produto == null) {
                    LogService.audit("PEDIDO_PRODUTO_INEXISTENTE", "produto", produtoId, "pedido=" + pedidoId);
                } else {
                    if (delta > 0) {
                        LogService.audit("PEDIDO_ENTRADA", "produto", produtoId,
                                "qtd=" + delta + " pedido=" + pedidoId);
                        estoqueService.registrarEntrada(
                                produtoId,
                                delta,
                                "Recebimento do Pedido " + pedidoId,
                                usuario);
                    } else {
                        int qtdASubtrair = Math.abs(delta);
                        LogService.audit("PEDIDO_CORRECAO", "produto", produtoId,
                                "qtd=" + qtdASubtrair + " pedido=" + pedidoId);
                        estoqueService.registrarSaida(
                                produtoId,
                                qtdASubtrair,
                                "Correcao de recebimento do Pedido " + pedidoId,
                                usuario);
                    }
                }
            } else {
                LogService.audit("PEDIDO_SEM_ALTERACAO", "pedido_item", itemId, "pedido=" + pedidoId);
            }

            // 2.3) Atualiza o status do item (pendente/parcial/completo)
            String novoStatus = (qtdNovaRecebida >= qtdPedida) ? "completo"
                    : (qtdNovaRecebida > 0) ? "parcial"
                            : "pendente";

            itemAntigo.setStatus(novoStatus);

            // 2.4) Persiste nova quantidadeRecebida e status
            itemAntigo.setQuantidadeRecebida(qtdNovaRecebida);
            itemDAO.atualizar(itemAntigo);

            LogService.audit(
                    "PEDIDO_ITEM_ATUALIZADO",
                    "pedido_item",
                    itemId,
                    "pedido=" + pedidoId + " recebida=" + qtdNovaRecebida + " status=" + novoStatus);

            LogService.info(
                    "PedidoItem atualizado: item=" + itemId
                            + " pedido=" + pedidoId
                            + " recebida=" + qtdNovaRecebida
                            + " status=" + novoStatus);
        }

        // 3. Depois de processar todos os itens, recalcula o status geral do pedido
        pedidoDAO.recalcularStatus(pedidoId);
        LogService.audit("PEDIDO_STATUS_ATUALIZADO", "pedido", pedidoId, "status recalculado");
    }
}
