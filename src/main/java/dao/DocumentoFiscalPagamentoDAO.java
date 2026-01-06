package dao;

import model.DocumentoFiscalPagamentoModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentoFiscalPagamentoDAO {

    public void inserir(Connection conn, DocumentoFiscalPagamentoModel p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO documentos_fiscais_pagamentos (documento_id, tipo, valor)
            VALUES (?,?,?)
        """)) {
            ps.setString(1, p.documentoId);
            ps.setString(2, p.tipo);
            ps.setDouble(3, p.valor);
            ps.executeUpdate();
        }
    }

    public List<DocumentoFiscalPagamentoModel> listarPorDocumento(Connection conn, String docId) throws SQLException {
        List<DocumentoFiscalPagamentoModel> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT * FROM documentos_fiscais_pagamentos
            WHERE documento_id = ?
            ORDER BY id ASC
        """)) {
            ps.setString(1, docId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentoFiscalPagamentoModel p = new DocumentoFiscalPagamentoModel();
                    p.id = rs.getInt("id");
                    p.documentoId = rs.getString("documento_id");
                    p.tipo = rs.getString("tipo");
                    p.valor = rs.getDouble("valor");
                    out.add(p);
                }
            }
        }
        return out;
    }
}
