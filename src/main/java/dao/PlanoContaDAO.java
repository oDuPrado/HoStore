package dao;

import model.PlanoContaModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanoContaDAO {

    /** Insere nova conta no banco */
    public void inserir(PlanoContaModel p) throws SQLException {
        String sql = "INSERT INTO planos_contas "
                   + "(id,codigo,descricao,tipo,parent_id,observacoes) "
                   + "VALUES(?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
    
            ps.setString(1, p.getId());
            ps.setString(2, p.getCodigo());
            ps.setString(3, p.getDescricao());
            ps.setString(4, p.getTipo());
            if (p.getParentId() != null) ps.setString(5, p.getParentId());
            else ps.setNull(5, Types.VARCHAR);
            ps.setString(6, p.getObservacoes());
    
            // 👇 LOG DE DEBUG AQUI
            System.out.println("[DAO] Inserindo PlanoConta: id=" + p.getId() +
                               " desc=" + p.getDescricao() +
                               " tipo=" + p.getTipo() +
                               " parent=" + p.getParentId());
    
            ps.executeUpdate();
    
            // 👇 CONFIRMAÇÃO
            System.out.println("[DAO] Inserção concluída.");
        }
    }
    

    /** Atualiza conta existente */
    public void atualizar(PlanoContaModel p) throws SQLException {
        String sql = "UPDATE planos_contas SET "
                   + "codigo=?, descricao=?, tipo=?, parent_id=?, observacoes=? "
                   + "WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getDescricao());
            ps.setString(3, p.getTipo());
            if (p.getParentId() != null) ps.setString(4, p.getParentId());
            else ps.setNull(4, Types.VARCHAR);
            ps.setString(5, p.getObservacoes());
            ps.setString(6, p.getId());
            ps.executeUpdate();
        }
    }

    /** Exclui conta por ID */
    public void excluir(String id) throws SQLException {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM planos_contas WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /** Busca conta por ID */
    public PlanoContaModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT * FROM planos_contas WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    /** Lista todas as contas (ordenadas por codigo interno) */
    public List<PlanoContaModel> listarTodos() throws SQLException {
        List<PlanoContaModel> out = new ArrayList<>();
        String sql = "SELECT * FROM planos_contas ORDER BY codigo";
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    /** Mapeia um ResultSet para o modelo */
    private PlanoContaModel map(ResultSet rs) throws SQLException {
        return new PlanoContaModel(
            rs.getString("id"),
            rs.getString("codigo"),
            rs.getString("descricao"),
            rs.getString("tipo"),
            rs.getString("parent_id"),
            rs.getString("observacoes")
        );
    }
}
