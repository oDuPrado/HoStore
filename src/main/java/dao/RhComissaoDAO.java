package dao;

import model.RhComissaoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RhComissaoDAO {

    public void inserir(RhComissaoModel m) throws SQLException {
        String sql = "INSERT OR IGNORE INTO rh_comissoes(venda_id,funcionario_id,percentual,valor,data,observacoes) VALUES (?,?,?,?,?,?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, m.getVendaId());
            ps.setString(2, m.getFuncionarioId());
            ps.setDouble(3, m.getPercentual());
            ps.setDouble(4, m.getValor());
            ps.setString(5, m.getData());
            ps.setString(6, m.getObservacoes());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_comissoes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<RhComissaoModel> listar(String dataIni, String dataFim) throws SQLException {
        List<RhComissaoModel> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM rh_comissoes WHERE 1=1");
        if (dataIni != null && !dataIni.isBlank()) sb.append(" AND date(data) >= date('").append(dataIni).append("')");
        if (dataFim != null && !dataFim.isBlank()) sb.append(" AND date(data) <= date('").append(dataFim).append("')");
        sb.append(" ORDER BY date(data) DESC, id DESC");
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sb.toString()); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RhComissaoModel m = new RhComissaoModel();
                m.setId(rs.getInt("id"));
                m.setVendaId((Integer) rs.getObject("venda_id"));
                m.setFuncionarioId(rs.getString("funcionario_id"));
                m.setPercentual(rs.getDouble("percentual"));
                m.setValor(rs.getDouble("valor"));
                m.setData(rs.getString("data"));
                m.setObservacoes(rs.getString("observacoes"));
                out.add(m);
            }
        }
        return out;
    }

    public List<RhComissaoModel> listarPorFuncionario(String funcionarioId) throws SQLException {
        List<RhComissaoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM rh_comissoes WHERE funcionario_id=? ORDER BY date(data) DESC, id DESC";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, funcionarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RhComissaoModel m = new RhComissaoModel();
                    m.setId(rs.getInt("id"));
                    m.setVendaId((Integer) rs.getObject("venda_id"));
                    m.setFuncionarioId(rs.getString("funcionario_id"));
                    m.setPercentual(rs.getDouble("percentual"));
                    m.setValor(rs.getDouble("valor"));
                    m.setData(rs.getString("data"));
                    m.setObservacoes(rs.getString("observacoes"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    public double somarPorFuncionarioPeriodo(String funcionarioId, String dataIni, String dataFim) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor),0) AS total FROM rh_comissoes WHERE funcionario_id=? AND date(data) >= date(?) AND date(data) <= date(?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, funcionarioId);
            ps.setString(2, dataIni);
            ps.setString(3, dataFim);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0.0;
    }
}
