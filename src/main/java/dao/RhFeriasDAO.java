package dao;

import model.RhFeriasModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RhFeriasDAO {

    public void inserir(RhFeriasModel m) throws SQLException {
        String sql = "INSERT INTO rh_ferias(funcionario_id,data_inicio,data_fim,abono,status,observacoes,criado_em) VALUES (?,?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, false);
            ps.executeUpdate();
        }
    }

    public void atualizar(RhFeriasModel m) throws SQLException {
        String sql = "UPDATE rh_ferias SET funcionario_id=?,data_inicio=?,data_fim=?,abono=?,status=?,observacoes=? WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, true);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, RhFeriasModel m, boolean includeId) throws SQLException {
        int i = 1;
        ps.setString(i++, m.getFuncionarioId());
        ps.setString(i++, m.getDataInicio());
        ps.setString(i++, m.getDataFim());
        ps.setInt(i++, m.getAbono());
        ps.setString(i++, m.getStatus());
        ps.setString(i++, m.getObservacoes());
        if (includeId) {
            ps.setInt(i, m.getId());
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_ferias WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<RhFeriasModel> listar(String funcionarioId) throws SQLException {
        List<RhFeriasModel> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM rh_ferias WHERE 1=1");
        if (funcionarioId != null && !funcionarioId.isBlank()) sb.append(" AND funcionario_id='").append(funcionarioId).append("'");
        sb.append(" ORDER BY date(data_inicio) DESC, id DESC");
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sb.toString()); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RhFeriasModel m = new RhFeriasModel();
                m.setId(rs.getInt("id"));
                m.setFuncionarioId(rs.getString("funcionario_id"));
                m.setDataInicio(rs.getString("data_inicio"));
                m.setDataFim(rs.getString("data_fim"));
                m.setAbono(rs.getInt("abono"));
                m.setStatus(rs.getString("status"));
                m.setObservacoes(rs.getString("observacoes"));
                out.add(m);
            }
        }
        return out;
    }
}
