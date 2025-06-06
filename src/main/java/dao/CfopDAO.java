package dao;

import model.CfopModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por inserir, atualizar e buscar a tabela 'cfop'.
 */
public class CfopDAO {

    /**
     * Sincroniza a lista de CFOPs obtida da API com o banco local.
     * Se o código já existir, faz UPDATE; senão, faz INSERT.
     */
    public void sincronizarComApi(List<CfopModel> lista) throws SQLException {
        String sql = "INSERT INTO cfop (codigo, descricao) VALUES (?, ?) " +
                     "ON CONFLICT(codigo) DO UPDATE SET descricao = excluded.descricao";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (CfopModel cfop : lista) {
                ps.setString(1, cfop.getCodigo());
                ps.setString(2, cfop.getDescricao());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    /**
     * Retorna todos os CFOPs existentes no banco, ordenados por código.
     */
    public List<CfopModel> buscarTodos() throws SQLException {
        List<CfopModel> lista = new ArrayList<>();
        String sql = "SELECT codigo, descricao FROM cfop ORDER BY codigo";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String descricao = rs.getString("descricao");
                lista.add(new CfopModel(codigo, descricao));
            }
        }
        return lista;
    }
}
