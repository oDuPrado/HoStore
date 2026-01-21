package util;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import javax.swing.JOptionPane;

import dao.SetDAO;
import dao.ColecaoDAO;
import dao.SetJogoDAO;

import model.ColecaoModel;
import model.SetModel;

/**
 * DB: inicialização robusta do SQLite + sync tolerante a falhas.
 *
 * Regra de ouro:
 * - Inicialização do app NÃO DEPENDE DE INTERNET.
 * - Sync remoto é opcional e sempre tem fallback por cache (data/cache).
 */
public class DB {

        private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("hostore.debug", "false"));

        private static final String DATA_DIR = System.getProperty("user.dir") + File.separator + "data";
        private static final String DB_PATH = DATA_DIR + File.separator + "hostore.db";
        private static final String URL = "jdbc:sqlite:" + DB_PATH;

        // Estado de sincronização
        private static final Path CACHE_DIR = Paths.get(System.getProperty("user.dir"), "data", "cache");
        private static final Path SYNC_STATE_FILE = CACHE_DIR.resolve("sync_state.properties");

        // Janela mínima entre syncs (7 dias)
        private static final long SYNC_MIN_INTERVAL_SECONDS = 7L * 24L * 60L * 60L;

        // Cache files (padronizados)
        private static final Path POKEMON_SETS_CACHE = CACHE_DIR.resolve("pokemontcg_sets.json");
        private static final Path YGO_SETS_CACHE = CACHE_DIR.resolve("yugioh_sets.json");
        private static final Path MAGIC_SETS_CACHE = CACHE_DIR.resolve("magic_sets.json");
        private static final Path DIGIMON_SETS_CACHE = CACHE_DIR.resolve("digimon_sets.json");
        private static final Path ONEPIECE_SETS_CACHE = CACHE_DIR.resolve("onepiece_sets.json");

        public static Connection get() throws SQLException {
                Connection conn = DriverManager.getConnection(URL);
                configureConnection(conn);
                return conn;
        }

        private static void configureConnection(Connection conn) {
                try (Statement st = conn.createStatement()) {
                        st.execute("PRAGMA foreign_keys=ON");
                        st.execute("PRAGMA busy_timeout=5000");
                        st.execute("PRAGMA journal_mode=WAL");
                        st.execute("PRAGMA synchronous=NORMAL");
                } catch (Exception e) {
                        logWarn("Falha ao configurar SQLite PRAGMAs: " + e.getMessage(), e);
                }
        }

        public static void prepararBancoSeNecessario() {
                showUserFeedback("Inicializando", "Verificando banco de dados...", false);

                try {
                        ensureDataDirectoryExists();
                        ensureCacheDirectoryExists();

                        boolean existedBefore = databaseFileExists();
                        if (!existedBefore) {
                                System.out.println("Banco de dados não encontrado. Criando novo banco e tabelas...");
                        } else {
                                System.out.println("Banco de dados encontrado. Verificando/atualizando tabelas...");
                        }

                        // 1) Schema + seeds (sem internet)
                        try (Connection conn = get()) {
                                conn.setAutoCommit(false);

                                initSchema(conn);
                                seedBaseData(conn);
                                ensureAdminUser(conn);

                                conn.commit();
                        }

                        // 2) Sync opcional (NUNCA quebra inicialização)
                        if (shouldSyncRemoteData(existedBefore)) {
                                System.out.println(
                                                "🔄 Sincronização de APIs habilitada (first-run ou janela expirada).");
                                syncRemoteDataSafely();
                                updateLastSyncNow();
                        } else {
                                System.out.println("⏭️ Sincronização de APIs ignorada (janela de sync ainda válida).");
                        }

                        if (!GraphicsEnvironment.isHeadless()) {
                                if (!existedBefore) {
                                        showUserFeedback("Instalação concluída",
                                                        "Banco de dados criado com sucesso e pronto para uso.",
                                                        false);
                                } else {
                                        showUserFeedback("Verificação concluída",
                                                        "Banco de dados verificado/atualizado com sucesso.",
                                                        false);
                                }
                        }

                } catch (Exception e) {
                        String erro = "Falha ao preparar o banco de dados: " + e.getMessage();
                        System.err.println(erro);
                        logError("Erro fatal na preparação do banco", e);

                        if (!GraphicsEnvironment.isHeadless()) {
                                showUserFeedback("Erro de instalação", erro, true);
                        }
                }
        }

        private static void ensureDataDirectoryExists() throws SQLException {
                File dir = new File(DATA_DIR);
                if (!dir.exists()) {
                        if (dir.mkdirs()) {
                                System.out.println("Diretório de dados criado em: " + dir.getAbsolutePath());
                        } else {
                                throw new SQLException("Não foi possível criar o diretório de dados: "
                                                + dir.getAbsolutePath());
                        }
                }
        }

        private static void ensureCacheDirectoryExists() throws SQLException {
                try {
                        Files.createDirectories(CACHE_DIR);
                } catch (Exception e) {
                        throw new SQLException("Não foi possível criar diretório de cache: "
                                        + CACHE_DIR.toAbsolutePath() + " | "
                                        + e.getMessage());
                }
        }

        private static boolean databaseFileExists() {
                return new File(DB_PATH).exists();
        }

        // ------------------------------- SCHEMA --------------------------------

        /**
         * Cria/atualiza tabelas (somente DDL). Mantém os mesmos campos para não quebrar
         * o sistema.
         */
        private static void initSchema(Connection c) throws SQLException {
                try (Statement st = c.createStatement()) {

                        // ========== TABELAS "BASE" ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS clientes (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "telefone TEXT, " +
                                                        "cpf TEXT, " +
                                                        "data_nasc TEXT, " +
                                                        "tipo TEXT, " +
                                                        "endereco TEXT, " +
                                                        "cidade TEXT, " +
                                                        "estado TEXT, " +
                                                        "observacoes TEXT, " +
                                                        "criado_em TEXT, " +
                                                        "criado_por TEXT, " +
                                                        "alterado_em TEXT, " +
                                                        "alterado_por TEXT" +
                                                        ")",
                                        "clientes");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS fornecedores (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "telefone TEXT, " +
                                                        "email TEXT, " +
                                                        "cnpj TEXT, " +
                                                        "contato TEXT, " +
                                                        "endereco TEXT, " +
                                                        "cidade TEXT, " +
                                                        "estado TEXT, " +
                                                        "observacoes TEXT, " +
                                                        "pagamento_tipo TEXT, " +
                                                        "prazo INTEGER, " +
                                                        "criado_em TEXT, " +
                                                        "alterado_em TEXT" +
                                                        ")",
                                        "fornecedores");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS usuarios (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "usuario TEXT NOT NULL UNIQUE, " +
                                                        "senha TEXT NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "ativo INTEGER NOT NULL DEFAULT 1" +
                                                        ")",
                                        "usuarios");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS logs_acessos(" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "usuario_id TEXT, " +
                                                        "data TEXT, " +
                                                        "tipo TEXT, " +
                                                        "descricao TEXT, " +
                                                        "FOREIGN KEY(usuario_id) REFERENCES usuarios(id)" +
                                                        ")",
                                        "logs_acessos");

                        // ========== FISCAL ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS ncm (" +
                                                        "codigo TEXT PRIMARY KEY, " +
                                                        "descricao TEXT NOT NULL" +
                                                        ");",
                                        "ncm");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS cfop (" +
                                                        "codigo TEXT PRIMARY KEY, " +
                                                        "descricao TEXT NOT NULL" +
                                                        ");",
                                        "cfop");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS csosn (" +
                                                        "codigo TEXT PRIMARY KEY, " +
                                                        "descricao TEXT NOT NULL" +
                                                        ");",
                                        "csosn");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS origem (" +
                                                        "codigo TEXT PRIMARY KEY, " +
                                                        "descricao TEXT NOT NULL" +
                                                        ");",
                                        "origem");

                        // unidades
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS unidades (" +
                                                        "codigo TEXT PRIMARY KEY, " +
                                                        "descricao TEXT NOT NULL" +
                                                        ")",
                                        "unidades");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS config_fiscal (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "cliente_id TEXT UNIQUE, " +
                                                        "regime_tributario TEXT, " +
                                                        "cfop_padrao TEXT, " +
                                                        "csosn_padrao TEXT, " +
                                                        "origem_padrao TEXT, " +
                                                        "ncm_padrao TEXT, " +
                                                        "unidade_padrao TEXT, " +
                                                        "FOREIGN KEY(cliente_id) REFERENCES clientes(id), " +
                                                        "FOREIGN KEY(cfop_padrao) REFERENCES cfop(codigo), " +
                                                        "FOREIGN KEY(csosn_padrao) REFERENCES csosn(codigo), " +
                                                        "FOREIGN KEY(origem_padrao) REFERENCES origem(codigo), " +
                                                        "FOREIGN KEY(ncm_padrao) REFERENCES ncm(codigo), " +
                                                        "FOREIGN KEY(unidade_padrao) REFERENCES unidades(codigo) " +
                                                        ");",
                                        "config_fiscal");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS config_fiscal_default (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "regime_tributario TEXT, " +
                                                        "cfop_padrao TEXT, " +
                                                        "csosn_padrao TEXT, " +
                                                        "origem_padrao TEXT, " +
                                                        "ncm_padrao TEXT, " +
                                                        "unidade_padrao TEXT, " +
                                                        "FOREIGN KEY(cfop_padrao) REFERENCES cfop(codigo), " +
                                                        "FOREIGN KEY(csosn_padrao) REFERENCES csosn(codigo), " +
                                                        "FOREIGN KEY(origem_padrao) REFERENCES origem(codigo), " +
                                                        "FOREIGN KEY(ncm_padrao) REFERENCES ncm(codigo), " +
                                                        "FOREIGN KEY(unidade_padrao) REFERENCES unidades(codigo) " +
                                                        ");",
                                        "config_fiscal_default");

                        // ========== TRILHO FISCAL (DOCUMENTOS) ==========
                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS sequencias_fiscais (
                                              id TEXT PRIMARY KEY,
                                              modelo TEXT NOT NULL,
                                              codigo_modelo INTEGER NOT NULL,
                                              serie INTEGER NOT NULL,
                                              ambiente TEXT NOT NULL,
                                              ultimo_numero INTEGER NOT NULL DEFAULT 0,
                                              criado_em TEXT NOT NULL,
                                              alterado_em TEXT
                                            )
                                        """, "sequencias_fiscais");

                        executeComLog(st, """
                                            CREATE UNIQUE INDEX IF NOT EXISTS ux_seq_fiscal
                                            ON sequencias_fiscais (modelo, codigo_modelo, serie, ambiente)
                                        """, "ux_seq_fiscal");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS documentos_fiscais (
                                              id TEXT PRIMARY KEY,
                                              venda_id INTEGER NOT NULL,
                                              modelo TEXT NOT NULL,
                                              codigo_modelo INTEGER NOT NULL,
                                              serie INTEGER NOT NULL,
                                              numero INTEGER NOT NULL,
                                              ambiente TEXT NOT NULL,
                                              status TEXT NOT NULL,

                                              chave_acesso TEXT,
                                              protocolo TEXT,
                                              recibo TEXT,
                                              xml TEXT,
                                              erro TEXT,

                                              total_produtos REAL,
                                              total_desconto REAL,
                                              total_acrescimo REAL,
                                              total_final REAL,

                                              criado_em TEXT NOT NULL,
                                              criado_por TEXT,
                                              atualizado_em TEXT,
                                              cancelado_em TEXT,
                                              cancelado_por TEXT,

                                              FOREIGN KEY (venda_id) REFERENCES vendas(id)
                                            )
                                        """, "documentos_fiscais");

                        executeComLog(st, """
                                            CREATE UNIQUE INDEX IF NOT EXISTS ux_doc_fiscal_unico
                                            ON documentos_fiscais (modelo, codigo_modelo, serie, numero, ambiente)
                                        """, "ux_doc_fiscal_unico");

                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_doc_fiscal_venda ON documentos_fiscais(venda_id)",
                                        "idx_doc_fiscal_venda");

                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_doc_fiscal_status ON documentos_fiscais(status)",
                                        "idx_doc_fiscal_status");

                        executeComLog(st,
                                        """
                                                            CREATE TABLE IF NOT EXISTS documentos_fiscais_itens (
                                                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                              documento_id TEXT NOT NULL,
                                                              venda_item_id INTEGER,
                                                              produto_id TEXT,
                                                              descricao TEXT NOT NULL,

                                                              ncm TEXT NOT NULL,
                                                              cfop TEXT NOT NULL,
                                                              csosn TEXT NOT NULL,
                                                              origem TEXT NOT NULL,
                                                              unidade TEXT NOT NULL,

                                                              quantidade INTEGER NOT NULL,
                                                              valor_unit REAL NOT NULL,
                                                              desconto REAL NOT NULL DEFAULT 0,
                                                              acrescimo REAL NOT NULL DEFAULT 0,
                                                              total_item REAL NOT NULL,
                                                              observacoes TEXT,

                                                              FOREIGN KEY (documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
                                                            )
                                                        """,
                                        "documentos_fiscais_itens");

                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_doc_fiscal_itens_doc ON documentos_fiscais_itens(documento_id)",
                                        "idx_doc_fiscal_itens_doc");

                        executeComLog(st,
                                        """
                                                            CREATE TABLE IF NOT EXISTS documentos_fiscais_pagamentos (
                                                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                              documento_id TEXT NOT NULL,
                                                              tipo TEXT NOT NULL,
                                                              valor REAL NOT NULL,
                                                              FOREIGN KEY (documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
                                                            )
                                                        """,
                                        "documentos_fiscais_pagamentos");

                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_doc_fiscal_pag_doc ON documentos_fiscais_pagamentos(documento_id)",
                                        "idx_doc_fiscal_pag_doc");

                        // ========== PRODUTOS / ESTOQUE ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS produtos (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "jogo_id TEXT, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "quantidade INTEGER NOT NULL DEFAULT 0, " +
                                                        "preco_compra REAL, " +
                                                        "preco_venda REAL, " +
                                                        "codigo_barras TEXT, " +

                                                        // ===== FISCAL (mínimo para NFC-e) =====
                                                        "ncm TEXT, " + // ex: 95049090 etc
                                                        "cfop TEXT, " + // ex: 5102 etc
                                                        "csosn TEXT, " + // ex: 102, 500 etc (Simples Nacional)
                                                        "origem TEXT, " + // ex: 0,1,2...
                                                        "unidade TEXT, " + // ex: UN, KG, CX...

                                                        "lucro REAL GENERATED ALWAYS AS (preco_venda - preco_compra) VIRTUAL, "
                                                        +
                                                        "criado_em TEXT, " +
                                                        "alterado_em TEXT, " +
                                                        "fornecedor_id TEXT, " +

                                                        // ✅ SOFT DELETE
                                                        "ativo INTEGER NOT NULL DEFAULT 1, " +
                                                        "inativado_em TEXT, " +
                                                        "inativado_por TEXT, " +
                                                        "FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id), " +
                                                        "FOREIGN KEY (ncm) REFERENCES ncm(codigo), " +
                                                        "FOREIGN KEY (cfop) REFERENCES cfop(codigo), " +
                                                        "FOREIGN KEY (csosn) REFERENCES csosn(codigo), " +
                                                        "FOREIGN KEY (unidade) REFERENCES unidades(codigo), " +
                                                        "FOREIGN KEY (origem) REFERENCES origem(codigo) " +
                                                        ")",
                                        "produtos");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS boosters (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "jogo_id TEXT," +
                                                        "serie TEXT, " +
                                                        "colecao TEXT, " +
                                                        "tipo TEXT, " +
                                                        "idioma TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "quantidade INTEGER, " +
                                                        "custo REAL, " +
                                                        "preco_venda REAL, " +
                                                        "fornecedor_id TEXT, " +
                                                        "data_lancamento TEXT, " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id), " +
                                                        "FOREIGN KEY(jogo_id) REFERENCES jogos(id)" +
                                                        ")",
                                        "boosters");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS decks (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor TEXT, " +
                                                        "colecao TEXT, " +
                                                        "jogo_id TEXT, " +
                                                        "tipo_deck TEXT, " +
                                                        "categoria TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "FOREIGN KEY(id) REFERENCES produtos(id) ON DELETE CASCADE, " +
                                                        "FOREIGN KEY(jogo_id) REFERENCES jogos(id)" +
                                                        ")",
                                        "decks");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS etbs (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor TEXT, " +
                                                        "jogo_id TEXT, " +
                                                        "serie TEXT, " +
                                                        "colecao TEXT, " +
                                                        "tipo TEXT, " +
                                                        "versao TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "FOREIGN KEY(id) REFERENCES produtos(id) ON DELETE CASCADE, " +
                                                        "FOREIGN KEY(jogo_id) REFERENCES jogos(id)" +
                                                        ")",
                                        "etbs");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS acessorios (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "arte TEXT, " +
                                                        "cor TEXT, " +
                                                        "quantidade INTEGER NOT NULL, " +
                                                        "custo REAL, " +
                                                        "preco_venda REAL, " +
                                                        "fornecedor_id TEXT, " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")",
                                        "acessorios");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS produtos_alimenticios (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "categoria TEXT, " +
                                                        "subtipo TEXT, " +
                                                        "marca TEXT, " +
                                                        "sabor TEXT, " +
                                                        "lote TEXT, " +
                                                        "peso REAL, " +
                                                        "unidade_peso TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "data_validade TEXT, " +
                                                        "quantidade INTEGER, " +
                                                        "preco_compra REAL, " +
                                                        "preco_venda REAL, " +
                                                        "fornecedor_id TEXT, " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")",
                                        "produtos_alimenticios");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS estoque_lotes(" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "produto_id TEXT NOT NULL, " +
                                                        "fornecedor_id TEXT, " +
                                                        "codigo_lote TEXT, " +
                                                        "data_entrada TEXT NOT NULL, " +
                                                        "validade TEXT, " +
                                                        "custo_unit REAL, " +
                                                        "preco_venda_unit REAL, " +
                                                        "qtd_inicial INTEGER NOT NULL, " +
                                                        "qtd_disponivel INTEGER NOT NULL, " +
                                                        "status TEXT DEFAULT 'ativo', " +
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id), " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")",
                                        "estoque_lotes");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS estoque_movimentacoes(" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "produto_id TEXT, " +
                                                        "lote_id INTEGER, " +
                                                        "tipo_mov TEXT," +
                                                        "quantidade INTEGER, " +
                                                        "motivo TEXT, " +
                                                        "data TEXT, " +
                                                        "usuario TEXT," +
                                                        "evento_id TEXT," +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id), " +
                                                        "FOREIGN KEY(lote_id) REFERENCES estoque_lotes(id)" +
                                                        ")",
                                        "estoque_movimentacoes");

                        ensureColumn(c, "estoque_movimentacoes", "lote_id", "INTEGER");
                        ensureColumn(c, "estoque_movimentacoes", "evento_id", "TEXT");

                        // ========== VENDAS ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "cliente_id TEXT NOT NULL, " +
                                                        "data_venda TEXT NOT NULL, " +
                                                        "forma_pagamento TEXT NOT NULL, " +
                                                        "parcelas INTEGER DEFAULT 1, " +
                                                        "desconto REAL DEFAULT 0, " +
                                                        "acrescimo REAL DEFAULT 0, " +
                                                        "total_bruto REAL NOT NULL, " +
                                                        "total_liquido REAL NOT NULL, " +
                                                        "status TEXT DEFAULT 'fechada', " +
                                                        "observacoes TEXT, " +
                                                        "criado_em TEXT, " +
                                                        "criado_por TEXT, " +
                                                        "cancelado_em TEXT, " +
                                                        "cancelado_por TEXT, " +
                                                        "juros REAL DEFAULT 0, " +
                                                        "intervalo_dias INTEGER DEFAULT 30, " +
                                                        "FOREIGN KEY (cliente_id) REFERENCES clientes(id)" +
                                                        ")",
                                        "vendas");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas_itens (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "produto_id TEXT NOT NULL, " +
                                                        "qtd INTEGER NOT NULL, " +
                                                        "preco REAL NOT NULL, " +
                                                        "desconto REAL DEFAULT 0, " +
                                                        "acrescimo REAL DEFAULT 0, " +
                                                        "total_item REAL NOT NULL, " +
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY (venda_id) REFERENCES vendas(id)" +
                                                        ")",
                                        "vendas_itens");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas_pagamentos (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "valor REAL NOT NULL, " +
                                                        "FOREIGN KEY (venda_id) REFERENCES vendas(id)" +
                                                        ")",
                                        "vendas_pagamentos");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas_estornos_pagamentos (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "pagamento_id INTEGER NOT NULL, " +
                                                        "tipo_pagamento TEXT NOT NULL, " +
                                                        "valor_estornado REAL NOT NULL, " +
                                                        "data TEXT NOT NULL, " +
                                                        "observacao TEXT, " +
                                                        "criado_em TEXT, " +
                                                        "criado_por TEXT" +
                                                        ")",
                                        "vendas_estornos_pagamentos");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas_devolucoes (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "produto_id TEXT NOT NULL, " +
                                                        "qtd INTEGER NOT NULL, " +
                                                        "valor_unit REAL, " +
                                                        "motivo TEXT, " +
                                                        "data TEXT, " +
                                                        "usuario TEXT, " +
                                                        "FOREIGN KEY(venda_id) REFERENCES vendas(id), " +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id)" +
                                                        ")",
                                        "vendas_devolucoes");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas_itens_lotes(" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_item_id INTEGER NOT NULL, " +
                                                        "lote_id INTEGER NOT NULL, " +
                                                        "qtd INTEGER NOT NULL, " +
                                                        "custo_unit REAL, " +
                                                        "FOREIGN KEY(venda_item_id) REFERENCES vendas_itens(id), " +
                                                        "FOREIGN KEY(lote_id) REFERENCES estoque_lotes(id)" +
                                                        ")",
                                        "vendas_itens_lotes");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS vendas_devolucoes_lotes(" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "devolucao_id INTEGER NOT NULL, " +
                                                        "lote_id INTEGER NOT NULL, " +
                                                        "qtd INTEGER NOT NULL, " +
                                                        "custo_unit REAL, " +
                                                        "FOREIGN KEY(devolucao_id) REFERENCES vendas_devolucoes(id), " +
                                                        "FOREIGN KEY(lote_id) REFERENCES estoque_lotes(id)" +
                                                        ")",
                                        "vendas_devolucoes_lotes");

                        // ========== CATÁLOGO / METADADOS (cartas) ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS condicoes (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "condicoes");
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS linguagens (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "linguagens");
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS tipo_cartas (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "tipo_cartas");
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS subtipo_cartas (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "subtipo_cartas");
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS raridades (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "raridades");
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS sub_raridades (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "sub_raridades");
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS ilustracoes (id TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                                        "ilustracoes");

                        // Pokémon (sets/coleções/carta)
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS sets(" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "series TEXT, " +
                                                        "colecao_id TEXT, " +
                                                        "data_lancamento TEXT" +
                                                        ")",
                                        "sets");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS colecoes(" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "sigla TEXT, " +
                                                        "codigo TEXT, " +
                                                        "data_lancamento TEXT, " +
                                                        "series TEXT, " +
                                                        "observacoes TEXT" +
                                                        ")",
                                        "colecoes");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS cartas (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "set_id TEXT, " +
                                                        "colecao TEXT, " +
                                                        "numero TEXT, " +
                                                        "qtd INTEGER, " +
                                                        "preco REAL, " +
                                                        "preco_loja REAL, " +
                                                        "preco_consignado REAL, " +
                                                        "percentual_loja REAL, " +
                                                        "valor_loja REAL, " +
                                                        "custo REAL, " +
                                                        "condicao_id TEXT, " +
                                                        "linguagem_id TEXT, " +
                                                        "consignado INTEGER DEFAULT 0, " +
                                                        "dono TEXT, " +
                                                        "tipo_id TEXT, " +
                                                        "subtipo_id TEXT, " +
                                                        "raridade_id TEXT, " +
                                                        "sub_raridade_id TEXT, " +
                                                        "ilustracao_id TEXT, " +
                                                        "fornecedor_id TEXT, " +
                                                        "FOREIGN KEY(set_id) REFERENCES sets(id), " +
                                                        "FOREIGN KEY(condicao_id) REFERENCES condicoes(id), " +
                                                        "FOREIGN KEY(linguagem_id) REFERENCES linguagens(id), " +
                                                        "FOREIGN KEY(tipo_id) REFERENCES tipo_cartas(id), " +
                                                        "FOREIGN KEY(subtipo_id) REFERENCES subtipo_cartas(id), " +
                                                        "FOREIGN KEY(raridade_id) REFERENCES raridades(id), " +
                                                        "FOREIGN KEY(sub_raridade_id) REFERENCES sub_raridades(id), " +
                                                        "FOREIGN KEY(ilustracao_id) REFERENCES ilustracoes(id), " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")",
                                        "cartas");

                        // ========== FINANCEIRO ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS formas_pagamento(" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "taxa REAL DEFAULT 0" +
                                                        ")",
                                        "formas_pagamento");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS categorias_produtos(" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "descricao TEXT" +
                                                        ")",
                                        "categorias_produtos");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS titulos_contas_receber (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "cliente_id TEXT, " +
                                                        "codigo_selecao TEXT, " +
                                                        "data_geracao TEXT, " +
                                                        "valor_total REAL, " +
                                                        "status TEXT, " +
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY(cliente_id) REFERENCES clientes(id)" +
                                                        ")",
                                        "titulos_contas_receber");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS parcelas_contas_receber (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "titulo_id TEXT, " +
                                                        "numero_parcela INTEGER, " +
                                                        "vencimento TEXT, " +
                                                        "valor_nominal REAL, " +
                                                        "valor_juros REAL DEFAULT 0, " +
                                                        "valor_acrescimo REAL DEFAULT 0, " +
                                                        "valor_desconto REAL DEFAULT 0, " +
                                                        "valor_pago REAL DEFAULT 0, " +
                                                        "data_pagamento TEXT, " +
                                                        "data_compensacao TEXT, " +
                                                        "pago_com_desconto INTEGER DEFAULT 0, " +
                                                        "forma_pagamento TEXT, " +
                                                        "status TEXT DEFAULT 'aberto', " +
                                                        "FOREIGN KEY(titulo_id) REFERENCES titulos_contas_receber(id)" +
                                                        ")",
                                        "parcelas_contas_receber");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS pagamentos_contas_receber (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "parcela_id INTEGER, " +
                                                        "forma_pagamento TEXT, " +
                                                        "valor_pago REAL, " +
                                                        "data_pagamento TEXT, " +
                                                        "FOREIGN KEY(parcela_id) REFERENCES parcelas_contas_receber(id)"
                                                        +
                                                        ")",
                                        "pagamentos_contas_receber");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS taxas_cartao (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "bandeira TEXT NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "min_parcelas INTEGER NOT NULL, " +
                                                        "max_parcelas INTEGER NOT NULL, " +
                                                        "mes_vigencia TEXT NOT NULL, " +
                                                        "taxa_pct REAL NOT NULL, " +
                                                        "observacoes TEXT" +
                                                        ")",
                                        "taxas_cartao");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS pedidos_compras (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "data TEXT, " + // yyyy-MM-dd
                                                        "status TEXT, " + // rascunho|enviado|parcialmente
                                                                          // recebido|recebido|cancelado
                                                        "fornecedor_id TEXT, " + // opcional (pode ser null)
                                                        "data_prevista TEXT, " + // opcional (yyyy-MM-dd) - nível pedido
                                                                                 // (resumo)
                                                        "prazo_dias INTEGER, " + // opcional - nível pedido (resumo)
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")",
                                        "pedidos_compras");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS pedido_produtos (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "pedido_id TEXT NOT NULL, " +
                                                        "produto_id TEXT NOT NULL, " +
                                                        "fornecedor_id TEXT, " + // fornecedor por item
                                                        "quantidade_pedida INTEGER NOT NULL, " +
                                                        "quantidade_recebida INTEGER DEFAULT 0, " +
                                                        "data_prevista TEXT, " + // yyyy-MM-dd
                                                        "prazo_dias INTEGER, " + // dias
                                                        "status TEXT DEFAULT 'pendente', " + // pendente|parcial|completo
                                                        "FOREIGN KEY(pedido_id) REFERENCES pedidos_compras(id) ON DELETE CASCADE, "
                                                        +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id), " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")",
                                        "pedido_produtos");

                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_pedido_produtos_pedido ON pedido_produtos(pedido_id)",
                                        "idx_pedido_produtos_pedido");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_pedido_produtos_produto ON pedido_produtos(produto_id)",
                                        "idx_pedido_produtos_produto");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_pedido_produtos_fornecedor ON pedido_produtos(fornecedor_id)",
                                        "idx_pedido_produtos_fornecedor");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS planos_contas (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "codigo TEXT NOT NULL, " +
                                                        "descricao TEXT NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "parent_id TEXT, " +
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY(parent_id) REFERENCES planos_contas(id)" +
                                                        ")",
                                        "planos_contas");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS titulos_contas_pagar (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor_id TEXT, " +
                                                        "plano_conta_id TEXT, " +
                                                        "codigo_selecao TEXT, " +
                                                        "data_geracao TEXT, " +
                                                        "valor_total REAL, " +
                                                        "status TEXT, " +
                                                        "observacoes TEXT, " +
                                                        "pedido_id TEXT, " +
                                                        "FOREIGN KEY(pedido_id) REFERENCES pedidos_compras(id), " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id), " +
                                                        "FOREIGN KEY(plano_conta_id) REFERENCES planos_contas(id)" +
                                                        ")",
                                        "titulos_contas_pagar");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS parcelas_contas_pagar (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "titulo_id TEXT, " +
                                                        "numero_parcela INTEGER, " +
                                                        "vencimento TEXT, " +
                                                        "valor_nominal REAL, " +
                                                        "valor_juros REAL DEFAULT 0, " +
                                                        "valor_acrescimo REAL DEFAULT 0, " +
                                                        "valor_desconto REAL DEFAULT 0, " +
                                                        "valor_pago REAL DEFAULT 0, " +
                                                        "data_pagamento TEXT, " +
                                                        "data_compensacao TEXT, " +
                                                        "pago_com_desconto INTEGER DEFAULT 0, " +
                                                        "forma_pagamento TEXT, " +
                                                        "status TEXT DEFAULT 'aberto', " +
                                                        "FOREIGN KEY(titulo_id) REFERENCES titulos_contas_pagar(id)" +
                                                        ")",
                                        "parcelas_contas_pagar");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS pagamentos_contas_pagar (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "parcela_id INTEGER, " +
                                                        "forma_pagamento TEXT, " +
                                                        "valor_pago REAL, " +
                                                        "data_pagamento TEXT, " +
                                                        "FOREIGN KEY(parcela_id) REFERENCES parcelas_contas_pagar(id)" +
                                                        ")",
                                        "pagamentos_contas_pagar");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS contas_pagar_pedidos (" +
                                                        "conta_pagar_id TEXT NOT NULL, " +
                                                        "pedido_id TEXT NOT NULL, " +
                                                        "PRIMARY KEY (conta_pagar_id, pedido_id), " +
                                                        "FOREIGN KEY (conta_pagar_id) REFERENCES titulos_contas_pagar(id), "
                                                        +
                                                        "FOREIGN KEY (pedido_id) REFERENCES pedidos_compras(id)" +
                                                        ")",
                                        "contas_pagar_pedidos");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS bancos (
                                                id TEXT PRIMARY KEY,
                                                nome TEXT NOT NULL,
                                                agencia TEXT,
                                                conta TEXT,
                                                observacoes TEXT
                                            )
                                        """, "bancos");

                        // ========== CONFIG / PROMOÇÕES / VIP ==========
                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS config_loja (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "nome_fantasia TEXT, " +
                                                        "cnpj TEXT NOT NULL, " +
                                                        "inscricao_estadual TEXT, " +
                                                        "regime_tributario TEXT, " +
                                                        "cnae TEXT, " +
                                                        "endereco_logradouro TEXT, " +
                                                        "endereco_numero TEXT, " +
                                                        "endereco_complemento TEXT, " +
                                                        "endereco_bairro TEXT, " +
                                                        "endereco_municipio TEXT, " +
                                                        "endereco_uf TEXT, " +
                                                        "endereco_cep TEXT, " +
                                                        "telefone TEXT, " +
                                                        "email TEXT, " +
                                                        "socios TEXT, " +
                                                        "modelo_nota TEXT, " +
                                                        "serie_nota TEXT, " +
                                                        "numero_inicial_nota INTEGER, " +
                                                        "ambiente_nfce TEXT, " +
                                                        "csc TEXT, " +
                                                        "token_csc TEXT, " +
                                                        "certificado_path TEXT, " +
                                                        "certificado_senha TEXT, " +
                                                        "nome_impressora TEXT, " +
                                                        "texto_rodape_nota TEXT, " +
                                                        "url_webservice_nfce TEXT, " +
                                                        "proxy_host TEXT, " +
                                                        "proxy_port INTEGER, " +
                                                        "proxy_usuario TEXT, " +
                                                        "proxy_senha TEXT" +
                                                        ");",
                                        "config_loja");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS tipos_promocao (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "descricao TEXT" +
                                                        ")",
                                        "tipos_promocao");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS promocoes (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "desconto REAL NOT NULL, " +
                                                        "tipo_desconto TEXT NOT NULL, " +
                                                        "aplica_em TEXT NOT NULL, " +
                                                        "tipo_id TEXT, " +
                                                        "data_inicio TEXT NOT NULL, " +
                                                        "data_fim TEXT NOT NULL, " +
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY(tipo_id) REFERENCES tipos_promocao(id)" +
                                                        ")",
                                        "promocoes");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS promocao_produtos (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "promocao_id TEXT, " +
                                                        "produto_id TEXT, " +
                                                        "FOREIGN KEY(promocao_id) REFERENCES promocoes(id) ON DELETE CASCADE, "
                                                        +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id) ON DELETE CASCADE"
                                                        +
                                                        ")",
                                        "promocao_produtos");

                        executeComLog(st,
                                        "CREATE TABLE IF NOT EXISTS clientes_vip (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "cpf TEXT, " +
                                                        "telefone TEXT, " +
                                                        "categoria TEXT, " +
                                                        "criado_em TEXT, " +
                                                        "observacoes TEXT" +
                                                        ")",
                                        "clientes_vip");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS credito_loja (
                                                id TEXT PRIMARY KEY,
                                                cliente_id TEXT NOT NULL,
                                                valor REAL NOT NULL DEFAULT 0,
                                                FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                                            )
                                        """, "credito_loja");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS credito_loja_movimentacoes (
                                                id TEXT PRIMARY KEY,
                                                cliente_id TEXT NOT NULL,
                                                valor REAL NOT NULL,
                                                tipo TEXT NOT NULL,
                                                referencia TEXT,
                                                data TEXT NOT NULL,
                                                evento_id TEXT,
                                                FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                                            )
                                        """, "credito_loja_movimentacoes");
                        ensureColumn(c, "credito_loja_movimentacoes", "evento_id", "TEXT");

                        // ========== JOGOS + SETS_JOGOS ==========
                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS jogos (
                                                id TEXT PRIMARY KEY,
                                                nome TEXT NOT NULL
                                            )
                                        """, "jogos");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS cartas_jogos (
                                                id TEXT PRIMARY KEY,
                                                jogo_id TEXT NOT NULL,
                                                nome TEXT NOT NULL,
                                                set_id TEXT,
                                                numero TEXT,
                                                linguagem TEXT,
                                                raridade TEXT,
                                                tipo TEXT,
                                                subtipo TEXT,
                                                ilustracao TEXT,
                                                preco REAL,
                                                observacoes TEXT,
                                                FOREIGN KEY (jogo_id) REFERENCES jogos(id)
                                            )
                                        """, "cartas_jogos");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS sets_jogos (
                                                set_id TEXT,
                                                nome TEXT NOT NULL,
                                                jogo_id TEXT NOT NULL,
                                                data_lancamento TEXT,
                                                qtd_cartas INTEGER,
                                                codigo_externo TEXT,
                                                PRIMARY KEY (set_id, jogo_id),
                                                FOREIGN KEY (jogo_id) REFERENCES jogos(id)
                                            )
                                        """, "sets_jogos");

                        // Índices simples úteis (não quebram nada)
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_vendas_cliente ON vendas(cliente_id)",
                                        "idx_vendas_cliente");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_vendas_data ON vendas(data_venda)",
                                        "idx_vendas_data");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_vendas_itens_venda ON vendas_itens(venda_id)",
                                        "idx_vendas_itens_venda");
                        
                        // ✅ NOVOS: Índices para tabelas críticas (evitam full scans)
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_parcelas_receber_titulo ON parcelas_contas_receber(titulo_id)",
                                        "idx_parcelas_receber_titulo");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_parcelas_receber_status ON parcelas_contas_receber(status)",
                                        "idx_parcelas_receber_status");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_parcelas_receber_vencimento ON parcelas_contas_receber(vencimento)",
                                        "idx_parcelas_receber_vencimento");
                        
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_parcelas_pagar_titulo ON parcelas_contas_pagar(titulo_id)",
                                        "idx_parcelas_pagar_titulo");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_parcelas_pagar_status ON parcelas_contas_pagar(status)",
                                        "idx_parcelas_pagar_status");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_parcelas_pagar_vencimento ON parcelas_contas_pagar(vencimento)",
                                        "idx_parcelas_pagar_vencimento");
                        
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_estoque_movimentacoes_produto ON estoque_movimentacoes(produto_id)",
                                        "idx_estoque_movimentacoes_produto");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_estoque_movimentacoes_data ON estoque_movimentacoes(data)",
                                        "idx_estoque_movimentacoes_data");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_estoque_movimentacoes_lote ON estoque_movimentacoes(lote_id)",
                                        "idx_estoque_movimentacoes_lote");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_estoque_lotes_produto_data ON estoque_lotes(produto_id, data_entrada, id)",
                                        "idx_estoque_lotes_produto_data");
                        executeComLog(st, "DROP INDEX IF EXISTS ux_produtos_codigo_barras_ativo",
                                        "drop_ux_produtos_codigo_barras_ativo");

                        migrarEstoqueParaLotes(c);
                }
        }

        /** Seeds base (sem internet). */
        private static void seedBaseData(Connection c) throws SQLException {
                try (Statement st = c.createStatement()) {
                        // fallback fiscal
                        executeComLog(st,
                                        "INSERT OR IGNORE INTO ncm (codigo, descricao) VALUES " +
                                                        "('00000000','NCM não informado')," +

                                                        // TCG / jogos / hobby (fallbacks comuns)
                                                        "('95049090','Jogos e artigos para diversão (outros)')," +

                                                        // Acessórios típicos (sleeves, deck box, dados, playmat etc.)
                                                        "('39269090','Artefatos de plástico (outros)')," +
                                                        "('42029200','Bolsas/estojos/caixas (outros)')," +

                                                        // Papelaria/impresso (às vezes útil para itens com
                                                        // papel/cartão)
                                                        "('49119900','Impressos (outros)')," +

                                                        // Alimentos e bebidas (fallbacks genéricos para não travar)
                                                        "('19059090','Produtos de padaria e confeitaria (outros)')," +
                                                        "('20089900','Frutas e outras partes comestíveis preparadas ou conservadas (outros)'),"
                                                        +
                                                        "('22021000','Águas, incluindo águas minerais e gaseificadas, com adição de açúcar ou aromatizadas'),"
                                                        +
                                                        "('21069090','Preparações alimentícias (outros)');",
                                        "fallback_ncm_robusto");

                        executeComLog(st,
                                        "INSERT OR IGNORE INTO cfop (codigo, descricao) VALUES " +
                                                        "('5101','Venda de produção do estabelecimento')," +
                                                        "('5102','Venda de mercadoria adquirida ou recebida de terceiros'),"
                                                        +
                                                        "('5405','Venda de mercadoria adquirida ou recebida de terceiros em operação com ST'),"
                                                        +
                                                        "('5929','Lançamento efetuado a título de baixa de estoque decorrente de perda, roubo ou deterioração'),"
                                                        +
                                                        "('5202','Devolução de compra para comercialização')," +
                                                        "('1202','Devolução de venda de mercadoria adquirida ou recebida de terceiros'),"
                                                        +
                                                        "('5949','Outra saída de mercadoria ou prestação de serviço não especificada');",
                                        "fallback_cfop_robusto");

                        executeComLog(st,
                                        "INSERT OR IGNORE INTO csosn (codigo, descricao) VALUES " +
                                                        "('101','Tributada pelo Simples Nacional com permissão de crédito'),"
                                                        +
                                                        "('102','Tributada pelo Simples Nacional sem permissão de crédito'),"
                                                        +
                                                        "('103','Isenção do ICMS no Simples Nacional para faixa de receita bruta'),"
                                                        +
                                                        "('201','Tributada pelo Simples Nacional com permissão de crédito e com cobrança de ST'),"
                                                        +
                                                        "('202','Tributada pelo Simples Nacional sem permissão de crédito e com cobrança de ST'),"
                                                        +
                                                        "('400','Não tributada pelo Simples Nacional')," +
                                                        "('500','ICMS cobrado anteriormente por substituição tributária'),"
                                                        +
                                                        "('900','Outros');",
                                        "fallback_csosn_robusto");

                        executeComLog(st,
                                        "INSERT OR IGNORE INTO origem (codigo, descricao) VALUES " +
                                                        "('0','Nacional, exceto as indicadas nos códigos 3 a 5')," +
                                                        "('1','Estrangeira – Importação direta, exceto a indicada no código 6'),"
                                                        +
                                                        "('2','Estrangeira – Adquirida no mercado interno, exceto a indicada no código 7'),"
                                                        +
                                                        "('3','Nacional, mercadoria ou bem com Conteúdo de Importação superior a 40%'),"
                                                        +
                                                        "('8','Nacional, mercadoria ou bem com Conteúdo de Importação superior a 70%');",
                                        "fallback_origem_robusto");
                        executeComLog(st,
                                        "INSERT OR IGNORE INTO unidades (codigo, descricao) VALUES " +
                                                        "('UN','Unidade')," +
                                                        "('CX','Caixa')," +
                                                        "('PC','Peça')," +
                                                        "('PCT','Pacote')," +
                                                        "('FD','Fardo')," +
                                                        "('KG','Quilograma')," +
                                                        "('G','Grama')," +
                                                        "('L','Litro')," +
                                                        "('ML','Mililitro');",
                                        "fallback_unidades");

                        executeComLog(st,
                                        "INSERT OR IGNORE INTO config_fiscal_default " +
                                                        "(id, regime_tributario, cfop_padrao, csosn_padrao, origem_padrao, ncm_padrao, unidade_padrao) VALUES "
                                                        +
                                                        "('DEFAULT','SIMPLES','5102','102','0','00000000','UN');",
                                        "fallback_config_fiscal_default");

                        executeComLog(st, """
                                            INSERT OR IGNORE INTO sequencias_fiscais
                                            (id, modelo, codigo_modelo, serie, ambiente, ultimo_numero, criado_em)
                                            VALUES
                                            ('NFCe-65-SERIE-1-OFF',     'NFCe', 65, 1, 'OFF',      0, datetime('now')),
                                            ('NFCe-65-SERIE-1-HOMOLOG', 'NFCe', 65, 1, 'HOMOLOG',  0, datetime('now')),
                                            ('NFCe-65-SERIE-1-PRODUCAO','NFCe', 65, 1, 'PRODUCAO', 0, datetime('now'));
                                        """, "seed_sequencias_fiscais");

                        // cartas - base
                        executeComLog(st, "INSERT OR IGNORE INTO tipo_cartas (id,nome) VALUES " +
                                        "('T1','Pokémon'),('T2','Treinador'),('T3','Energia')", "insert_tipo_cartas");

                        executeComLog(st, "INSERT OR IGNORE INTO subtipo_cartas (id,nome) VALUES " +
                                        "('S1','Básico'),('S2','Estágio 1'),('S3','Estágio 2')," +
                                        "('S4','Item'),('S5','Suporte'),('S6','Estádio'),('S7','Ferramenta')," +
                                        "('S8','Água'),('S9','Fogo'),('S10','Grama'),('S11','Elétrico'),('S12','Lutador'),"
                                        +
                                        "('S13','Noturno'),('S14','Psíquico'),('S15','Metálico'),('S16','Dragão'),('S17','Incolor')",
                                        "insert_subtipo_cartas");

                        executeComLog(st, "INSERT OR IGNORE INTO raridades (id,nome) VALUES " +
                                        "('R1','Comum'),('R2','Incomum'),('R3','Rara'),('R4','Promo')," +
                                        "('R5','Foil'),('R6','Foil Reverse'),('R7','Secreta')", "insert_raridades");

                        executeComLog(st, "INSERT OR IGNORE INTO sub_raridades (id,nome) VALUES " +
                                        "('SR1','EX'),('SR2','GX'),('SR3','V'),('SR4','VMAX'),('SR5','VSTAR'),('SR6','TERA')",
                                        "insert_sub_raridades");

                        executeComLog(st, "INSERT OR IGNORE INTO ilustracoes (id,nome) VALUES " +
                                        "('IL1','Regular'),('IL2','Full Art'),('IL3','Secreta')", "insert_ilustracoes");

                        // jogos
                        executeComLog(st, """
                                            INSERT OR IGNORE INTO jogos (id, nome) VALUES
                                                ('POKEMON', 'Pokémon TCG'),
                                                ('YUGIOH', 'Yu-Gi-Oh!'),
                                                ('MAGIC', 'Magic: The Gathering'),
                                                ('ONEPIECE', 'One Piece Card Game'),
                                                ('DIGIMON', 'Digimon Card Game'),
                                                ('DRAGONBALL', 'Dragon Ball Super Card Game')
                                        """, "insert_jogos");
                        // ========== COMANDAS ==========
                        executeComLog(st,
                                        """
                                                            CREATE TABLE IF NOT EXISTS comandas (
                                                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                                cliente_id TEXT,
                                                                nome_cliente TEXT,
                                                                mesa TEXT,
                                                                status TEXT NOT NULL DEFAULT 'aberta', -- aberta | pendente | fechada | cancelada

                                                                -- ✅ vínculo com venda gerada ao fechar comanda
                                                                venda_id INTEGER,

                                                                total_bruto REAL NOT NULL DEFAULT 0,
                                                                desconto REAL NOT NULL DEFAULT 0,
                                                                acrescimo REAL NOT NULL DEFAULT 0,
                                                                total_liquido REAL NOT NULL DEFAULT 0,
                                                                total_pago REAL NOT NULL DEFAULT 0,

                                                                observacoes TEXT,

                                                                criado_em TEXT NOT NULL,
                                                                criado_por TEXT,
                                                                fechado_em TEXT,
                                                                fechado_por TEXT,
                                                                cancelado_em TEXT,
                                                                cancelado_por TEXT,

                                                                FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                                                                FOREIGN KEY (venda_id) REFERENCES vendas(id)
                                                            );
                                                        """,
                                        "comandas");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS comandas_itens (
                                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                comanda_id INTEGER NOT NULL,
                                                produto_id TEXT NOT NULL,

                                                qtd INTEGER NOT NULL,
                                                preco REAL NOT NULL,
                                                desconto REAL NOT NULL DEFAULT 0,
                                                acrescimo REAL NOT NULL DEFAULT 0,
                                                total_item REAL NOT NULL,

                                                observacoes TEXT,
                                                criado_em TEXT NOT NULL,
                                                criado_por TEXT,

                                                FOREIGN KEY (comanda_id) REFERENCES comandas(id) ON DELETE CASCADE,
                                                FOREIGN KEY (produto_id) REFERENCES produtos(id)
                                            );
                                        """, "comandas_itens");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS comandas_pagamentos (
                                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                comanda_id INTEGER NOT NULL,
                                                tipo TEXT NOT NULL,   -- PIX | DINHEIRO | CARTAO | OUTRO etc
                                                valor REAL NOT NULL,
                                                data TEXT NOT NULL,
                                                usuario TEXT,

                                                FOREIGN KEY (comanda_id) REFERENCES comandas(id) ON DELETE CASCADE
                                            );
                                        """, "comandas_pagamentos");

                        // Índices (pra lista ficar rápida)
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_comandas_status ON comandas(status)",
                                        "idx_comandas_status");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_comandas_venda_id ON comandas(venda_id)",
                                        "idx_comandas_venda_id");
                        executeComLog(st, "CREATE INDEX IF NOT EXISTS idx_comandas_criado_em ON comandas(criado_em)",
                                        "idx_comandas_criado_em");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_comandas_itens_comanda ON comandas_itens(comanda_id)",
                                        "idx_comandas_itens_comanda");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_comandas_pag_comanda ON comandas_pagamentos(comanda_id)",
                                        "idx_comandas_pag_comanda");

                        // ========== EVENTOS / LIGAS ==========
                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS eventos (
                                                id TEXT PRIMARY KEY,
                                                nome TEXT NOT NULL,
                                                jogo_id TEXT,
                                                data_inicio TEXT,
                                                data_fim TEXT,
                                                status TEXT NOT NULL DEFAULT 'rascunho', -- rascunho | aberto | fechado | cancelado
                                                taxa_inscricao REAL NOT NULL DEFAULT 0,
                                                produto_inscricao_id TEXT,
                                                regras_texto TEXT,
                                                limite_participantes INTEGER,
                                                observacoes TEXT,
                                                criado_em TEXT,
                                                criado_por TEXT,
                                                alterado_em TEXT,
                                                alterado_por TEXT,
                                                FOREIGN KEY (jogo_id) REFERENCES jogos(id),
                                                FOREIGN KEY (produto_inscricao_id) REFERENCES produtos(id)
                                            );
                                        """, "eventos");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS eventos_participantes (
                                                id TEXT PRIMARY KEY,
                                                evento_id TEXT NOT NULL,
                                                cliente_id TEXT,
                                                nome_avulso TEXT,
                                                status TEXT NOT NULL DEFAULT 'inscrito', -- inscrito | pago | presente | desistente | desclassificado
                                                venda_id INTEGER,
                                                comanda_id INTEGER,
                                                comanda_item_id INTEGER,
                                                data_checkin TEXT,
                                                criado_em TEXT,
                                                criado_por TEXT,
                                                alterado_em TEXT,
                                                alterado_por TEXT,
                                                FOREIGN KEY (evento_id) REFERENCES eventos(id),
                                                FOREIGN KEY (cliente_id) REFERENCES clientes(id),
                                                FOREIGN KEY (venda_id) REFERENCES vendas(id),
                                                FOREIGN KEY (comanda_id) REFERENCES comandas(id)
                                            );
                                        """, "eventos_participantes");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS eventos_ranking (
                                                id TEXT PRIMARY KEY,
                                                evento_id TEXT NOT NULL,
                                                participante_id TEXT NOT NULL,
                                                pontos INTEGER NOT NULL DEFAULT 0,
                                                colocacao INTEGER,
                                                observacao TEXT,
                                                FOREIGN KEY (evento_id) REFERENCES eventos(id),
                                                FOREIGN KEY (participante_id) REFERENCES eventos_participantes(id)
                                            );
                                        """, "eventos_ranking");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS eventos_premiacao_regras (
                                                id TEXT PRIMARY KEY,
                                                evento_id TEXT NOT NULL,
                                                colocacao_inicio INTEGER,
                                                colocacao_fim INTEGER,
                                                tipo TEXT NOT NULL, -- BOOSTER | CREDITO | PRODUTO
                                                produto_id TEXT,
                                                quantidade INTEGER,
                                                valor_credito REAL,
                                                observacoes TEXT,
                                                FOREIGN KEY (evento_id) REFERENCES eventos(id),
                                                FOREIGN KEY (produto_id) REFERENCES produtos(id)
                                            );
                                        """, "eventos_premiacao_regras");

                        executeComLog(st, """
                                            CREATE TABLE IF NOT EXISTS eventos_premiacoes (
                                                id TEXT PRIMARY KEY,
                                                evento_id TEXT NOT NULL,
                                                participante_id TEXT NOT NULL,
                                                tipo TEXT NOT NULL, -- BOOSTER | CREDITO | PRODUTO
                                                produto_id TEXT,
                                                quantidade INTEGER,
                                                valor_credito REAL,
                                                status TEXT NOT NULL DEFAULT 'pendente', -- pendente | entregue | estornado
                                                movimentacao_estoque_id INTEGER,
                                                credito_mov_id TEXT,
                                                entregue_em TEXT,
                                                entregue_por TEXT,
                                                estornado_em TEXT,
                                                estornado_por TEXT,
                                                observacoes TEXT,
                                                FOREIGN KEY (evento_id) REFERENCES eventos(id),
                                                FOREIGN KEY (participante_id) REFERENCES eventos_participantes(id),
                                                FOREIGN KEY (produto_id) REFERENCES produtos(id)
                                            );
                                        """, "eventos_premiacoes");

                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_eventos_status ON eventos(status)",
                                        "idx_eventos_status");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_eventos_participantes_evento ON eventos_participantes(evento_id)",
                                        "idx_eventos_participantes_evento");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_eventos_ranking_evento ON eventos_ranking(evento_id)",
                                        "idx_eventos_ranking_evento");
                        executeComLog(st,
                                        "CREATE INDEX IF NOT EXISTS idx_eventos_premiacoes_evento ON eventos_premiacoes(evento_id)",
                                        "idx_eventos_premiacoes_evento");

                        executeComLog(st,
                                        "INSERT OR IGNORE INTO clientes " +
                                                        "(id, nome, telefone, cpf, data_nasc, tipo, endereco, cidade, estado, observacoes, criado_em, criado_por, alterado_em, alterado_por) VALUES "
                                                        +
                                                        "('AVULSO', 'Consumidor (Sem Cadastro)', '', '00000000000', '', 'AVULSO', '', 'Campo Grande', 'MS', "
                                                        +
                                                        "'Cliente padrão do sistema para vendas/comandas rápidas', datetime('now'), 'SYSTEM', datetime('now'), 'SYSTEM');",
                                        "seed_cliente_avulso");

                }

        }

        // ------------------------------ ADMIN ---------------------------------

        private static void ensureAdminUser(Connection conn) throws SQLException {
                String checkSql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                        ps.setString(1, "admin");
                        try (ResultSet rs = ps.executeQuery()) {
                                boolean exists = rs.next() && rs.getInt(1) > 0;
                                if (exists)
                                        return;
                        }
                }

                String id = UUID.randomUUID().toString();
                String sql = "INSERT INTO usuarios (id, nome, usuario, senha, tipo, ativo) VALUES (?,?,?,?,?,1)";
                try (PreparedStatement p = conn.prepareStatement(sql)) {
                        p.setString(1, id);
                        p.setString(2, "Administrador");
                        p.setString(3, "admin");
                        p.setString(4, hashSenha("admin123"));
                        p.setString(5, "Admin");
                        p.executeUpdate();
                        // ✅ Não logar a senha padrão por razões de segurança
                        System.out.println("✅ Usuário padrão 'admin' criado");
                }
        }

        // ------------------------------ SYNC APIs ------------------------------

        private static boolean shouldSyncRemoteData(boolean existedBefore) {
                if (!existedBefore)
                        return true;
                if (Boolean.parseBoolean(System.getProperty("hostore.sync.force", "false")))
                        return true;

                Properties props = loadSyncProps();
                long last = parseLong(props.getProperty("last_sync_epoch", "0"), 0);

                long now = Instant.now().getEpochSecond();
                long delta = now - last;

                return delta >= SYNC_MIN_INTERVAL_SECONDS;
        }

        private static void syncRemoteDataSafely() {
                syncFiscalSafely();
                syncPokemonSetsWithCacheFallback();
                syncOtherGamesWithCacheFallback();
        }

        private static void syncFiscalSafely() {
                try {
                        boolean okAny = false;

                        try {
                                List<model.CfopModel> cfops = service.FiscalApiService.listarCfops();
                                new dao.CfopDAO().sincronizarComApi(cfops);
                                okAny = true;
                                System.out.println("✅ CFOP sincronizado via API.");
                        } catch (Exception e) {
                                System.err.println("⚠ CFOP API falhou. Mantendo fallback local.");
                                logWarn("CFOP API fail: " + e.getMessage(), e);
                        }

                        try {
                                List<model.CsosnModel> csosns = service.FiscalApiService.listarCsosns();
                                new dao.CsosnDAO().sincronizarComApi(csosns);
                                okAny = true;
                                System.out.println("✅ CSOSN sincronizado via API.");
                        } catch (Exception e) {
                                System.err.println("⚠ CSOSN API falhou. Mantendo fallback local.");
                                logWarn("CSOSN API fail: " + e.getMessage(), e);
                        }

                        try {
                                List<model.OrigemModel> origens = service.FiscalApiService.listarOrigens();
                                new dao.OrigemDAO().sincronizarComApi(origens);
                                okAny = true;
                                System.out.println("✅ Origem sincronizada via API.");
                        } catch (Exception e) {
                                System.err.println("⚠ Origem API falhou. Mantendo fallback local.");
                                logWarn("Origem API fail: " + e.getMessage(), e);
                        }

                        System.out.println(
                                        okAny ? "✅ Fiscal: pronto (API + fallback local)."
                                                        : "✅ Fiscal: pronto (somente fallback local).");

                } catch (Exception e) {
                        System.err.println("⚠ Erro geral na sincronização fiscal. Mantendo fallback local.");
                        logWarn("Fiscal sync error: " + e.getMessage(), e);
                }
        }

        /**
         * Pokémon: tenta API via Service; se falhar, usa cache em
         * data/cache/pokemontcg_sets.json.
         */
        private static void syncPokemonSetsWithCacheFallback() {
                String json = null;

                // 1) tenta API normal (SetService/ColecaoService internamente chamam
                // PokeTcgApi)
                try {
                        // A ideia aqui é: usar o mesmo JSON para sets e coleções.
                        // Então chamamos uma vez só: PokeTcgApi.listarColecoes() (que na prática é
                        // /sets).
                        json = api.PokeTcgApi.listarColecoes();
                        System.out.println("✅ Pokémon: JSON obtido via API.");
                } catch (Exception e) {
                        System.err.println("⚠ Pokémon: API falhou. Tentando cache local...");
                        logWarn("Pokemon API fail: " + e.getMessage(), e);
                }

                // 2) fallback cache
                if (json == null || json.isBlank()) {
                        json = readText(POKEMON_SETS_CACHE);
                        if (json == null || json.isBlank()) {
                                System.err.println(
                                                "❌ Pokémon: cache não encontrado. Mantendo o que já existe no banco.");
                                return;
                        }
                        System.out.println("✅ Pokémon: usando CACHE local.");
                }

                // 3) parse do MESMO JSON para sets e coleções
                List<SetModel> sets;
                List<ColecaoModel> colecoes;

                try {
                        sets = service.SetService.parseSetsFromJson(json);
                        colecoes = service.ColecaoService.parseColecoesFromSetsJson(json);
                } catch (Exception e) {
                        System.err.println("❌ Pokémon: falha ao parsear JSON (API/cache). Mantendo o banco.");
                        logWarn("Pokemon parse fail: " + e.getMessage(), e);
                        return;
                }

                if ((sets == null || sets.isEmpty()) && (colecoes == null || colecoes.isEmpty())) {
                        System.err.println("❌ Pokémon: parser retornou vazio. Mantendo o banco.");
                        return;
                }

                // 4) grava no banco (ordem + tolerância)
                try {
                        // Se sua ColecaoDAO/SetDAO abrem a própria conexão, ok.
                        // Se você quiser transação única MESMO, tem que adicionar versões que aceitem
                        // Connection.
                        if (sets != null && !sets.isEmpty()) {
                                new SetDAO().sincronizarComApi(sets);
                        }

                        if (colecoes != null && !colecoes.isEmpty()) {
                                new ColecaoDAO().sincronizarComApi(colecoes);
                        }

                        System.out.println("✅ Pokémon: sincronizado (sets=" +
                                        (sets != null ? sets.size() : 0) +
                                        ", colecoes=" +
                                        (colecoes != null ? colecoes.size() : 0) +
                                        ").");

                } catch (Exception e) {
                        System.err.println("⚠ Pokémon: falha ao gravar no banco. Mantendo o banco como está.");
                        logWarn("Pokemon DB write fail: " + e.getMessage(), e);
                }
        }

        /**
         * Sets dos outros jogos: tenta Service; se falhar, usa cache JSON em
         * data/cache/{game}_sets.json
         */
        private static void syncOtherGamesWithCacheFallback() {
                SetJogoDAO setJogoDAO = new SetJogoDAO();

                syncOneGame(setJogoDAO, "YUGIOH", YGO_SETS_CACHE, () -> service.SetJogoService.listarSetsYugioh());
                syncOneGame(setJogoDAO, "MAGIC", MAGIC_SETS_CACHE, () -> service.SetJogoService.listarSetsMagic());
                syncOneGame(setJogoDAO, "DIGIMON", DIGIMON_SETS_CACHE,
                                () -> service.SetJogoService.listarSetsDigimon());
                syncOneGame(setJogoDAO, "ONEPIECE", ONEPIECE_SETS_CACHE,
                                () -> service.SetJogoService.listarSetsOnePiece());

                System.out.println(
                                "✅ Sets de TCGs (exceto Pokémon): sincronização finalizada (tolerante a falhas/cache).");
        }

        private interface SupplierThrows<T> {
                T get() throws Exception;
        }

        private static void syncOneGame(SetJogoDAO dao, String gameId, Path cacheFile,
                        SupplierThrows<List<model.SetJogoModel>> apiCall) {
                try {
                        List<model.SetJogoModel> list = apiCall.get();
                        dao.sincronizarComApi(list);
                        System.out.println("✅ " + gameId + ": sets sincronizados via API (" + list.size() + ").");
                        return;
                } catch (Exception e) {
                        System.err.println("⚠ " + gameId + ": API falhou. Tentando cache local...");
                        logWarn(gameId + " API sync fail: " + e.getMessage(), e);
                }

                try {
                        String cached = readText(cacheFile);
                        if (cached == null || cached.isBlank()) {
                                System.err.println("❌ " + gameId + ": cache não encontrado. Mantendo o banco.");
                                return;
                        }

                        List<model.SetJogoModel> fromCache = tryParseSetJogoFromJson(gameId, cached);
                        if (fromCache == null || fromCache.isEmpty()) {
                                System.err
                                                .println("❌ " + gameId
                                                                + ": cache lido, mas não consegui converter em sets. Mantendo o banco.");
                                return;
                        }

                        dao.sincronizarComApi(fromCache);
                        System.out.println("✅ " + gameId + ": sincronizado via CACHE (" + fromCache.size() + ").");

                } catch (Exception ex) {
                        System.err.println("⚠ " + gameId + ": falha ao usar cache. Mantendo o banco.");
                        logWarn(gameId + " cache fallback fail: " + ex.getMessage(), ex);
                }
        }

        private static void updateLastSyncNow() {
                try {
                        Files.createDirectories(CACHE_DIR);
                        Properties props = loadSyncProps();
                        props.setProperty("last_sync_epoch", String.valueOf(Instant.now().getEpochSecond()));

                        try (OutputStream out = Files.newOutputStream(SYNC_STATE_FILE, StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING)) {
                                props.store(out, "HoStore sync state");
                        }
                } catch (Exception e) {
                        logWarn("Falha ao salvar estado de sync: " + e.getMessage(), e);
                }
        }

        private static Properties loadSyncProps() {
                Properties props = new Properties();
                if (!Files.exists(SYNC_STATE_FILE))
                        return props;

                try (InputStream in = Files.newInputStream(SYNC_STATE_FILE)) {
                        props.load(in);
                } catch (IOException e) {
                        logWarn("Falha ao ler sync_state.properties: " + e.getMessage(), e);
                }
                return props;
        }

        private static long parseLong(String s, long def) {
                try {
                        return Long.parseLong(s);
                } catch (Exception e) {
                        return def;
                }
        }

        // ------------------------------ CACHE READ -----------------------------

        private static String readText(Path file) {
                try {
                        if (!Files.exists(file))
                                return null;
                        return Files.readString(file, StandardCharsets.UTF_8);
                } catch (Exception e) {
                        logWarn("Falha ao ler cache: " + file + " | " + e.getMessage(),
                                        (e instanceof Exception) ? (Exception) e : null);
                        return null;
                }
        }

        /**
         * Esses parsers precisam existir no seu código em algum lugar.
         * Eu deixei via reflexão pra você não travar compilação agora.
         * O correto é você criar métodos fixos nos Services.
         */
        @SuppressWarnings("unchecked")
        private static List<SetModel> tryParsePokemonSetsFromJson(String json) {
                try {
                        // service.SetService.parseSetsFromJson(String)
                        java.lang.reflect.Method m = service.SetService.class.getMethod("parseSetsFromJson",
                                        String.class);
                        Object o = m.invoke(null, json);
                        return (List<SetModel>) o;
                } catch (Exception e) {
                        logWarn("Parser de sets Pokémon não encontrado/erro. Crie SetService.parseSetsFromJson(json).",
                                        e);
                        return null;
                }
        }

        @SuppressWarnings("unchecked")
        private static List<ColecaoModel> tryParsePokemonColecoesFromSetsJson(String json) {
                try {
                        // service.ColecaoService.parseColecoesFromSetsJson(String)
                        java.lang.reflect.Method m = service.ColecaoService.class.getMethod("parseColecoesFromSetsJson",
                                        String.class);
                        Object o = m.invoke(null, json);
                        return (List<ColecaoModel>) o;
                } catch (Exception e) {
                        // opcional, não é erro fatal
                        return null;
                }
        }

        @SuppressWarnings("unchecked")
        private static List<model.SetJogoModel> tryParseSetJogoFromJson(String gameId, String json) {
                try {
                        // service.SetJogoService.parseSetsFromJson(String gameId, String json)
                        java.lang.reflect.Method m = service.SetJogoService.class.getMethod("parseSetsFromJson",
                                        String.class,
                                        String.class);
                        Object o = m.invoke(null, gameId, json);
                        return (List<model.SetJogoModel>) o;
                } catch (Exception e) {
                        logWarn("Parser de SetJogo não encontrado/erro. Crie SetJogoService.parseSetsFromJson(gameId,json).",
                                        e);
                        return null;
                }
        }

        // ------------------------------ UI FEEDBACK ----------------------------

        private static void showUserFeedback(String title, String message, boolean isError) {
                if (!GraphicsEnvironment.isHeadless()) {
                        try {
                                JOptionPane.showMessageDialog(null, message, title,
                                                isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ignored) {
                                if (isError)
                                        System.err.println(title + ": " + message);
                                else
                                        System.out.println(title + ": " + message);
                        }
                } else {
                        if (isError)
                                System.err.println(title + ": " + message);
                        else
                                System.out.println(title + ": " + message);
                }
        }

        // ------------------------------ UTIL -----------------------------------

        private static String hashSenha(String senha) {
                try {
                        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                        byte[] hash = md.digest(senha.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hash)
                                sb.append(String.format("%02x", b));
                        return sb.toString();
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
        }

        private static void executeComLog(Statement st, String sql, String nome) {
                try {
                        st.execute(sql);
                } catch (SQLException e) {
                        System.err.println("Erro ao criar/atualizar tabela ou inserir dados: " + nome);
                        if (DEBUG)
                                e.printStackTrace();
                }
        }

        private static void ensureColumn(Connection c, String table, String column, String type) throws SQLException {
                if (columnExists(c, table, column))
                        return;
                try (Statement st = c.createStatement()) {
                        st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                }
        }

        private static boolean columnExists(Connection c, String table, String column) throws SQLException {
                try (PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")")) {
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        if (column.equalsIgnoreCase(rs.getString("name")))
                                                return true;
                                }
                        }
                }
                return false;
        }

        public static boolean isConnected() {
                try (Connection c = get()) {
                        return c != null && !c.isClosed();
                } catch (SQLException e) {
                        return false;
                }
        }

        private static void logWarn(String msg, Exception e) {
                System.err.println("WARN: " + msg);
                if (DEBUG && e != null)
                        e.printStackTrace();
        }

        private static void logError(String msg, Exception e) {
                System.err.println("ERROR: " + msg);
                if (DEBUG && e != null)
                        e.printStackTrace();
        }

        private static void migrarEstoqueParaLotes(Connection c) {
                String selectProdutos = """
                                SELECT p.id, p.quantidade, p.preco_compra, p.preco_venda, p.fornecedor_id
                                  FROM produtos p
                                 WHERE p.quantidade > 0
                            """;
                String selectExisteLote = "SELECT 1 FROM estoque_lotes WHERE produto_id = ? LIMIT 1";
                String insertLote = """
                                INSERT INTO estoque_lotes
                                (produto_id, fornecedor_id, codigo_lote, data_entrada, validade, custo_unit,
                                 preco_venda_unit, qtd_inicial, qtd_disponivel, status, observacoes)
                                VALUES (?, ?, ?, datetime('now'), NULL, ?, ?, ?, ?, 'ativo', 'MIGRACAO_INICIAL')
                            """;

                try (PreparedStatement psProdutos = c.prepareStatement(selectProdutos);
                                ResultSet rs = psProdutos.executeQuery();
                                PreparedStatement psExiste = c.prepareStatement(selectExisteLote);
                                PreparedStatement psInsert = c.prepareStatement(insertLote)) {

                        while (rs.next()) {
                                String produtoId = rs.getString("id");
                                int qtd = rs.getInt("quantidade");
                                double custo = rs.getDouble("preco_compra");
                                double preco = rs.getDouble("preco_venda");
                                String fornecedorId = rs.getString("fornecedor_id");

                                psExiste.setString(1, produtoId);
                                try (ResultSet rsExiste = psExiste.executeQuery()) {
                                        if (rsExiste.next())
                                                continue;
                                }

                                psInsert.setString(1, produtoId);
                                psInsert.setString(2, fornecedorId);
                                psInsert.setString(3, "MIGRACAO_INICIAL");
                                psInsert.setDouble(4, custo);
                                psInsert.setDouble(5, preco);
                                psInsert.setInt(6, qtd);
                                psInsert.setInt(7, qtd);
                                psInsert.executeUpdate();
                        }
                } catch (Exception e) {
                        logWarn("Falha ao migrar estoque para lotes: " + e.getMessage(), (e instanceof Exception) ? (Exception) e : null);
                }
        }

        public static void popularColecoesPokemonDoCacheSePossivel() {
                try {
                        Path cacheDir = Paths.get(System.getProperty("user.dir"), "data", "cache");
                        Path file = cacheDir.resolve("pokemontcg_sets.json");

                        if (!Files.exists(file)) {
                                System.err.println("⚠ Coleções: cache pokemontcg_sets.json não encontrado. Pulando.");
                                return;
                        }

                        String json = Files.readString(file, StandardCharsets.UTF_8);
                        if (json == null || json.isBlank()) {
                                System.err.println("⚠ Coleções: cache vazio. Pulando.");
                                return;
                        }

                        // parseia coleções a partir do JSON de sets (1 por set: id=name/series)
                        List<model.ColecaoModel> colecoes = service.ColecaoService.parseColecoesFromSetsJson(json);
                        if (colecoes == null || colecoes.isEmpty()) {
                                System.err.println("⚠ Coleções: parser retornou vazio. Pulando.");
                                return;
                        }

                        new dao.ColecaoDAO().sincronizarComApi(colecoes);
                        System.out.println("✅ Coleções (Pokémon) populadas via CACHE: " + colecoes.size());

                } catch (Exception e) {
                        System.err.println("⚠ Falha ao popular coleções via cache: " + e.getMessage());
                }
        }

}
