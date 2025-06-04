package dao;

import model.SetModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SetDAO {

    public void sincronizarComApi(List<SetModel> sets) throws Exception {
        try (Connection c = DB.get()) {
            String sql = "INSERT OR IGNORE INTO sets(id, nome, series, colecao_id, data_lancamento) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (SetModel s : sets) {
                    ps.setString(1, s.getId());
                    ps.setString(2, s.getNome());
                    ps.setString(3, s.getSeries());
                    ps.setString(4, s.getColecaoId());
                    ps.setString(5, s.getDataLancamento());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    public List<SetModel> listarTodas() throws Exception {
        List<SetModel> out = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM sets ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new SetModel(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("series"),
                    rs.getString("colecao_id"),
                    rs.getString("data_lancamento")
                ));
            }
        }
        return out;
    }

    public List<SetModel> listarPorSerie(String serie) throws Exception {
        List<SetModel> out = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM sets WHERE series = ? ORDER BY nome")) {
            ps.setString(1, serie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SetModel(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("series"),
                        rs.getString("colecao_id"),
                        rs.getString("data_lancamento")
                    ));
                }
            }
        }
        return out;
    }

    public List<String> listarSeriesUnicas() throws Exception {
        List<String> out = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("SELECT DISTINCT series FROM sets ORDER BY series");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(rs.getString("series"));
            }
        }
        return out;
    }

    public void salvar(List<SetModel> sets) throws Exception {
        try (Connection c = DB.get()) {
            String sql = "INSERT OR IGNORE INTO sets(id, nome, series, colecao_id, data_lancamento) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (SetModel s : sets) {
                    ps.setString(1, s.getId());
                    ps.setString(2, s.getNome());
                    ps.setString(3, s.getSeries());
                    ps.setString(4, s.getColecaoId());
                    ps.setString(5, s.getDataLancamento());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }
}
