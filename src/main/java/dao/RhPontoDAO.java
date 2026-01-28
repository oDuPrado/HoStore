package dao;

import model.RhPontoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RhPontoDAO {

    public void inserir(RhPontoModel m) throws SQLException {
        String sql = "INSERT INTO rh_ponto(funcionario_id,data,entrada,saida,intervalo_inicio,intervalo_fim,horas_trabalhadas,origem,criado_em) VALUES (?,?,?,?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, false);
            ps.executeUpdate();
        }
    }

    public void atualizar(RhPontoModel m) throws SQLException {
        String sql = "UPDATE rh_ponto SET funcionario_id=?,data=?,entrada=?,saida=?,intervalo_inicio=?,intervalo_fim=?,horas_trabalhadas=?,origem=? WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, true);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, RhPontoModel m, boolean includeId) throws SQLException {
        int i = 1;
        ps.setString(i++, m.getFuncionarioId());
        ps.setString(i++, m.getData());
        ps.setString(i++, m.getEntrada());
        ps.setString(i++, m.getSaida());
        ps.setString(i++, m.getIntervaloInicio());
        ps.setString(i++, m.getIntervaloFim());
        ps.setDouble(i++, m.getHorasTrabalhadas());
        ps.setString(i++, m.getOrigem());
        if (includeId) {
            ps.setInt(i, m.getId());
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_ponto WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<RhPontoModel> listar(String funcionarioId, String dataIni, String dataFim) throws SQLException {
        List<RhPontoModel> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM rh_ponto WHERE 1=1");
        if (funcionarioId != null && !funcionarioId.isBlank()) sb.append(" AND funcionario_id='").append(funcionarioId).append("'");
        if (dataIni != null && !dataIni.isBlank()) sb.append(" AND date(data) >= date('").append(dataIni).append("')");
        if (dataFim != null && !dataFim.isBlank()) sb.append(" AND date(data) <= date('").append(dataFim).append("')");
        sb.append(" ORDER BY date(data) DESC, id DESC");
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sb.toString()); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RhPontoModel m = new RhPontoModel();
                m.setId(rs.getInt("id"));
                m.setFuncionarioId(rs.getString("funcionario_id"));
                m.setData(rs.getString("data"));
                m.setEntrada(rs.getString("entrada"));
                m.setSaida(rs.getString("saida"));
                m.setIntervaloInicio(rs.getString("intervalo_inicio"));
                m.setIntervaloFim(rs.getString("intervalo_fim"));
                m.setHorasTrabalhadas(rs.getDouble("horas_trabalhadas"));
                m.setOrigem(rs.getString("origem"));
                out.add(m);
            }
        }
        return out;
    }
}
