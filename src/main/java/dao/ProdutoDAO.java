package dao;

import model.ProdutoModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    // Helpers centralizados (pra você parar de repetir SQL)
    private static final String WHERE_ATIVO = " WHERE ativo = 1 ";
    private static final String AND_ATIVO = " AND ativo = 1 ";

    private String whereAtivo(boolean incluirInativos) {
        return incluirInativos ? "" : WHERE_ATIVO;
    }

    private String andAtivo(boolean incluirInativos) {
        return incluirInativos ? "" : AND_ATIVO;
    }

    /* ==================== CRUD (SOFT DELETE) ==================== */

    public void insert(ProdutoModel p) throws SQLException {
        String sql = """
                    INSERT INTO produtos
                    (id, nome, jogo_id, tipo, categoria, quantidade, preco_compra, preco_venda, codigo_barras,
                     ncm, cfop, csosn, origem, unidade, criado_em, alterado_em, fornecedor_id, ativo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            bindInsert(ps, p);
            ps.executeUpdate();
        }
    }

    public void update(ProdutoModel p) throws SQLException {
        String sql = """
                    UPDATE produtos SET
                        nome = ?, jogo_id = ?, tipo = ?, categoria = ?, quantidade = ?, preco_compra = ?, preco_venda = ?,
                        codigo_barras = ?, ncm = ?, cfop = ?, csosn = ?, origem = ?, unidade = ?,
                        fornecedor_id = ?, alterado_em = ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            bindUpdate(ps, p);
            ps.executeUpdate();
        }
    }

    public void update(ProdutoModel p, Connection c) throws SQLException {
        String sql = """
                    UPDATE produtos SET
                        nome = ?, jogo_id = ?, tipo = ?, categoria = ?, quantidade = ?, preco_compra = ?, preco_venda = ?,
                        codigo_barras = ?, ncm = ?, cfop = ?, csosn = ?, origem = ?, unidade = ?,
                        fornecedor_id = ?, alterado_em = ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindUpdate(ps, p);
            ps.executeUpdate();
        }
    }

    public void atualizarPrecoCustoFornecedor(String id, Double precoCompra, Double precoVenda, String fornecedorId,
            Connection c) throws SQLException {
        String sql = """
                    UPDATE produtos SET
                        preco_compra = ?,
                        preco_venda = ?,
                        fornecedor_id = ?,
                        alterado_em = ?
                    WHERE id = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (precoCompra == null) {
                ps.setNull(1, Types.REAL);
            } else {
                ps.setDouble(1, precoCompra);
            }
            if (precoVenda == null) {
                ps.setNull(2, Types.REAL);
            } else {
                ps.setDouble(2, precoVenda);
            }
            if (fornecedorId == null || fornecedorId.isBlank()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, fornecedorId);
            }
            ps.setString(4, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            ps.setString(5, id);
            ps.executeUpdate();
        }
    }

    /**
     * UI continua chamando delete(), mas internamente é soft delete.
     */
    public void delete(String id) throws SQLException {
        inativar(id, null);
    }

    public void inativar(String id, String usuarioId) throws SQLException {
        String sql = """
                    UPDATE produtos
                       SET ativo = 0,
                           inativado_em = datetime('now'),
                           inativado_por = ?
                     WHERE id = ?
                """;

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    public void reativar(String id) throws SQLException {
        String sql = """
                    UPDATE produtos
                       SET ativo = 1,
                           inativado_em = NULL,
                           inativado_por = NULL
                     WHERE id = ?
                """;

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /* ==================== SELECTs (COM FILTRO CENTRAL) ==================== */

    public ProdutoModel findById(String id) {
        return findById(id, false);
    }

    public ProdutoModel findById(String id, Connection c) {
        return findById(id, c, false);
    }

    public ProdutoModel findById(String id, boolean incluirInativos) {
        String sql = """
                    SELECT * FROM produtos
                     WHERE id = ?
                """ + andAtivo(incluirInativos);

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ProdutoModel findById(String id, Connection c, boolean incluirInativos) {
        String sql = """
                    SELECT * FROM produtos
                     WHERE id = ?
                """ + andAtivo(incluirInativos);

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ProdutoModel> listAll() {
        return listAll(false);
    }

    public List<ProdutoModel> listAll(boolean incluirInativos) {
        List<ProdutoModel> out = new ArrayList<>();
        String sql = "SELECT * FROM produtos" + whereAtivo(incluirInativos) + " ORDER BY nome";

        try (PreparedStatement ps = DB.get().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                out.add(map(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Exemplo útil: busca por nome (pra sua tela de estoque / filtro).
     * Se você já tem isso no controller/DAO, traga pra cá e centraliza.
     */
    public List<ProdutoModel> searchByNome(String termo, boolean incluirInativos) {
        List<ProdutoModel> out = new ArrayList<>();
        String sql = """
                    SELECT * FROM produtos
                     WHERE nome LIKE ?
                """ + andAtivo(incluirInativos) + " ORDER BY nome";

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Exemplo útil: buscar por código de barras (scanner).
     * Isso aqui é CRÍTICO pra não vender produto inativado.
     */
    public ProdutoModel findByCodigoBarras(String codigo, boolean incluirInativos) {
        String sql = """
                    SELECT * FROM produtos
                     WHERE codigo_barras = ?
                """ + andAtivo(incluirInativos);

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ProdutoModel> findByCodigoBarrasList(String codigo, boolean incluirInativos) {
        List<ProdutoModel> out = new ArrayList<>();
        String sql = """
                    SELECT * FROM produtos
                     WHERE codigo_barras = ?
                """ + andAtivo(incluirInativos) + " ORDER BY nome";

        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public int contarPorCodigoBarrasAtivo(String codigo, String idExcluir) {
        if (codigo == null || codigo.trim().isEmpty())
            return 0;
        String sql = "SELECT COUNT(1) AS total FROM produtos WHERE codigo_barras = ? AND ativo = 1 AND id <> ?";
        try (PreparedStatement ps = DB.get().prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            ps.setString(2, idExcluir == null ? "" : idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /* ==================== MAP / BINDS ==================== */

    private ProdutoModel map(ResultSet rs) throws SQLException {
        ProdutoModel p = new ProdutoModel(
                rs.getString("id"),
                rs.getString("nome"),
                rs.getString("tipo"),
                rs.getInt("quantidade"),
                rs.getDouble("preco_compra"),
                rs.getDouble("preco_venda"));

        p.setJogoId(rs.getString("jogo_id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNcm(rs.getString("ncm"));
        p.setCfop(rs.getString("cfop"));
        p.setCsosn(rs.getString("csosn"));
        p.setOrigem(rs.getString("origem"));
        p.setUnidade(rs.getString("unidade"));
        p.setCategoria(rs.getString("categoria"));
        p.setFornecedorId(rs.getString("fornecedor_id"));

        return p;
    }

    private void bindInsert(PreparedStatement ps, ProdutoModel p) throws SQLException {
        ps.setString(1, p.getId());
        ps.setString(2, p.getNome());
        ps.setString(3, p.getJogoId());
        ps.setString(4, p.getTipo());
        ps.setString(5, p.getCategoria());
        ps.setInt(6, p.getQuantidade());
        ps.setDouble(7, p.getPrecoCompra());
        ps.setDouble(8, p.getPrecoVenda());
        ps.setString(9, p.getCodigoBarras());
        ps.setString(10, p.getNcm());
        ps.setString(11, p.getCfop());
        ps.setString(12, p.getCsosn());
        ps.setString(13, p.getOrigem());
        ps.setString(14, p.getUnidade());
        ps.setString(15, p.getCriadoEm().toString());
        ps.setString(16, p.getAlteradoEm().toString());
        ps.setString(17, p.getFornecedorId());
        ps.setInt(18, 1);
    }

    private void bindUpdate(PreparedStatement ps, ProdutoModel p) throws SQLException {
        ps.setString(1, p.getNome());
        ps.setString(2, p.getJogoId());
        ps.setString(3, p.getTipo());
        ps.setString(4, p.getCategoria());
        ps.setInt(5, p.getQuantidade());
        ps.setDouble(6, p.getPrecoCompra());
        ps.setDouble(7, p.getPrecoVenda());
        ps.setString(8, p.getCodigoBarras());
        ps.setString(9, p.getNcm());
        ps.setString(10, p.getCfop());
        ps.setString(11, p.getCsosn());
        ps.setString(12, p.getOrigem());
        ps.setString(13, p.getUnidade());
        ps.setString(14, p.getFornecedorId());
        ps.setString(15, p.getAlteradoEm().toString());
        ps.setString(16, p.getId());
    }

}
