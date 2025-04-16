package dao;

import model.Carta;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartaDAO {

    /**
     * Busca nomes de carta para auto‑complete.
     * Se termo for vazio ou null, retorna as 20 primeiras cartas.
     */
    public List<String> buscarNomesLike(String termo) {
        List<String> nomes = new ArrayList<>();
        String sql;
        boolean usarLike = termo != null && !termo.trim().isEmpty();

        if (usarLike) {
            sql = "SELECT DISTINCT nome FROM cartas WHERE nome LIKE ? ORDER BY nome LIMIT 20";
        } else {
            sql = "SELECT DISTINCT nome FROM cartas ORDER BY nome LIMIT 20";
        }

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            if (usarLike) {
                ps.setString(1, "%" + termo + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nomes.add(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nomes;
    }

    public Carta buscarPorNomeUnico(String nome) {
        try (PreparedStatement ps = DB.get().prepareStatement(
                "SELECT * FROM cartas WHERE nome = ? LIMIT 1")) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Carta(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("colecao"),
                    rs.getString("numero"),
                    rs.getInt("qtd"),
                    rs.getDouble("preco")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void inserirCartaFake() {
        try (PreparedStatement ps = DB.get().prepareStatement(
                "INSERT OR IGNORE INTO cartas (id, nome, colecao, numero, qtd, preco) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "001");
            ps.setString(2, "Pikachu");
            ps.setString(3, "Base Set");
            ps.setString(4, "58/102");
            ps.setInt(5, 50);
            ps.setDouble(6, 9.90);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* devolve todas as coleções distintas – ordenadas */
public List<String> listarColecoes() {
    List<String> out = new ArrayList<>();
    try (PreparedStatement ps = DB.get().prepareStatement(
            "SELECT DISTINCT colecao FROM cartas ORDER BY colecao")) {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) out.add(rs.getString(1));
    } catch (SQLException e) { e.printStackTrace(); }
    return out;
}

/* lista cartas aplicando filtros de nome/coleção e ordenação */
public List<Carta> listarCartas(String termo, String colecao, String orderBy) {
    StringBuilder sb = new StringBuilder("SELECT * FROM cartas WHERE 1=1");
    List<Object> params = new ArrayList<>();

    if (termo != null && !termo.trim().isEmpty()) {
        sb.append(" AND nome LIKE ?");
        params.add("%" + termo + "%");
    }
    if (colecao != null && !colecao.equals("Todas")) {
        sb.append(" AND colecao = ?");
        params.add(colecao);
    }

    switch (orderBy) {
        case "Nome":     sb.append(" ORDER BY nome");             break;
        case "Número":   sb.append(" ORDER BY numero");           break;
        case "Mais novo":sb.append(" ORDER BY rowid DESC");       break;
        default:         sb.append(" ORDER BY rowid ASC");        break; // Mais antigo
    }

    List<Carta> out = new ArrayList<>();
    try (PreparedStatement ps = DB.get().prepareStatement(sb.toString())) {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            out.add(new Carta(
                rs.getString("id"),
                rs.getString("nome"),
                rs.getString("colecao"),
                rs.getString("numero"),
                rs.getInt("qtd"),
                rs.getDouble("preco")
            ));
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return out;
}

}
