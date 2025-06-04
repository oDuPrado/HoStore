package dao;

import util.DB;

import java.sql.*;
import java.util.*;

public class CadastroGenericoDAO {

    private final String tabela;
    private final String[] campos;

    public CadastroGenericoDAO(String tabela, String... campos) {
        this.tabela = tabela;
        this.campos = campos;
    }

    public List<Map<String, String>> listar() throws Exception {
        List<Map<String, String>> out = new ArrayList<>();
        String sql = "SELECT * FROM " + tabela + " ORDER BY " + campos[0];
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> linha = new LinkedHashMap<>();
                for (String campo : campos) {
                    linha.put(campo, rs.getString(campo));
                }
                out.add(linha);
            }
        }
        return out;
    }

    public void inserir(Map<String, String> dados) throws Exception {
        String sql = "INSERT INTO " + tabela + " (" + String.join(", ", campos) + ") VALUES (" +
                String.join(", ", Collections.nCopies(campos.length, "?")) + ")";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < campos.length; i++) {
                ps.setString(i + 1, dados.get(campos[i]));
            }
            ps.execute();
        }
    }

    public void atualizar(String id, Map<String, String> dados) throws Exception {
        StringBuilder sql = new StringBuilder("UPDATE " + tabela + " SET ");
        for (int i = 1; i < campos.length; i++) {
            sql.append(campos[i]).append(" = ?");
            if (i < campos.length - 1) sql.append(", ");
        }
        sql.append(" WHERE ").append(campos[0]).append(" = ?");
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 1; i < campos.length; i++) {
                ps.setString(i, dados.get(campos[i]));
            }
            ps.setString(campos.length, id);
            ps.execute();
        }
    }

    public void excluir(String id) throws Exception {
        String sql = "DELETE FROM " + tabela + " WHERE " + campos[0] + " = ?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.execute();
        }
    }
}
