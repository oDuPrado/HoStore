// PedidoCompraDAO – Financeiro (CRUD de Pedidos de Compra)
// Procure: "// PedidoCompraDAO – Financeiro"
package dao;

import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class PedidoCompraDAO {

    /**
     * Insere um novo Pedido de Compra no banco.
     */
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

    /**
     * Atualiza os dados de um Pedido de Compra existente.
     */
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

    public void atualizar(PedidoCompraModel p, Connection c) throws SQLException {
        String sql = "UPDATE pedidos_compras SET "
                + "nome=?, data=?, status=?, fornecedor_id=?, observacoes=? "
                + "WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getData());
            ps.setString(3, p.getStatus());
            ps.setString(4, p.getFornecedorId());
            ps.setString(5, p.getObservacoes());
            ps.setString(6, p.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Exclui um Pedido de Compra pelo ID.
     */
    public void excluir(String id) throws SQLException {
        String sql = "DELETE FROM pedidos_compras WHERE id=?";
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Busca um Pedido de Compra pelo ID.
     */
    public PedidoCompraModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM pedidos_compras WHERE id=?";
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                return new PedidoCompraModel(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("data"),
                        rs.getString("status"),
                        rs.getString("fornecedor_id"),
                        rs.getString("observacoes"));
            }
        }
    }

    public PedidoCompraModel buscarPorId(String id, Connection c) throws SQLException {
        String sql = "SELECT * FROM pedidos_compras WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                return new PedidoCompraModel(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("data"),
                        rs.getString("status"),
                        rs.getString("fornecedor_id"),
                        rs.getString("observacoes"));
            }
        }
    }

    /**
     * Lista todos os Pedidos de Compra, ordenados por data e nome.
     */
    public List<PedidoCompraModel> listarTodos() throws SQLException {
        List<PedidoCompraModel> out = new ArrayList<>();
        String sql = "SELECT * FROM pedidos_compras ORDER BY data DESC, nome";
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
                        rs.getString("observacoes")));
            }
        }
        return out;
    }

    /**
     * Atualiza automaticamente o status do pedido baseado nas quantidades
     * recebidas.
     *
     * Regras:
     * - Se nada recebido: "enviado"
     * - Se parcialmente recebido: "parcialmente recebido"
     * - Se tudo recebido: "recebido"
     *
     * @param idPedido ID do pedido a ser avaliado.
     */
    public void atualizarStatusAutomatico(String idPedido) {
        String sqlTotal = "SELECT SUM(quantidade_pedida) FROM pedido_produtos WHERE pedido_id = ?";
        String sqlRecebido = "SELECT SUM(quantidade_recebida) FROM pedido_produtos WHERE pedido_id = ?";
        try (Connection conn = DB.get();
                PreparedStatement pstTotal = conn.prepareStatement(sqlTotal);
                PreparedStatement pstRec = conn.prepareStatement(sqlRecebido)) {

            pstTotal.setString(1, idPedido);
            pstRec.setString(1, idPedido);

            ResultSet rsTotal = pstTotal.executeQuery();
            ResultSet rsRecebido = pstRec.executeQuery();

            int total = rsTotal.next() ? rsTotal.getInt(1) : 0;
            int recebido = rsRecebido.next() ? rsRecebido.getInt(1) : 0;

            String novoStatus;
            if (recebido == 0) {
                novoStatus = "enviado";
            } else if (recebido >= total) {
                novoStatus = "recebido";
            } else {
                novoStatus = "parcialmente recebido";
            }

            String sqlUpdate = "UPDATE pedidos_compras SET status = ? WHERE id = ?";
            try (PreparedStatement pstUp = conn.prepareStatement(sqlUpdate)) {
                pstUp.setString(1, novoStatus);
                pstUp.setString(2, idPedido);
                pstUp.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recalcularStatus(String pedidoId) throws SQLException {
        List<PedidoEstoqueProdutoModel> itens = new PedidoEstoqueProdutoDAO().listarPorPedido(pedidoId);

        int totalPedida = 0;
        int totalRecebida = 0;

        for (PedidoEstoqueProdutoModel it : itens) {
            totalPedida += it.getQuantidadePedida();
            totalRecebida += it.getQuantidadeRecebida();
        }

        String novoStatus = (totalRecebida == 0) ? "enviado"
                : (totalRecebida >= totalPedida) ? "recebido"
                        : "parcialmente recebido";

        PedidoCompraModel pedido = buscarPorId(pedidoId);
        pedido.setStatus(novoStatus);
        atualizar(pedido);
    }

    public void recalcularStatus(String pedidoId, Connection c) throws SQLException {
        List<PedidoEstoqueProdutoModel> itens = new PedidoEstoqueProdutoDAO().listarPorPedido(pedidoId, c);

        int totalPedida = 0;
        int totalRecebida = 0;

        for (PedidoEstoqueProdutoModel it : itens) {
            totalPedida += it.getQuantidadePedida();
            totalRecebida += it.getQuantidadeRecebida();
        }

        String novoStatus = (totalRecebida == 0) ? "enviado"
                : (totalRecebida >= totalPedida) ? "recebido"
                        : "parcialmente recebido";

        PedidoCompraModel pedido = buscarPorId(pedidoId, c);
        if (pedido == null)
            return;
        pedido.setStatus(novoStatus);
        atualizar(pedido, c);
    }
        /**
     * Lista pedidos de compra filtrando por intervalo de datas e status.
     * @param dataIni data inicial (inclusive) — se null ignora o filtro
     * @param dataFim data final (inclusive) — se null ignora o filtro
     * @param status  status exato a filtrar — se null ou vazio ignora o filtro
     * @return lista de PedidoCompraModel ordenada por data DESC, nome
     * @throws SQLException em caso de falha no banco
     */
    public List<PedidoCompraModel> listarPorDataEStatus(Date dataIni,
                                                        Date dataFim,
                                                        String status) throws SQLException {
        List<PedidoCompraModel> out = new ArrayList<>();
        // Monta SQL dinamicamente
        StringBuilder sb = new StringBuilder(
            "SELECT * FROM pedidos_compras WHERE 1=1"
        );
        if (status != null && !status.isBlank()) {
            sb.append(" AND status = ?");
        }
        if (dataIni != null) {
            sb.append(" AND date(data) >= date(?)");
        }
        if (dataFim != null) {
            sb.append(" AND date(data) <= date(?)");
        }
        sb.append(" ORDER BY data DESC, nome");

        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int idx = 1;
            // vincula parâmetros conforme foram adicionados
            if (status != null && !status.isBlank()) {
                ps.setString(idx++, status);
            }
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            if (dataIni != null) {
                ps.setString(idx++, df.format(dataIni));
            }
            if (dataFim != null) {
                ps.setString(idx++, df.format(dataFim));
            }
            try (ResultSet rs = ps.executeQuery()) {
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
        }
        return out;
    }


}
