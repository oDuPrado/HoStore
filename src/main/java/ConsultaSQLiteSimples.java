import java.sql.*;

public class ConsultaSQLiteSimples {

    // 🛠️ Caminho para seu banco (ajuste se precisar)
    private static final String DATABASE_URL = "jdbc:sqlite:C:/Users/Marco Prado/Documents/APP/ERP/HoStore/data/hostore.db";

    // 🔍 Consulta a tabela sets_jogos ordenando por jogo e nome do set
    private static final String QUERY = "SELECT * FROM taxas_cartao";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY)) {

            // 🔥 Pega automaticamente todas as colunas
            ResultSetMetaData meta = rs.getMetaData();
            int colunas = meta.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= colunas; i++) {
                    String nomeColuna = meta.getColumnName(i);
                    Object valor = rs.getObject(i);
                    System.out.print(nomeColuna + ": " + valor + " | ");
                }
                System.out.println(); // nova linha a cada registro
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
