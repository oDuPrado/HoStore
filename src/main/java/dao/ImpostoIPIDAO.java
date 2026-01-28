package dao;

import model.ImpostoIpiModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImpostoIPIDAO {

    public void inserir(ImpostoIpiModel imposto) throws SQLException {
        String sql = "INSERT INTO imposto_ipi (ncm, aliquota, cnpj_produtor, ativo) VALUES (?, ?, ?, ?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, imposto.getNcm());
            ps.setObject(2, imposto.getAliquota());
            ps.setString(3, imposto.getCnpjProdutor());
            ps.setInt(4, imposto.isAtivo() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public ImpostoIpiModel buscarPorNcm(String ncm) throws SQLException {
        String sql = "SELECT * FROM imposto_ipi WHERE ncm = ? AND ativo = 1";
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

    public List<ImpostoIpiModel> listarTodos() throws SQLException {
        List<ImpostoIpiModel> impostos = new ArrayList<>();
        String sql = "SELECT * FROM imposto_ipi ORDER BY ncm";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                impostos.add(map(rs));
            }
        }
        return impostos;
    }

    private ImpostoIpiModel map(ResultSet rs) throws SQLException {
        ImpostoIpiModel imposto = new ImpostoIpiModel();
        imposto.setId(rs.getInt("id"));
        imposto.setNcm(rs.getString("ncm"));
        imposto.setAliquota(rs.getObject("aliquota", Double.class));
        imposto.setCnpjProdutor(rs.getString("cnpj_produtor"));
        imposto.setAtivo(rs.getInt("ativo") == 1);
        return imposto;
    }
}
