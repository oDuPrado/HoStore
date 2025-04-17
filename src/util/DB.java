package util;

import java.sql.*;

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
                "id TEXT PRIMARY KEY, " +          // mesmo id da tabela produtos
                "tipo_especifico TEXT, " +         // chaveiro, playmat, etc.
                "colecao TEXT, " +                 // coleção Pokémon, set, etc.
                "subtipo TEXT, " +                 // unitário, quadripack...
                "categoria_extra TEXT, " +         // estrela, master...
                "versao TEXT, " +                  // nacional, americana...
                "validade TEXT" +                  // para alimentos
                ")");


    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
