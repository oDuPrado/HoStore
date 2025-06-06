package dao;

import model.NcmModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por inserir, atualizar e buscar a tabela 'ncm'.
 */
public class NcmDAO {

    /**
     * Sincroniza a lista de NCMs obtida da API com o banco local.
     * Se o código já existir, faz UPDATE; senão, faz INSERT.
     */
    public void sincronizarComApi(List<NcmModel> lista) throws SQLException {
        String sql = "INSERT INTO ncm (codigo, descricao) VALUES (?, ?) " +
                     "ON CONFLICT(codigo) DO UPDATE SET descricao = excluded.descricao";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (NcmModel ncm : lista) {
                ps.setString(1, ncm.getCodigo());
                ps.setString(2, ncm.getDescricao());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    /**
     * Retorna todos os NCMs existentes no banco, ordenados por código.
     */
    public List<NcmModel> buscarTodos() throws SQLException {
        List<NcmModel> lista = new ArrayList<>();
        String sql = "SELECT codigo, descricao FROM ncm ORDER BY codigo";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String descricao = rs.getString("descricao");
                lista.add(new NcmModel(codigo, descricao));
            }
        }
        return lista;
    }
}
