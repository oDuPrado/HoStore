package service;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import service.ProdutoEstoqueService;
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

    // Service que registra as movimentações de estoque no histórico

    /**
     * Recebe todos os itens de um pedido de compra e ajusta o estoque incrementalmente.
     *
     * Fluxo:
     * 1. Para cada itemId presente em mapaRecebimento:
     *    a) Busca o item antigo no banco para saber a quantidade recebida anterior.
     *    b) Calcula delta = qtdRecebidaNova - qtdRecebidaAnterior.
     *    c) Se delta > 0, soma esta diferença ao estoque do produto. Registra movimentação "entrada".
     *       Se delta < 0, subtrai (valor absoluto) do estoque do produto. Registra movimentação "saída".
     *       Se delta == 0, não altera o estoque.
     *    d) Atualiza o status do item ("pendente", "parcial" ou "completo") com base em qtdRecebidaNova e qtdPedida.
     *    e) Persiste no banco a nova quantidadeRecebida e o novo status.
     * 2. Ao final, chama pedidoDAO.recalcularStatus(pedidoId) para recalcular o status geral do pedido.
     *
     * @param pedidoId        ID do pedido de compra a ser recebido.
     * @param mapaRecebimento mapa contendo (itemId → qtdRecebidaNova) para cada linha da UI.
     * @param usuario         identificador de quem está registrando (ex: "sistema").
     * @throws Exception se ocorrer falha no banco de dados.
     */
    public void receberPedido(String pedidoId, Map<String, Integer> mapaRecebimento, String usuario) throws Exception {
        // 1. Busca todos os itens vinculados a esse pedido (podemos usar só para contagem ou validação)
        List<PedidoEstoqueProdutoModel> todosItens = itemDAO.listarPorPedido(pedidoId);

        // 2. Percorre cada entrada do mapa (itemId + qtdRecebidaNova)
        for (Map.Entry<String, Integer> entry : mapaRecebimento.entrySet()) {
            String itemId = entry.getKey();
            int qtdNovaRecebida = entry.getValue();

            // 2.1) Lê do banco o "itemAntigo" para saber a quantidade recebida até agora
            PedidoEstoqueProdutoModel itemAntigo = itemDAO.buscarPorId(itemId);
            if (itemAntigo == null) {
                System.err.println("[ERRO] Item de pedido não encontrado no banco: " + itemId);
                continue; // pula para o próximo item
            }

            int qtdRecebidaAnterior = itemAntigo.getQuantidadeRecebida();
            int qtdPedida         = itemAntigo.getQuantidadePedida();
            int delta             = qtdNovaRecebida - qtdRecebidaAnterior;

            System.out.println("⏳ Processando item ID: " + itemId);
            System.out.println("   - Quantidade ANTERIOR recebida: " + qtdRecebidaAnterior);
            System.out.println("   - Quantidade NOVA recebida:     " + qtdNovaRecebida);
            System.out.println("   - DELTA calculado:              " + delta);

            // 2.2) Ajusta o estoque do produto somente se delta != 0
            if (delta != 0) {
                // 2.2.1) Busca o produto para ajustar seu estoque
                String produtoId = itemAntigo.getProdutoId();
                ProdutoModel produto = produtoDAO.findById(produtoId);
                if (produto == null) {
                    System.err.println("[ERRO] Produto não encontrado no estoque: " + produtoId);
                } else {
                    if (delta > 0) {
                        System.out.println("Entrada: somando +" + delta
                                + " ao estoque via lote para o pedido " + pedidoId);
                        new ProdutoEstoqueService().registrarEntrada(
                                produtoId,
                                delta,
                                "Recebimento do Pedido " + pedidoId,
                                usuario);

                    } else {
                        // Se delta < 0: novaRecebida < recebidaAnterior -> correcao de recebimento
                        int qtdASubtrair = Math.abs(delta);
                        System.out.println("Correcao: subtraindo -" + qtdASubtrair
                                + " do estoque via lote para o pedido " + pedidoId);
                        new ProdutoEstoqueService().registrarSaida(
                                produtoId,
                                qtdASubtrair,
                                "Correcao de recebimento do Pedido " + pedidoId,
                                usuario);
                    }
                }
            } else {
                System.out.println("→ Nenhuma alteração no estoque necessária para item " + itemId);
            }

            // 2.3) Atualiza o status do item (pendente/parcial/completo)
            String novoStatus = (qtdNovaRecebida >= qtdPedida) ? "completo"
                    : (qtdNovaRecebida > 0) ? "parcial"
                    : "pendente";
            itemAntigo.setStatus(novoStatus);

            // 2.4) Grava, **após** ajustar o estoque, a nova quantidadeRecebida e status
            itemAntigo.setQuantidadeRecebida(qtdNovaRecebida);
            itemDAO.atualizar(itemAntigo);
            System.out.println("   → Item " + itemId + " atualizado no pedido: Recebida=" + qtdNovaRecebida
                    + " | Status=" + novoStatus);
        }

        // 3. Depois de processar todos os itens, recalcula o status geral do pedido
        pedidoDAO.recalcularStatus(pedidoId);
        System.out.println(">> [LOG] pedidoDAO.recalcularStatus(" + pedidoId + ") executado.");
    }
}
