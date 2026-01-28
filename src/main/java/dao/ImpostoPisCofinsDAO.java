package dao;

import model.ImpostoPisCofinsModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImpostoPisCofinsDAO {

    public void inserir(ImpostoPisCofinsModel imposto) throws SQLException {
        String sql = """
            INSERT INTO imposto_pis_cofins (ncm, cst_pis, aliquota_pis, cst_cofins, aliquota_cofins, ativo)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, imposto.getNcm());
            ps.setString(2, imposto.getCstPis());
            ps.setObject(3, imposto.getAliquotaPis());
            ps.setString(4, imposto.getCstCofins());
            ps.setObject(5, imposto.getAliquotaCofins());
            ps.setInt(6, imposto.isAtivo() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public ImpostoPisCofinsModel buscarPorNcm(String ncm) throws SQLException {
        String sql = "SELECT * FROM imposto_pis_cofins WHERE ncm = ? AND ativo = 1 LIMIT 1";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ncm);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<ImpostoPisCofinsModel> listarTodos() throws SQLException {
        List<ImpostoPisCofinsModel> impostos = new ArrayList<>();
        String sql = "SELECT * FROM imposto_pis_cofins WHERE ativo = 1 ORDER BY ncm";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                impostos.add(map(rs));
            }
        }
        return impostos;
    }

    private ImpostoPisCofinsModel map(ResultSet rs) throws SQLException {
        ImpostoPisCofinsModel imposto = new ImpostoPisCofinsModel();
        imposto.setId(rs.getInt("id"));
        imposto.setNcm(rs.getString("ncm"));
        imposto.setCstPis(rs.getString("cst_pis"));
        imposto.setAliquotaPis(rs.getObject("aliquota_pis", Double.class));
        imposto.setCstCofins(rs.getString("cst_cofins"));
        imposto.setAliquotaCofins(rs.getObject("aliquota_cofins", Double.class));
        imposto.setAtivo(rs.getInt("ativo") == 1);
        return imposto;
    }
}
