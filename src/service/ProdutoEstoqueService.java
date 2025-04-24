package service;

import dao.ProdutoDAO;
import dao.BoosterDAO;
import model.ProdutoModel;
import model.AcessorioModel;
import model.BoosterModel;
import dao.DeckDAO;
import dao.EtbDAO;
import model.DeckModel;
import model.EtbModel;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProdutoEstoqueService {

    private final ProdutoDAO dao = new ProdutoDAO();

    /* ==================== PRODUTOS GENÉRICOS ==================== */

    public void salvar(ProdutoModel p) throws SQLException {
        ProdutoModel existente = dao.findById(p.getId());

        if (existente == null) {
            dao.insert(p);
        } else {
            p.setAlteradoEmNow();
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
        if (termo == null || termo.trim().isEmpty())
            return listarTudo();
        String t = termo.toLowerCase();
        return listarTudo().stream()
                .filter(p -> p.getNome().toLowerCase().contains(t) ||
                        p.getTipo().toLowerCase().contains(t))
                .collect(Collectors.toList());
    }

    public void baixarEstoque(String idProduto, int qtd) throws Exception {
        ProdutoModel p = dao.findById(idProduto);
        if (p == null)
            throw new Exception("Produto não encontrado!");
        if (p.getQuantidade() < qtd)
            throw new Exception("Estoque insuficiente!");
        p.setQuantidade(p.getQuantidade() - qtd);
        p.setAlteradoEmNow();
        dao.update(p);
    }

    public boolean estoqueBaixo(ProdutoModel p, int limite) {
        return p.getQuantidade() <= limite;
    }

    /* ==================== MÉTODOS ESPECÍFICOS PARA BOOSTER ==================== */

    /** Insere um novo booster (ou REPLACE, se já existir) */
    public void salvarNovoBooster(BoosterModel b) throws Exception {
        new BoosterDAO().insert(b);
    }

    /** Atualiza um booster existente via INSERT OR REPLACE */
    public void atualizarBooster(BoosterModel b) throws Exception {
        new BoosterDAO().insert(b);
    }

    /** Salva um novo deck nas tabelas produtos e decks */
    public void salvarNovoDeck(DeckModel d) throws Exception {
        new DeckDAO().insert(d);
    }

    /** Atualiza um deck nas tabelas produtos e decks */
    public void atualizarDeck(DeckModel d) throws Exception {
        new DeckDAO().update(d);
    }

    public void salvarNovoEtb(EtbModel e) throws Exception {
        new EtbDAO().insert(e);
    }

    public void atualizarEtb(EtbModel e) throws Exception {
        new EtbDAO().update(e);
    }

    public void salvarNovoAcessorio(AcessorioModel a) throws Exception {
    new dao.AcessorioDAO().salvar(a); // salva na tabela acessorios (detalhes)
    salvar(a); // salva na tabela produtos (resumo)
}

public void atualizarAcessorio(AcessorioModel a) throws Exception {
    new dao.AcessorioDAO().atualizar(a); // atualiza acessorios (detalhes)
    salvar(a); // atualiza produtos (resumo)
}

/* ==================== ALIMENTO ==================== */

public void salvarNovoAlimento(model.AlimentoModel a) throws Exception {
    new dao.AlimentoDAO().salvar(a); // salva na tabela produtos_alimenticios
    salvar(a);                        // salva na tabela produtos
}

public void atualizarAlimento(model.AlimentoModel a) throws Exception {
    new dao.AlimentoDAO().atualizar(a); // atualiza produtos_alimenticios
    salvar(a);                           // atualiza produtos
}


}
