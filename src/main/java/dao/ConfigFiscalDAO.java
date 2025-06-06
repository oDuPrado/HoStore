package dao;

import model.ConfigFiscalModel;
import util.DB;

import java.sql.*;

/**
 * DAO para inserir, atualizar e buscar configuração fiscal de um cliente.
 * Assumimos que existe **no máximo um registro** por cliente_id.
 */
public class ConfigFiscalDAO {

    /**
     * Busca a configuração fiscal para o cliente especificado (ou null, se não existir).
     */
    public ConfigFiscalModel buscarPorCliente(String clienteId) throws SQLException {
        String sql = "SELECT * FROM config_fiscal WHERE cliente_id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String regime = rs.getString("regime_tributario");
                    String cfop = rs.getString("cfop_padrao");
                    String csosn = rs.getString("csosn_padrao");
                    String origem = rs.getString("origem_padrao");
                    String ncm = rs.getString("ncm_padrao");
                    String unidade = rs.getString("unidade_padrao");

                    return new ConfigFiscalModel(
                        id, clienteId, regime, cfop, csosn, origem, ncm, unidade
                    );
                }
                return null;
            }
        }
    }

    /**
     * Insere nova configuração fiscal para o cliente.
     * Se já existir, prefira usar atualizar().
     */
    public void inserir(ConfigFiscalModel cfg) throws SQLException {
        String sql = "INSERT INTO config_fiscal (" +
                     " id, cliente_id, regime_tributario, cfop_padrao, csosn_padrao, " +
                     " origem_padrao, ncm_padrao, unidade_padrao " +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, cfg.getId());
            ps.setString(idx++, cfg.getClienteId());
            ps.setString(idx++, cfg.getRegimeTributario());
            ps.setString(idx++, cfg.getCfopPadrao());
            ps.setString(idx++, cfg.getCsosnPadrao());
            ps.setString(idx++, cfg.getOrigemPadrao());
            ps.setString(idx++, cfg.getNcmPadrao());
            ps.setString(idx,   cfg.getUnidadePadrao());

            ps.executeUpdate();
        }
    }

    /**
     * Atualiza a configuração fiscal existente (onde cliente_id já existe).
     * Se não existir, nada acontece.
     */
    public void atualizar(ConfigFiscalModel cfg) throws SQLException {
        String sql = "UPDATE config_fiscal SET " +
                     " regime_tributario = ?, " +
                     " cfop_padrao = ?, " +
                     " csosn_padrao = ?, " +
                     " origem_padrao = ?, " +
                     " ncm_padrao = ?, " +
                     " unidade_padrao = ? " +
                     "WHERE cliente_id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, cfg.getRegimeTributario());
            ps.setString(idx++, cfg.getCfopPadrao());
            ps.setString(idx++, cfg.getCsosnPadrao());
            ps.setString(idx++, cfg.getOrigemPadrao());
            ps.setString(idx++, cfg.getNcmPadrao());
            ps.setString(idx++, cfg.getUnidadePadrao());
            ps.setString(idx,   cfg.getClienteId());

            ps.executeUpdate();
        }
    }

    /**
     * Se existir configuração para o cliente, chama atualizar(); senão, chama inserir().
     */
    public void salvar(ConfigFiscalModel cfg) throws SQLException {
        ConfigFiscalModel existente = buscarPorCliente(cfg.getClienteId());
        if (existente == null) {
            inserir(cfg);
        } else {
            // Garante que use o mesmo ID caso queira manter histórico
            cfg = new ConfigFiscalModel(
                existente.getId(),
                cfg.getClienteId(),
                cfg.getRegimeTributario(),
                cfg.getCfopPadrao(),
                cfg.getCsosnPadrao(),
                cfg.getOrigemPadrao(),
                cfg.getNcmPadrao(),
                cfg.getUnidadePadrao()
            );
            atualizar(cfg);
        }
    }
}
