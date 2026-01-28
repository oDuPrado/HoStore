package dao;

import model.RhFuncionarioModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RhFuncionarioDAO {

    public String inserir(RhFuncionarioModel m) throws SQLException {
        if (m.getId() == null || m.getId().isBlank()) {
            m.setId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO rh_funcionarios(" +
                "id,nome,tipo_contrato,cpf,cnpj,rg,pis,data_admissao,data_demissao,cargo_id,salario_base,comissao_pct,usuario_id,email,telefone,endereco,ativo,observacoes,criado_em" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,datetime('now'))";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindInsert(ps, m);
            ps.executeUpdate();
        }
        return m.getId();
    }

    public void atualizar(RhFuncionarioModel m) throws SQLException {
        String sql = "UPDATE rh_funcionarios SET nome=?,tipo_contrato=?,cpf=?,cnpj=?,rg=?,pis=?,data_admissao=?,data_demissao=?,cargo_id=?,salario_base=?,comissao_pct=?,usuario_id=?,email=?,telefone=?,endereco=?,ativo=?,observacoes=?,alterado_em=datetime('now') WHERE id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindUpdate(ps, m);
            ps.executeUpdate();
        }
    }

    private void bindInsert(PreparedStatement ps, RhFuncionarioModel m) throws SQLException {
        int i = 1;
        ps.setString(i++, m.getId());
        ps.setString(i++, m.getNome());
        ps.setString(i++, m.getTipoContrato());
        ps.setString(i++, m.getCpf());
        ps.setString(i++, m.getCnpj());
        ps.setString(i++, m.getRg());
        ps.setString(i++, m.getPis());
        ps.setString(i++, m.getDataAdmissao());
        ps.setString(i++, m.getDataDemissao());
        ps.setString(i++, m.getCargoId());
        ps.setDouble(i++, m.getSalarioBase());
        ps.setDouble(i++, m.getComissaoPct());
        ps.setString(i++, m.getUsuarioId());
        ps.setString(i++, m.getEmail());
        ps.setString(i++, m.getTelefone());
        ps.setString(i++, m.getEndereco());
        ps.setInt(i++, m.getAtivo());
        ps.setString(i++, m.getObservacoes());
    }

    private void bindUpdate(PreparedStatement ps, RhFuncionarioModel m) throws SQLException {
        int i = 1;
        ps.setString(i++, m.getNome());
        ps.setString(i++, m.getTipoContrato());
        ps.setString(i++, m.getCpf());
        ps.setString(i++, m.getCnpj());
        ps.setString(i++, m.getRg());
        ps.setString(i++, m.getPis());
        ps.setString(i++, m.getDataAdmissao());
        ps.setString(i++, m.getDataDemissao());
        ps.setString(i++, m.getCargoId());
        ps.setDouble(i++, m.getSalarioBase());
        ps.setDouble(i++, m.getComissaoPct());
        ps.setString(i++, m.getUsuarioId());
        ps.setString(i++, m.getEmail());
        ps.setString(i++, m.getTelefone());
        ps.setString(i++, m.getEndereco());
        ps.setInt(i++, m.getAtivo());
        ps.setString(i++, m.getObservacoes());
        ps.setString(i, m.getId());
    }

    public void excluir(String id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM rh_funcionarios WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public RhFuncionarioModel buscarPorId(String id) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM rh_funcionarios WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public RhFuncionarioModel buscarPorUsuarioId(String usuarioId) throws SQLException {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM rh_funcionarios WHERE usuario_id=?")) {
            ps.setString(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<RhFuncionarioModel> listar(boolean somenteAtivos) throws SQLException {
        List<RhFuncionarioModel> out = new ArrayList<>();
        String sql = "SELECT * FROM rh_funcionarios" + (somenteAtivos ? " WHERE ativo=1" : "") + " ORDER BY nome";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    private RhFuncionarioModel map(ResultSet rs) throws SQLException {
        RhFuncionarioModel m = new RhFuncionarioModel();
        m.setId(rs.getString("id"));
        m.setNome(rs.getString("nome"));
        m.setTipoContrato(rs.getString("tipo_contrato"));
        m.setCpf(rs.getString("cpf"));
        m.setCnpj(rs.getString("cnpj"));
        m.setRg(rs.getString("rg"));
        m.setPis(rs.getString("pis"));
        m.setDataAdmissao(rs.getString("data_admissao"));
        m.setDataDemissao(rs.getString("data_demissao"));
        m.setCargoId(rs.getString("cargo_id"));
        m.setSalarioBase(rs.getDouble("salario_base"));
        m.setComissaoPct(rs.getDouble("comissao_pct"));
        m.setUsuarioId(rs.getString("usuario_id"));
        m.setEmail(rs.getString("email"));
        m.setTelefone(rs.getString("telefone"));
        m.setEndereco(rs.getString("endereco"));
        m.setAtivo(rs.getInt("ativo"));
        m.setObservacoes(rs.getString("observacoes"));
        return m;
    }
}
