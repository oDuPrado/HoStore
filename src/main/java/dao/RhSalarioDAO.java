package dao;

import model.RhSalarioModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RhSalarioDAO {

    public void inserir(RhSalarioModel m) throws SQLException {
        String sql = "INSERT INTO rh_salarios(funcionario_id,cargo_id,salario_base,data_inicio,data_fim,motivo,criado_em) VALUES (?,?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getFuncionarioId());
            ps.setString(2, m.getCargoId());
            ps.setDouble(3, m.getSalarioBase());
            ps.setString(4, m.getDataInicio());
            ps.setString(5, m.getDataFim());
            ps.setString(6, m.getMotivo());
            ps.executeUpdate();
        }
    }

    public List<RhSalarioModel> listarPorFuncionario(String funcionarioId) throws SQLException {
        List<RhSalarioModel> out = new ArrayList<>();
        String sql = "SELECT * FROM rh_salarios WHERE funcionario_id=? ORDER BY data_inicio DESC";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, funcionarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RhSalarioModel m = new RhSalarioModel();
                    m.setId(rs.getInt("id"));
                    m.setFuncionarioId(rs.getString("funcionario_id"));
                    m.setCargoId(rs.getString("cargo_id"));
                    m.setSalarioBase(rs.getDouble("salario_base"));
                    m.setDataInicio(rs.getString("data_inicio"));
                    m.setDataFim(rs.getString("data_fim"));
                    m.setMotivo(rs.getString("motivo"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    public RhSalarioModel buscarVigente(String funcionarioId, String dataIso) throws SQLException {
        String sql = "SELECT * FROM rh_salarios WHERE funcionario_id=? AND data_inicio<=? AND (data_fim IS NULL OR data_fim>=?) ORDER BY data_inicio DESC LIMIT 1";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, funcionarioId);
            ps.setString(2, dataIso);
            ps.setString(3, dataIso);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                RhSalarioModel m = new RhSalarioModel();
                m.setId(rs.getInt("id"));
                m.setFuncionarioId(rs.getString("funcionario_id"));
                m.setCargoId(rs.getString("cargo_id"));
                m.setSalarioBase(rs.getDouble("salario_base"));
                m.setDataInicio(rs.getString("data_inicio"));
                m.setDataFim(rs.getString("data_fim"));
                m.setMotivo(rs.getString("motivo"));
                return m;
            }
        }
    }
}
