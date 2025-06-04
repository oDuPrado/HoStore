// src/dao/ConfigLojaDAO.java
package dao;

import model.ConfigLojaModel;
import util.DB;

import java.sql.*;

/**
 * DAO responsável por inserir, atualizar e buscar a configuração única da loja.
 * Assume que existe apenas um registro em "config_loja". Se não existir, buscar() retorna null.
 */
public class ConfigLojaDAO {

    /**
     * Busca a única configuração da loja (ou null, se não existir).
     */
    public ConfigLojaModel buscar() throws SQLException {
        String sql = "SELECT * FROM config_loja LIMIT 1";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                // Lê cada coluna do resultado e monta o ConfigLojaModel
                String id                  = rs.getString("id");
                String nome                = rs.getString("nome");
                String nomeFantasia        = rs.getString("nome_fantasia");
                String cnpj                = rs.getString("cnpj");
                String inscricaoEstadual   = rs.getString("inscricao_estadual");
                String regimeTributario    = rs.getString("regime_tributario");
                String cnae                = rs.getString("cnae");

                String enderecoLogradouro  = rs.getString("endereco_logradouro");
                String enderecoNumero      = rs.getString("endereco_numero");
                String enderecoComplemento = rs.getString("endereco_complemento");
                String enderecoBairro      = rs.getString("endereco_bairro");
                String enderecoMunicipio   = rs.getString("endereco_municipio");
                String enderecoUf          = rs.getString("endereco_uf");
                String enderecoCep         = rs.getString("endereco_cep");

                String telefone            = rs.getString("telefone");
                String email               = rs.getString("email");

                String modeloNota          = rs.getString("modelo_nota");
                String serieNota           = rs.getString("serie_nota");
                int    numeroInicialNota   = rs.getInt("numero_inicial_nota");
                String ambienteNfce        = rs.getString("ambiente_nfce");
                String csc                 = rs.getString("csc");
                String tokenCsc            = rs.getString("token_csc");
                String certificadoPath     = rs.getString("certificado_path");
                String certificadoSenha    = rs.getString("certificado_senha");

                String nomeImpressora      = rs.getString("nome_impressora");
                String textoRodapeNota     = rs.getString("texto_rodape_nota");

                String urlWebServiceNfce   = rs.getString("url_webservice_nfce");
                String proxyHost           = rs.getString("proxy_host");
                int    proxyPort           = rs.getInt("proxy_port");
                String proxyUsuario        = rs.getString("proxy_usuario");
                String proxySenha          = rs.getString("proxy_senha");

                return new ConfigLojaModel(
                        id,
                        nome,
                        nomeFantasia,
                        cnpj,
                        inscricaoEstadual,
                        regimeTributario,
                        cnae,
                        enderecoLogradouro,
                        enderecoNumero,
                        enderecoComplemento,
                        enderecoBairro,
                        enderecoMunicipio,
                        enderecoUf,
                        enderecoCep,
                        telefone,
                        email,
                        modeloNota,
                        serieNota,
                        numeroInicialNota,
                        ambienteNfce,
                        csc,
                        tokenCsc,
                        certificadoPath,
                        certificadoSenha,
                        nomeImpressora,
                        textoRodapeNota,
                        urlWebServiceNfce,
                        proxyHost,
                        proxyPort,
                        proxyUsuario,
                        proxySenha
                );
            }
            return null;
        }
    }

    /**
     * Insere uma nova configuração da loja no banco.
     * Caso já exista um registro, chame atualizar() em vez de inserir().
     */
    public void inserir(ConfigLojaModel cfg) throws SQLException {
        String sql = "INSERT INTO config_loja (" +
                "id, nome, nome_fantasia, cnpj, inscricao_estadual, regime_tributario, cnae, " +
                "endereco_logradouro, endereco_numero, endereco_complemento, endereco_bairro, " +
                "endereco_municipio, endereco_uf, endereco_cep, " +
                "telefone, email, " +
                "modelo_nota, serie_nota, numero_inicial_nota, ambiente_nfce, csc, token_csc, certificado_path, certificado_senha, " +
                "nome_impressora, texto_rodape_nota, url_webservice_nfce, proxy_host, proxy_port, proxy_usuario, proxy_senha" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, cfg.getId());
            ps.setString(idx++, cfg.getNome());
            ps.setString(idx++, cfg.getNomeFantasia());
            ps.setString(idx++, cfg.getCnpj());
            ps.setString(idx++, cfg.getInscricaoEstadual());
            ps.setString(idx++, cfg.getRegimeTributario());
            ps.setString(idx++, cfg.getCnae());

            ps.setString(idx++, cfg.getEnderecoLogradouro());
            ps.setString(idx++, cfg.getEnderecoNumero());
            ps.setString(idx++, cfg.getEnderecoComplemento());
            ps.setString(idx++, cfg.getEnderecoBairro());
            ps.setString(idx++, cfg.getEnderecoMunicipio());
            ps.setString(idx++, cfg.getEnderecoUf());
            ps.setString(idx++, cfg.getEnderecoCep());

            ps.setString(idx++, cfg.getTelefone());
            ps.setString(idx++, cfg.getEmail());

            ps.setString(idx++, cfg.getModeloNota());
            ps.setString(idx++, cfg.getSerieNota());
            ps.setInt(idx++,    cfg.getNumeroInicialNota());
            ps.setString(idx++, cfg.getAmbienteNfce());
            ps.setString(idx++, cfg.getCsc());
            ps.setString(idx++, cfg.getTokenCsc());
            ps.setString(idx++, cfg.getCertificadoPath());
            ps.setString(idx++, cfg.getCertificadoSenha());

            ps.setString(idx++, cfg.getNomeImpressora());
            ps.setString(idx++, cfg.getTextoRodapeNota());

            ps.setString(idx++, cfg.getUrlWebServiceNfce());
            ps.setString(idx++, cfg.getProxyHost());
            ps.setInt(idx++,    cfg.getProxyPort());
            ps.setString(idx++, cfg.getProxyUsuario());
            ps.setString(idx++, cfg.getProxySenha());

            ps.executeUpdate();
        }
    }

    /**
     * Atualiza a configuração existente (baseado no id).
     * Se nenhum registro corresponder, nada ocorre.
     */
    public void atualizar(ConfigLojaModel cfg) throws SQLException {
        String sql = "UPDATE config_loja SET " +
                "nome = ?, " +
                "nome_fantasia = ?, " +
                "cnpj = ?, " +
                "inscricao_estadual = ?, " +
                "regime_tributario = ?, " +
                "cnae = ?, " +
                "endereco_logradouro = ?, " +
                "endereco_numero = ?, " +
                "endereco_complemento = ?, " +
                "endereco_bairro = ?, " +
                "endereco_municipio = ?, " +
                "endereco_uf = ?, " +
                "endereco_cep = ?, " +
                "telefone = ?, " +
                "email = ?, " +
                "modelo_nota = ?, " +
                "serie_nota = ?, " +
                "numero_inicial_nota = ?, " +
                "ambiente_nfce = ?, " +
                "csc = ?, " +
                "token_csc = ?, " +
                "certificado_path = ?, " +
                "certificado_senha = ?, " +
                "nome_impressora = ?, " +
                "texto_rodape_nota = ?, " +
                "url_webservice_nfce = ?, " +
                "proxy_host = ?, " +
                "proxy_port = ?, " +
                "proxy_usuario = ?, " +
                "proxy_senha = ? " +
                "WHERE id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, cfg.getNome());
            ps.setString(idx++, cfg.getNomeFantasia());
            ps.setString(idx++, cfg.getCnpj());
            ps.setString(idx++, cfg.getInscricaoEstadual());
            ps.setString(idx++, cfg.getRegimeTributario());
            ps.setString(idx++, cfg.getCnae());

            ps.setString(idx++, cfg.getEnderecoLogradouro());
            ps.setString(idx++, cfg.getEnderecoNumero());
            ps.setString(idx++, cfg.getEnderecoComplemento());
            ps.setString(idx++, cfg.getEnderecoBairro());
            ps.setString(idx++, cfg.getEnderecoMunicipio());
            ps.setString(idx++, cfg.getEnderecoUf());
            ps.setString(idx++, cfg.getEnderecoCep());

            ps.setString(idx++, cfg.getTelefone());
            ps.setString(idx++, cfg.getEmail());

            ps.setString(idx++, cfg.getModeloNota());
            ps.setString(idx++, cfg.getSerieNota());
            ps.setInt(idx++,    cfg.getNumeroInicialNota());
            ps.setString(idx++, cfg.getAmbienteNfce());
            ps.setString(idx++, cfg.getCsc());
            ps.setString(idx++, cfg.getTokenCsc());
            ps.setString(idx++, cfg.getCertificadoPath());
            ps.setString(idx++, cfg.getCertificadoSenha());

            ps.setString(idx++, cfg.getNomeImpressora());
            ps.setString(idx++, cfg.getTextoRodapeNota());

            ps.setString(idx++, cfg.getUrlWebServiceNfce());
            ps.setString(idx++, cfg.getProxyHost());
            ps.setInt(idx++,    cfg.getProxyPort());
            ps.setString(idx++, cfg.getProxyUsuario());
            ps.setString(idx++, cfg.getProxySenha());

            ps.setString(idx, cfg.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Se não existir configuração, insere; caso contrário, atualiza.
     */
    public void salvar(ConfigLojaModel cfg) throws SQLException {
        ConfigLojaModel existente = buscar();
        if (existente == null) {
            inserir(cfg);
        } else {
            // Garante que o ID permaneça o mesmo
            cfg = new ConfigLojaModel(
                    existente.getId(),
                    cfg.getNome(),
                    cfg.getNomeFantasia(),
                    cfg.getCnpj(),
                    cfg.getInscricaoEstadual(),
                    cfg.getRegimeTributario(),
                    cfg.getCnae(),
                    cfg.getEnderecoLogradouro(),
                    cfg.getEnderecoNumero(),
                    cfg.getEnderecoComplemento(),
                    cfg.getEnderecoBairro(),
                    cfg.getEnderecoMunicipio(),
                    cfg.getEnderecoUf(),
                    cfg.getEnderecoCep(),
                    cfg.getTelefone(),
                    cfg.getEmail(),
                    cfg.getModeloNota(),
                    cfg.getSerieNota(),
                    cfg.getNumeroInicialNota(),
                    cfg.getAmbienteNfce(),
                    cfg.getCsc(),
                    cfg.getTokenCsc(),
                    cfg.getCertificadoPath(),
                    cfg.getCertificadoSenha(),
                    cfg.getNomeImpressora(),
                    cfg.getTextoRodapeNota(),
                    cfg.getUrlWebServiceNfce(),
                    cfg.getProxyHost(),
                    cfg.getProxyPort(),
                    cfg.getProxyUsuario(),
                    cfg.getProxySenha()
            );
            atualizar(cfg);
        }
    }
}
