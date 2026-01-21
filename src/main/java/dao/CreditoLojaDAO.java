package dao;

import model.CreditoLojaModel;
import model.CreditoLojaMovimentacaoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para manipular saldo de crédito de loja e seu histórico de movimentações.
 */
public class CreditoLojaDAO {

    /**
     * Retorna o objeto CreditoLojaModel para um cliente, ou null se não existir.
     */
    public CreditoLojaModel getByClienteId(String clienteId) throws SQLException {
        String sql = "SELECT id, cliente_id, valor FROM credito_loja WHERE cliente_id = ?";
        try (Connection conn = DB.get();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, clienteId);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    CreditoLojaModel m = new CreditoLojaModel();
                    m.setId(rs.getString("id"));
                    m.setClienteId(rs.getString("cliente_id"));
                    m.setValor(rs.getDouble("valor"));
                    return m;
                }
                return null;
            }
        }
    }

    /**
     * Insere uma nova linha em credito_loja.
     */
    public void insertCredito(CreditoLojaModel model) throws SQLException {
        String sql = "INSERT INTO credito_loja (id, cliente_id, valor) VALUES (?, ?, ?)";
        try (Connection conn = DB.get();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, model.getId());
            p.setString(2, model.getClienteId());
            p.setDouble(3, model.getValor());
            p.executeUpdate();
        }
    }

    /**
     * Atualiza o saldo de crédito de loja.
     */
    public void updateCredito(CreditoLojaModel model) throws SQLException {
        String sql = "UPDATE credito_loja SET valor = ? WHERE id = ?";
        try (Connection conn = DB.get();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setDouble(1, model.getValor());
            p.setString(2, model.getId());
            p.executeUpdate();
        }
    }

    /**
     * Insere uma movimentação no histórico.
     */
    public void insertMovimentacao(CreditoLojaMovimentacaoModel mov) throws SQLException {
        String sql = "INSERT INTO credito_loja_movimentacoes (id, cliente_id, valor, tipo, referencia, data, evento_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DB.get();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, mov.getId());
            p.setString(2, mov.getClienteId());
            p.setDouble(3, mov.getValor());
            p.setString(4, mov.getTipo());
            p.setString(5, mov.getReferencia());
            p.setString(6, mov.getData());
            p.setString(7, mov.getEventoId());
            p.executeUpdate();
        }
    }

    /**
     * Retorna todas as movimentações de um cliente, ordenadas da mais recente para a mais antiga.
     */
    public List<CreditoLojaMovimentacaoModel> getMovimentacoes(String clienteId) throws SQLException {
        String sql = "SELECT id, cliente_id, valor, tipo, referencia, data, evento_id "
                   + "FROM credito_loja_movimentacoes WHERE cliente_id = ? ORDER BY data DESC";
        List<CreditoLojaMovimentacaoModel> lista = new ArrayList<>();
        try (Connection conn = DB.get();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, clienteId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    CreditoLojaMovimentacaoModel m = new CreditoLojaMovimentacaoModel();
                    m.setId(rs.getString("id"));
                    m.setClienteId(rs.getString("cliente_id"));
                    m.setValor(rs.getDouble("valor"));
                    m.setTipo(rs.getString("tipo"));
                    m.setReferencia(rs.getString("referencia"));
                    m.setData(rs.getString("data"));
                    m.setEventoId(rs.getString("evento_id"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
