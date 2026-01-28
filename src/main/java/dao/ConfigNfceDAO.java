package dao;

import model.ConfigNfceModel;
import model.ConfigLojaModel;
import dao.ConfigLojaDAO;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigNfceDAO {

    public ConfigNfceModel getConfig() throws SQLException {
        String sql = "SELECT * FROM config_nfce LIMIT 1";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return map(rs);
            }
        }
        ConfigNfceModel fallback = buildFallbackConfig();
        if (fallback != null) {
            saveConfig(fallback);
        }
        return fallback;
    }

    public ConfigNfceModel getConfig(Connection conn) throws SQLException {
        String sql = "SELECT * FROM config_nfce LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return map(rs);
            }
        }
        return null;
    }

    private ConfigNfceModel buildFallbackConfig() throws SQLException {
        ConfigNfceModel cfg = new ConfigNfceModel();
        cfg.setId("CONFIG_PADRAO");
        cfg.setEmitirNfce(1);
        cfg.setSerieNfce(1);
        cfg.setNumeroInicialNfce(1);
        cfg.setAmbiente("OFF");
        cfg.setRegimeTributario("Simples Nacional");
        cfg.setModoEmissao("OFFLINE_VALIDACAO");

        ConfigLojaModel loja = null;
        try {
            loja = new ConfigLojaDAO().buscar();
        } catch (Exception ignored) {
        }

        if (loja != null) {
            cfg.setNomeEmpresa(loja.getNome());
            cfg.setNomeFantasia(loja.getNomeFantasia());
            cfg.setCnpj(loja.getCnpj());
            cfg.setInscricaoEstadual(loja.getInscricaoEstadual());
            cfg.setRegimeTributario(loja.getRegimeTributario());
            cfg.setUf(loja.getEnderecoUf());
            cfg.setEnderecoLogradouro(loja.getEnderecoLogradouro());
            cfg.setEnderecoNumero(loja.getEnderecoNumero());
            cfg.setEnderecoComplemento(loja.getEnderecoComplemento());
            cfg.setEnderecoBairro(loja.getEnderecoBairro());
            cfg.setEnderecoMunicipio(loja.getEnderecoMunicipio());
            cfg.setEnderecoCep(loja.getEnderecoCep());

            cfg.setCsc(loja.getTokenCsc());
            if (loja.getCsc() != null) {
                try {
                    cfg.setIdCsc(Integer.parseInt(loja.getCsc().trim()));
                } catch (NumberFormatException ignored) {
                }
            }

            cfg.setCertA1Path(loja.getCertificadoPath());
            cfg.setCertA1Senha(loja.getCertificadoSenha());
        }

        return cfg;
    }

    private ConfigNfceModel map(ResultSet rs) throws SQLException {
        ConfigNfceModel config = new ConfigNfceModel();
        config.setId(rs.getString("id"));
        int emitir = rs.getInt("emitir_nfce");
        if (rs.wasNull()) {
            emitir = 1;
        }
        config.setEmitirNfce(emitir);
        config.setCsc(rs.getString("csc_nfce"));
        config.setIdCsc(rs.getInt("id_csc_nfce"));
        config.setCertA1Path(rs.getString("cert_a1_path"));
        config.setCertA1Senha(rs.getString("cert_a1_senha"));
        config.setSerieNfce(rs.getInt("serie_nfce"));
        config.setNumeroInicialNfce(rs.getInt("numero_inicial_nfce"));
        config.setAmbiente(rs.getString("ambiente"));
        config.setRegimeTributario(rs.getString("regime_tributario"));
        config.setNomeEmpresa(rs.getString("nome_empresa"));
        config.setCnpj(rs.getString("cnpj"));
        config.setInscricaoEstadual(rs.getString("inscricao_estadual"));
        config.setUf(rs.getString("uf"));
        config.setNomeFantasia(rs.getString("nome_fantasia"));
        config.setEnderecoLogradouro(rs.getString("endereco_logradouro"));
        config.setEnderecoNumero(rs.getString("endereco_numero"));
        config.setEnderecoComplemento(rs.getString("endereco_complemento"));
        config.setEnderecoBairro(rs.getString("endereco_bairro"));
        config.setEnderecoMunicipio(rs.getString("endereco_municipio"));
        config.setEnderecoCep(rs.getString("endereco_cep"));
        config.setModoEmissao(rs.getString("modo_emissao"));
        return config;
    }

    public void updateConfig(ConfigNfceModel config) throws SQLException {
        String sql = "UPDATE config_nfce SET " +
                "emitir_nfce = ?, csc_nfce = ?, id_csc_nfce = ?, cert_a1_path = ?, cert_a1_senha = ?, " +
                "serie_nfce = ?, numero_inicial_nfce = ?, ambiente = ?, regime_tributario = ?, " +
                "nome_empresa = ?, cnpj = ?, inscricao_estadual = ?, uf = ?, nome_fantasia = ?, " +
                "endereco_logradouro = ?, endereco_numero = ?, endereco_complemento = ?, " +
                "endereco_bairro = ?, endereco_municipio = ?, endereco_cep = ?, modo_emissao = ? " +
                "WHERE id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, config.getEmitirNfce());
            ps.setString(2, config.getCsc());
            ps.setInt(3, config.getIdCsc());
            ps.setString(4, config.getCertA1Path());
            ps.setString(5, config.getCertA1Senha());
            ps.setInt(6, config.getSerieNfce());
            ps.setInt(7, config.getNumeroInicialNfce());
            ps.setString(8, config.getAmbiente());
            ps.setString(9, config.getRegimeTributario());
            ps.setString(10, config.getNomeEmpresa());
            ps.setString(11, config.getCnpj());
            ps.setString(12, config.getInscricaoEstadual());
            ps.setString(13, config.getUf());
            ps.setString(14, config.getNomeFantasia());
            ps.setString(15, config.getEnderecoLogradouro());
            ps.setString(16, config.getEnderecoNumero());
            ps.setString(17, config.getEnderecoComplemento());
            ps.setString(18, config.getEnderecoBairro());
            ps.setString(19, config.getEnderecoMunicipio());
            ps.setString(20, config.getEnderecoCep());
            ps.setString(21, config.getModoEmissao());
            ps.setString(22, config.getId());

            ps.executeUpdate();
        }
    }

    public void saveConfig(ConfigNfceModel config) throws SQLException {
        if (config.getId() == null || config.getId().isBlank()) {
            config.setId("CONFIG_PADRAO");
        }

        String sql = """
            INSERT OR REPLACE INTO config_nfce
            (id, emitir_nfce, csc_nfce, id_csc_nfce, cert_a1_path, cert_a1_senha,
             serie_nfce, numero_inicial_nfce, ambiente, regime_tributario,
             nome_empresa, cnpj, inscricao_estadual, uf, nome_fantasia,
             endereco_logradouro, endereco_numero, endereco_complemento,
             endereco_bairro, endereco_municipio, endereco_cep, modo_emissao)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, config.getId());
            ps.setInt(i++, config.getEmitirNfce());
            ps.setString(i++, config.getCsc());
            ps.setInt(i++, config.getIdCsc());
            ps.setString(i++, config.getCertA1Path());
            ps.setString(i++, config.getCertA1Senha());
            ps.setInt(i++, config.getSerieNfce());
            ps.setInt(i++, config.getNumeroInicialNfce());
            ps.setString(i++, config.getAmbiente());
            ps.setString(i++, config.getRegimeTributario());
            ps.setString(i++, config.getNomeEmpresa());
            ps.setString(i++, config.getCnpj());
            ps.setString(i++, config.getInscricaoEstadual());
            ps.setString(i++, config.getUf());
            ps.setString(i++, config.getNomeFantasia());
            ps.setString(i++, config.getEnderecoLogradouro());
            ps.setString(i++, config.getEnderecoNumero());
            ps.setString(i++, config.getEnderecoComplemento());
            ps.setString(i++, config.getEnderecoBairro());
            ps.setString(i++, config.getEnderecoMunicipio());
            ps.setString(i++, config.getEnderecoCep());
            ps.setString(i++, config.getModoEmissao());
            ps.executeUpdate();
        }
    }
}
