package util;

import java.sql.*;
import java.util.List;
import java.io.File;
import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;

import java.util.UUID;

import model.ColecaoModel;
import model.SetModel;

import model.SetJogoModel;
import dao.SetJogoDAO;
import service.SetJogoService;

public class DB {

    private static final String DATA_DIR = System.getProperty("user.dir") + File.separator + "data";
    private static final String DB_PATH = DATA_DIR + File.separator + "hostore.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    static {
        prepararBancoSeNecessario();
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void prepararBancoSeNecessario() {
        showUserFeedback("Inicializando", "Verificando banco de dados...", false);
        try {
            ensureDataDirectoryExists();
            boolean existedBefore = databaseFileExists();
            if (!existedBefore) {
                System.out.println("Banco de dados não encontrado. Criando novo banco e tabelas...");
            } else {
                System.out.println("Banco de dados encontrado. Verificando/atualizando tabelas...");
            }
            init();
            criarUsuarioAdminPadraoSeNecessario();
            if (!GraphicsEnvironment.isHeadless()) {
                if (!existedBefore) {
                    showUserFeedback("Instalação concluída", "Banco de dados criado com sucesso e povoado.", false);
                } else {
                    showUserFeedback("Verificação concluída", "Banco de dados verificado e atualizado.", false);
                }
            }
        } catch (Exception e) {
            String erro = "Falha ao preparar o banco de dados: " + e.getMessage();
            System.err.println(erro);
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
                throw new SQLException("Não foi possível criar o diretório de dados: " + dir.getAbsolutePath());
            }
        }
    }

    private static boolean databaseFileExists() {
        File db = new File(DB_PATH);
        return db.exists();
    }

    public static void criarUsuarioAdminPadraoSeNecessario() {
        try (Connection conn = get(); Statement st = conn.createStatement()) {
            // garante que a tabela existe (caso tenha falhado no init anterior)
            st.execute("CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id TEXT PRIMARY KEY, nome TEXT NOT NULL, usuario TEXT NOT NULL UNIQUE, senha TEXT NOT NULL, tipo TEXT NOT NULL, ativo INTEGER NOT NULL DEFAULT 1" +
                    ")");

            try (Statement stCheck = conn.createStatement();
                 ResultSet rs = stCheck.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String id = UUID.randomUUID().toString();
                    String nome = "Administrador";
                    String usuario = "admin";
                    String senha = hashSenha("admin123");
                    String tipo = "Admin";
                    String sql = "INSERT INTO usuarios (id, nome, usuario, senha, tipo, ativo) VALUES (?,?,?,?,?,1)";
                    try (PreparedStatement p = conn.prepareStatement(sql)) {
                        p.setString(1, id);
                        p.setString(2, nome);
                        p.setString(3, usuario);
                        p.setString(4, senha);
                        p.setString(5, tipo);
                        p.executeUpdate();
                        System.out.println("✅ Usuário padrão 'admin' criado (senha: admin123)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro criando ou verificando usuário admin padrão:");
            e.printStackTrace();
        }
    }

    private static void showUserFeedback(String title, String message, boolean isError) {
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                if (isError) {
                    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ignored) {
                if (isError) System.err.println(title + ": " + message);
                else System.out.println(title + ": " + message);
            }
        } else {
            if (isError) System.err.println(title + ": " + message);
            else System.out.println(title + ": " + message);
        }
    }

    /** Cria todas as tabelas se não existirem */
    private static void init() {
        try (Connection c = get();
             Statement st = c.createStatement()) {

            // clientes
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
                ")", "clientes");

            // vendas
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
                ")", "vendas");

            // itens de venda
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
                ")", "vendas_itens");

            // @CREATE_TABLE: vendas_estornos_pagamentos
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
                ")", "vendas_estornos_pagamentos");

            // pagamentos de venda
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS vendas_pagamentos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "venda_id INTEGER NOT NULL, " +
                    "tipo TEXT NOT NULL, " + // ex: pix, dinheiro, cartão
                    "valor REAL NOT NULL, " +
                    "FOREIGN KEY (venda_id) REFERENCES vendas(id)" +
                ")", "vendas_pagamentos");

            // vendas_devolucoes
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
                ")", "vendas_devolucoes");

            // condicoes
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS condicoes (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "condicoes");

            // linguagens
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS linguagens (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "linguagens");

            // tipo_cartas
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS tipo_cartas (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "tipo_cartas");

            // subtipo_cartas
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS subtipo_cartas (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "subtipo_cartas");

            // raridades
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS raridades (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "raridades");

            // sub_raridades
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS sub_raridades (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "sub_raridades");

            // ilustracoes
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS ilustracoes (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL" +
                ")", "ilustracoes");

            // cartas
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
                ")", "cartas");

            // produtos (estoque geral) - corrected version with FK in parentheses
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS produtos (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL, " +
                    "jogo_id TEXT, " +
                    "tipo TEXT NOT NULL, " +
                    "quantidade INTEGER NOT NULL, " +
                    "preco_compra REAL, " +
                    "preco_venda REAL, " +
                    "codigo_barras TEXT, " +
                    "ncm TEXT, " +
                    "lucro REAL GENERATED ALWAYS AS (preco_venda - preco_compra) VIRTUAL, " +
                    "criado_em TEXT, " +
                    "alterado_em TEXT, " +
                    "fornecedor_id TEXT, " +
                    "FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id)" +
                ")", "produtos");

            // boosters
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
                ")", "boosters");

            // fornecedores
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
                ")", "fornecedores");

            // sets (Pokémon)
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS sets(" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT, " +
                    "series TEXT, " +
                    "colecao_id TEXT, " +
                    "data_lancamento TEXT" +
                ")", "sets");

            // colecoes
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS colecoes(" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT, " +
                    "sigla TEXT, " +
                    "codigo TEXT, " +
                    "data_lancamento TEXT, " +
                    "series TEXT, " +
                    "observacoes TEXT, " +
                    "FOREIGN KEY(series) REFERENCES sets(id)" +
                ")", "colecoes");

            // decks
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
                ")", "decks");

            // etbs
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
                ")", "etbs");

            // acessorios
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
                ")", "acessorios");

            // produtos_alimenticios
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
                ")", "produtos_alimenticios");

            // formas_pagamento
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS formas_pagamento(" +
                    "id TEXT PRIMARY KEY, nome TEXT, taxa REAL DEFAULT 0)", "formas_pagamento");

            // categorias_produtos
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS categorias_produtos(" +
                    "id TEXT PRIMARY KEY, nome TEXT, descricao TEXT)", "categorias_produtos");

            // estoque_movimentacoes
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS estoque_movimentacoes(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, produto_id TEXT, tipo_mov TEXT," +
                    "quantidade INTEGER, motivo TEXT, data TEXT, usuario TEXT," +
                    "FOREIGN KEY(produto_id) REFERENCES produtos(id))", "estoque_movimentacoes");

            // titulos_contas_receber
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
                ")", "titulos_contas_receber");

            // config_loja
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
                ");", "config_loja");

            // parcelas_contas_receber
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
                ")", "parcelas_contas_receber");

            // pagamentos_contas_receber
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS pagamentos_contas_receber (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "parcela_id INTEGER, " +
                    "forma_pagamento TEXT, " +
                    "valor_pago REAL, " +
                    "data_pagamento TEXT, " +
                    "FOREIGN KEY(parcela_id) REFERENCES parcelas_contas_receber(id)" +
                ")", "pagamentos_contas_receber");

            // taxas_cartao
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
                ")", "taxas_cartao");

            // pedidos_compras
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS pedidos_compras (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT, " +
                    "data TEXT, " +
                    "status TEXT, " +
                    "fornecedor_id TEXT, " +
                    "observacoes TEXT, " +
                    "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                ")", "pedidos_compras");

            // pedido_produtos
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS pedido_produtos (" +
                    "id TEXT PRIMARY KEY, " +
                    "pedido_id TEXT NOT NULL, " +
                    "produto_id TEXT NOT NULL, " +
                    "quantidade_pedida INTEGER NOT NULL, " +
                    "quantidade_recebida INTEGER DEFAULT 0, " +
                    "status TEXT DEFAULT 'pendente', " +
                    "FOREIGN KEY(pedido_id) REFERENCES pedidos_compras(id), " +
                    "FOREIGN KEY(produto_id) REFERENCES produtos(id)" +
                ")", "pedido_produtos");

            // titulos_contas_pagar
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
                ")", "titulos_contas_pagar");

            // parcelas_contas_pagar
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
                ")", "parcelas_contas_pagar");

            // pagamentos_contas_pagar
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS pagamentos_contas_pagar (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "parcela_id INTEGER, " +
                    "forma_pagamento TEXT, " +
                    "valor_pago REAL, " +
                    "data_pagamento TEXT, " +
                    "FOREIGN KEY(parcela_id) REFERENCES parcelas_contas_pagar(id)" +
                ")", "pagamentos_contas_pagar");

            // contas_pagar_pedidos - corrected version
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS contas_pagar_pedidos (" +
                    "conta_pagar_id TEXT NOT NULL, " +
                    "pedido_id TEXT NOT NULL, " +
                    "PRIMARY KEY (conta_pagar_id, pedido_id), " +
                    "FOREIGN KEY (conta_pagar_id) REFERENCES titulos_contas_pagar(id), " +
                    "FOREIGN KEY (pedido_id) REFERENCES pedidos_compras(id)" +
                ")", "contas_pagar_pedidos");

            // planos_contas
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS planos_contas (" +
                    "id TEXT PRIMARY KEY, " +
                    "codigo TEXT NOT NULL, " +
                    "descricao TEXT NOT NULL, " +
                    "tipo TEXT NOT NULL, " +
                    "parent_id TEXT, " +
                    "observacoes TEXT, " +
                    "FOREIGN KEY(parent_id) REFERENCES planos_contas(id)" +
                ")", "planos_contas");

            // bancos
            executeComLog(st, """
                CREATE TABLE IF NOT EXISTS bancos (
                    id TEXT PRIMARY KEY,
                    nome TEXT NOT NULL,
                    agencia TEXT,
                    conta TEXT,
                    observacoes TEXT
                )
            """, "bancos");

            // usuarios (canonical definition, only here)
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id TEXT PRIMARY KEY, nome TEXT NOT NULL, usuario TEXT NOT NULL UNIQUE, senha TEXT NOT NULL, tipo TEXT NOT NULL, ativo INTEGER NOT NULL DEFAULT 1" +
                ")", "usuarios");

            // logs_acessos
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS logs_acessos(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id TEXT, data TEXT," +
                    "tipo TEXT, descricao TEXT, FOREIGN KEY(usuario_id) REFERENCES usuarios(id))", "logs_acessos");

            // promocoes
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
                ")", "promocoes");

            // tipos_promocao
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS tipos_promocao (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT NOT NULL, " +
                    "descricao TEXT" +
                ")", "tipos_promocao");

            // promocao_produtos
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS promocao_produtos (" +
                    "id TEXT PRIMARY KEY, " +
                    "promocao_id TEXT, " +
                    "produto_id TEXT, " +
                    "FOREIGN KEY(promocao_id) REFERENCES promocoes(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(produto_id) REFERENCES produtos(id) ON DELETE CASCADE" +
                ")", "promocao_produtos");

            // clientes_vip
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS clientes_vip (" +
                    "id TEXT PRIMARY KEY, " +
                    "nome TEXT, " +
                    "cpf TEXT, " +
                    "telefone TEXT, " +
                    "categoria TEXT, " +
                    "criado_em TEXT, " +
                    "observacoes TEXT)", "clientes_vip");

            // credito_loja
            executeComLog(st, """
                CREATE TABLE IF NOT EXISTS credito_loja (
                    id TEXT PRIMARY KEY,
                    cliente_id TEXT NOT NULL,
                    valor REAL NOT NULL DEFAULT 0,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
            """, "credito_loja");

            // credito_loja_movimentacoes
            executeComLog(st, """
                CREATE TABLE IF NOT EXISTS credito_loja_movimentacoes (
                    id TEXT PRIMARY KEY,
                    cliente_id TEXT NOT NULL,
                    valor REAL NOT NULL,
                    tipo TEXT NOT NULL,
                    referencia TEXT,
                    data TEXT NOT NULL,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
            """, "credito_loja_movimentacoes");

            /* ─────────── POVOAMENTO INICIAL (INSERT OR IGNORE) ─────────── */
            executeComLog(st, "INSERT OR IGNORE INTO tipo_cartas (id,nome) VALUES " +
                "('T1','Pokémon')," +
                "('T2','Treinador')," +
                "('T3','Energia')", "insert_tipo_cartas");

            executeComLog(st, "INSERT OR IGNORE INTO subtipo_cartas (id,nome) VALUES " +
                "('S1','Básico'),('S2','Estágio 1'),('S3','Estágio 2')," +
                "('S4','Item'),('S5','Suporte'),('S6','Estádio'),('S7','Ferramenta')," +
                "('S8','Água'),('S9','Fogo'),('S10','Grama'),('S11','Elétrico'),('S12','Lutador')," +
                "('S13','Noturno'),('S14','Psíquico'),('S15','Metálico'),('S16','Dragão'),('S17','Incolor')", "insert_subtipo_cartas");

            executeComLog(st, "INSERT OR IGNORE INTO raridades (id,nome) VALUES " +
                "('R1','Comum'),('R2','Incomum'),('R3','Rara'),('R4','Promo')," +
                "('R5','Foil'),('R6','Foil Reverse'),('R7','Secreta')", "insert_raridades");

            executeComLog(st, "INSERT OR IGNORE INTO sub_raridades (id,nome) VALUES " +
                "('SR1','EX'),('SR2','GX'),('SR3','V'),('SR4','VMAX'),('SR5','VSTAR'),('SR6','TERA')", "insert_sub_raridades");

            executeComLog(st, "INSERT OR IGNORE INTO ilustracoes (id,nome) VALUES " +
                "('IL1','Regular'),('IL2','Full Art'),('IL3','Secreta')", "insert_ilustracoes");

            // jogos
            executeComLog(st, """
                CREATE TABLE IF NOT EXISTS jogos (
                    id TEXT PRIMARY KEY,
                    nome TEXT NOT NULL
                )
            """, "jogos");
            executeComLog(st, """
                INSERT OR IGNORE INTO jogos (id, nome) VALUES
                    ('POKEMON', 'Pokémon TCG'),
                    ('YUGIOH', 'Yu-Gi-Oh!'),
                    ('MAGIC', 'Magic: The Gathering'),
                    ('ONEPIECE', 'One Piece Card Game'),
                    ('DIGIMON', 'Digimon Card Game'),
                    ('DRAGONBALL', 'Dragon Ball Super Card Game')
            """, "insert_jogos");

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

            // sets_jogos (corrected version)
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

            // FISCAL TABLES
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS ncm (" +
                    "codigo TEXT PRIMARY KEY, " +
                    "descricao TEXT NOT NULL" +
                ");", "ncm");
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS cfop (" +
                    "codigo TEXT PRIMARY KEY, " +
                    "descricao TEXT NOT NULL" +
                ");", "cfop");
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS csosn (" +
                    "codigo TEXT PRIMARY KEY, " +
                    "descricao TEXT NOT NULL" +
                ");", "csosn");
            executeComLog(st,
                "CREATE TABLE IF NOT EXISTS origem (" +
                    "codigo TEXT PRIMARY KEY, " +
                    "descricao TEXT NOT NULL" +
                ");", "origem");
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
                    "FOREIGN KEY(cliente_id) REFERENCES clientes(id) " +
                ");", "config_fiscal");

            // Dados de fallback para tabelas fiscais (valores iniciais de contingência)
            executeComLog(st, "INSERT OR IGNORE INTO cfop (codigo, descricao) VALUES " +
                "('5101','Venda de produção do estabelecimento')," +
                "('5102','Venda de mercadoria adquirida ou recebida de terceiros');", "fallback_cfop");
            executeComLog(st, "INSERT OR IGNORE INTO csosn (codigo, descricao) VALUES " +
                "('102','Tributada pelo Simples Nacional sem permissão de crédito')," +
                "('500','ICMS cobrado anteriormente por substituição tributária');", "fallback_csosn");
            executeComLog(st, "INSERT OR IGNORE INTO origem (codigo, descricao) VALUES " +
                "('0','Nacional, exceto as indicadas nos códigos 3 a 5')," +
                "('1','Estrangeira – Importação direta, exceto a indicada no código 6')," +
                "('2','Estrangeira – Adquirida no mercado interno, exceto a indicada no código 7');", "fallback_origem");

            // ─────────── SINCRONIZAÇÃO FISCAL AUTOMÁTICA ───────────
                        try {

                                // CFOP
                                try {
                                        List<model.CfopModel> listaCfops = service.FiscalApiService.listarCfops();
                                        dao.CfopDAO cfopDAO = new dao.CfopDAO();
                                        cfopDAO.sincronizarComApi(listaCfops);
                                } catch (Exception e) {
                                        System.err.println("⚠ Falha na API de CFOP. Usando fallback...");
                                        try (Connection conn = get(); Statement stcfop = conn.createStatement()) {
                                                stcfop.execute("INSERT OR IGNORE INTO cfop (codigo, descricao) VALUES "
                                                                +
                                                                "('5101','Venda de produção do estabelecimento')," +
                                                                "('5102','Venda de mercadoria adquirida ou recebida de terceiros');");
                                        }
                                }

                                // CSOSN
                                try {
                                        List<model.CsosnModel> listaCsosns = service.FiscalApiService.listarCsosns();
                                        dao.CsosnDAO csosnDAO = new dao.CsosnDAO();
                                        csosnDAO.sincronizarComApi(listaCsosns);
                                } catch (Exception e) {
                                        System.err.println("⚠ Falha na API de CSOSN. Usando fallback...");
                                        try (Connection conn = get(); Statement stcsosn = conn.createStatement()) {
                                                stcsosn.execute("INSERT OR IGNORE INTO csosn (codigo, descricao) VALUES "
                                                                +
                                                                "('102','Tributada pelo Simples Nacional sem permissão de crédito'),"
                                                                +
                                                                "('500','ICMS cobrado anteriormente por substituição tributária');");
                                        }
                                }

                                // Origem
                                try {
                                        List<model.OrigemModel> listaOrigens = service.FiscalApiService.listarOrigens();
                                        dao.OrigemDAO origemDAO = new dao.OrigemDAO();
                                        origemDAO.sincronizarComApi(listaOrigens);
                                } catch (Exception e) {
                                        System.err.println("⚠ Falha na API de Origem. Usando fallback...");
                                        try (Connection conn = get(); Statement storg = conn.createStatement()) {
                                                storg.execute("INSERT OR IGNORE INTO origem (codigo, descricao) VALUES "
                                                                +
                                                                "('0','Nacional, exceto as indicadas nos códigos 3 a 5'),"
                                                                +
                                                                "('1','Estrangeira – Importação direta, exceto a indicada no código 6'),"
                                                                +
                                                                "('2','Estrangeira – Adquirida no mercado interno, exceto a indicada no código 7');");
                                        }
                                }

                                System.out.println("✅ Dados fiscais sincronizados com sucesso (com ou sem fallback)");

                        } catch (Exception ex) {
                                System.err.println("Erro geral na sincronização fiscal:");
                                ex.printStackTrace();
                        }

                        try {
                                dao.ColecaoDAO colecaoDAO = new dao.ColecaoDAO();
                                List<ColecaoModel> colecoes = service.ColecaoService.listarColecoes();
                                colecaoDAO.sincronizarComApi(colecoes);

                                dao.SetDAO setDAO = new dao.SetDAO();
                                List<SetModel> sets = service.SetService.listarSets();
                                setDAO.sincronizarComApi(sets);

                                System.out.println("✅ Coleções e Sets sincronizados com sucesso.");
                        } catch (Exception ex) {
                                System.err.println("Erro ao sincronizar dados da API Pokémon:");
                                ex.printStackTrace();
                        }

                        try {
                                dao.SetJogoDAO setJogoDAO = new dao.SetJogoDAO();

                                List<model.SetJogoModel> ygoSets = service.SetJogoService.listarSetsYugioh();
                                setJogoDAO.sincronizarComApi(ygoSets);

                                List<model.SetJogoModel> magicSets = service.SetJogoService.listarSetsMagic();
                                setJogoDAO.sincronizarComApi(magicSets);

                                List<model.SetJogoModel> digimonSets = service.SetJogoService.listarSetsDigimon();
                                setJogoDAO.sincronizarComApi(digimonSets);

                                List<model.SetJogoModel> onePieceSets = service.SetJogoService.listarSetsOnePiece();
                                setJogoDAO.sincronizarComApi(onePieceSets);

                                System.out.println("✅ Sets dos jogos TCG (exceto Pokémon) sincronizados com sucesso.");
                        } catch (Exception ex) {
                                System.err.println("Erro ao sincronizar sets_jogos:");
                                ex.printStackTrace();
                        }

        } catch (SQLException e) {
            System.err.println("Erro ao criar tabelas no banco de dados:");
            e.printStackTrace();
        }
    }

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

        /** Executa o comando SQL e loga erro por tabela (não aborta) */
        private static void executeComLog(Statement st, String sql, String nome) {
            try {
                st.execute(sql);
            } catch (SQLException e) {
                System.err.println("Erro ao criar/atualizar tabela ou inserir dados: " + nome);
                e.printStackTrace();
            }
        }

        public static boolean isConnected() {
            try (Connection c = get()) {
                return c != null && !c.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }

}
