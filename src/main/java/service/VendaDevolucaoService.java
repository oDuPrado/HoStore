package service;

import dao.VendaDevolucaoDAO;
import dao.ProdutoDAO;
import model.VendaDevolucaoModel;
import model.ProdutoModel;
import model.MovimentacaoEstoqueModel;

import java.time.LocalDateTime;

/**
 * Service responsável por registrar devoluções de venda:
 *  1. Insere a devolução no banco via VendaDevolucaoDAO.
 *  2. Ajusta o estoque do produto (repondo a quantidade devolvida).
 *  3. Registra a movimentação de entrada no histórico de estoque.
 */
public class VendaDevolucaoService {

    private final VendaDevolucaoDAO devolucaoDAO = new VendaDevolucaoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final MovimentacaoEstoqueService movService = new MovimentacaoEstoqueService();

    /**
     * Registra uma devolução de venda.
     *
     * Passos:
     *  1. Persiste o registro de devolução (tabela vendas_devolucoes).
     *  2. Atualiza o estoque do produto (+ quantidade devolvida).
     *  3. Cria um MovimentacaoEstoqueModel do tipo "entrada" e o persiste no histórico.
     *
     * @param dev Modelo contendo dados da devolução:
     *            - vendaId (ID da venda original)
     *            - produtoId (código do produto)
     *            - quantidade (quantidade devolvida)
     *            - valor (valor unitário, não usado no estoque, mas armazenado)
     *            - data (data da devolução)
     *            - motivo (texto livre)
     * @throws Exception se falhar ao inserir ou atualizar o estoque/movimentação.
     */
    public void registrarDevolucao(VendaDevolucaoModel dev) throws Exception {
        // 1. Insere a devolução no banco (vendas_devolucoes)
        devolucaoDAO.inserir(dev);

        // 2. Ajusta o estoque do produto
        ProdutoModel produto = produtoDAO.findById(dev.getProdutoId());
        if (produto != null) {
            // Soma a quantidade devolvida ao estoque atual
            int estoqueAtual = produto.getQuantidade();
            int novoEstoque = estoqueAtual + dev.getQuantidade();
            produto.setQuantidade(novoEstoque);
            produtoDAO.update(produto);

            // 3. Registra mov. de estoque do tipo "entrada"
            MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                    dev.getProdutoId(),
                    "entrada",
                    dev.getQuantidade(),
                    "Devolução da Venda " + dev.getVendaId(),
                    "sistema"
            );
            // Definimos a data/hora com base na data da devolução (início do dia)
            LocalDateTime horaDevolucao = dev.getData().atStartOfDay();
            mov.setData(horaDevolucao);

            movService.registrar(mov);
            System.out.println("✅ Estoque atualizado para produto " + dev.getProdutoId()
                    + ": " + estoqueAtual + " → " + novoEstoque);
        } else {
            System.err.println("[ERRO] Produto não encontrado no estoque: " + dev.getProdutoId());
        }
    }
}
