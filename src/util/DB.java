package util;

import java.sql.*;
import java.util.List;

import model.ColecaoModel;
import model.SetModel;

public class DB {

        private static final String URL = "jdbc:sqlite:data/hostore.db";

        static {
                init();
        }

        public static Connection get() throws SQLException {
                return DriverManager.getConnection(URL);
        }

        /** Cria todas as tabelas se não existirem */
        private static void init() {
                try (Connection c = get();
                                Statement st = c.createStatement()) {

                        // clientes
                        st.execute(
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
                                                        ")");

                        // vendas
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS vendas (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "cliente_id TEXT NOT NULL, " +
                                                        "data_venda TEXT NOT NULL, " +
                                                        "forma_pagamento TEXT NOT NULL, " +
                                                        "parcelas INTEGER DEFAULT 1, " +
                                                        "desconto REAL DEFAULT 0, " +
                                                        "total_bruto REAL NOT NULL, " +
                                                        "total_liquido REAL NOT NULL, " +
                                                        "status TEXT DEFAULT 'fechada', " + // << AQUI
                                                        "criado_em TEXT, " +
                                                        "criado_por TEXT, " +
                                                        "FOREIGN KEY (cliente_id) REFERENCES clientes(id)" +
                                                        ")");

                        // itens de venda
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS vendas_itens (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "carta_id TEXT NOT NULL, " +
                                                        "qtd INTEGER NOT NULL, " +
                                                        "preco REAL NOT NULL, " +
                                                        "desconto REAL DEFAULT 0, " + // % individual do item
                                                        "total_item REAL NOT NULL, " +
                                                        "FOREIGN KEY (venda_id) REFERENCES vendas(id)" +
                                                        ")");

                        // tipos de condição (novo, usado, etc.)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS condicoes (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // linguagens de impressão/edição
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS linguagens (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // tipo de carta (treinador, pokemon, energia)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS tipo_cartas (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // subtipo de carta (suporte, item, estadio, etc.)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS subtipo_cartas (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // raridade (rara, incomum, comum, etc.)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS raridades (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // sub-raridade (reverse, comum, etc.)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS sub_raridades (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // ilustração (Regular, Full Art, Secreta)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS ilustracoes (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL" +
                                                        ")");

                        // cartas (entidade principal)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS cartas (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "set_id TEXT, " + // nova coluna para Set/Series
                                                        "colecao TEXT, " +
                                                        "numero TEXT, " +
                                                        "qtd INTEGER, " +
                                                        "preco REAL, " +
                                                        "preco_loja REAL, " + // renomeado de preco
                                                        "preco_consignado REAL, " + // preço se consignado
                                                        "percentual_loja REAL, " + // % que a loja recebe em consignado
                                                        "valor_loja REAL, " + // valor que a loja recebe calculado
                                                        "custo REAL, " + // custo de aquisição
                                                        "condicao_id TEXT, " +
                                                        "linguagem_id TEXT, " +
                                                        "consignado INTEGER DEFAULT 0, " + // 0=loja,1=consignado
                                                        "dono TEXT, " + // id do cliente/filial
                                                        "tipo_id TEXT, " +
                                                        "subtipo_id TEXT, " +
                                                        "raridade_id TEXT, " +
                                                        "sub_raridade_id TEXT, " +
                                                        "ilustracao_id TEXT, " +
                                                        "fornecedor_id TEXT, " + // vincula ao fornecedor
                                                        // chaves estrangeiras
                                                        "FOREIGN KEY(set_id) REFERENCES sets(id), " +
                                                        "FOREIGN KEY(condicao_id) REFERENCES condicoes(id), " +
                                                        "FOREIGN KEY(linguagem_id) REFERENCES linguagens(id), " +
                                                        "FOREIGN KEY(tipo_id) REFERENCES tipo_cartas(id), " +
                                                        "FOREIGN KEY(subtipo_id) REFERENCES subtipo_cartas(id), " +
                                                        "FOREIGN KEY(raridade_id) REFERENCES raridades(id), " +
                                                        "FOREIGN KEY(sub_raridade_id) REFERENCES sub_raridades(id), " +
                                                        "FOREIGN KEY(ilustracao_id) REFERENCES ilustracoes(id), " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")");

                        // produtos (estoque geral)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS produtos (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " +
                                                        "quantidade INTEGER NOT NULL, " +
                                                        "preco_compra REAL, " +
                                                        "preco_venda REAL, " +
                                                        "lucro REAL GENERATED ALWAYS AS (preco_venda - preco_compra) VIRTUAL, "
                                                        +
                                                        "criado_em TEXT, " +
                                                        "alterado_em TEXT" +
                                                        ")");

                        // detalhes extras por categoria (guarda campos específicos)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS boosters (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "serie TEXT, " +
                                                        "colecao TEXT, " +
                                                        "tipo TEXT, " +
                                                        "idioma TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "quantidade INTEGER, " +
                                                        "custo REAL, " +
                                                        "preco_venda REAL, " +
                                                        "fornecedor_id TEXT, " +
                                                        "data_lancamento TEXT, " + // ✅ coluna que estava faltando
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")");

                        // Fornecedores
                        st.execute(
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
                                                        ")");

                        // Sets Pokémon (séries principais)
                        st.execute("CREATE TABLE IF NOT EXISTS sets(" +
                                        "id TEXT PRIMARY KEY, " +
                                        "nome TEXT, " +
                                        "series TEXT, " + // <- AQUI ESTAVA FALTANDO
                                        "colecao_id TEXT, " + // <- Se quiser manter
                                        "data_lancamento TEXT" +
                                        ")");

                        // Coleções Pokémon (vinculadas a uma série/set)
                        st.execute("CREATE TABLE IF NOT EXISTS colecoes(" +
                                        "id TEXT PRIMARY KEY, " + // ex: sv9
                                        "nome TEXT, " + // ex: Temporal Forces
                                        "codigo TEXT, " + // pode ser redundante, mas útil
                                        "data_lancamento TEXT, " +
                                        "series TEXT, " + // nome do set/série
                                        "observacoes TEXT, " +
                                        "FOREIGN KEY(series) REFERENCES sets(id)" +
                                        ")");

                        // decks (detalhes de decks)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS decks (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor TEXT, " + // pode ser ID ou nome
                                                        "colecao TEXT, " +
                                                        "tipo_deck TEXT, " +
                                                        "categoria TEXT, " +
                                                        "FOREIGN KEY(id) REFERENCES produtos(id) ON DELETE CASCADE" +
                                                        ")");

                        // etbs (entidade com detalhes de selados tipo ETB)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS etbs (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor TEXT, " +
                                                        "serie TEXT, " +
                                                        "colecao TEXT, " +
                                                        "tipo TEXT, " +
                                                        "versao TEXT, " +
                                                        "FOREIGN KEY(id) REFERENCES produtos(id) ON DELETE CASCADE" +
                                                        ")");

                        // acessórios (detalhes por tipo)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS acessorios (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " + // ex: Sleeve, Playmat, etc
                                                        "arte TEXT, " + // ex: Pokémon, Treinador, Outros, Cor Única
                                                        "cor TEXT, " + // usada apenas se arte = Cor Única
                                                        "quantidade INTEGER NOT NULL, " +
                                                        "custo REAL, " +
                                                        "preco_venda REAL, " +
                                                        "fornecedor_id TEXT, " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")");

                        // ─────────── PRODUTOS ALIMENTÍCIOS ───────────
                        st.execute(
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
                                                        ")");
                        // ─────────────────────────────────────────────────

                        // Formas de Pagamento
                        st.execute("CREATE TABLE IF NOT EXISTS formas_pagamento(" +
                                        "id TEXT PRIMARY KEY, nome TEXT, taxa REAL DEFAULT 0)");

                        // Categorias Produtos
                        st.execute("CREATE TABLE IF NOT EXISTS categorias_produtos(" +
                                        "id TEXT PRIMARY KEY, nome TEXT, descricao TEXT)");

                        // Movimentações Estoque
                        st.execute("CREATE TABLE IF NOT EXISTS estoque_movimentacoes(" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, produto_id TEXT, tipo_mov TEXT," +
                                        "quantidade INTEGER, motivo TEXT, data TEXT, usuario TEXT," +
                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id))");

                        // Contas a Receber
                        st.execute("CREATE TABLE IF NOT EXISTS contas_receber(" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id TEXT, valor REAL," +
                                        "vencimento TEXT, pagamento TEXT, status TEXT," +
                                        "FOREIGN KEY(cliente_id) REFERENCES clientes(id))");

                        // Contas a Pagar
                        st.execute("CREATE TABLE IF NOT EXISTS contas_pagar(" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, fornecedor_id TEXT, valor REAL," +
                                        "vencimento TEXT, pagamento TEXT, status TEXT," +
                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id))");

                        // Usuários do Sistema
                        st.execute("CREATE TABLE IF NOT EXISTS usuarios(" +
                                        "id TEXT PRIMARY KEY, nome TEXT, usuario TEXT, senha TEXT, tipo TEXT, ativo INTEGER DEFAULT 1)");

                        // Logs Acessos
                        st.execute("CREATE TABLE IF NOT EXISTS logs_acessos(" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id TEXT, data TEXT," +
                                        "tipo TEXT, descricao TEXT, FOREIGN KEY(usuario_id) REFERENCES usuarios(id))");

                        // Promoções (versão final correta)
                        st.execute("CREATE TABLE IF NOT EXISTS promocoes (" +
                                        "id TEXT PRIMARY KEY, " +
                                        "nome TEXT, " +
                                        "desconto REAL, " +
                                        "data_inicio TEXT, " +
                                        "data_fim TEXT, " +
                                        "observacoes TEXT, " +
                                        "tipo_id TEXT, " +
                                        "FOREIGN KEY(tipo_id) REFERENCES tipos_promocao(id)" +
                                        ")");

                        // Tipos de Promoção
                        st.execute("CREATE TABLE IF NOT EXISTS tipos_promocao (" +
                                        "id TEXT PRIMARY KEY, " +
                                        "nome TEXT NOT NULL, " +
                                        "descricao TEXT" +
                                        ")");

                        // Relação entre promoção e produtos
                        st.execute("CREATE TABLE IF NOT EXISTS promocao_produtos (" +
                                        "id TEXT PRIMARY KEY, " +
                                        "promocao_id TEXT, " +
                                        "produto_id TEXT, " +
                                        "FOREIGN KEY(promocao_id) REFERENCES promocoes(id) ON DELETE CASCADE, " +
                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id) ON DELETE CASCADE" +
                                        ")");

                        // Clientes VIP
                        st.execute("CREATE TABLE IF NOT EXISTS clientes_vip (" +
                                        "id TEXT PRIMARY KEY, " +
                                        "nome TEXT, " +
                                        "cpf TEXT, " +
                                        "telefone TEXT, " +
                                        "categoria TEXT, " +
                                        "criado_em TEXT, " +
                                        "observacoes TEXT)");

                        // usuários
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS usuarios (" +
                                                        "  id TEXT PRIMARY KEY, " +
                                                        "  nome TEXT NOT NULL, " +
                                                        "  usuario TEXT NOT NULL UNIQUE, " +
                                                        "  senha TEXT NOT NULL, " +
                                                        "  tipo TEXT NOT NULL, " +
                                                        "  ativo INTEGER NOT NULL DEFAULT 1" +
                                                        ")");

                        /* ─────────── POVOAMENTO INICIAL (INSERT OR IGNORE) ─────────── */

                        // TIPOS
                        st.execute("INSERT OR IGNORE INTO tipo_cartas (id,nome) VALUES " +
                                        "('T1','Pokémon')," +
                                        "('T2','Treinador')," +
                                        "('T3','Energia')");

                        // SUBTIPOS
                        st.execute("INSERT OR IGNORE INTO subtipo_cartas (id,nome) VALUES " +
                                        "('S1','Básico'),('S2','Estágio 1'),('S3','Estágio 2')," +
                                        "('S4','Item'),('S5','Suporte'),('S6','Estádio'),('S7','Ferramenta')," +
                                        "('S8','Água'),('S9','Fogo'),('S10','Grama'),('S11','Elétrico'),('S12','Lutador'),"
                                        +
                                        "('S13','Noturno'),('S14','Psíquico'),('S15','Metálico'),('S16','Dragão'),('S17','Incolor')");

                        // RARIDADES
                        st.execute("INSERT OR IGNORE INTO raridades (id,nome) VALUES " +
                                        "('R1','Comum'),('R2','Incomum'),('R3','Rara'),('R4','Promo')," +
                                        "('R5','Foil'),('R6','Foil Reverse'),('R7','Secreta')");

                        // SUB-RARIDADES
                        st.execute("INSERT OR IGNORE INTO sub_raridades (id,nome) VALUES " +
                                        "('SR1','EX'),('SR2','GX'),('SR3','V'),('SR4','VMAX'),('SR5','VSTAR'),('SR6','TERA')");

                        // ILUSTRAÇÕES
                        st.execute("INSERT OR IGNORE INTO ilustracoes (id,nome) VALUES " +
                                        "('IL1','Regular'),('IL2','Full Art'),('IL3','Secreta')");

                        // No final do método init() em util/DB.java
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

                        // Cria usuário admin padrão se for o primeiro acesso
                        try (Connection conn = get();
                                        Statement stCheck = conn.createStatement();
                                        ResultSet rs = stCheck.executeQuery("SELECT COUNT(*) FROM usuarios")) {

                                if (rs.next() && rs.getInt(1) == 0) {
                                        String id = java.util.UUID.randomUUID().toString();
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

                        } catch (Exception ex) {
                                System.err.println("Erro ao verificar/criar usuário admin:");
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
}
