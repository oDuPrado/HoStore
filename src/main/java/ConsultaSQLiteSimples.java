import java.sql.*;
import java.io.File;

public class ConsultaSQLiteSimples {

    // 🔥 Caminho dinâmico igual ao DB.java
    private static final String DATABASE_URL = buildDatabaseUrl();

    private static final String QUERY = "SELECT * FROM sets_jogos";

    public static void main(String[] args) {
        System.out.println("Usando banco em: " + DATABASE_URL);

        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colunas = meta.getColumnCount();

            int linhas = 0;
            while (rs.next()) {
                linhas++;
                for (int i = 1; i <= colunas; i++) {
                    String nomeColuna = meta.getColumnName(i);
                    Object valor = rs.getObject(i);
                    System.out.print(nomeColuna + ": " + valor + " | ");
                }
                System.out.println();
            }

            System.out.println("Total de registros: " + linhas);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String buildDatabaseUrl() {
        String userDir = System.getProperty("user.dir");
        File dbFile = new File(userDir, "data" + File.separator + "hostore.db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }
}
