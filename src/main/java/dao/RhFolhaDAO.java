package dao;

import model.RhFolhaModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RhFolhaDAO {

    public void inserir(RhFolhaModel m) throws SQLException {
        String sql = "INSERT INTO rh_folha(competencia,funcionario_id,salario_base,horas_trabalhadas,horas_extras,descontos,comissao,total_bruto,total_liquido,status,criado_em) VALUES (?,?,?,?,?,?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, false);
            ps.executeUpdate();
        }
    }

    public void atualizar(RhFolhaModel m) throws SQLException {
        String sql = "UPDATE rh_folha SET competencia=?,funcionario_id=?,salario_base=?,horas_trabalhadas=?,horas_extras=?,descontos=?,comissao=?,total_bruto=?,total_liquido=?,status=? WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m, true);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, RhFolhaModel m, boolean includeId) throws SQLException {
        int i = 1;
        ps.setString(i++, m.getCompetencia());
        ps.setString(i++, m.getFuncionarioId());
        ps.setDouble(i++, m.getSalarioBase());
        ps.setDouble(i++, m.getHorasTrabalhadas());
        ps.setDouble(i++, m.getHorasExtras());
        ps.setDouble(i++, m.getDescontos());
        ps.setDouble(i++, m.getComissao());
        ps.setDouble(i++, m.getTotalBruto());
        ps.setDouble(i++, m.getTotalLiquido());
        ps.setString(i++, m.getStatus());
        if (includeId) {
            ps.setInt(i, m.getId());
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_folha WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<RhFolhaModel> listarPorCompetencia(String competencia) throws SQLException {
        List<RhFolhaModel> out = new ArrayList<>();
        String sql = "SELECT * FROM rh_folha WHERE competencia=? ORDER BY id DESC";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, competencia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RhFolhaModel m = map(rs);
                    out.add(m);
                }
            }
        }
        return out;
    }

    public List<RhFolhaModel> listarPorFuncionario(String funcionarioId) throws SQLException {
        List<RhFolhaModel> out = new ArrayList<>();
        String sql = "SELECT * FROM rh_folha WHERE funcionario_id=? ORDER BY competencia DESC, id DESC";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, funcionarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    public RhFolhaModel buscarPorCompetenciaFuncionario(String competencia, String funcionarioId) throws SQLException {
        String sql = "SELECT * FROM rh_folha WHERE competencia=? AND funcionario_id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, competencia);
            ps.setString(2, funcionarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    private RhFolhaModel map(ResultSet rs) throws SQLException {
        RhFolhaModel m = new RhFolhaModel();
        m.setId(rs.getInt("id"));
        m.setCompetencia(rs.getString("competencia"));
        m.setFuncionarioId(rs.getString("funcionario_id"));
        m.setSalarioBase(rs.getDouble("salario_base"));
        m.setHorasTrabalhadas(rs.getDouble("horas_trabalhadas"));
        m.setHorasExtras(rs.getDouble("horas_extras"));
        m.setDescontos(rs.getDouble("descontos"));
        m.setComissao(rs.getDouble("comissao"));
        m.setTotalBruto(rs.getDouble("total_bruto"));
        m.setTotalLiquido(rs.getDouble("total_liquido"));
        m.setStatus(rs.getString("status"));
        return m;
    }
}
