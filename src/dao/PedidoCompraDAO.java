// PedidoCompraDAO – Financeiro (CRUD de Pedidos de Compra)
// Procure: "// PedidoCompraDAO – Financeiro"
package dao;

import model.PedidoCompraModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PedidoCompraDAO {

    public void inserir(PedidoCompraModel p) throws SQLException {
        String sql = "INSERT INTO pedidos_compras("
                   + "id,nome,data,status,fornecedor_id,observacoes"
                   + ") VALUES (?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getData());
            ps.setString(4, p.getStatus());
            ps.setString(5, p.getFornecedorId());
            ps.setString(6, p.getObservacoes());
            ps.executeUpdate();
        }
    }

    public void atualizar(PedidoCompraModel p) throws SQLException {
        String sql = "UPDATE pedidos_compras SET "
                   + "nome=?, data=?, status=?, fornecedor_id=?, observacoes=? "
                   + "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getData());
            ps.setString(3, p.getStatus());
            ps.setString(4, p.getFornecedorId());
            ps.setString(5, p.getObservacoes());
            ps.setString(6, p.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(String id) throws SQLException {
        String sql = "DELETE FROM pedidos_compras WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public PedidoCompraModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM pedidos_compras WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PedidoCompraModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("data"),
                    rs.getString("status"),
                    rs.getString("fornecedor_id"),
                    rs.getString("observacoes")
                );
            }
        }
    }

    public List<PedidoCompraModel> listarTodos() throws SQLException {
        List<PedidoCompraModel> out = new ArrayList<>();
        String sql = "SELECT * FROM pedidos_compras ORDER BY data DESC,nome";
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new PedidoCompraModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("data"),
                    rs.getString("status"),
                    rs.getString("fornecedor_id"),
                    rs.getString("observacoes")
                ));
            }
        }
        return out;
    }
}
