package dao;

import model.ConfiguracaoNfeNfceModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfiguracaoNfeNfceDAO {

    /**
     * Salva ou atualiza a configuração fiscal.
     * Utiliza INSERT OR REPLACE para simplificar a operação, já que haverá apenas uma linha.
     * @param config O objeto de configuração a ser salvo.
     * @throws SQLException Se ocorrer um erro de banco de dados.
     */
    public void salvar(ConfiguracaoNfeNfceModel config) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO configuracao_nfe_nfce
            (id, emitir_nfe, certificado_path_nfe, certificado_senha_nfe, serie_nfe, numero_inicial_nfe,
             emitir_nfce, csc_nfce, id_csc_nfce, certificado_path_nfce, certificado_senha_nfce, serie_nfce, numero_inicial_nfce,
             ambiente, regime_tributario)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, ConfiguracaoNfeNfceModel.ID_UNICO);
            ps.setInt(i++, config.isEmitirNfe() ? 1 : 0);
            ps.setString(i++, config.getCertificadoPathNfe());
            ps.setString(i++, config.getCertificadoSenhaNfe());
            ps.setInt(i++, config.getSerieNfe());
            ps.setInt(i++, config.getNumeroInicialNfe());
            ps.setInt(i++, config.isEmitirNfce() ? 1 : 0);
            ps.setString(i++, config.getCscNfce());
            ps.setInt(i++, config.getIdCscNfce());
            ps.setString(i++, config.getCertificadoPathNfce());
            ps.setString(i++, config.getCertificadoSenhaNfce());
            ps.setInt(i++, config.getSerieNfce());
            ps.setInt(i++, config.getNumeroInicialNfce());
            ps.setString(i++, config.getAmbiente());
            ps.setString(i++, config.getRegimeTributario());
            ps.executeUpdate();
        }
    }

    /**
     * Obtém a configuração fiscal do banco de dados.
     * @return O objeto de configuração, ou um novo objeto com valores padrão se não for encontrado.
     * @throws SQLException Se ocorrer um erro de banco de dados.
     */
    public ConfiguracaoNfeNfceModel obter() throws SQLException {
        String sql = "SELECT * FROM configuracao_nfe_nfce WHERE id = ?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ConfiguracaoNfeNfceModel.ID_UNICO);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        // Se não encontrar, retorna uma configuração padrão.
        return new ConfiguracaoNfeNfceModel();
    }

    private ConfiguracaoNfeNfceModel map(ResultSet rs) throws SQLException {
        ConfiguracaoNfeNfceModel config = new ConfiguracaoNfeNfceModel();
        config.setId(rs.getString("id"));
        config.setEmitirNfe(rs.getInt("emitir_nfe") == 1);
        config.setCertificadoPathNfe(rs.getString("certificado_path_nfe"));
        config.setCertificadoSenhaNfe(rs.getString("certificado_senha_nfe"));
        config.setSerieNfe(rs.getInt("serie_nfe"));
        config.setNumeroInicialNfe(rs.getInt("numero_inicial_nfe"));
        config.setEmitirNfce(rs.getInt("emitir_nfce") == 1);
        config.setCscNfce(rs.getString("csc_nfce"));
        config.setIdCscNfce(rs.getInt("id_csc_nfce"));
        config.setCertificadoPathNfce(rs.getString("certificado_path_nfce"));
        config.setCertificadoSenhaNfce(rs.getString("certificado_senha_nfce"));
        config.setSerieNfce(rs.getInt("serie_nfce"));
        config.setNumeroInicialNfce(rs.getInt("numero_inicial_nfce"));
        config.setAmbiente(rs.getString("ambiente"));
        config.setRegimeTributario(rs.getString("regime_tributario"));
        return config;
    }
}
