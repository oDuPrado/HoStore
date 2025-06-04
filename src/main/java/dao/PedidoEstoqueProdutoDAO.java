package dao;

import model.PedidoEstoqueProdutoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para manipular a tabela de produtos vinculados aos pedidos de estoque.
 */
public class PedidoEstoqueProdutoDAO {

    /**
     * Insere um novo produto vinculado a um pedido.
     */
    public void inserir(PedidoEstoqueProdutoModel m) throws SQLException {
        String sql = "INSERT INTO pedido_produtos(" +
                     "id, pedido_id, produto_id, quantidade_pedida, quantidade_recebida, status" +
                     ") VALUES (?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getPedidoId());
            ps.setString(3, m.getProdutoId());
            ps.setInt   (4, m.getQuantidadePedida());
            ps.setInt   (5, m.getQuantidadeRecebida());
            ps.setString(6, m.getStatus());
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza um produto vinculado a um pedido.
     */
    public void atualizar(PedidoEstoqueProdutoModel m) throws SQLException {
        String sql = "UPDATE pedido_produtos SET " +
                     "quantidade_pedida=?, quantidade_recebida=?, status=? " +
                     "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt   (1, m.getQuantidadePedida());
            ps.setInt   (2, m.getQuantidadeRecebida());
            ps.setString(3, m.getStatus());
            ps.setString(4, m.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Exclui um produto vinculado a um pedido.
     */
    public void excluir(String id) throws SQLException {
        String sql = "DELETE FROM pedido_produtos WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Lista todos os produtos vinculados a um determinado pedido.
     */
    public List<PedidoEstoqueProdutoModel> listarPorPedido(String pedidoId) throws SQLException {
        List<PedidoEstoqueProdutoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM pedido_produtos WHERE pedido_id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PedidoEstoqueProdutoModel(
                        rs.getString("id"),
                        rs.getString("pedido_id"),
                        rs.getString("produto_id"),
                        rs.getInt   ("quantidade_pedida"),
                        rs.getInt   ("quantidade_recebida"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return out;
    }

    /**
     * Atualiza apenas a quantidade recebida de um item de pedido.
     * @param idPedidoEstoqueProduto id do item
     * @param quantidadeRecebida nova quantidade recebida
     */
    public void atualizarQuantidadeRecebida(String idPedidoEstoqueProduto, int quantidadeRecebida) {
        String sql = "UPDATE pedido_produtos SET quantidade_recebida = ? WHERE id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantidadeRecebida);
            ps.setString(2, idPedidoEstoqueProduto);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Atualiza apenas a quantidade pedida de um item de pedido.
     * @param idPedidoEstoqueProduto id do item
     * @param novaQuantidade nova quantidade pedida
     */
    public void atualizarQuantidade(String idPedidoEstoqueProduto, int novaQuantidade) {
        String sql = "UPDATE pedido_produtos SET quantidade_pedida = ? WHERE id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, novaQuantidade);
            ps.setString(2, idPedidoEstoqueProduto);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
        /**
     * Busca um item específico de produto de pedido pelo ID.
     * @param id ID do item
     * @return PedidoEstoqueProdutoModel ou null se não encontrado
     * @throws SQLException caso ocorra erro de banco
     */
    public PedidoEstoqueProdutoModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM pedido_produtos WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PedidoEstoqueProdutoModel(
                        rs.getString("id"),
                        rs.getString("pedido_id"),
                        rs.getString("produto_id"),
                        rs.getInt("quantidade_pedida"),
                        rs.getInt("quantidade_recebida"),
                        rs.getString("status")
                );
            }
        }
    }

}
