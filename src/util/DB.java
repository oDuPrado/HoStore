package util;

import java.sql.*;

public class DB {

    private static final String URL = "jdbc:sqlite:data/hostore.db";

    static { init(); }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /* cria tabelas se não existirem */
    private static void init() {
            try (Connection c = get(); Statement st = c.createStatement()) {
        
                st.execute(
                    "CREATE TABLE IF NOT EXISTS clientes (" +
                    "id TEXT PRIMARY KEY," +
                    "nome TEXT NOT NULL," +
                    "telefone TEXT," +
                    "cpf TEXT," +
                    "data_nasc TEXT," +
                    "tipo TEXT," +
                    "endereco TEXT," +
                    "cidade TEXT," +
                    "estado TEXT," +
                    "observacoes TEXT," +
                    "criado_em TEXT," +
                    "criado_por TEXT," +
                    "alterado_em TEXT," +
                    "alterado_por TEXT" +
                    ")"
                );
        
                st.execute(
                    "CREATE TABLE IF NOT EXISTS vendas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "data_venda TEXT," +
                    "cliente_id TEXT," +
                    "total REAL," +
                    "FOREIGN KEY (cliente_id) REFERENCES clientes(id)" +
                    ")"
                );
        
                st.execute(
                    "CREATE TABLE IF NOT EXISTS vendas_itens (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "venda_id INTEGER," +
                    "carta_id TEXT," +
                    "qtd INTEGER," +
                    "preco REAL," +
                    "FOREIGN KEY (venda_id) REFERENCES vendas(id)" +
                    ")"
                );
        
                st.execute(
                    "CREATE TABLE IF NOT EXISTS cartas (" +
                    "id TEXT PRIMARY KEY," +
                    "nome TEXT," +
                    "colecao TEXT," +
                    "numero TEXT," +
                    "qtd INTEGER," +
                    "preco REAL" +
                    ")"
                );
        
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    
}
