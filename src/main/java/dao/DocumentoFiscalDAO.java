package dao;

import model.DocumentoFiscalModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentoFiscalDAO {

    public void inserir(Connection conn, DocumentoFiscalModel d) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO documentos_fiscais
            (id, venda_id, modelo, codigo_modelo, serie, numero, ambiente, status,
             chave_acesso, protocolo, recibo, xml, xml_path, xml_sha256, xml_tamanho, erro,
             total_produtos, total_desconto, total_acrescimo, total_final,
             criado_em, criado_por, atualizado_em, cancelado_em, cancelado_por)
            VALUES (?,?,?,?,?,?,?,?,
                    ?,?,?,?,?,?,?,?,
                    ?,?,?,?,
                    datetime('now'),?, datetime('now'), ?, ?)
        """)) {
            int i = 1;
            ps.setString(i++, d.id);
            ps.setInt(i++, d.vendaId);
            ps.setString(i++, d.modelo);
            ps.setInt(i++, d.codigoModelo);
            ps.setInt(i++, d.serie);
            ps.setInt(i++, d.numero);
            ps.setString(i++, d.ambiente);
            ps.setString(i++, d.status);

            ps.setString(i++, d.chaveAcesso);
            ps.setString(i++, d.protocolo);
            ps.setString(i++, d.recibo);
            ps.setString(i++, d.xml);
            ps.setString(i++, d.xmlPath);
            ps.setString(i++, d.xmlSha256);
            if (d.xmlTamanho == null) ps.setNull(i++, Types.INTEGER); else ps.setInt(i++, d.xmlTamanho);
            ps.setString(i++, d.erro);

            if (d.totalProdutos == null) ps.setNull(i++, Types.REAL); else ps.setDouble(i++, d.totalProdutos);
            if (d.totalDesconto == null) ps.setNull(i++, Types.REAL); else ps.setDouble(i++, d.totalDesconto);
            if (d.totalAcrescimo == null) ps.setNull(i++, Types.REAL); else ps.setDouble(i++, d.totalAcrescimo);
            if (d.totalFinal == null) ps.setNull(i++, Types.REAL); else ps.setDouble(i++, d.totalFinal);

            ps.setString(i++, d.criadoPor);
            ps.setString(i++, d.canceladoEm);
            ps.setString(i++, d.canceladoPor);

            ps.executeUpdate();
        }
    }

    public DocumentoFiscalModel buscarPorId(Connection conn, String docId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT * FROM documentos_fiscais WHERE id = ?
        """)) {
            ps.setString(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public DocumentoFiscalModel buscarPorVenda(Connection conn, int vendaId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT * FROM documentos_fiscais
            WHERE venda_id = ?
            ORDER BY criado_em DESC
            LIMIT 1
        """)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<DocumentoFiscalModel> listarPorStatus(Connection conn, String status, int limit) throws SQLException {
        List<DocumentoFiscalModel> out = new ArrayList<>();
        boolean filtrar = status != null && !status.isBlank();
        String sql = filtrar
                ? """
                    SELECT * FROM documentos_fiscais
                    WHERE lower(status) = lower(?)
                    ORDER BY criado_em DESC
                    LIMIT ?
                  """
                : """
                    SELECT * FROM documentos_fiscais
                    ORDER BY criado_em DESC
                    LIMIT ?
                  """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (filtrar) {
                ps.setString(idx++, status);
            }
            ps.setInt(idx, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void atualizarStatus(Connection conn, String docId, String status, String erro, String xml, String chave, String protocolo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE documentos_fiscais
            SET status = ?,
                erro = ?,
                xml = COALESCE(?, xml),
                chave_acesso = COALESCE(?, chave_acesso),
                protocolo = COALESCE(?, protocolo),
                atualizado_em = datetime('now')
            WHERE id = ?
        """)) {
            ps.setString(1, status);
            ps.setString(2, erro);
            ps.setString(3, xml);
            ps.setString(4, chave);
            ps.setString(5, protocolo);
            ps.setString(6, docId);
            ps.executeUpdate();
        }
    }

    public void atualizarXmlInfo(Connection conn, String docId, String path, String sha256, Integer tamanho) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE documentos_fiscais
            SET xml_path = ?,
                xml_sha256 = ?,
                xml_tamanho = ?,
                atualizado_em = datetime('now')
            WHERE id = ?
        """)) {
            ps.setString(1, path);
            ps.setString(2, sha256);
            if (tamanho == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, tamanho);
            ps.setString(4, docId);
            ps.executeUpdate();
        }
    }

    private DocumentoFiscalModel map(ResultSet rs) throws SQLException {
        DocumentoFiscalModel d = new DocumentoFiscalModel();
        d.id = rs.getString("id");
        d.vendaId = rs.getInt("venda_id");
        d.modelo = rs.getString("modelo");
        d.codigoModelo = rs.getInt("codigo_modelo");
        d.serie = rs.getInt("serie");
        d.numero = rs.getInt("numero");
        d.ambiente = rs.getString("ambiente");
        d.status = rs.getString("status");

        d.chaveAcesso = rs.getString("chave_acesso");
        d.protocolo = rs.getString("protocolo");
        d.recibo = rs.getString("recibo");
        d.xml = rs.getString("xml");
        d.xmlPath = rs.getString("xml_path");
        d.xmlSha256 = rs.getString("xml_sha256");
        int t;
        t = rs.getInt("xml_tamanho"); d.xmlTamanho = rs.wasNull() ? null : t;
        d.erro = rs.getString("erro");

        double v;
        v = rs.getDouble("total_produtos"); d.totalProdutos = rs.wasNull() ? null : v;
        v = rs.getDouble("total_desconto"); d.totalDesconto = rs.wasNull() ? null : v;
        v = rs.getDouble("total_acrescimo"); d.totalAcrescimo = rs.wasNull() ? null : v;
        v = rs.getDouble("total_final"); d.totalFinal = rs.wasNull() ? null : v;

        d.criadoEm = rs.getString("criado_em");
        d.criadoPor = rs.getString("criado_por");
        d.atualizadoEm = rs.getString("atualizado_em");
        d.canceladoEm = rs.getString("cancelado_em");
        d.canceladoPor = rs.getString("cancelado_por");
        return d;
    }
}
