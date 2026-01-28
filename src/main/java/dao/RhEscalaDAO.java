package dao;

import model.RhEscalaModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RhEscalaDAO {

    public void inserir(RhEscalaModel m) throws SQLException {
        String sql = "INSERT INTO rh_escala(funcionario_id,data,inicio,fim,observacoes,criado_em) VALUES (?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, false);
            ps.executeUpdate();
        }
    }

    public void atualizar(RhEscalaModel m) throws SQLException {
        String sql = "UPDATE rh_escala SET funcionario_id=?,data=?,inicio=?,fim=?,observacoes=? WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, true);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, RhEscalaModel m, boolean includeId) throws SQLException {
        int i = 1;
        ps.setString(i++, m.getFuncionarioId());
        ps.setString(i++, m.getData());
        ps.setString(i++, m.getInicio());
        ps.setString(i++, m.getFim());
        ps.setString(i++, m.getObservacoes());
        if (includeId) {
            ps.setInt(i, m.getId());
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_escala WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<RhEscalaModel> listar(String funcionarioId, String dataIni, String dataFim) throws SQLException {
        List<RhEscalaModel> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM rh_escala WHERE 1=1");
        if (funcionarioId != null && !funcionarioId.isBlank()) sb.append(" AND funcionario_id='").append(funcionarioId).append("'");
        if (dataIni != null && !dataIni.isBlank()) sb.append(" AND date(data) >= date('").append(dataIni).append("')");
        if (dataFim != null && !dataFim.isBlank()) sb.append(" AND date(data) <= date('").append(dataFim).append("')");
        sb.append(" ORDER BY date(data) DESC, id DESC");
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sb.toString()); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RhEscalaModel m = new RhEscalaModel();
                m.setId(rs.getInt("id"));
                m.setFuncionarioId(rs.getString("funcionario_id"));
                m.setData(rs.getString("data"));
                m.setInicio(rs.getString("inicio"));
                m.setFim(rs.getString("fim"));
                m.setObservacoes(rs.getString("observacoes"));
                out.add(m);
            }
        }
        return out;
    }
}
