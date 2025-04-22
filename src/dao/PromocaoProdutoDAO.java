package dao;

import model.PromocaoProdutoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PromocaoProdutoDAO {

    public void vincularProduto(PromocaoProdutoModel vinculo) throws Exception {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO promocao_produtos (id, promocao_id, produto_id) VALUES (?, ?, ?)")) {
            ps.setString(1, vinculo.getId());
            ps.setString(2, vinculo.getPromocaoId());
            ps.setString(3, vinculo.getProdutoId());
            ps.execute();
        }
    }

    public void desvincularProduto(String id) throws Exception {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM promocao_produtos WHERE id = ?")) {
            ps.setString(1, id);
            ps.execute();
        }
    }

    public List<PromocaoProdutoModel> listarPorPromocao(String promocaoId) throws Exception {
        List<PromocaoProdutoModel> lista = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM promocao_produtos WHERE promocao_id = ?")) {
            ps.setString(1, promocaoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PromocaoProdutoModel m = new PromocaoProdutoModel();
                    m.setId(rs.getString("id"));
                    m.setPromocaoId(rs.getString("promocao_id"));
                    m.setProdutoId(rs.getString("produto_id"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
