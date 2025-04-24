package dao;

import model.AlimentoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlimentoDAO {

    public void salvar(AlimentoModel a) throws SQLException {
        String sql =
            "INSERT INTO produtos_alimenticios " +
            "(id, nome, categoria, subtipo, marca, sabor, lote, peso, unidade_peso, " +
            " codigo_barras, data_validade, quantidade, preco_compra, preco_venda, fornecedor_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString (1, a.getId());
            p.setString (2, a.getNome());
            p.setString (3, a.getCategoria());
            p.setString (4, a.getSubtipo());
            p.setString (5, a.getMarca());
            p.setString (6, a.getSabor());
            p.setString (7, a.getLote());
            p.setDouble (8, a.getPeso());
            p.setString (9, a.getUnidadePeso());
            p.setString (10, a.getCodigoBarras());
            p.setString (11, a.getDataValidade());
            p.setInt    (12, a.getQuantidade());
            p.setDouble (13, a.getPrecoCompra());
            p.setDouble (14, a.getPrecoVenda());
            p.setString (15, a.getFornecedorId());
            p.executeUpdate();
        }
    }

    public void atualizar(AlimentoModel a) throws SQLException {
        String sql =
            "UPDATE produtos_alimenticios SET " +
            "categoria=?, subtipo=?, marca=?, sabor=?, lote=?, peso=?, unidade_peso=?, " +
            "codigo_barras=?, data_validade=?, quantidade=?, preco_compra=?, preco_venda=?, fornecedor_id=? " +
            "WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString (1, a.getCategoria());
            p.setString (2, a.getSubtipo());
            p.setString (3, a.getMarca());
            p.setString (4, a.getSabor());
            p.setString (5, a.getLote());
            p.setDouble (6, a.getPeso());
            p.setString (7, a.getUnidadePeso());
            p.setString (8, a.getCodigoBarras());
            p.setString (9, a.getDataValidade());
            p.setInt    (10, a.getQuantidade());
            p.setDouble (11, a.getPrecoCompra());
            p.setDouble (12, a.getPrecoVenda());
            p.setString (13, a.getFornecedorId());
            p.setString (14, a.getId());
            p.executeUpdate();
        }
    }

    public AlimentoModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM produtos_alimenticios WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            ResultSet rs = p.executeQuery();
            if (rs.next()) {
                AlimentoModel a = new AlimentoModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getInt   ("quantidade"),
                    rs.getDouble("preco_compra"),
                    rs.getDouble("preco_venda"),
                    rs.getString("fornecedor_id"),
                    rs.getString("categoria"),
                    rs.getString("subtipo"),
                    rs.getString("marca"),
                    rs.getString("sabor"),
                    rs.getString("lote"),
                    rs.getDouble("peso"),
                    rs.getString("unidade_peso"),
                    rs.getString("codigo_barras"),
                    rs.getString("data_validade")
                );
                // fornecedorNome você pode carregar via join ou manter em memória
                return a;
            }
        }
        return null;
    }

    public void excluir(String id) throws SQLException {
        String sql = "DELETE FROM produtos_alimenticios WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            p.executeUpdate();
        }
    }

    public List<AlimentoModel> listarTodos() throws SQLException {
        List<AlimentoModel> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos_alimenticios";
        try (Connection c = DB.get(); PreparedStatement p = c.prepareStatement(sql)) {
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                lista.add(buscarPorId(rs.getString("id")));
            }
        }
        return lista;
    }
}
