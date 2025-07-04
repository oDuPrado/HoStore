package dao;

import model.Carta;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartaDAO {

    /**
     * Busca nomes de carta para auto‑complete.
     * Se termo for vazio ou null, retorna as 20 primeiras cartas.
     */
    public List<String> buscarNomesLike(String termo) {
        List<String> nomes = new ArrayList<>();
        String sql;
        boolean usarLike = termo != null && !termo.trim().isEmpty();

        if (usarLike) {
            sql = "SELECT DISTINCT nome FROM cartas WHERE nome LIKE ? ORDER BY nome LIMIT 20";
        } else {
            sql = "SELECT DISTINCT nome FROM cartas ORDER BY nome LIMIT 20";
        }

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            if (usarLike) {
                ps.setString(1, "%" + termo + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nomes.add(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nomes;
    }

    /** Insere uma nova carta */
    public void insert(Carta cta) throws SQLException {
        String sql = "INSERT INTO cartas (" +
                "id,nome,set_id,colecao,numero,qtd," +
                "preco_loja,preco_consignado,percentual_loja,valor_loja," +
                "custo,condicao_id,linguagem_id,consignado,dono," +
                "tipo_id,subtipo_id,raridade_id,sub_raridade_id,ilustracao_id,fornecedor_id" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            bind(ps, cta);
            ps.executeUpdate();
        }
    }

    /** Atualiza todos os campos editáveis */
    public void update(Carta cta) throws SQLException {
        String sql = "UPDATE cartas SET " +
            "nome=?, set_id=?, colecao=?, numero=?, qtd=?," +
            "preco_loja=?, preco_consignado=?, percentual_loja=?, valor_loja=?," +
            "custo=?, condicao_id=?, linguagem_id=?, consignado=?, dono=?," +
            "tipo_id=?, subtipo_id=?, raridade_id=?, sub_raridade_id=?, ilustracao_id=?, fornecedor_id=? " +
            "WHERE id=?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            // bind dos 20 campos do SET
            ps.setString(1,  cta.getNome());
            ps.setString(2,  cta.getSetId());
            ps.setString(3,  cta.getColecao());
            ps.setString(4,  cta.getNumero());
            ps.setInt   (5,  cta.getQtd());
            ps.setDouble(6,  cta.getPrecoLoja());
            ps.setDouble(7,  cta.getPrecoConsignado());
            ps.setDouble(8,  cta.getPercentualLoja());
            ps.setDouble(9,  cta.getValorLoja());
            ps.setDouble(10, cta.getCusto());
            ps.setString(11, cta.getCondicaoId());
            ps.setString(12, cta.getLinguagemId());
            ps.setInt   (13, cta.isConsignado() ? 1 : 0);
            ps.setString(14, cta.getDono());
            ps.setString(15, cta.getTipoId());
            ps.setString(16, cta.getSubtipoId());
            ps.setString(17, cta.getRaridadeId());
            ps.setString(18, cta.getSubRaridadeId());
            ps.setString(19, cta.getIlustracaoId());
            ps.setString(20, cta.getFornecedorId());
            // por fim, o id no WHERE
            ps.setString(21, cta.getId());
    
            ps.executeUpdate();
        }
    }
      

    /** Apaga pelo id */
    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = DB.get()
                .prepareStatement("DELETE FROM cartas WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public Carta buscarPorNomeUnico(String nome) {
        try (PreparedStatement ps = DB.get().prepareStatement(
                "SELECT * FROM cartas WHERE nome = ? LIMIT 1")) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Carta(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("colecao"),
                        rs.getString("numero"),
                        rs.getInt("qtd"),
                        rs.getDouble("preco"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Carta buscarPorId(String id) {
        String sql = "SELECT * FROM cartas WHERE id = ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Carta(
                    rs.getString("id"),
                    rs.getString("nome"),
                    rs.getString("set_id"),
                    rs.getString("colecao"),
                    rs.getString("numero"),
                    rs.getInt("qtd"),
                    rs.getDouble("preco_loja"),
                    rs.getDouble("preco_consignado"),
                    rs.getDouble("percentual_loja"),
                    rs.getDouble("valor_loja"),
                    rs.getDouble("custo"),
                    rs.getString("condicao_id"),
                    rs.getString("linguagem_id"),
                    rs.getInt("consignado") == 1,
                    rs.getString("dono"),
                    rs.getString("tipo_id"),
                    rs.getString("subtipo_id"),
                    rs.getString("raridade_id"),
                    rs.getString("sub_raridade_id"),
                    rs.getString("ilustracao_id"),
                    rs.getString("fornecedor_id")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    

    public void inserirCartaFake() {
        try (Connection c = DB.get()) {
            // Primeiro insere os dados base nas tabelas relacionais (ignora se já existe)
            try (Statement st = c.createStatement()) {
                st.execute("INSERT OR IGNORE INTO condicoes (id, nome) VALUES ('C1', 'Nova')");
                st.execute("INSERT OR IGNORE INTO linguagens (id, nome) VALUES ('L1', 'Português')");
                st.execute("INSERT OR IGNORE INTO tipo_cartas (id, nome) VALUES ('T1', 'Pokémon')");
                st.execute("INSERT OR IGNORE INTO subtipo_cartas (id, nome) VALUES ('ST1', 'Básico')");
                st.execute("INSERT OR IGNORE INTO raridades (id, nome) VALUES ('R1', 'Rara')");
                st.execute("INSERT OR IGNORE INTO sub_raridades (id, nome) VALUES ('SR1', 'Reverse')");
                st.execute("INSERT OR IGNORE INTO ilustracoes (id, nome) VALUES ('IL1', 'Regular')");
            }

            // Agora insere a carta com os campos completos
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR REPLACE INTO cartas (" +
                            "id, nome, colecao, numero, qtd, preco, condicao_id, custo, linguagem_id, consignado, dono, "
                            +
                            "tipo_id, subtipo_id, raridade_id, sub_raridade_id, ilustracao_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                ps.setString(1, "001"); // id
                ps.setString(2, "Pikachu"); // nome
                ps.setString(3, "Base Set"); // colecao
                ps.setString(4, "58/102"); // numero
                ps.setInt(5, 50); // qtd
                ps.setDouble(6, 9.90); // preco
                ps.setString(7, "C1"); // condicao_id
                ps.setDouble(8, 4.50); // custo
                ps.setString(9, "L1"); // linguagem_id
                ps.setInt(10, 0); // consignado (0 = loja)
                ps.setString(11, "L-0001"); // dono (id loja)
                ps.setString(12, "T1"); // tipo_id
                ps.setString(13, "ST1"); // subtipo_id
                ps.setString(14, "R1"); // raridade_id
                ps.setString(15, "SR1"); // sub_raridade_id
                ps.setString(16, "IL1"); // ilustracao_id

                ps.executeUpdate();
            }

            System.out.println("Carta fake inserida com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* devolve todas as coleções distintas – ordenadas */
    public List<String> listarColecoes() {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = DB.get().prepareStatement(
                "SELECT DISTINCT colecao FROM cartas ORDER BY colecao")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                out.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /* lista cartas aplicando filtros de nome/coleção e ordenação */
    public List<Carta> listarCartas(String termo, String colecao, String orderBy) {
        StringBuilder sb = new StringBuilder("SELECT * FROM cartas WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (termo != null && !termo.trim().isEmpty()) {
            sb.append(" AND nome LIKE ?");
            params.add("%" + termo + "%");
        }
        if (colecao != null && !colecao.equals("Todas")) {
            sb.append(" AND colecao = ?");
            params.add(colecao);
        }

        switch (orderBy) {
            case "Nome":
                sb.append(" ORDER BY nome");
                break;
            case "Número":
                sb.append(" ORDER BY numero");
                break;
            case "Mais novo":
                sb.append(" ORDER BY rowid DESC");
                break;
            default:
                sb.append(" ORDER BY rowid ASC");
                break; // Mais antigo
        }

        List<Carta> out = new ArrayList<>();
        try (PreparedStatement ps = DB.get().prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(new Carta(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("colecao"),
                        rs.getString("numero"),
                        rs.getInt("qtd"),
                        rs.getDouble("preco")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /* ---------- Helper para bindar campos ---------- */
    private void bind(PreparedStatement ps, Carta c) throws SQLException {
        ps.setString(1,  c.getId());
        ps.setString(2,  c.getNome());
        ps.setString(3,  c.getSetId());
        ps.setString(4,  c.getColecao());
        ps.setString(5,  c.getNumero());
        ps.setInt   (6,  c.getQtd());
        ps.setDouble(7,  c.getPrecoLoja());
        ps.setDouble(8,  c.getPrecoConsignado());
        ps.setDouble(9,  c.getPercentualLoja());
        ps.setDouble(10, c.getValorLoja());
        ps.setDouble(11, c.getCusto());
        ps.setString(12, c.getCondicaoId());
        ps.setString(13, c.getLinguagemId());
        ps.setInt   (14, c.isConsignado() ? 1 : 0);
        ps.setString(15, c.getDono());
        ps.setString(16, c.getTipoId());
        ps.setString(17, c.getSubtipoId());
        ps.setString(18, c.getRaridadeId());
        ps.setString(19, c.getSubRaridadeId());
        ps.setString(20, c.getIlustracaoId());
        ps.setString(21, c.getFornecedorId());
    }    
}
