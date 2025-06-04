// Procure em: src/dao/JogoDAO.java
package dao;

import model.JogoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por todas as operações CRUD na tabela 'jogos'.
 * Usado para listar, buscar, inserir, atualizar e remover jogos.
 */
public class JogoDAO {

    /**
     * Busca e retorna todos os jogos cadastrados na tabela 'jogos',
     * ordenados por nome.
     * 
     * @return List<JogoModel> com todos os jogos; lista vazia se nenhum existir.
     * @throws SQLException se houver erro de acesso ao banco.
     */
    public List<JogoModel> listarTodos() throws SQLException {
        List<JogoModel> lista = new ArrayList<>();
        // SQL para selecionar id e nome da tabela
        String sql = "SELECT id, nome FROM jogos ORDER BY nome";

        // Tenta abrir conexão e executar consulta
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Para cada linha retornada, cria um JogoModel e adiciona à lista
            while (rs.next()) {
                JogoModel jogo = new JogoModel();
                jogo.setId(rs.getString("id"));
                jogo.setNome(rs.getString("nome"));
                lista.add(jogo);
            }
        }
        return lista;
    }

    /**
     * Busca um jogo pelo seu identificador único.
     * 
     * @param id Identificador do jogo (PRIMARY KEY).
     * @return JogoModel correspondente; null se não encontrado.
     * @throws SQLException se houver erro de acesso ao banco.
     */
    public JogoModel buscarPorId(String id) throws SQLException {
        String sql = "SELECT id, nome FROM jogos WHERE id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Se achou, popula um model e retorna
                    JogoModel jogo = new JogoModel();
                    jogo.setId(rs.getString("id"));
                    jogo.setNome(rs.getString("nome"));
                    return jogo;
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Insere um novo jogo na tabela 'jogos'.
     * Deve ser usado quando se quer cadastrar um jogo novo via interface.
     * 
     * @param jogo Objeto JogoModel contendo id e nome.
     * @throws SQLException se houver violação de UNIQUE ou outro erro.
     */
    public void inserir(JogoModel jogo) throws SQLException {
        String sql = "INSERT INTO jogos (id, nome) VALUES (?, ?)";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jogo.getId());
            ps.setString(2, jogo.getNome());
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza dados de um jogo existente (atualiza apenas o nome).
     * 
     * @param jogo JogoModel com id já existente e novo nome.
     * @throws SQLException se houver erro de acesso ao banco.
     */
    public void atualizar(JogoModel jogo) throws SQLException {
        String sql = "UPDATE jogos SET nome = ? WHERE id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jogo.getNome());
            ps.setString(2, jogo.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Remove um jogo pelo identificador. Cuidado: pode quebrar integridade se houver
     * produtos vinculados a esse jogo.
     * 
     * @param id Identificador do jogo a ser removido.
     * @throws SQLException se houver chave estrangeira referenciando esse jogo.
     */
    public void remover(String id) throws SQLException {
        String sql = "DELETE FROM jogos WHERE id = ?";
        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}
