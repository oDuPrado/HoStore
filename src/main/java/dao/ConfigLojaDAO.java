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
                // Monta o modelo com base nos campos retornados
                String id              = rs.getString("id");
                String nome            = rs.getString("nome");
                String cnpj            = rs.getString("cnpj");
                String telefone        = rs.getString("telefone");
                String socios          = rs.getString("socios");
                String modeloNota      = rs.getString("modelo_nota");
                String serieNota       = rs.getString("serie_nota");
                int    numeroInicial   = rs.getInt("numero_inicial_nota");
                String nomeImpressora  = rs.getString("nome_impressora");
                String textoRodapeNota = rs.getString("texto_rodape_nota");

                return new ConfigLojaModel(
                    id,
                    nome, cnpj, telefone, socios,
                    modeloNota, serieNota, numeroInicial,
                    nomeImpressora, textoRodapeNota
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
                "id, nome, cnpj, telefone, socios, " +
                "modelo_nota, serie_nota, numero_inicial_nota, nome_impressora, texto_rodape_nota" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cfg.getId());
            ps.setString(2, cfg.getNome());
            ps.setString(3, cfg.getCnpj());
            ps.setString(4, cfg.getTelefone());
            ps.setString(5, cfg.getSocios());
            ps.setString(6, cfg.getModeloNota());
            ps.setString(7, cfg.getSerieNota());
            ps.setInt(8, cfg.getNumeroInicialNota());
            ps.setString(9, cfg.getNomeImpressora());
            ps.setString(10, cfg.getTextoRodapeNota());
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
                "cnpj = ?, " +
                "telefone = ?, " +
                "socios = ?, " +
                "modelo_nota = ?, " +
                "serie_nota = ?, " +
                "numero_inicial_nota = ?, " +
                "nome_impressora = ?, " +
                "texto_rodape_nota = ? " +
            "WHERE id = ?";

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cfg.getNome());
            ps.setString(2, cfg.getCnpj());
            ps.setString(3, cfg.getTelefone());
            ps.setString(4, cfg.getSocios());
            ps.setString(5, cfg.getModeloNota());
            ps.setString(6, cfg.getSerieNota());
            ps.setInt(7, cfg.getNumeroInicialNota());
            ps.setString(8, cfg.getNomeImpressora());
            ps.setString(9, cfg.getTextoRodapeNota());
            ps.setString(10, cfg.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Método utilitário: se não existir configuração, insere; caso contrário, atualiza.
     */
    public void salvar(ConfigLojaModel cfg) throws SQLException {
        ConfigLojaModel existente = buscar();
        if (existente == null) {
            inserir(cfg);
        } else {
            // mantém o mesmo ID (sobrescreve demais campos)
            cfg = new ConfigLojaModel(
                existente.getId(),
                cfg.getNome(),
                cfg.getCnpj(),
                cfg.getTelefone(),
                cfg.getSocios(),
                cfg.getModeloNota(),
                cfg.getSerieNota(),
                cfg.getNumeroInicialNota(),
                cfg.getNomeImpressora(),
                cfg.getTextoRodapeNota()
            );
            atualizar(cfg);
        }
    }
}
