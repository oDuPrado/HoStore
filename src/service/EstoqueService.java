package service;

import util.DB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EstoqueService {

    /** Verifica se há estoque suficiente usando a mesma conexão */
    public boolean possuiEstoque(Connection c, String cartaId, int qtdNecessaria) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT qtd FROM cartas WHERE id = ?")) {
            ps.setString(1, cartaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("qtd") >= qtdNecessaria;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Dá baixa no estoque dentro da mesma conexão */
    public void baixarEstoque(Connection c, String cartaId, int qtd) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE cartas SET qtd = qtd - ? WHERE id = ?")) {
            ps.setInt(1, qtd);
            ps.setString(2, cartaId);
            ps.executeUpdate();
        }
    }
}
