package util;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DatabaseMigration: Sistema de versionamento e execução de migrações de banco de dados.
 * 
 * Funcionalidade:
 * - Detecta versão atual do BD
 * - Executa ALTER TABLE scripts em sequência mantendo dados
 * - Rastreia migrações executadas
 * - Evita execução duplicada
 */
public class DatabaseMigration {
    private static final String MIGRATIONS_TABLE = "db_migrations";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Verifica e executa migrações necessárias
     */
    public static void runMigrationsIfNeeded(Connection conn) throws SQLException {
        System.out.println("🔄 Verificando migrações de banco de dados...");
        
        // Criar tabela de rastreamento se não existir
        createMigrationsTrackingTable(conn);
        
        // Executar migrações pendentes
        List<Migration> migrations = getAllMigrations();
        for (Migration migration : migrations) {
            if (!hasMigrationRun(conn, migration.version)) {
                System.out.println("▶️ Executando migração: " + migration.name + " (v" + migration.version + ")");
                try {
                    executeMigration(conn, migration);
                    recordMigration(conn, migration);
                    System.out.println("✅ Migração " + migration.version + " concluída com sucesso");
                } catch (SQLException e) {
                    System.err.println("❌ Erro na migração " + migration.version + ": " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    /**
     * Cria tabela de rastreamento de migrações
     */
    private static void createMigrationsTrackingTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS db_migrations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                version TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                executed_at TEXT NOT NULL,
                description TEXT
            )
            """;
        
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /**
     * Verifica se uma migração já foi executada
     */
    private static boolean hasMigrationRun(Connection conn, String version) throws SQLException {
        String sql = "SELECT COUNT(*) FROM db_migrations WHERE version = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Registra que uma migração foi executada
     */
    private static void recordMigration(Connection conn, Migration migration) throws SQLException {
        String sql = """
            INSERT INTO db_migrations (version, name, executed_at, description)
            VALUES (?, ?, ?, ?)
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, migration.version);
            ps.setString(2, migration.name);
            ps.setString(3, LocalDateTime.now().format(DATE_FORMAT));
            ps.setString(4, migration.description);
            ps.executeUpdate();
        }
    }

    /**
     * Executa uma migração com tratamento de erro para colunas que já existem
     */
    private static void executeMigration(Connection conn, Migration migration) throws SQLException {
        String[] statements = migration.sql.split(";");
        
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    try (Statement st = conn.createStatement()) {
                        st.execute(trimmed);
                    }
                } catch (SQLException e) {
                    // Ignorar erro de coluna já existente em SQLite
                    if (e.getMessage().contains("duplicate column name") || 
                        e.getMessage().contains("already exists")) {
                        System.out.println("⚠️ Coluna já existe, ignorando: " + trimmed.substring(0, Math.min(50, trimmed.length())));
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Retorna lista de todas as migrações disponíveis
     */
    private static List<Migration> getAllMigrations() {
        List<Migration> migrations = new ArrayList<>();
        
        // V001: Adicionar campos fiscais em produtos (26/01/2026)
        // NOTA: SQLite não suporta IF NOT EXISTS em ALTER TABLE
        // As colunas são verificadas durante a execução via columnExists()
        migrations.add(new Migration(
            "001",
            "Adicionar campos fiscais em produtos",
            "Adiciona NCM, CFOP, CSOSN, Origem e Unidade aos produtos",
            """
                ALTER TABLE produtos ADD COLUMN ncm TEXT;
                ALTER TABLE produtos ADD COLUMN cfop TEXT;
                ALTER TABLE produtos ADD COLUMN csosn TEXT;
                ALTER TABLE produtos ADD COLUMN origem TEXT;
                ALTER TABLE produtos ADD COLUMN unidade TEXT;
                """
        ));
        
        // V002: Tabelas de referência fiscal
        migrations.add(new Migration(
            "002",
            "Criar tabelas de referência fiscal",
            "Cria NCM, CFOP, CSOSN, Origem e Unidades",
            """
                CREATE TABLE IF NOT EXISTS ncm (
                    codigo TEXT PRIMARY KEY,
                    descricao TEXT NOT NULL,
                    ativo INTEGER DEFAULT 1,
                    criado_em TEXT
                );
                
                CREATE TABLE IF NOT EXISTS cfop (
                    codigo TEXT PRIMARY KEY,
                    descricao TEXT NOT NULL,
                    ativo INTEGER DEFAULT 1,
                    criado_em TEXT
                );
                
                CREATE TABLE IF NOT EXISTS csosn (
                    codigo TEXT PRIMARY KEY,
                    descricao TEXT NOT NULL,
                    ativo INTEGER DEFAULT 1,
                    criado_em TEXT
                );
                
                CREATE TABLE IF NOT EXISTS origem (
                    codigo TEXT PRIMARY KEY,
                    descricao TEXT NOT NULL,
                    ativo INTEGER DEFAULT 1,
                    criado_em TEXT
                );
                
                CREATE TABLE IF NOT EXISTS unidades (
                    codigo TEXT PRIMARY KEY,
                    descricao TEXT NOT NULL,
                    ativo INTEGER DEFAULT 1,
                    criado_em TEXT
                );
                """
        ));
        
        // V003: Configuração fiscal NFCe
        migrations.add(new Migration(
            "003",
            "Criar tabela de configuração NFCe",
            "Tabela para armazenar dados fiscais da empresa (CNPJ, certificado, etc)",
            """
                CREATE TABLE IF NOT EXISTS config_nfce (
                    id TEXT PRIMARY KEY,
                    emitir_nfce INTEGER DEFAULT 1,
                    csc_nfce TEXT,
                    id_csc_nfce INTEGER,
                    cert_a1_path TEXT,
                    cert_a1_senha TEXT,
                    serie_nfce INTEGER DEFAULT 1,
                    numero_inicial_nfce INTEGER DEFAULT 1,
                    ambiente TEXT DEFAULT 'homologacao',
                    regime_tributario TEXT DEFAULT 'Simples Nacional',
                    nome_empresa TEXT,
                    cnpj TEXT,
                    inscricao_estadual TEXT,
                    uf TEXT,
                    nome_fantasia TEXT,
                    endereco_logradouro TEXT,
                    endereco_numero TEXT,
                    endereco_complemento TEXT,
                    endereco_bairro TEXT,
                    endereco_municipio TEXT,
                    endereco_cep TEXT,
                    criado_em TEXT,
                    alterado_em TEXT
                );
                """
        ));
        
        // V004: Documentos Fiscais NFCe
        migrations.add(new Migration(
            "004",
            "Criar tabelas de documentos fiscais",
            "Tabelas para armazenar NFCe, itens, pagamentos e impostos",
            """
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
                    FOREIGN KEY(venda_id) REFERENCES vendas(id)
                );
                
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
                    FOREIGN KEY(documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
                );
                
                CREATE TABLE IF NOT EXISTS documentos_fiscais_pagamentos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    documento_id TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    valor REAL NOT NULL,
                    FOREIGN KEY(documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
                );
                
                CREATE INDEX IF NOT EXISTS idx_doc_fiscal_venda ON documentos_fiscais(venda_id);
                CREATE INDEX IF NOT EXISTS idx_doc_fiscal_status ON documentos_fiscais(status);
                CREATE INDEX IF NOT EXISTS idx_doc_fiscal_chave ON documentos_fiscais(chave_acesso);
                """
        ));
        
        // V005: Sequência fiscal
        migrations.add(new Migration(
            "005",
            "Criar tabela de sequências fiscais",
            "Rastreia últimos números de NFCe emitidas",
            """
                CREATE TABLE IF NOT EXISTS sequencias_fiscais (
                    id TEXT PRIMARY KEY,
                    modelo TEXT NOT NULL,
                    codigo_modelo INTEGER NOT NULL,
                    serie INTEGER NOT NULL,
                    ambiente TEXT NOT NULL,
                    ultimo_numero INTEGER NOT NULL DEFAULT 0,
                    criado_em TEXT NOT NULL,
                    alterado_em TEXT,
                    UNIQUE(modelo, codigo_modelo, serie, ambiente)
                );
                
                CREATE INDEX IF NOT EXISTS idx_seq_fiscais ON sequencias_fiscais(modelo, codigo_modelo, serie, ambiente);
                """
        ));
        
        // V006: Campos adicionais em vendas
        migrations.add(new Migration(
            "006",
            "Adicionar campos fiscais em vendas",
            "Adiciona número de NFCe e status fiscal em vendas",
            """
                ALTER TABLE vendas ADD COLUMN numero_nfce TEXT;
                ALTER TABLE vendas ADD COLUMN status_fiscal TEXT DEFAULT 'pendente';
                """
        ));
        
        // V007: Popular referências fiscais
        migrations.add(new Migration(
            "007",
            "Popular dados de referência fiscal",
            "Insere unidades, origem, CFOP, CSOSN padrão",
            getMigrationV007SQL()
        ));
        
        // V008: Adicionar campos para certificado A1/A3 e modo de emissão
        migrations.add(new Migration(
            "008",
            "Adicionar campos de certificado A1/A3 e modo de emissão",
            "Novos campos: modo_emissao, cert_a1_path, cert_a1_senha, cert_a3_path, etc",
            """
                ALTER TABLE config_nfce ADD COLUMN modo_emissao TEXT DEFAULT 'OFFLINE_VALIDACAO';
                ALTER TABLE config_nfce ADD COLUMN cert_a1_path TEXT;
                ALTER TABLE config_nfce ADD COLUMN cert_a1_senha TEXT;
                ALTER TABLE config_nfce ADD COLUMN cert_a3_host TEXT;
                ALTER TABLE config_nfce ADD COLUMN cert_a3_porta INTEGER;
                ALTER TABLE config_nfce ADD COLUMN cert_a3_usuario TEXT;
                ALTER TABLE config_nfce ADD COLUMN cert_a3_senha TEXT;
                ALTER TABLE config_nfce ADD COLUMN usa_cert_laboratorio INTEGER DEFAULT 0;
                ALTER TABLE config_nfce ADD COLUMN cert_lab_path TEXT;
                ALTER TABLE config_nfce ADD COLUMN cert_lab_senha TEXT;
                ALTER TABLE config_nfce ADD COLUMN xsd_versao TEXT DEFAULT '4.00';
                """
        ));
        
        // V009: Adicionar campos de status fiscal detalhado em documentos
        migrations.add(new Migration(
            "009",
            "Adicionar campos de step status em documentos fiscais",
            "Rastreamento detalhado do pipeline: XML → XSD → Assinatura → Envio",
            """
                ALTER TABLE documentos_fiscais ADD COLUMN step_status TEXT DEFAULT 'PENDENTE';
                ALTER TABLE documentos_fiscais ADD COLUMN xml_pre TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN xml_assinado TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN xsd_validado INTEGER DEFAULT 0;
                ALTER TABLE documentos_fiscais ADD COLUMN xsd_ok_at TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN assinado_at TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN assinado_por TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN enviado_at TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN resposta_sefaz TEXT;
                """
        ));

        // V010: Ajustes de compatibilidade (campos faltantes em versões antigas)
        migrations.add(new Migration(
            "010",
            "Ajustes de colunas em documentos fiscais",
            "Garante colunas usadas pelo app em bases antigas",
            """
                ALTER TABLE documentos_fiscais ADD COLUMN codigo_modelo INTEGER;
                ALTER TABLE documentos_fiscais ADD COLUMN recibo TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN cancelado_por TEXT;
                ALTER TABLE documentos_fiscais_itens ADD COLUMN observacoes TEXT;
                ALTER TABLE documentos_fiscais_pagamentos ADD COLUMN tipo TEXT;
                """
        ));

        // V011: Tempo de perman?ncia da comanda
        migrations.add(new Migration(
            "011",
            "Adicionar tempo de perman?ncia na comanda",
            "Registra minutos na loja (cap 8h) ao fechar comanda",
            """
                ALTER TABLE comandas ADD COLUMN tempo_permanencia_min INTEGER DEFAULT 0;
                """
        ));

        // V012: Armazenar metadados do XML (path/sha/tamanho)
        migrations.add(new Migration(
            "012",
            "Adicionar metadados do XML da NFC-e",
            "Novos campos: xml_path, xml_sha256, xml_tamanho",
            """
                ALTER TABLE documentos_fiscais ADD COLUMN xml_path TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN xml_sha256 TEXT;
                ALTER TABLE documentos_fiscais ADD COLUMN xml_tamanho INTEGER;
                """
        ));
        
        // V013: Modulo RH (funcionarios, ponto, escalas, folha)
        migrations.add(new Migration(
            "013",
            "Criar tabelas de RH",
            "Cadastros de funcionarios, cargos, ponto, escala, ferias, comissoes e folha",
            """
                CREATE TABLE IF NOT EXISTS rh_cargos (
                    id TEXT PRIMARY KEY,
                    nome TEXT NOT NULL,
                    descricao TEXT,
                    salario_base REAL,
                    ativo INTEGER DEFAULT 1,
                    criado_em TEXT,
                    alterado_em TEXT
                );

                CREATE TABLE IF NOT EXISTS rh_funcionarios (
                    id TEXT PRIMARY KEY,
                    nome TEXT NOT NULL,
                    tipo_contrato TEXT NOT NULL,
                    cpf TEXT,
                    cnpj TEXT,
                    rg TEXT,
                    pis TEXT,
                    data_admissao TEXT,
                    data_demissao TEXT,
                    cargo_id TEXT,
                    salario_base REAL,
                    comissao_pct REAL DEFAULT 0,
                    usuario_id TEXT,
                    email TEXT,
                    telefone TEXT,
                    endereco TEXT,
                    ativo INTEGER DEFAULT 1,
                    observacoes TEXT,
                    criado_em TEXT,
                    alterado_em TEXT
                );

                CREATE TABLE IF NOT EXISTS rh_salarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    funcionario_id TEXT NOT NULL,
                    cargo_id TEXT,
                    salario_base REAL NOT NULL,
                    data_inicio TEXT NOT NULL,
                    data_fim TEXT,
                    motivo TEXT,
                    criado_em TEXT
                );

                CREATE TABLE IF NOT EXISTS rh_ponto (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    funcionario_id TEXT NOT NULL,
                    data TEXT NOT NULL,
                    entrada TEXT,
                    saida TEXT,
                    intervalo_inicio TEXT,
                    intervalo_fim TEXT,
                    horas_trabalhadas REAL,
                    origem TEXT,
                    criado_em TEXT
                );

                CREATE TABLE IF NOT EXISTS rh_escala (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    funcionario_id TEXT NOT NULL,
                    data TEXT NOT NULL,
                    inicio TEXT,
                    fim TEXT,
                    observacoes TEXT,
                    criado_em TEXT
                );

                CREATE TABLE IF NOT EXISTS rh_ferias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    funcionario_id TEXT NOT NULL,
                    data_inicio TEXT NOT NULL,
                    data_fim TEXT NOT NULL,
                    abono INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'programada',
                    observacoes TEXT,
                    criado_em TEXT
                );

                CREATE TABLE IF NOT EXISTS rh_comissoes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venda_id INTEGER,
                    funcionario_id TEXT NOT NULL,
                    percentual REAL,
                    valor REAL,
                    data TEXT,
                    observacoes TEXT,
                    UNIQUE(venda_id, funcionario_id)
                );

                CREATE TABLE IF NOT EXISTS rh_folha (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    competencia TEXT NOT NULL,
                    funcionario_id TEXT NOT NULL,
                    salario_base REAL,
                    horas_trabalhadas REAL,
                    horas_extras REAL,
                    descontos REAL,
                    comissao REAL,
                    total_bruto REAL,
                    total_liquido REAL,
                    status TEXT DEFAULT 'aberta',
                    criado_em TEXT
                );
                """
        ));

        // V014: Metadados de parcelamento e taxas de cartao
        migrations.add(new Migration(
            "014",
            "Adicionar metadados de cartao em vendas_pagamentos",
            "Campos para bandeira, tipo, parcelas, taxa e intervalo de cartao",
            """
                ALTER TABLE vendas ADD COLUMN acrescimo REAL DEFAULT 0;
                ALTER TABLE vendas ADD COLUMN juros REAL;
                ALTER TABLE vendas ADD COLUMN intervalo_dias INTEGER;
                ALTER TABLE vendas_pagamentos ADD COLUMN bandeira TEXT;
                ALTER TABLE vendas_pagamentos ADD COLUMN tipo_cartao TEXT;
                ALTER TABLE vendas_pagamentos ADD COLUMN parcelas INTEGER;
                ALTER TABLE vendas_pagamentos ADD COLUMN intervalo_dias INTEGER;
                ALTER TABLE vendas_pagamentos ADD COLUMN taxa_pct REAL;
                ALTER TABLE vendas_pagamentos ADD COLUMN taxa_valor REAL;
                ALTER TABLE vendas_pagamentos ADD COLUMN taxa_quem TEXT;
                """
        ));

        // V015: Estornos com separacao de taxa
        migrations.add(new Migration(
            "015",
            "Adicionar tipo de estorno e taxa_quem em estornos",
            "Permite separar estorno de valor liquido e taxa de cartao",
            """
                ALTER TABLE vendas_estornos_pagamentos ADD COLUMN tipo_estorno TEXT;
                ALTER TABLE vendas_estornos_pagamentos ADD COLUMN taxa_quem TEXT;
                """
        ));

        // V016: Promo??es completas (categoria, aplica??o e hist?rico)
        migrations.add(new Migration(
            "016",
            "Promo??es: categoria, hist?rico e itens",
            "Adiciona categoria em produtos e promocoes, hist?rico de aplica??o e campos em vendas_itens",
            """
                ALTER TABLE produtos ADD COLUMN categoria TEXT;
                ALTER TABLE promocoes ADD COLUMN categoria TEXT;
                ALTER TABLE promocoes ADD COLUMN ativo INTEGER DEFAULT 1;
                ALTER TABLE promocoes ADD COLUMN prioridade INTEGER DEFAULT 0;

                ALTER TABLE vendas_itens ADD COLUMN promocao_id TEXT;
                ALTER TABLE vendas_itens ADD COLUMN desconto_origem TEXT;
                ALTER TABLE vendas_itens ADD COLUMN desconto_valor REAL;
                ALTER TABLE vendas_itens ADD COLUMN desconto_tipo TEXT;

                CREATE TABLE IF NOT EXISTS promocoes_aplicacoes (
                  id TEXT PRIMARY KEY,
                  promocao_id TEXT,
                  venda_id INTEGER,
                  venda_item_id INTEGER,
                  produto_id TEXT,
                  cliente_id TEXT,
                  qtd INTEGER,
                  preco_original REAL,
                  desconto_valor REAL,
                  preco_final REAL,
                  desconto_tipo TEXT,
                  data_aplicacao TEXT
                );
                """
        ));

        // V017: Persistir pasta do certificado
        migrations.add(new Migration(
            "017",
            "Salvar diret?rio do certificado",
            "Adiciona coluna certificado_dir em config_loja",
            """
                ALTER TABLE config_loja ADD COLUMN certificado_dir TEXT;
                """
        ));

        return migrations;
    }

    /**
     * SQL para migração V007 - Popular referências
     */
    private static String getMigrationV007SQL() {
        return """
            INSERT OR IGNORE INTO unidades (codigo, descricao) VALUES
                ('UN', 'Unidade'),
                ('KG', 'Quilograma'),
                ('L', 'Litro'),
                ('M', 'Metro'),
                ('M2', 'Metro Quadrado'),
                ('CX', 'Caixa'),
                ('DZ', 'Dúzia'),
                ('PCT', 'Pacote'),
                ('HR', 'Hora');
            
            INSERT OR IGNORE INTO origem (codigo, descricao) VALUES
                ('0', 'Nacional'),
                ('1', 'Importado'),
                ('2', 'Nacional com conteúdo importado'),
                ('3', 'Nacional, com fração de importado'),
                ('4', 'Nacional, conforme lei complementar'),
                ('5', 'Importado, com fração de nacional'),
                ('6', 'Importado, conforme lei complementar'),
                ('7', 'Armazenado nacional'),
                ('8', 'Armazenado importado');
            
            INSERT OR IGNORE INTO cfop (codigo, descricao) VALUES
                ('5102', 'Venda para Consumidor Final'),
                ('5101', 'Venda ao Contribuinte'),
                ('6102', 'Devolução de Venda para Consumidor Final'),
                ('6101', 'Devolução de Venda ao Contribuinte');
            
            INSERT OR IGNORE INTO csosn (codigo, descricao) VALUES
                ('102', 'Tributada pelo Simples Nacional sem Permissão de Crédito'),
                ('103', 'Isenção do ICMS no Simples Nacional'),
                ('300', 'Imunidade do ICMS'),
                ('400', 'Não Tributada pelo ICMS'),
                ('500', 'ICMS Cobrado Anteriormente por ST'),
                ('900', 'Outros');
            
            INSERT OR IGNORE INTO config_nfce (id, emitir_nfce, ambiente, regime_tributario)
            VALUES ('CONFIG_PADRAO', 1, 'homologacao', 'Simples Nacional');
            
            INSERT OR IGNORE INTO sequencias_fiscais (id, modelo, codigo_modelo, serie, ambiente, ultimo_numero, criado_em)
            VALUES
                ('NFCe-65-SERIE-1-OFF', 'NFCe', 65, 1, 'OFF', 0, datetime('now')),
                ('NFCe-65-SERIE-1-HOMOLOG', 'NFCe', 65, 1, 'HOMOLOG', 0, datetime('now')),
                ('NFCe-65-SERIE-1-PRODUCAO', 'NFCe', 65, 1, 'PRODUCAO', 0, datetime('now'));
            """;
    }

    /**
     * Classe interna para representar uma migração
     */
    private static class Migration {
        String version;
        String name;
        String description;
        String sql;

        Migration(String version, String name, String description, String sql) {
            this.version = version;
            this.name = name;
            this.description = description;
            this.sql = sql;
        }
    }
}
