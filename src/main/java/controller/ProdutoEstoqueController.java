package controller;

import model.ProdutoModel;
import service.ProdutoEstoqueService;
import util.AlertUtils;

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
            AlertUtils.info("Produto salvo com sucesso!");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.error("Erro ao salvar: " + e.getMessage());
        }
    }
    

    public void remover(String id) {
        try {
            service.remover(id);
            AlertUtils.info("Produto removido.");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtils.error("Erro ao remover: " + e.getMessage());
        }
    }
}
