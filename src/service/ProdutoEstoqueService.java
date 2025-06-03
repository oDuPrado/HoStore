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

import service.MovimentacaoEstoqueService;
import model.MovimentacaoEstoqueModel;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ProdutoEstoqueService {

    private final ProdutoDAO dao = new ProdutoDAO();

    /* ==================== PRODUTOS GENÉRICOS ==================== */

    public void salvar(ProdutoModel p) throws SQLException {
        ProdutoModel existente = dao.findById(p.getId());
        boolean novo = (existente == null);

        if (novo) {
            int qtdInicial = p.getQuantidade(); // guarda a quantidade original
            p.setQuantidade(0); // zera para não salvar duplicado

            dao.insert(p); // insere o produto com estoque 0

            // Se tiver quantidade inicial, registrar como entrada no histórico
            if (qtdInicial > 0) {
                try {
                    registrarEntrada(
                            p.getId(),
                            qtdInicial,
                            "Cadastro inicial",
                            "sistema");
                } catch (Exception e) {
                    e.printStackTrace(); // em produção, use log
                }
            }

        } else {
            p.setAlteradoEmNow(); // atualiza timestamp de modificação
            dao.update(p); // apenas atualiza, sem mexer em estoque
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
        new BoosterDAO().insert(b); // salva os detalhes específicos
        salvar(b); // salva o resumo em produtos, incluindo jogoId
    }

    /** Atualiza um booster existente via INSERT OR REPLACE */
    public void atualizarBooster(BoosterModel b) throws Exception {
        new BoosterDAO().insert(b);
        salvar(b); // ← ATUALIZA também na tabela produtos
    }

    public void salvarNovoDeck(DeckModel d) throws Exception {
        new DeckDAO().insert(d);
        salvar(d); // ← ESSENCIAL
    }

    public void atualizarDeck(DeckModel d) throws Exception {
        new DeckDAO().update(d);
        salvar(d); // ← ESSENCIAL
    }

    public void salvarNovoEtb(EtbModel e) throws Exception {
        new EtbDAO().insert(e);
        salvar(e); // ← ESSENCIAL
    }

    public void atualizarEtb(EtbModel e) throws Exception {
        new EtbDAO().update(e);
        salvar(e); // ← ESSENCIAL
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
        salvar(a); // salva na tabela produtos
    }

    public void atualizarAlimento(model.AlimentoModel a) throws Exception {
        new dao.AlimentoDAO().atualizar(a); // atualiza produtos_alimenticios
        salvar(a); // atualiza produtos
    }

    public void registrarEntrada(String produtoId, int quantidade, String motivo, String usuario) throws Exception {
        ProdutoModel produto = dao.findById(produtoId);
        if (produto == null)
            throw new Exception("Produto não encontrado!");

        // atualiza quantidade
        produto.setQuantidade(produto.getQuantidade() + quantidade);
        produto.setAlteradoEmNow();
        dao.update(produto);

        // registra movimentação
        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, "entrada", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());

        new MovimentacaoEstoqueService().registrar(mov);
    }

    public void registrarEntrada(String produtoId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        ProdutoModel produto = dao.findById(produtoId);
        if (produto == null)
            throw new Exception("Produto não encontrado!");

        // atualiza quantidade
        produto.setQuantidade(produto.getQuantidade() + quantidade);
        produto.setAlteradoEmNow();
        dao.update(produto, c); // usa a mesma conexão

        // registra movimentação com a mesma conexão
        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, "entrada", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());

        new MovimentacaoEstoqueService().registrar(mov, c);
    }

    public void registrarSaida(String produtoId, int quantidade, String motivo, String usuario) throws Exception {
        ProdutoModel produto = dao.findById(produtoId);
        if (produto == null)
            throw new Exception("Produto não encontrado!");

        if (produto.getQuantidade() < quantidade)
            throw new Exception("Estoque insuficiente para saída!");

        // atualiza quantidade
        produto.setQuantidade(produto.getQuantidade() - quantidade);
        produto.setAlteradoEmNow();
        dao.update(produto);

        // registra movimentação
        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, "saida", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());

        new MovimentacaoEstoqueService().registrar(mov);
    }

    public void registrarSaida(String produtoId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        ProdutoModel produto = dao.findById(produtoId);
        if (produto == null)
            throw new Exception("Produto não encontrado!");

        if (produto.getQuantidade() < quantidade)
            throw new Exception("Estoque insuficiente para saída!");

        // atualiza quantidade
        produto.setQuantidade(produto.getQuantidade() - quantidade);
        produto.setAlteradoEmNow();
        dao.update(produto, c); // ← usa a mesma conexão

        // registra movimentação com a mesma conexão
        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, "saida", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());

        new MovimentacaoEstoqueService().registrar(mov, c); // ← sem DB.get()
    }

    public int obterQuantidade(String produtoId) throws SQLException {
        ProdutoModel p = dao.findById(produtoId);
        if (p == null)
            throw new SQLException("Produto não encontrado!");
        return p.getQuantidade();
    }

    public void atualizarQuantidade(String produtoId, int novaQtd) throws SQLException {
        ProdutoModel p = dao.findById(produtoId);
        if (p == null)
            throw new SQLException("Produto não encontrado!");

        p.setQuantidade(novaQtd);
        p.setAlteradoEmNow();
        dao.update(p);
    }

}
