package service;

import dao.CartaDAO;
import model.Carta;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class EstoqueService {

    private CartaDAO cartaDAO = new CartaDAO();

    /* ---------- Verificações ---------- */

    public boolean possuiEstoque(Connection c, String cartaId, int qtdNec) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT qtd FROM cartas WHERE id = ?")) {
            ps.setString(1, cartaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) >= qtdNec;
            }
        }
    }

    /* ---------- Baixa / Devolução ---------- */

    public void baixarEstoque(Connection c, String cartaId, int qtd) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE cartas SET qtd = qtd - ? WHERE id = ?")) {
            ps.setInt(1, qtd);
            ps.setString(2, cartaId);
            ps.executeUpdate();
        }
    }

    public void devolverEstoque(Connection c, String cartaId, int qtd) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE cartas SET qtd = qtd + ? WHERE id = ?")) {
            ps.setInt(1, qtd);
            ps.setString(2, cartaId);
            ps.executeUpdate();
        }
    }

    /* ---------- Interfaces para UI ---------- */

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
