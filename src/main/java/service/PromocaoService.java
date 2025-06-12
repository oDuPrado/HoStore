package service;

import dao.PromocaoDAO;
import dao.PromocaoProdutoDAO;
import model.PromocaoModel;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Contém toda a regra de negócio de promoções.
 */
public class PromocaoService {

    private final PromocaoDAO promocaoDAO = new PromocaoDAO();
    private final PromocaoProdutoDAO promoProdDAO = new PromocaoProdutoDAO();

    /**
     * Retorna a promoção ativa mais vantajosa para um dado produto (ou empty se nenhuma).
     */
    public Optional<PromocaoModel> buscarPromocaoValidaParaProduto(String produtoId) throws Exception {
        Date hoje = new Date();
        // pega todos os vínculos desse produto
        List<PromocaoModel> candidatas = promoProdDAO.listarPorPromocao(produtoId).stream()
            .map(v -> {
                try {
                    return promocaoDAO.buscarPorId(v.getPromocaoId()).orElse(null);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(p -> p != null
                && !hoje.before(p.getDataInicio())
                && !hoje.after(p.getDataFim()))
            .toList();

        // escolhe a de maior desconto
        return candidatas.stream()
                .max((p1, p2) -> p1.getDesconto().compareTo(p2.getDesconto()));
    }

    /**
     * Retorna só o valor do desconto em % ou R$, caso haja promoção válida.
     */
    public Optional<Double> buscarDescontoParaProduto(String produtoId) throws Exception {
        return buscarPromocaoValidaParaProduto(produtoId)
                .map(PromocaoModel::getDesconto);
    }

    /**
     * Lista todas promoções ativas hoje.
     */
    public List<PromocaoModel> listarPromocoesAtivas() throws Exception {
        return promocaoDAO.listarAtivas();
    }
}
