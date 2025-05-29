package service;

import dao.CartaDAO;
import model.Carta;
import model.ProdutoEstoqueDTO;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Serviço central de controle de estoque.
 */
public class EstoqueService {

    private final ProdutoEstoqueService produtoEstoqueService;
    private final CartaDAO cartaDAO;

    public EstoqueService() {
        this.produtoEstoqueService = new ProdutoEstoqueService();
        this.cartaDAO = new CartaDAO();
    }

    // ------------------------------------------------------------
    // 1) Métodos genéricos usados por VendaService e UI
    // ------------------------------------------------------------

    /**
     * Busca a quantidade atual de um produto genérico pelo ID.
     */
    public int buscarQuantidadeAtual(String tipoProduto, int produtoId) {
        try {
            return produtoEstoqueService.obterQuantidade(String.valueOf(produtoId));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar estoque: " + e.getMessage(), e);
        }
    }

    
    /**
 * Dá baixa no estoque usando o ProdutoEstoqueService.
 * Valida se há estoque suficiente antes de registrar saída.
 */
public void darBaixa(String tipoProduto, int produtoId, int quantidade) throws SQLException {
    try {
        int atual = produtoEstoqueService.obterQuantidade(String.valueOf(produtoId));
        if (atual < quantidade) {
            throw new SQLException("Estoque insuficiente para o produto " + produtoId +
                                   " (Disponível: " + atual + ", Necessário: " + quantidade + ")");
        }

        produtoEstoqueService.registrarSaida(
            String.valueOf(produtoId),
            quantidade,
            "Venda de produto",
            "sistema"
        );
    } catch (Exception e) {
        throw new SQLException("Erro ao dar baixa no estoque: " + e.getMessage(), e);
    }
}

/**
 * Valida se o produto tem estoque suficiente.
 */
public boolean possuiEstoque(Connection c, String produtoId, int qtdNec) throws SQLException {
    try {
        int atual = produtoEstoqueService.obterQuantidade(produtoId);
        return atual >= qtdNec;
    } catch (Exception e) {
        throw new SQLException("Erro ao verificar estoque do produto " + produtoId + ": " + e.getMessage(), e);
    }
}
    
    /**
     * Lista produtos disponíveis (com estoque > 0) com dados simplificados.
     */
    public List<ProdutoEstoqueDTO> buscarProdutosDisponiveis() {
    return produtoEstoqueService.listarTudo().stream()
        .filter(p -> p.getQuantidade() > 0)
        .map(p -> {
            ProdutoEstoqueDTO dto = new ProdutoEstoqueDTO();
            try {
                dto.setId(Integer.parseInt(p.getId()));
            } catch (NumberFormatException e) {
                dto.setId(-1); // fallback se o ID não for número
            }
            dto.setTipoDisplay(p.getTipo());
            dto.setNome(p.getNome());
            dto.setQuantidade(p.getQuantidade());
            dto.setPrecoVenda(BigDecimal.valueOf(p.getPrecoVenda()));
            return dto;
        })
        .collect(Collectors.toList());
}

    // ------------------------------------------------------------
    // 2) Métodos de carta (legado)
    // ------------------------------------------------------------


    @Deprecated
    public void baixarEstoque(Connection c, String cartaId, int qtd) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE cartas SET qtd = qtd - ? WHERE id = ?")) {
            ps.setInt(1, qtd);
            ps.setString(2, cartaId);
            ps.executeUpdate();
        }
    }

    @Deprecated
    public void devolverEstoque(Connection c, String cartaId, int qtd) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE cartas SET qtd = qtd + ? WHERE id = ?")) {
            ps.setInt(1, qtd);
            ps.setString(2, cartaId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // 3) CRUD de cartas (legado)
    // ------------------------------------------------------------

    public List<Carta> listarCartas(String termo, String colecao, String orderBy) {
        return cartaDAO.listarCartas(termo, colecao, orderBy);
    }

    public void salvarNovaCarta(Carta c) throws Exception {
        cartaDAO.insert(c);
    }

    public void atualizarCarta(Carta c) throws Exception {
        cartaDAO.update(c);
    }

    public void excluirCarta(String id) throws Exception {
        cartaDAO.delete(id);
    }
}
