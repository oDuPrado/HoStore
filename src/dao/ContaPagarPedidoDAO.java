package dao;

import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO para a tabela de vínculo entre Contas a Pagar e Pedidos de Compra.
 * 
 * Tabela: contas_pagar_pedidos
 * Colunas:
 *   - conta_pagar_id TEXT NOT NULL
 *   - pedido_id      TEXT NOT NULL
 * 
 * Chave primária composta em (conta_pagar_id, pedido_id).
 */
public class ContaPagarPedidoDAO {

    /**
     * Persiste o conjunto de pedidos vinculados a uma conta a pagar.
     * Se já houver vínculos antigos, eles são removidos antes.
     *
     * @param contaPagarId ID da conta a pagar
     * @param pedidoIds    Conjunto de IDs de pedidos para vincular
     * @throws SQLException em caso de falha no banco de dados
     */
    public void vincularPedidos(String contaPagarId, Set<String> pedidoIds) throws SQLException {
        Connection c = DB.get();
        try {
            // Desligamos o autocommit para garantir atomicidade
            c.setAutoCommit(false);

            // 1) Remove vínculos pré-existentes
            removerVinculos(contaPagarId, c);

            // 2) Insere novos vínculos em batch
            if (pedidoIds != null && !pedidoIds.isEmpty()) {
                String sqlInsert = 
                    "INSERT INTO contas_pagar_pedidos(conta_pagar_id, pedido_id) VALUES (?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sqlInsert)) {
                    for (String pedidoId : pedidoIds) {
                        ps.setString(1, contaPagarId);
                        ps.setString(2, pedidoId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // Confirma todas as alterações de uma vez
            c.commit();
        } catch (SQLException ex) {
            // Em caso de erro, desfaz tudo
            c.rollback();
            throw ex;
        } finally {
            // Restaura o comportamento padrão e fecha conexão
            c.setAutoCommit(true);
            c.close();
        }
    }

    /**
     * Remove todos os vínculos de pedidos para a conta especificada.
     * Usa a própria conexão recebida (para chamadas internas em transação).
     *
     * @param contaPagarId ID da conta a pagar
     * @param c            Conexão ativa (sem autocommit)
     * @throws SQLException em caso de falha no banco de dados
     */
    private void removerVinculos(String contaPagarId, Connection c) throws SQLException {
        String sqlDelete = 
            "DELETE FROM contas_pagar_pedidos WHERE conta_pagar_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sqlDelete)) {
            ps.setString(1, contaPagarId);
            ps.executeUpdate();
        }
    }

    /**
     * Remove todos os vínculos de pedidos para a conta especificada.
     * Abre e fecha uma conexão própria.
     *
     * @param contaPagarId ID da conta a pagar
     * @throws SQLException em caso de falha no banco de dados
     */
    public void removerVinculos(String contaPagarId) throws SQLException {
        try (Connection c = DB.get()) {
            removerVinculos(contaPagarId, c);
        }
    }

    /**
     * Lista os IDs de todos os pedidos vinculados a uma determinada conta a pagar.
     *
     * @param contaPagarId ID da conta a pagar
     * @return Lista de pedido_id vinculados (pode ser vazia)
     * @throws SQLException em caso de falha no banco de dados
     */
    public List<String> listarPedidosPorConta(String contaPagarId) throws SQLException {
        List<String> pedidos = new ArrayList<>();
        String sql = 
            "SELECT pedido_id FROM contas_pagar_pedidos WHERE conta_pagar_id = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, contaPagarId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(rs.getString("pedido_id"));
                }
            }
        }
        return pedidos;
    }
}
