package dao;

import model.ConfigFiscalModel;
import util.DB;

import java.sql.*;

/**
 * DAO para configurações fiscais padrão da loja (config_fiscal_default).
 */
public class ConfigFiscalDefaultDAO {

    public ConfigFiscalModel getDefault() throws SQLException {
        String sql = "SELECT * FROM config_fiscal_default WHERE id = 'DEFAULT' LIMIT 1";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new ConfigFiscalModel(
                        rs.getString("id"),
                        "DEFAULT",
                        rs.getString("regime_tributario"),
                        rs.getString("cfop_padrao"),
                        rs.getString("csosn_padrao"),
                        rs.getString("origem_padrao"),
                        rs.getString("ncm_padrao"),
                        rs.getString("unidade_padrao")
                );
            }
        }
        return null;
    }

    public void saveDefault(ConfigFiscalModel cfg) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO config_fiscal_default
            (id, regime_tributario, cfop_padrao, csosn_padrao, origem_padrao, ncm_padrao, unidade_padrao)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, "DEFAULT");
            ps.setString(i++, cfg.getRegimeTributario());
            ps.setString(i++, cfg.getCfopPadrao());
            ps.setString(i++, cfg.getCsosnPadrao());
            ps.setString(i++, cfg.getOrigemPadrao());
            ps.setString(i++, cfg.getNcmPadrao());
            ps.setString(i++, cfg.getUnidadePadrao());
            ps.executeUpdate();
        }
    }
}
