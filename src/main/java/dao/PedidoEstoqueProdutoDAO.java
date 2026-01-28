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
                     "id, pedido_id, produto_id, fornecedor_id, custo_unit, preco_venda_unit, " +
                     "quantidade_pedida, quantidade_recebida, status" +
                     ") VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindInsert(ps, m);
            ps.executeUpdate();
        }
    }

    public void inserir(PedidoEstoqueProdutoModel m, Connection c) throws SQLException {
        String sql = "INSERT INTO pedido_produtos(" +
                     "id, pedido_id, produto_id, fornecedor_id, custo_unit, preco_venda_unit, " +
                     "quantidade_pedida, quantidade_recebida, status" +
                     ") VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindInsert(ps, m);
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza um produto vinculado a um pedido.
     */
    public void atualizar(PedidoEstoqueProdutoModel m) throws SQLException {
        String sql = "UPDATE pedido_produtos SET " +
                     "fornecedor_id=?, custo_unit=?, preco_venda_unit=?, " +
                     "quantidade_pedida=?, quantidade_recebida=?, status=? " +
                     "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            validarLockRecebimento(m, c);
            bindUpdate(ps, m);
            ps.executeUpdate();
        }
    }

    public void atualizar(PedidoEstoqueProdutoModel m, Connection c) throws SQLException {
        String sql = "UPDATE pedido_produtos SET " +
                     "fornecedor_id=?, custo_unit=?, preco_venda_unit=?, " +
                     "quantidade_pedida=?, quantidade_recebida=?, status=? " +
                     "WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            validarLockRecebimento(m, c);
            bindUpdate(ps, m);
            ps.executeUpdate();
        }
    }

    public void atualizarRecebimento(String id, int quantidadeRecebida, String status) throws SQLException {
        String sql = "UPDATE pedido_produtos SET quantidade_recebida = ?, status = ? WHERE id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantidadeRecebida);
            ps.setString(2, status);
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public void atualizarRecebimento(String id, int quantidadeRecebida, String status, Connection c) throws SQLException {
        String sql = "UPDATE pedido_produtos SET quantidade_recebida = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantidadeRecebida);
            ps.setString(2, status);
            ps.setString(3, id);
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
                        rs.getString("fornecedor_id"),
                        (Double) rs.getObject("custo_unit"),
                        (Double) rs.getObject("preco_venda_unit"),
                        rs.getInt   ("quantidade_pedida"),
                        rs.getInt   ("quantidade_recebida"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return out;
    }

    public List<PedidoEstoqueProdutoModel> listarPorPedido(String pedidoId, Connection c) throws SQLException {
        List<PedidoEstoqueProdutoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM pedido_produtos WHERE pedido_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PedidoEstoqueProdutoModel(
                        rs.getString("id"),
                        rs.getString("pedido_id"),
                        rs.getString("produto_id"),
                        rs.getString("fornecedor_id"),
                        (Double) rs.getObject("custo_unit"),
                        (Double) rs.getObject("preco_venda_unit"),
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
                        rs.getString("fornecedor_id"),
                        (Double) rs.getObject("custo_unit"),
                        (Double) rs.getObject("preco_venda_unit"),
                        rs.getInt("quantidade_pedida"),
                        rs.getInt("quantidade_recebida"),
                        rs.getString("status")
                );
            }
        }
    }

    public PedidoEstoqueProdutoModel buscarPorId(String id, Connection c) throws SQLException {
        String sql = "SELECT * FROM pedido_produtos WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PedidoEstoqueProdutoModel(
                        rs.getString("id"),
                        rs.getString("pedido_id"),
                        rs.getString("produto_id"),
                        rs.getString("fornecedor_id"),
                        (Double) rs.getObject("custo_unit"),
                        (Double) rs.getObject("preco_venda_unit"),
                        rs.getInt("quantidade_pedida"),
                        rs.getInt("quantidade_recebida"),
                        rs.getString("status")
                );
            }
        }
    }

    private void bindInsert(PreparedStatement ps, PedidoEstoqueProdutoModel m) throws SQLException {
        ps.setString(1, m.getId());
        ps.setString(2, m.getPedidoId());
        ps.setString(3, m.getProdutoId());
        if (m.getFornecedorId() == null || m.getFornecedorId().isBlank()) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, m.getFornecedorId());
        }
        if (m.getCustoUnit() == null) {
            ps.setNull(5, Types.REAL);
        } else {
            ps.setDouble(5, m.getCustoUnit());
        }
        if (m.getPrecoVendaUnit() == null) {
            ps.setNull(6, Types.REAL);
        } else {
            ps.setDouble(6, m.getPrecoVendaUnit());
        }
        ps.setInt   (7, m.getQuantidadePedida());
        ps.setInt   (8, m.getQuantidadeRecebida());
        ps.setString(9, m.getStatus());
    }

    private void bindUpdate(PreparedStatement ps, PedidoEstoqueProdutoModel m) throws SQLException {
        if (m.getFornecedorId() == null || m.getFornecedorId().isBlank()) {
            ps.setNull(1, Types.VARCHAR);
        } else {
            ps.setString(1, m.getFornecedorId());
        }
        if (m.getCustoUnit() == null) {
            ps.setNull(2, Types.REAL);
        } else {
            ps.setDouble(2, m.getCustoUnit());
        }
        if (m.getPrecoVendaUnit() == null) {
            ps.setNull(3, Types.REAL);
        } else {
            ps.setDouble(3, m.getPrecoVendaUnit());
        }
        ps.setInt   (4, m.getQuantidadePedida());
        ps.setInt   (5, m.getQuantidadeRecebida());
        ps.setString(6, m.getStatus());
        ps.setString(7, m.getId());
    }

    private void validarLockRecebimento(PedidoEstoqueProdutoModel novo, Connection c) throws SQLException {
        if (novo == null || novo.getId() == null)
            return;
        PedidoEstoqueProdutoModel atual = buscarPorId(novo.getId(), c);
        if (atual == null)
            return;
        if (atual.getQuantidadeRecebida() <= 0)
            return;

        boolean mudouQtdPedida = atual.getQuantidadePedida() != novo.getQuantidadePedida();
        boolean mudouFornecedor = !java.util.Objects.equals(atual.getFornecedorId(), novo.getFornecedorId());
        boolean mudouCusto = !doubleEq(atual.getCustoUnit(), novo.getCustoUnit());
        boolean mudouPreco = !doubleEq(atual.getPrecoVendaUnit(), novo.getPrecoVendaUnit());

        if (mudouQtdPedida || mudouFornecedor || mudouCusto || mudouPreco) {
            throw new SQLException("Item ja recebido: edicao bloqueada (quantidade, fornecedor, custo ou preco).");
        }
    }

    private boolean doubleEq(Double a, Double b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return Math.abs(a - b) < 0.000001;
    }

}
