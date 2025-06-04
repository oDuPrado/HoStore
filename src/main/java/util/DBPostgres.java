package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBPostgres {

    private static final String URL = "jdbc:postgresql://localhost:5432/hostore";
    private static final String USER = "postgres"; // ou "hostore_admin" se tiver criado outro
    private static final String PASSWORD = "110300"; // ou a senha que você colocou

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
