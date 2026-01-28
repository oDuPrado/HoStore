package dao;

import model.ImpostoIcmsModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ImpostoICMSDAO {

    public void inserir(ImpostoIcmsModel imposto) throws SQLException {
        String sql = """
            INSERT INTO imposto_icms (estado, estado_destino, ncm, aliquota_consumidor, aliquota_contribuinte, reducao_base, mva_bc, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, imposto.getEstado());
            ps.setString(2, imposto.getEstadoDestino());
            ps.setString(3, imposto.getNcm());
            ps.setObject(4, imposto.getAliquotaConsumidor());
            ps.setObject(5, imposto.getAliquotaContribuinte());
            ps.setDouble(6, imposto.getReducaoBase());
            ps.setDouble(7, imposto.getMvaBc());
            ps.setInt(8, imposto.isAtivo() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public ImpostoIcmsModel buscarPorNcmEUf(String ncm, String estado, String estadoDestino) throws SQLException {
        String sql = "SELECT * FROM imposto_icms WHERE ncm = ? AND estado = ? AND estado_destino = ? AND ativo = 1";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ncm);
            ps.setString(2, estado);
            ps.setString(3, estadoDestino);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<ImpostoIcmsModel> listarTodos() throws SQLException {
        List<ImpostoIcmsModel> impostos = new ArrayList<>();
        String sql = "SELECT * FROM imposto_icms ORDER BY estado, ncm";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                impostos.add(map(rs));
            }
        }
        return impostos;
    }

    private ImpostoIcmsModel map(ResultSet rs) throws SQLException {
        ImpostoIcmsModel imposto = new ImpostoIcmsModel();
        imposto.setId(rs.getInt("id"));
        imposto.setEstado(rs.getString("estado"));
        imposto.setEstadoDestino(rs.getString("estado_destino"));
        imposto.setNcm(rs.getString("ncm"));
        imposto.setAliquotaConsumidor(rs.getObject("aliquota_consumidor", Double.class));
        imposto.setAliquotaContribuinte(rs.getObject("aliquota_contribuinte", Double.class));
        imposto.setReducaoBase(rs.getDouble("reducao_base"));
        imposto.setMvaBc(rs.getDouble("mva_bc"));
        imposto.setAtivo(rs.getInt("ativo") == 1);
        return imposto;
    }
}
