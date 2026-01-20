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
import util.DB;

import service.MovimentacaoEstoqueService;
import model.MovimentacaoEstoqueModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProdutoEstoqueService {

    private final ProdutoDAO dao = new ProdutoDAO();
    private final dao.EstoqueLoteDAO loteDAO = new dao.EstoqueLoteDAO();
    private final MovimentacaoEstoqueService movService = new MovimentacaoEstoqueService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static class LoteConsumo {
        public final int loteId;
        public final int qtdConsumida;
        public final double custoUnit;

        public LoteConsumo(int loteId, int qtdConsumida, double custoUnit) {
            this.loteId = loteId;
            this.qtdConsumida = qtdConsumida;
            this.custoUnit = custoUnit;
        }
    }

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
        registrarSaida(idProduto, qtd, "Baixa de estoque", "sistema");
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
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            registrarEntrada(produtoId, quantidade, motivo, usuario, c);
            c.commit();
        }
    }

    public void registrarEntrada(String produtoId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        ProdutoModel produto = dao.findById(produtoId, c);
        if (produto == null)
            throw new Exception("Produto nao encontrado!");

        String dataEntrada = LocalDateTime.now().format(FMT);
        int loteId = loteDAO.inserirLote(
                produtoId,
                produto.getFornecedorId(),
                null,
                dataEntrada,
                null,
                produto.getPrecoCompra(),
                produto.getPrecoVenda(),
                quantidade,
                motivo,
                c);

        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, loteId, "entrada", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());
        movService.registrar(mov, c);

        atualizarQuantidadeCache(produtoId, c);
    }

    public void registrarSaida(String produtoId, int quantidade, String motivo, String usuario) throws Exception {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            registrarSaida(produtoId, quantidade, motivo, usuario, c);
            c.commit();
        }
    }

    public void registrarSaida(String produtoId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        ProdutoModel produto = dao.findById(produtoId, c);
        if (produto == null)
            throw new Exception("Produto nao encontrado!");

        consumirFIFO(produtoId, quantidade, motivo, usuario, c);
        atualizarQuantidadeCache(produtoId, c);
    }

    public int obterQuantidade(String produtoId) throws SQLException {
        try (Connection c = DB.get()) {
            return obterQuantidade(produtoId, c);
        }
    }

    public int obterQuantidade(String produtoId, Connection c) throws SQLException {
        ProdutoModel p = dao.findById(produtoId, c);
        if (p == null)
            throw new SQLException("Produto nao encontrado!");
        int total = loteDAO.somarSaldoProduto(produtoId, c);
        if (p.getQuantidade() != total) {
            p.setQuantidade(total);
            p.setAlteradoEmNow();
            dao.update(p, c);
        }
        return total;
    }

    public void atualizarQuantidade(String produtoId, int novaQtd) throws SQLException {
        try (Connection c = DB.get()) {
            atualizarQuantidadeCache(produtoId, c);
        }
    }


    public List<LoteConsumo> consumirFIFO(String produtoId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        if (quantidade <= 0)
            throw new Exception("Quantidade invalida: " + quantidade);

        List<LoteConsumo> consumos = new ArrayList<>();
        int restante = quantidade;

        for (dao.EstoqueLoteDAO.LoteSaldo lote : loteDAO.listarLotesDisponiveisFIFO(produtoId, c)) {
            if (restante <= 0)
                break;
            int consumir = Math.min(restante, lote.qtdDisponivel);
            loteDAO.consumirDoLote(lote.loteId, consumir, c);
            MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                    produtoId, lote.loteId, "saida", consumir, motivo, usuario);
            mov.setData(LocalDateTime.now());
            movService.registrar(mov, c);
            consumos.add(new LoteConsumo(lote.loteId, consumir, lote.custoUnit));
            restante -= consumir;
        }

        if (restante > 0) {
            throw new Exception("Estoque insuficiente para o produto " + produtoId);
        }

        return consumos;
    }

    public void reporNoLote(String produtoId, int loteId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        if (quantidade <= 0)
            throw new Exception("Quantidade invalida: " + quantidade);
        loteDAO.reporNoLote(loteId, quantidade, c);
        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, loteId, "entrada", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());
        movService.registrar(mov, c);
        atualizarQuantidadeCache(produtoId, c);
    }

    public void ajustarLote(String produtoId, int loteId, int delta, String motivo, String usuario, Connection c)
            throws Exception {
        if (delta == 0)
            throw new Exception("Quantidade invalida: 0");

        int qtd = Math.abs(delta);
        if (delta > 0) {
            loteDAO.reporNoLote(loteId, qtd, c);
        } else {
            loteDAO.consumirDoLote(loteId, qtd, c);
        }

        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, loteId, "ajuste", qtd, motivo, usuario);
        mov.setData(LocalDateTime.now());
        movService.registrar(mov, c);
        atualizarQuantidadeCache(produtoId, c);
    }

    public int criarLoteAjuste(String produtoId, int quantidade, String motivo, String usuario, Connection c)
            throws Exception {
        if (quantidade <= 0)
            throw new Exception("Quantidade invalida: " + quantidade);

        ProdutoModel produto = dao.findById(produtoId, c);
        if (produto == null)
            throw new Exception("Produto nao encontrado!");

        int loteId = loteDAO.inserirLote(
                produtoId,
                null,
                "AJUSTE_MANUAL",
                LocalDateTime.now().format(FMT),
                null,
                produto.getPrecoCompra(),
                produto.getPrecoVenda(),
                quantidade,
                motivo,
                c);

        MovimentacaoEstoqueModel mov = new MovimentacaoEstoqueModel(
                produtoId, loteId, "ajuste", quantidade, motivo, usuario);
        mov.setData(LocalDateTime.now());
        movService.registrar(mov, c);
        atualizarQuantidadeCache(produtoId, c);
        return loteId;
    }

    public void atualizarQuantidadeCache(String produtoId, Connection c) throws SQLException {
        ProdutoModel p = dao.findById(produtoId, c);
        if (p == null)
            throw new SQLException("Produto nao encontrado!");
        int total = loteDAO.somarSaldoProduto(produtoId, c);
        p.setQuantidade(total);
        p.setAlteradoEmNow();
        dao.update(p, c);
    }

}
