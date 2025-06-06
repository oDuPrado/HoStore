package dao;

import model.CsosnModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por inserir, atualizar e buscar a tabela 'csosn'.
 */
public class CsosnDAO {

    /**
     * Sincroniza a lista de CSOSNs obtida da API com o banco local.
     * Se o código já existir, faz UPDATE; senão, faz INSERT.
     */
    public void sincronizarComApi(List<CsosnModel> lista) throws SQLException {
        String sql = "INSERT INTO csosn (codigo, descricao) VALUES (?, ?) " +
                     "ON CONFLICT(codigo) DO UPDATE SET descricao = excluded.descricao";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (CsosnModel csosn : lista) {
                ps.setString(1, csosn.getCodigo());
                ps.setString(2, csosn.getDescricao());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        }
    }

    /**
     * Retorna todos os CSOSNs existentes no banco, ordenados por código.
     */
    public List<CsosnModel> buscarTodos() throws SQLException {
        List<CsosnModel> lista = new ArrayList<>();
        String sql = "SELECT codigo, descricao FROM csosn ORDER BY codigo";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String descricao = rs.getString("descricao");
                lista.add(new CsosnModel(codigo, descricao));
            }
        }
        return lista;
    }
}
