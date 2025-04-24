// Procure no seu projeto (Ctrl+F) por "package service" e cole isto em src/service/MovimentacaoEstoqueService.java
package service;

import dao.MovimentacaoEstoqueDAO;
import model.MovimentacaoEstoqueModel;

import java.time.LocalDateTime;
import java.util.List;

public class MovimentacaoEstoqueService {
    private final MovimentacaoEstoqueDAO dao = new MovimentacaoEstoqueDAO();
    private final ProdutoEstoqueService produtoService = new ProdutoEstoqueService();

    /**
     * Registra a movimentação e já ajusta a quantidade no produto.
     * @param mov movimentação (sem ID e sem data)
     * @return o mesmo objeto com ID e data preenchidos
     */
    public MovimentacaoEstoqueModel registrar(MovimentacaoEstoqueModel mov) throws Exception {
        // define data atual
        mov.setData(LocalDateTime.now());

        // obtém quantidade atual do produto
        int qtdAtual = produtoService.obterQuantidade(mov.getProdutoId());

        // calcula nova quantidade
        int novaQtd = switch (mov.getTipoMov().toLowerCase()) {
            case "entrada" -> qtdAtual + mov.getQuantidade();
            case "saida"   -> qtdAtual - mov.getQuantidade();
            default        -> qtdAtual;  // ajuste manual não altera?
        };

        // atualiza estoque no produto
        produtoService.atualizarQuantidade(mov.getProdutoId(), novaQtd);

        // salva movimentação
        return dao.inserir(mov);
    }

    /** Lista todas as movimentações */
    public List<MovimentacaoEstoqueModel> listarTodas() throws Exception {
        return dao.listarTodas();
    }

    /** Lista movimentações de um produto específico */
    public List<MovimentacaoEstoqueModel> listarPorProduto(String produtoId) throws Exception {
        return dao.listarPorProduto(produtoId);
    }
}
