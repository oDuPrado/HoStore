package dao;

import model.SetJogoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável por manipular os dados da tabela sets_jogos,
 * que armazena os sets dos jogos TCG exceto Pokémon.
 */
public class SetJogoDAO {

    /**
     * Insere ou atualiza os sets informados na tabela sets_jogos.
     *
     * @param sets lista de SetJogoModel vindos de API externa.
     */
    public void sincronizarComApi(List<SetJogoModel> sets) {
        String sql = """
                    INSERT INTO sets_jogos (set_id, jogo_id, nome, data_lancamento, qtd_cartas, codigo_externo)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(set_id, jogo_id) DO UPDATE SET
                        nome = excluded.nome,
                        data_lancamento = excluded.data_lancamento,
                        qtd_cartas = excluded.qtd_cartas,
                        codigo_externo = excluded.codigo_externo
                """;

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (SetJogoModel set : sets) {
                ps.setString(1, set.getSetId());
                ps.setString(2, set.getJogoId()); // corrigido aqui 👈
                ps.setString(3, set.getNome());
                ps.setString(4, set.getDataLancamento());
                ps.setObject(5, set.getQtdCartas());
                ps.setString(6, set.getCodigoExterno());
                ps.addBatch();
            }

            ps.executeBatch();
            System.out.printf("✅ %d sets sincronizados com sucesso para jogo: %s%n", sets.size(),
                    sets.isEmpty() ? "-" : sets.get(0).getJogoId());

        } catch (SQLException e) {
            System.err.println("Erro ao sincronizar sets_jogos:");
            e.printStackTrace();
        }
    }

    /**
     * Retorna os sets associados a um jogo específico (ex: YUGIOH).
     *
     * @param jogoId ID do jogo (ex: "YUGIOH", "MAGIC")
     * @return lista de SetJogoModel
     */
    public List<SetJogoModel> listarPorJogo(String jogoId) {
        List<SetJogoModel> sets = new ArrayList<>();

        String sql = "SELECT * FROM sets_jogos WHERE jogo_id = ? ORDER BY data_lancamento DESC";

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jogoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SetJogoModel set = new SetJogoModel();
                    set.setSetId(rs.getString("set_id"));
                    set.setJogoId(rs.getString("jogo_id"));
                    set.setNome(rs.getString("nome"));
                    set.setDataLancamento(rs.getString("data_lancamento"));
                    set.setQtdCartas(rs.getInt("qtd_cartas"));
                    set.setCodigoExterno(rs.getString("codigo_externo"));
                    sets.add(set);
                }
            }

        } catch (Exception e) {
            System.err.println("Erro ao buscar sets por jogo:");
            e.printStackTrace();
        }

        return sets;
    }
}
