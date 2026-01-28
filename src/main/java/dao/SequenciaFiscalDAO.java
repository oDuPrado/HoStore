package dao;

import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SequenciaFiscalDAO {

    /**
     * Retorna o proximo numero fiscal para modelo/serie/ambiente.
     * Versao standalone: abre a propria conexao e faz a transacao.
     */
    public int nextNumero(String modelo, int serie, String ambiente) throws SQLException {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            try {
                int numero = nextNumero(c, modelo, serie, ambiente);
                c.commit();
                return numero;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Retorna o proximo numero usando a MESMA conexao/transacao do chamador.
     * Isso evita consumir numero fora da transacao do documento.
     */
    public int nextNumero(Connection conn, String modelo, int serie, String ambiente) throws SQLException {
        String select = "SELECT ultimo_numero FROM sequencias_fiscais WHERE modelo = ? AND serie = ? AND ambiente = ?";
        String update = "UPDATE sequencias_fiscais SET ultimo_numero = ?, alterado_em = datetime('now') WHERE modelo = ? AND serie = ? AND ambiente = ?";

        int ultimoNumero = -1;

        try (PreparedStatement psSelect = conn.prepareStatement(select)) {
            psSelect.setString(1, modelo);
            psSelect.setInt(2, serie);
            psSelect.setString(3, ambiente);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    ultimoNumero = rs.getInt("ultimo_numero");
                } else {
                    throw new SQLException("Sequencia fiscal nao encontrada para modelo=" + modelo + ", serie=" + serie + ", ambiente=" + ambiente);
                }
            }
        }

        int proximoNumero = ultimoNumero + 1;

        try (PreparedStatement psUpdate = conn.prepareStatement(update)) {
            psUpdate.setInt(1, proximoNumero);
            psUpdate.setString(2, modelo);
            psUpdate.setInt(3, serie);
            psUpdate.setString(4, ambiente);
            int affectedRows = psUpdate.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Nao foi possivel atualizar a sequencia fiscal (lock concorrente?).");
            }
        }

        return proximoNumero;
    }
}
