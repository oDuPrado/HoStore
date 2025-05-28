package service;

import dao.MovimentacaoEstoqueDAO;
import model.MovimentacaoEstoqueModel;

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
     * Registra a movimentação no histórico.
     * 
     * ⚠️ Não altera a quantidade em estoque!
     * 
     * @param mov movimentação (sem ID e sem data)
     * @return o mesmo objeto com ID e data preenchidos
     * @throws Exception se falhar ao salvar
     */
    public MovimentacaoEstoqueModel registrar(MovimentacaoEstoqueModel mov) throws Exception {
        // Define data atual da movimentação
        mov.setData(LocalDateTime.now());

        // Apenas registra no histórico, sem alterar estoque
        return dao.inserir(mov);
    }

    /**
     * Lista todas as movimentações registradas.
     * 
     * @return lista completa
     * @throws Exception se falhar ao listar
     */
    public List<MovimentacaoEstoqueModel> listarTodas() throws Exception {
        return dao.listarTodas();
    }

    /**
     * Lista todas as movimentações de um produto específico.
     * 
     * @param produtoId ID do produto
     * @return lista de movimentações
     * @throws Exception se falhar ao listar
     */
    public List<MovimentacaoEstoqueModel> listarPorProduto(String produtoId) throws Exception {
        return dao.listarPorProduto(produtoId);
    }
}
