public class TesteConexao {
    public static void main(String[] args) {
        try (java.sql.Connection conn = util.DBPostgres.get()) {
            System.out.println("✅ Conexão com PostgreSQL OK!");
        } catch (Exception e) {
            System.err.println("❌ Falha na conexão:");
            e.printStackTrace();
        }
    }
}
