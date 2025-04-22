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
                                                        "colecao TEXT, " +
                                                        "numero TEXT, " +
                                                        "qtd INTEGER, " +
                                                        "preco REAL, " +
                                                        "condicao_id TEXT, " +
                                                        "custo REAL, " +
                                                        "linguagem_id TEXT, " +
                                                        "consignado INTEGER DEFAULT 0, " + // 0 = loja, 1 = consignado
                                                        "dono TEXT, " + // cliente_id ou id da filial
                                                        "tipo_id TEXT, " +
                                                        "subtipo_id TEXT, " +
                                                        "raridade_id TEXT, " +
                                                        "sub_raridade_id TEXT, " +
                                                        "ilustracao_id TEXT, " +
                                                        "FOREIGN KEY(condicao_id) REFERENCES condicoes(id), " +
                                                        "FOREIGN KEY(linguagem_id) REFERENCES linguagens(id), " +
                                                        "FOREIGN KEY(tipo_id) REFERENCES tipo_cartas(id), " +
                                                        "FOREIGN KEY(subtipo_id) REFERENCES subtipo_cartas(id), " +
                                                        "FOREIGN KEY(raridade_id) REFERENCES raridades(id), " +
                                                        "FOREIGN KEY(sub_raridade_id) REFERENCES sub_raridades(id), " +
                                                        "FOREIGN KEY(ilustracao_id) REFERENCES ilustracoes(id)" +
                                                        ")");

                        // produtos (estoque geral)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS produtos (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT NOT NULL, " +
                                                        "categoria TEXT NOT NULL, " +
                                                        "quantidade INTEGER NOT NULL, " +
                                                        "preco_compra REAL, " +
                                                        "preco_venda REAL, " +
                                                        "fornecedor TEXT, " +
                                                        "criado_em TEXT, " +
                                                        "alterado_em TEXT" +
                                                        ")");
                        // detalhes extras por categoria (guarda campos específicos)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS produtos_detalhes (" +
                                                        "id TEXT PRIMARY KEY, " + // FK para produtos.id
                                                        "tipo_especifico TEXT, " + // ex: para acessórios
                                                        "colecao TEXT, " + // coleção Pokémon
                                                        "subtipo TEXT, " + // ex: tipo de booster
                                                        "set_especifico TEXT, " + // ex: código do set
                                                        "idioma TEXT, " + // ex: Português, Inglês
                                                        "validade TEXT, " + // DD/MM/AAAA
                                                        "codigo_barras TEXT, " + // opcional
                                                        "categoria_extra TEXT, " + // para decks
                                                        "versao TEXT, " + // para ETB
                                                        "UNIQUE(id)" + // garante 1:1
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
                                ")"
                              );

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

                } catch (SQLException e) {
                        System.err.println("Erro ao criar tabelas no banco de dados:");
                        e.printStackTrace();
                }
        }
}
