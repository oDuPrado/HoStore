package service;

import dao.*;
import model.*;
import util.DB;

import java.sql.*;
import java.util.UUID;

public class DocumentoFiscalService {

    private final SequenciaFiscalDAO seqDAO = new SequenciaFiscalDAO();
    private final DocumentoFiscalDAO docDAO = new DocumentoFiscalDAO();
    private final DocumentoFiscalItemDAO itemDAO = new DocumentoFiscalItemDAO();
    private final DocumentoFiscalPagamentoDAO pagDAO = new DocumentoFiscalPagamentoDAO();

    // Defaults ultra seguros (não quebram NFC-e por falta de cadastro)
    private static final String DEF_NCM = "00000000";
    private static final String DEF_CFOP = "5102";
    private static final String DEF_CSOSN = "102";
    private static final String DEF_ORIGEM = "0";
    private static final String DEF_UN = "UN";

    public DocumentoFiscalModel criarDocumentoPendenteParaVenda(int vendaId, String criadoPor, String ambiente) throws SQLException {
        if (ambiente == null || ambiente.isBlank()) ambiente = DocumentoFiscalAmbiente.OFF;

        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);
            try {
                // Se já existe doc pra venda, não cria duplicado (evita bagunça)
                DocumentoFiscalModel existente = docDAO.buscarPorVenda(conn, vendaId);
                if (existente != null && !DocumentoFiscalStatus.CANCELADA.equals(existente.status)) {
                    conn.commit();
                    return existente;
                }

                int serie = 1;
                String modelo = "NFCe";
                int codigoModelo = 65;

                int numero = seqDAO.nextNumero(conn, modelo, codigoModelo, serie, ambiente);

                DocumentoFiscalModel d = new DocumentoFiscalModel();
                d.id = UUID.randomUUID().toString();
                d.vendaId = vendaId;
                d.modelo = modelo;
                d.codigoModelo = codigoModelo;
                d.serie = serie;
                d.numero = numero;
                d.ambiente = ambiente;
                d.status = DocumentoFiscalStatus.PENDENTE;
                d.criadoPor = criadoPor;

                // Totais puxados da venda (já confiáveis)
                preencherTotaisDaVenda(conn, d);

                docDAO.inserir(conn, d);

                // Snapshot itens
                snapshotItens(conn, d.id, vendaId);

                // Snapshot pagamentos (se houver)
                snapshotPagamentos(conn, d.id, vendaId);

                conn.commit();
                return d;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void preencherTotaisDaVenda(Connection conn, DocumentoFiscalModel d) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT total_bruto, desconto, acrescimo, total_liquido
            FROM vendas
            WHERE id = ?
        """)) {
            ps.setInt(1, d.vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Venda não encontrada: " + d.vendaId);

                d.totalProdutos = rs.getDouble("total_bruto");
                d.totalDesconto = rs.getDouble("desconto");
                d.totalAcrescimo = rs.getDouble("acrescimo");
                d.totalFinal = rs.getDouble("total_liquido");
            }
        }
    }

    private void snapshotItens(Connection conn, String docId, int vendaId) throws SQLException {
        // pega defaults do config_fiscal_default (se existir)
        FiscalDefaults defaults = carregarDefaultsFiscais(conn);

        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT
              vi.id AS venda_item_id,
              vi.produto_id,
              p.nome AS produto_nome,
              vi.qtd,
              vi.preco,
              vi.desconto,
              vi.acrescimo,
              vi.total_item,
              vi.observacoes,

              p.ncm, p.cfop, p.csosn, p.origem, p.unidade
            FROM vendas_itens vi
            LEFT JOIN produtos p ON p.id = vi.produto_id
            WHERE vi.venda_id = ?
            ORDER BY vi.id ASC
        """)) {
            ps.setInt(1, vendaId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;

                while (rs.next()) {
                    any = true;

                    DocumentoFiscalItemModel it = new DocumentoFiscalItemModel();
                    it.documentoId = docId;
                    it.vendaItemId = rs.getInt("venda_item_id");
                    it.produtoId = rs.getString("produto_id");

                    String nome = rs.getString("produto_nome");
                    if (nome == null || nome.isBlank()) nome = "Produto " + it.produtoId;
                    it.descricao = nome;

                    // resolve fiscal: produto -> defaults -> hard fallback
                    it.ncm = firstNonBlank(rs.getString("ncm"), defaults.ncm, DEF_NCM);
                    it.cfop = firstNonBlank(rs.getString("cfop"), defaults.cfop, DEF_CFOP);
                    it.csosn = firstNonBlank(rs.getString("csosn"), defaults.csosn, DEF_CSOSN);
                    it.origem = firstNonBlank(rs.getString("origem"), defaults.origem, DEF_ORIGEM);
                    it.unidade = firstNonBlank(rs.getString("unidade"), defaults.unidade, DEF_UN);

                    it.quantidade = rs.getInt("qtd");
                    it.valorUnit = rs.getDouble("preco");
                    it.desconto = rs.getDouble("desconto");
                    it.acrescimo = rs.getDouble("acrescimo");
                    it.totalItem = rs.getDouble("total_item");
                    it.observacoes = rs.getString("observacoes");

                    itemDAO.inserir(conn, it);
                }

                if (!any) throw new SQLException("Venda sem itens (venda_id=" + vendaId + ")");
            }
        }
    }

    private void snapshotPagamentos(Connection conn, String docId, int vendaId) throws SQLException {
        // Se não existir vendas_pagamentos, isso não quebra o fluxo.
        // Se existir mas não tiver linhas, também ok.
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT tipo, valor
            FROM vendas_pagamentos
            WHERE venda_id = ?
            ORDER BY id ASC
        """)) {
            ps.setInt(1, vendaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentoFiscalPagamentoModel p = new DocumentoFiscalPagamentoModel();
                    p.documentoId = docId;
                    p.tipo = rs.getString("tipo");
                    p.valor = rs.getDouble("valor");
                    pagDAO.inserir(conn, p);
                }
            }
        } catch (SQLException e) {
            // tabela pode não existir em algumas bases antigas
            // nesse caso, ignora: o documento fica sem pagamentos snapshot.
        }
    }

    // ===== helpers =====

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static class FiscalDefaults {
        String ncm = null;
        String cfop = null;
        String csosn = null;
        String origem = null;
        String unidade = null;
    }

    private FiscalDefaults carregarDefaultsFiscais(Connection conn) {
        FiscalDefaults d = new FiscalDefaults();

        // você criou config_fiscal_default com id='DEFAULT'
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT ncm_padrao, cfop_padrao, csosn_padrao, origem_padrao, unidade_padrao
            FROM config_fiscal_default
            WHERE id = 'DEFAULT'
            LIMIT 1
        """)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.ncm = rs.getString("ncm_padrao");
                    d.cfop = rs.getString("cfop_padrao");
                    d.csosn = rs.getString("csosn_padrao");
                    d.origem = rs.getString("origem_padrao");
                    d.unidade = rs.getString("unidade_padrao");
                }
            }
        } catch (Exception ignored) {
            // Se a tabela não existir (base antiga), segue hard fallback.
        }

        return d;
    }
}
