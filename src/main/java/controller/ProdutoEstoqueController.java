package controller;

import model.ProdutoModel;
import service.ProdutoEstoqueService;
import util.AlertUtils;
import util.LogService;

import java.sql.SQLException;
import java.util.List;

public class ProdutoEstoqueController {

    private final ProdutoEstoqueService service = new ProdutoEstoqueService();

    public List<ProdutoModel> listar(String filtro) {
        return service.filtrarPorNomeOuCat(filtro);
    }

    public void salvar(ProdutoModel p) {
        try {
            service.salvar(p);
            LogService.audit("PRODUTO_SALVAR", "produto", p.getId(), "nome=" + p.getNome());
            AlertUtils.info("Produto salvo com sucesso!");
        } catch (SQLException e) {
            e.printStackTrace();
            LogService.auditError("PRODUTO_SALVAR_ERRO", "produto", (p != null ? p.getId() : null),
                    "falha ao salvar", e);
            AlertUtils.error("Erro ao salvar: " + e.getMessage());
        }
    }
    

    public void remover(String id) {
        try {
            service.remover(id);
            LogService.audit("PRODUTO_REMOVER", "produto", id, "removido");
            AlertUtils.info("Produto removido.");
        } catch (SQLException e) {
            e.printStackTrace();
            LogService.auditError("PRODUTO_REMOVER_ERRO", "produto", id, "falha ao remover", e);
            AlertUtils.error("Erro ao remover: " + e.getMessage());
        }
    }
}
