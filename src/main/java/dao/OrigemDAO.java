package dao;

import model.OrigemModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por inserir, atualizar e buscar a tabela 'origem'.
 */
public class OrigemDAO {

    /**
     * Sincroniza a lista de Origens obtida da API com o banco local.
     * Se o código já existir, faz UPDATE; senão, faz INSERT.
     */
    public void sincronizarComApi(List<OrigemModel> lista) throws SQLException {
        String sql = "INSERT INTO origem (codigo, descricao) VALUES (?, ?) " +
                     "ON CONFLICT(codigo) DO UPDATE SET descricao = excluded.descricao";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (OrigemModel origem : lista) {
                ps.setString(1, origem.getCodigo());
                ps.setString(2, origem.getDescricao());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    /**
     * Retorna todas as origens existentes no banco, ordenadas por código.
     */
    public List<OrigemModel> buscarTodos() throws SQLException {
        List<OrigemModel> lista = new ArrayList<>();
        String sql = "SELECT codigo, descricao FROM origem ORDER BY codigo";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String descricao = rs.getString("descricao");
                lista.add(new OrigemModel(codigo, descricao));
            }
        }
        return lista;
    }
}
