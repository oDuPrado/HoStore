package dao;

import util.DB;

import java.sql.*;

public class SequenciaFiscalDAO {

    public int nextNumero(Connection conn, String modelo, int codigoModelo, int serie, String ambiente) throws SQLException {
        // id determinístico
        String id = modelo + "-" + codigoModelo + "-SERIE-" + serie + "-" + ambiente;

        // garante linha
        try (PreparedStatement ins = conn.prepareStatement("""
            INSERT OR IGNORE INTO sequencias_fiscais
            (id, modelo, codigo_modelo, serie, ambiente, ultimo_numero, criado_em)
            VALUES (?, ?, ?, ?, ?, 0, datetime('now'))
        """)) {
            ins.setString(1, id);
            ins.setString(2, modelo);
            ins.setInt(3, codigoModelo);
            ins.setInt(4, serie);
            ins.setString(5, ambiente);
            ins.executeUpdate();
        }

        // lê ultimo_numero
        int ultimo;
        try (PreparedStatement sel = conn.prepareStatement("""
            SELECT ultimo_numero FROM sequencias_fiscais
            WHERE id = ?
        """)) {
            sel.setString(1, id);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) throw new SQLException("Sequência fiscal não encontrada: " + id);
                ultimo = rs.getInt(1);
            }
        }

        int proximo = ultimo + 1;

        // atualiza
        try (PreparedStatement upd = conn.prepareStatement("""
            UPDATE sequencias_fiscais
            SET ultimo_numero = ?, alterado_em = datetime('now')
            WHERE id = ?
        """)) {
            upd.setInt(1, proximo);
            upd.setString(2, id);
            upd.executeUpdate();
        }

        return proximo;
    }

    // Conveniência se você não quiser passar Connection (menos ideal)
    public int nextNumero(String modelo, int codigoModelo, int serie, String ambiente) throws SQLException {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);
            try {
                int n = nextNumero(conn, modelo, codigoModelo, serie, ambiente);
                conn.commit();
                return n;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
