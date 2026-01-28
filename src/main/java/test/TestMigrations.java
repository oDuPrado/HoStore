package test;

import util.DB;
import util.DatabaseMigration;

/**
 * TestMigrations: Testa se as migrações rodam sem erros
 */
public class TestMigrations {
    public static void main(String[] args) {
        try {
            System.out.println("🔧 Testando migrações de banco de dados...");
            DB.prepararBancoSeNecessario();
            System.out.println("✅ Todas as migrações completadas com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ Erro durante migrações:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
