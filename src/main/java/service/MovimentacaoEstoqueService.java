package service;

import dao.MovimentacaoEstoqueDAO;
import model.MovimentacaoEstoqueModel;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável por registrar movimentações de estoque.
 * 
 * ⚠️ Importante: Este serviço NÃO altera mais o estoque do produto.
 * Apenas registra a movimentação no histórico.
 * 
 * A atualização real do estoque é feita por quem chama este serviço,
 * como, por exemplo, ProdutoEstoqueService.
 */
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueDAO dao = new MovimentacaoEstoqueDAO();

    /**
     * Registra a movimentação no histórico, criando nova conexão.
     * 
     * ⚠️ Use somente fora de transações.
     * 
     * @param mov movimentação (sem ID e sem data)
     * @return o mesmo objeto com data preenchida
     * @throws Exception se falhar ao salvar
     */
    public MovimentacaoEstoqueModel registrar(MovimentacaoEstoqueModel mov) throws Exception {
        mov.setData(LocalDateTime.now());
        return dao.inserir(mov); // abre nova conexão interna
    }

    /**
     * Registra a movimentação no histórico usando uma conexão existente.
     * 
     * ✅ Use este dentro de transações (como venda ou devolução).
     * 
     * @param mov movimentação
     * @param c conexão ativa (transacional)
     * @return movimentação com data preenchida
     * @throws Exception se falhar ao salvar
     */
    public MovimentacaoEstoqueModel registrar(MovimentacaoEstoqueModel mov, Connection c) throws Exception {
        mov.setData(LocalDateTime.now());
        return dao.inserir(mov, c); // usa conexão da transação
    }

    /**
     * Lista todas as movimentações registradas.
     */
    public List<MovimentacaoEstoqueModel> listarTodas() throws Exception {
        return dao.listarTodas();
    }

    /**
     * Lista todas as movimentações de um produto específico.
     */
    public List<MovimentacaoEstoqueModel> listarPorProduto(String produtoId) throws Exception {
        return dao.listarPorProduto(produtoId);
    }
}
