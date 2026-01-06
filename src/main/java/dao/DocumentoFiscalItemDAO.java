package dao;

import model.DocumentoFiscalItemModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentoFiscalItemDAO {

    public void inserir(Connection conn, DocumentoFiscalItemModel it) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO documentos_fiscais_itens
            (documento_id, venda_item_id, produto_id, descricao,
             ncm, cfop, csosn, origem, unidade,
             quantidade, valor_unit, desconto, acrescimo, total_item, observacoes)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """)) {
            int i = 1;
            ps.setString(i++, it.documentoId);
            if (it.vendaItemId == null) ps.setNull(i++, Types.INTEGER); else ps.setInt(i++, it.vendaItemId);
            ps.setString(i++, it.produtoId);
            ps.setString(i++, it.descricao);

            ps.setString(i++, it.ncm);
            ps.setString(i++, it.cfop);
            ps.setString(i++, it.csosn);
            ps.setString(i++, it.origem);
            ps.setString(i++, it.unidade);

            ps.setInt(i++, it.quantidade);
            ps.setDouble(i++, it.valorUnit);
            ps.setDouble(i++, it.desconto);
            ps.setDouble(i++, it.acrescimo);
            ps.setDouble(i++, it.totalItem);
            ps.setString(i++, it.observacoes);

            ps.executeUpdate();
        }
    }

    public List<DocumentoFiscalItemModel> listarPorDocumento(Connection conn, String docId) throws SQLException {
        List<DocumentoFiscalItemModel> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT * FROM documentos_fiscais_itens
            WHERE documento_id = ?
            ORDER BY id ASC
        """)) {
            ps.setString(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private DocumentoFiscalItemModel map(ResultSet rs) throws SQLException {
        DocumentoFiscalItemModel it = new DocumentoFiscalItemModel();
        it.id = rs.getInt("id");
        it.documentoId = rs.getString("documento_id");
        int vi = rs.getInt("venda_item_id"); it.vendaItemId = rs.wasNull() ? null : vi;
        it.produtoId = rs.getString("produto_id");
        it.descricao = rs.getString("descricao");

        it.ncm = rs.getString("ncm");
        it.cfop = rs.getString("cfop");
        it.csosn = rs.getString("csosn");
        it.origem = rs.getString("origem");
        it.unidade = rs.getString("unidade");

        it.quantidade = rs.getInt("quantidade");
        it.valorUnit = rs.getDouble("valor_unit");
        it.desconto = rs.getDouble("desconto");
        it.acrescimo = rs.getDouble("acrescimo");
        it.totalItem = rs.getDouble("total_item");
        it.observacoes = rs.getString("observacoes");
        return it;
    }
}
