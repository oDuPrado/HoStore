package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBPostgres {

    // ⚠️ CRÍTICO: Credenciais devem ser externalizadas via variáveis de ambiente ou properties
    private static final String URL = System.getenv("HOSTORE_DB_URL") != null
            ? System.getenv("HOSTORE_DB_URL")
            : "jdbc:postgresql://localhost:5432/hostore";
    private static final String USER = System.getenv("HOSTORE_DB_USER") != null
            ? System.getenv("HOSTORE_DB_USER")
            : "postgres";
    private static final String PASSWORD = System.getenv("HOSTORE_DB_PASSWORD") != null
            ? System.getenv("HOSTORE_DB_PASSWORD")
            : "";

    static {
        try {
            Class.forName("org.postgresql.Driver"); // força carregamento do driver JDBC
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("⚠️ Driver PostgreSQL não encontrado!", e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
