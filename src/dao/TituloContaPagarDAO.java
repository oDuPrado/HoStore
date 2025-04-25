package dao;

import model.TituloContaPagarModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para titulos_contas_pagar
 * Agora com suporte ao campo plano_conta_id (vínculo contábil).
 */
public class TituloContaPagarDAO {

    public void inserir(TituloContaPagarModel t) throws SQLException {
        String sql = "INSERT INTO titulos_contas_pagar("
            + "id, fornecedor_id, plano_conta_id, codigo_selecao, data_geracao, valor_total, status, observacoes"
            + ") VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getFornecedorId());
            ps.setString(3, t.getPlanoContaId());
            ps.setString(4, t.getCodigoSelecao());
            ps.setString(5, t.getDataGeracao());
            ps.setDouble(6, t.getValorTotal());
            ps.setString(7, t.getStatus());
            ps.setString(8, t.getObservacoes());
            ps.executeUpdate();
        }
    }

    public void atualizar(TituloContaPagarModel t) throws SQLException {
        String sql = "UPDATE titulos_contas_pagar SET "
            + "fornecedor_id=?, plano_conta_id=?, codigo_selecao=?, data_geracao=?, valor_total=?, status=?, observacoes=? "
            + "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getFornecedorId());
            ps.setString(2, t.getPlanoContaId());
            ps.setString(3, t.getCodigoSelecao());
            ps.setString(4, t.getDataGeracao());
            ps.setDouble(5, t.getValorTotal());
            ps.setString(6, t.getStatus());
            ps.setString(7, t.getObservacoes());
            ps.setString(8, t.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(String id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM titulos_contas_pagar WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public TituloContaPagarModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM titulos_contas_pagar WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new TituloContaPagarModel(
                    rs.getString("id"),
                    rs.getString("fornecedor_id"),
                    rs.getString("plano_conta_id"),
                    rs.getString("codigo_selecao"),
                    rs.getString("data_geracao"),
                    rs.getDouble("valor_total"),
                    rs.getString("status"),
                    rs.getString("observacoes")
                );
            }
        }
    }

    public List<TituloContaPagarModel> listarTodos() throws SQLException {
        List<TituloContaPagarModel> out = new ArrayList<>();
        String sql = "SELECT * FROM titulos_contas_pagar ORDER BY data_geracao DESC";
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new TituloContaPagarModel(
                    rs.getString("id"),
                    rs.getString("fornecedor_id"),
                    rs.getString("plano_conta_id"),
                    rs.getString("codigo_selecao"),
                    rs.getString("data_geracao"),
                    rs.getDouble("valor_total"),
                    rs.getString("status"),
                    rs.getString("observacoes")
                ));
            }
        }
        return out;
    }
}
