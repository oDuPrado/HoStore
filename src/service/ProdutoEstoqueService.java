package service;

import dao.ProdutoDAO;
import model.ProdutoModel;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProdutoEstoqueService {

    private final ProdutoDAO dao = new ProdutoDAO();

    /* ==================== AÇÕES PRINCIPAIS ==================== */

    public void salvar(ProdutoModel p) throws SQLException {
        ProdutoModel existente = dao.findById(p.getId());
        if (existente == null) dao.insert(p);
        else {
            existente.setAlteradoEmNow();
            dao.update(p);
        }
    }

    public void remover(String id) throws SQLException {
        dao.delete(id);
    }

    public List<ProdutoModel> listarTudo() {
        return dao.listAll();
    }

    public List<ProdutoModel> filtrarPorNomeOuCat(String termo) {
        if (termo == null || termo.trim().isEmpty()) return listarTudo();
        String t = termo.toLowerCase();
        return listarTudo().stream()
            .filter(p -> p.getNome().toLowerCase().contains(t)
                      || p.getCategoria().toLowerCase().contains(t))
            .collect(Collectors.toList());
    }

    public void baixarEstoque(String idProduto, int qtd) throws Exception {
        ProdutoModel p = dao.findById(idProduto);
        if (p == null) throw new Exception("Produto não encontrado!");
        if (p.getQuantidade() < qtd) throw new Exception("Estoque insuficiente!");
        p.setQuantidade(p.getQuantidade() - qtd);
        p.setAlteradoEmNow();
        dao.update(p);
    }

    public boolean estoqueBaixo(ProdutoModel p, int limite) {
        return p.getQuantidade() <= limite;
    }
}
