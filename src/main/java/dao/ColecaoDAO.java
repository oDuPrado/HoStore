package dao;

import model.ColecaoModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ColecaoDAO {

    /**
     * Sincroniza dados vindos da API com o banco local (sem sobrescrever
     * existentes)
     */
    public void sincronizarComApi(List<ColecaoModel> sets) throws Exception {
        try (Connection c = DB.get()) {
            String sql = """
                        INSERT OR IGNORE INTO colecoes(id, nome, series, data_lancamento, sigla)
                        VALUES (?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (ColecaoModel s : sets) {
                    ps.setString(1, s.getId());
                    ps.setString(2, s.getName());
                    ps.setString(3, s.getSeries());
                    ps.setString(4, s.getReleaseDate());
                    ps.setString(5, s.getSigla()); // ← AQUI
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    /** Verifica se existe pelo menos uma coleção no banco */
    public boolean existeAlgumaColecao() throws Exception {
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM colecoes");
                ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Retorna todas as coleções ordenadas por nome */
    public List<ColecaoModel> listarTodas() throws Exception {
        List<ColecaoModel> out = new ArrayList<>();
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM colecoes ORDER BY nome");
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ColecaoModel m = new ColecaoModel();
                m.setId(rs.getString("id"));
                m.setName(rs.getString("nome"));
                m.setSeries(rs.getString("series"));
                m.setReleaseDate(rs.getString("data_lancamento"));
                m.setSigla(rs.getString("sigla")); // ← ADICIONAR AQUI
                out.add(m);
            }

        }
        return out;
    }

    /** Lista coleções filtrando pela série (set) */
    public List<ColecaoModel> listarPorSerie(String serie) throws Exception {
        List<ColecaoModel> out = new ArrayList<>();
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM colecoes WHERE series = ? ORDER BY nome")) {
            ps.setString(1, serie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColecaoModel m = new ColecaoModel();
                    m.setId(rs.getString("id"));
                    m.setName(rs.getString("nome"));
                    m.setSeries(rs.getString("series"));
                    m.setReleaseDate(rs.getString("data_lancamento"));
                    m.setSigla(rs.getString("sigla")); // ← ADICIONAR AQUI
                    out.add(m);
                }

            }
        }
        return out;
    }

    /** Busca uma coleção pelo ID (retorna null se não encontrar) */
public ColecaoModel buscarPorId(String id) throws Exception {
    String sql = "SELECT * FROM colecoes WHERE id = ?";
    try (Connection c = DB.get();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ColecaoModel m = new ColecaoModel();
                m.setId(rs.getString("id"));
                m.setName(rs.getString("nome"));
                m.setSeries(rs.getString("series"));
                m.setReleaseDate(rs.getString("data_lancamento"));
                m.setSigla(rs.getString("sigla"));
                return m;
            }
            return null;
        }
    }
}

}
