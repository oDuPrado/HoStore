package util;

import java.sql.*;
import java.util.List;

import model.ColecaoModel;
import model.SetModel;

import model.SetJogoModel;
import dao.SetJogoDAO;
import service.SetJogoService;

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
                                                        ")");

                        // itens de venda
                        st.execute(
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
                                                        ")");

                        // @CREATE_TABLE: vendas_estornos_pagamentos
                        st.execute(
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
                                                        ")");

                        // pagamentos de venda
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS vendas_pagamentos (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "tipo TEXT NOT NULL, " + // ex: pix, dinheiro, cartão
                                                        "valor REAL NOT NULL, " +
                                                        "FOREIGN KEY (venda_id) REFERENCES vendas(id)" +
                                                        ")");

                        // vendas_devolucoes – registra devoluções parciais ou totais por produto
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS vendas_devolucoes (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                        "venda_id INTEGER NOT NULL, " +
                                                        "produto_id TEXT NOT NULL, " +
                                                        "qtd INTEGER NOT NULL, " + // <- o nome correto agora é `qtd`
                                                        "valor_unit REAL, " +
                                                        "motivo TEXT, " +
                                                        "data TEXT, " +
                                                        "usuario TEXT, " +
                                                        "FOREIGN KEY(venda_id) REFERENCES vendas(id), " +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id)" +
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
                                                        "jogo_id TEXT, " + // vincula ao jogo (Pokémon, etc)
                                                        "tipo TEXT NOT NULL, " +
                                                        "quantidade INTEGER NOT NULL, " +
                                                        "preco_compra REAL, " +
                                                        "preco_venda REAL, " +
                                                        "codigo_barras TEXT, " +
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
                                                        "data_lancamento TEXT, " + // ✅ coluna que estava faltando
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id), " +
                                                        "FOREIGN KEY(jogo_id) REFERENCES jogos(id)" +
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
                                        "sigla TEXT, " + // ex: TF
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
                                                        "jogo_id TEXT, " + // vincula ao jogo (Pokémon, etc)
                                                        "tipo_deck TEXT, " +
                                                        "categoria TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "FOREIGN KEY(id) REFERENCES produtos(id) ON DELETE CASCADE, " +
                                                        "FOREIGN KEY(jogo_id) REFERENCES jogos(id)" +
                                                        ")");

                        // etbs (entidade com detalhes de selados tipo ETB)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS etbs (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor TEXT, " +
                                                        "jogo_id TEXT, " + // vincula ao jogo (Pokémon, etc)
                                                        "serie TEXT, " +
                                                        "colecao TEXT, " +
                                                        "tipo TEXT, " +
                                                        "versao TEXT, " +
                                                        "codigo_barras TEXT, " +
                                                        "FOREIGN KEY(id) REFERENCES produtos(id) ON DELETE CASCADE, " +
                                                        "FOREIGN KEY(jogo_id) REFERENCES jogos(id)" +
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

                        // TITULOS - CONTAS A RECEBER
                        st.execute("CREATE TABLE IF NOT EXISTS titulos_contas_receber (" +
                                        "id TEXT PRIMARY KEY, " +
                                        "cliente_id TEXT, " +
                                        "codigo_selecao TEXT, " +
                                        "data_geracao TEXT, " +
                                        "valor_total REAL, " +
                                        "status TEXT, " +
                                        "observacoes TEXT, " +
                                        "FOREIGN KEY(cliente_id) REFERENCES clientes(id)" +
                                        ")");

                        st.execute(
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
                                                        ");");

                        // PARCELAS - CONTAS A RECEBER
                        st.execute("CREATE TABLE IF NOT EXISTS parcelas_contas_receber (" +
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
                                        ")");

                        // PAGAMENTOS - CONTAS A RECEBER
                        st.execute("CREATE TABLE IF NOT EXISTS pagamentos_contas_receber (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "parcela_id INTEGER, " +
                                        "forma_pagamento TEXT, " +
                                        "valor_pago REAL, " +
                                        "data_pagamento TEXT, " +
                                        "FOREIGN KEY(parcela_id) REFERENCES parcelas_contas_receber(id)" +
                                        ")");

                        // TAXAS DE CARTÃO - CONFIGURAÇÃO DE MAQUININHA
                        st.execute("CREATE TABLE IF NOT EXISTS taxas_cartao (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "bandeira TEXT NOT NULL, " + // ex: Cielo, Stone, Rede
                                        "tipo TEXT NOT NULL, " + // CREDITO, DEBITO
                                        "min_parcelas INTEGER NOT NULL, " +
                                        "max_parcelas INTEGER NOT NULL, " +
                                        "mes_vigencia TEXT NOT NULL, " + // formato 'YYYY-MM'
                                        "taxa_pct REAL NOT NULL, " + // ex: 3.49 (%)
                                        "observacoes TEXT" + // configs extras, JSON ou texto livre
                                        ")");
                        /*
                         * st.execute("INSERT INTO taxas_cartao (" +
                         * "bandeira, tipo, min_parcelas, max_parcelas, mes_vigencia, taxa_pct, observacoes"
                         * +
                         * ") VALUES (" +
                         * "'PagSeguro', 'CREDITO', 1, 12, '2025-06', 3.49, 'Taxa padrão para testes'" +
                         * ")");
                         */

                        // ──────── TABELA DE PEDIDOS DE COMPRA ────────
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS pedidos_compras (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "nome TEXT, " +
                                                        "data TEXT, " +
                                                        "status TEXT, " + // rascunho, enviado, recebido, etc.
                                                        "fornecedor_id TEXT, " +
                                                        "observacoes TEXT, " +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id)" +
                                                        ")");

                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS pedido_produtos (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "pedido_id TEXT NOT NULL, " +
                                                        "produto_id TEXT NOT NULL, " +
                                                        "quantidade_pedida INTEGER NOT NULL, " +
                                                        "quantidade_recebida INTEGER DEFAULT 0, " +
                                                        "status TEXT DEFAULT 'pendente', " + // 'pendente', 'parcial',
                                                                                             // 'completo'
                                                        "FOREIGN KEY(pedido_id) REFERENCES pedidos_compras(id), " +
                                                        "FOREIGN KEY(produto_id) REFERENCES produtos(id)" +
                                                        ")");

                        // TITULOS - CONTAS A PAGAR

                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS titulos_contas_pagar (" +
                                                        "id TEXT PRIMARY KEY, " +
                                                        "fornecedor_id TEXT, " +
                                                        "plano_conta_id TEXT, " + // ✅ NOVO: vincula ao Plano de Contas
                                                        "codigo_selecao TEXT, " + // UUID que agrupa o título
                                                        "data_geracao TEXT, " +
                                                        "valor_total REAL, " +
                                                        "status TEXT, " + // aberto, quitado, vencido, cancelado
                                                        "observacoes TEXT, " +
                                                        "pedido_id TEXT, " +
                                                        "FOREIGN KEY(pedido_id) REFERENCES pedidos_compras(id)" +
                                                        "FOREIGN KEY(fornecedor_id) REFERENCES fornecedores(id), " +
                                                        "FOREIGN KEY(plano_conta_id) REFERENCES planos_contas(id)" +
                                                        ")");

                        // PARCELAS - CONTAS A PAGAR
                        st.execute("CREATE TABLE IF NOT EXISTS parcelas_contas_pagar (" +
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
                                        ")");

                        // PAGAMENTOS - CONTAS A PAGAR
                        st.execute("CREATE TABLE IF NOT EXISTS pagamentos_contas_pagar (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "parcela_id INTEGER, " +
                                        "forma_pagamento TEXT, " +
                                        "valor_pago REAL, " +
                                        "data_pagamento TEXT, " +
                                        "FOREIGN KEY(parcela_id) REFERENCES parcelas_contas_pagar(id)" +
                                        ")");

                        // ──────── VÍNCULO ENTRE CONTAS A PAGAR E PEDIDOS ────────
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS contas_pagar_pedidos (" +
                                                        "conta_pagar_id TEXT NOT NULL, " +
                                                        "pedido_id TEXT NOT NULL, " +
                                                        "PRIMARY KEY (conta_pagar_id, pedido_id), " +
                                                        "FOREIGN KEY (conta_pagar_id) REFERENCES contas_pagar(id), " +
                                                        "FOREIGN KEY (pedido_id) REFERENCES pedidos_compras(id)" +
                                                        ")");

                        // Cria tabela de Planos de Contas
                        // ─── PLANOS DE CONTAS ───────────────────────────────────────────────────────
                        st.execute("CREATE TABLE IF NOT EXISTS planos_contas ("
                                        + "id TEXT PRIMARY KEY, "
                                        + "codigo TEXT NOT NULL, "
                                        + "descricao TEXT NOT NULL, "
                                        + "tipo TEXT NOT NULL, " // Ativo, Passivo, Custo, Receita
                                        + "parent_id TEXT, " // FK para planos_contas.id
                                        + "observacoes TEXT, "
                                        + "FOREIGN KEY(parent_id) REFERENCES planos_contas(id)"
                                        + ")");

                        // Cria tabela de Bancos (Contas Bancárias)
                        st.execute("""
                                            CREATE TABLE IF NOT EXISTS bancos (
                                                id TEXT PRIMARY KEY,
                                                nome TEXT NOT NULL,
                                                agencia TEXT,
                                                conta TEXT,
                                                observacoes TEXT
                                            )
                                        """);

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

                        // saldo de crédito de loja por cliente
                        st.execute("""
                                            CREATE TABLE IF NOT EXISTS credito_loja (
                                                id TEXT PRIMARY KEY,
                                                cliente_id TEXT NOT NULL,
                                                valor REAL NOT NULL DEFAULT 0,
                                                FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                                            )
                                        """);

                        // histórico de movimentações de crédito de loja
                        st.execute("""
                                            CREATE TABLE IF NOT EXISTS credito_loja_movimentacoes (
                                                id TEXT PRIMARY KEY,
                                                cliente_id TEXT NOT NULL,
                                                valor REAL NOT NULL,
                                                tipo TEXT NOT NULL, -- "entrada" ou "uso"
                                                referencia TEXT,     -- pode ser ID da venda, motivo, devolução, etc
                                                data TEXT NOT NULL,
                                                FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                                            )
                                        """);

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

                        // @TODO: TCG - Tabela de Jogos
                        st.execute("""
                                            CREATE TABLE IF NOT EXISTS jogos (
                                                id TEXT PRIMARY KEY,           -- Ex: 'POKEMON', 'YUGIOH'
                                                nome TEXT NOT NULL             -- Nome completo, ex: 'Pokémon TCG'
                                            )
                                        """);
                        // @TODO: Jogos Padrão
                        st.execute("""
                                            INSERT OR IGNORE INTO jogos (id, nome) VALUES
                                                ('POKEMON', 'Pokémon TCG'),
                                                ('YUGIOH', 'Yu-Gi-Oh!'),
                                                ('MAGIC', 'Magic: The Gathering'),
                                                ('ONEPIECE', 'One Piece Card Game'),
                                                ('DIGIMON', 'Digimon Card Game'),
                                                ('DRAGONBALL', 'Dragon Ball Super Card Game')
                                        """);

                        st.execute("""
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
                                        """);
                        // Sets de jogos (exceto Pokémon)
                        st.execute("""
                                            CREATE TABLE IF NOT EXISTS sets_jogos (
                                                set_id TEXT,
                                                nome TEXT NOT NULL,
                                                jogo_id TEXT NOT NULL,
                                                data_lancamento TEXT,
                                                qtd_cartas INTEGER,
                                                codigo_externo TEXT,
                                                PRIMARY KEY (set_id, jogo_id)
                                                FOREIGN KEY (jogo_id) REFERENCES jogos(id)
                                            )
                                        """);

                        // ─────────── TABELAS FISCAIS ───────────

                        // 1) NCM: código (8 dígitos) + descrição
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS ncm (" +
                                                        "   codigo TEXT PRIMARY KEY, " + // ex: "95044000" (sem ponto)
                                                        "   descricao TEXT NOT NULL        " + // ex: "Jogos de cartas,
                                                                                               // para jogos de salão ou
                                                                                               // de tabuleiro"
                                                        ");");

                        // 2) CFOP: código (4 dígitos) + descrição
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS cfop (" +
                                                        "   codigo TEXT PRIMARY KEY, " + // ex: "5102"
                                                        "   descricao TEXT NOT NULL        " + // ex: "Venda de produção
                                                                                               // do estabelecimento"
                                                        ");");

                        // 3) CSOSN (para Simples Nacional) ou CST (para regime normal)
                        // Aqui usaremos o nome "csosn" mesmo, mas você pode renomear para cst se for
                        // regime normal.
                        // A tabela guarda tanto CSOSN quanto CST (basta usar códigos predefinidos).
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS csosn (" +
                                                        "   codigo TEXT PRIMARY KEY, " + // ex: "102"
                                                        "   descricao TEXT NOT NULL        " + // ex: "Isento de ICMS
                                                                                               // (Simples Nacional –
                                                                                               // faixa de receita até
                                                                                               // X)"
                                                        ");");

                        // 4) ORIGEM: código (0–8) + descrição (origem do produto)
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS origem (" +
                                                        "   codigo TEXT PRIMARY KEY, " + // ex: "0"
                                                        "   descricao TEXT NOT NULL        " + // ex: "0 – Nacional"
                                                        ");");

                        // ─────────── TABELA DE CONFIGURAÇÃO FISCAL ───────────
                        st.execute(
                                        "CREATE TABLE IF NOT EXISTS config_fiscal (" +
                                                        "   id TEXT PRIMARY KEY, " +
                                                        "   cliente_id TEXT UNIQUE, " + // Vincula à tabela 'clientes';
                                                                                        // cada cliente tem no máximo
                                                                                        // uma configuração
                                                        "   regime_tributario TEXT,   " + // ex: "Simples Nacional",
                                                                                          // "Lucro Presumido", "Lucro
                                                                                          // Real"
                                                        "   cfop_padrao TEXT,         " + // ex: "5102"
                                                        "   csosn_padrao TEXT,        " + // ex: "102"
                                                        "   origem_padrao TEXT,       " + // ex: "0"
                                                        "   ncm_padrao TEXT,          " + // ex: "95044000"
                                                        "   unidade_padrao TEXT,      " + // ex: "UN", "CX", "KG"
                                                        "   FOREIGN KEY(cliente_id) REFERENCES clientes(id) " +
                                                        ");");

                        // ─────────── SINCRONIZAÇÃO FISCAL AUTOMÁTICA ───────────
                        try {
                                // NCM
                                try {
                                        List<model.NcmModel> listaNcms = service.FiscalApiService.listarNcms();
                                        dao.NcmDAO ncmDAO = new dao.NcmDAO();
                                        ncmDAO.sincronizarComApi(listaNcms);
                                } catch (Exception e) {
                                        System.err.println("⚠ Falha na API de NCM. Usando fallback...");
                                        try (Connection conn = get(); Statement stncm = conn.createStatement()) {
                                                stncm.execute("INSERT OR IGNORE INTO ncm (codigo, descricao) VALUES " +
                                                                "('95044000','Jogos de cartas para jogos de salão ou tabuleiro'),"
                                                                +
                                                                "('49019900','Outros impressos');");
                                        }
                                }

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

        public static boolean isConnected() {
                try (Connection c = get()) {
                        return c != null && !c.isClosed();
                } catch (SQLException e) {
                        return false;
                }
        }

}
