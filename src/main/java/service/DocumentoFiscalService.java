package service;

import dao.*;
import model.*;
import util.DB;
import util.LogService;

import java.sql.*;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
    private static final Path EXPORT_DIR = Paths.get("data", "export", "fiscal", "nfce");
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Object NFCE_LOCK = new Object();
    private static final int BUSY_RETRY_MAX = 10;
    private static final long BUSY_RETRY_BASE_MS = 1000;

    public DocumentoFiscalModel criarDocumentoPendenteParaVenda(int vendaId, String criadoPor, String ambiente) throws SQLException {
        final String ambienteFinal = normalizarAmbiente(ambiente);

        return runWithBusyRetry(() -> {
            synchronized (NFCE_LOCK) {
                try (Connection conn = DB.get()) {
                    conn.setAutoCommit(false);
                    boolean committed = false;
                    try {
                        LogService.info("[NFCE] Criando documento pendente venda=" + vendaId + " amb=" + ambienteFinal);

                        // Se já existe doc pra venda, não cria duplicado (evita bagunça)
                        DocumentoFiscalModel existente = docDAO.buscarPorVenda(conn, vendaId);
                        if (existente != null && !DocumentoFiscalStatus.CANCELADA.equals(existente.status)) {
                            LogService.info("[NFCE] Documento já existente para venda " + vendaId + ": " + existente.id);
                            conn.commit();
                            committed = true;
                            return existente;
                        }

                        int serie = 1;
                        String modelo = "NFCe";
                        int codigoModelo = 65;

                        int numero = seqDAO.nextNumero(conn, modelo, serie, ambienteFinal);

                        DocumentoFiscalModel d = new DocumentoFiscalModel();
                        d.id = UUID.randomUUID().toString();
                        d.vendaId = vendaId;
                        d.modelo = modelo;
                        d.codigoModelo = codigoModelo;
                        d.serie = serie;
                        d.numero = numero;
                        d.ambiente = ambienteFinal;
                        d.status = DocumentoFiscalStatus.PENDENTE;
                        d.criadoPor = criadoPor;

                        // Totais puxados da venda (já confiáveis)
                        preencherTotaisDaVenda(conn, d);

                        try {
                            docDAO.inserir(conn, d);
                        } catch (SQLException se) {
                            if (isUniqueVendaDoc(se)) {
                                LogService.warn("[NFCE] Documento duplicado detectado por venda=" + vendaId + ", retornando existente.");
                                conn.rollback();
                                DocumentoFiscalModel jaExiste = docDAO.buscarPorVenda(conn, vendaId);
                                if (jaExiste != null) {
                                    return jaExiste;
                                }
                            }
                            throw se;
                        }

                        // Snapshot itens
                        snapshotItens(conn, d.id, vendaId);

                        // Snapshot pagamentos (se houver)
                        snapshotPagamentos(conn, d.id, vendaId);

                        conn.commit();
                        committed = true;
                        LogService.info("[NFCE] Documento criado id=" + d.id + " numero=" + d.numero);
                        return d;

                    } catch (Exception e) {
                        if (!committed) {
                            conn.rollback();
                        }
                        LogService.error("[NFCE] Falha ao criar documento pendente venda=" + vendaId, e);
                        throw e;
                    } finally {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException ignored) {
                        }
                    }
                }
            }
        });
    }

    private static String normalizarAmbiente(String ambiente) {
        if (ambiente == null || ambiente.isBlank()) return DocumentoFiscalAmbiente.OFF;
        String norm = ambiente.trim().toUpperCase();
        if (norm.startsWith("PROD")) return DocumentoFiscalAmbiente.PRODUCAO;
        if (norm.startsWith("HOM")) return DocumentoFiscalAmbiente.HOMOLOG;
        if ("OFF".equals(norm) || "OFFLINE".equals(norm)) return DocumentoFiscalAmbiente.OFF;
        return DocumentoFiscalAmbiente.HOMOLOG;
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

    // ===== ETAPA 8: ORQUESTRAÇÃO - Métodos de processamento =====

    /**
     * Calcula impostos para todos os itens do documento
     */
    public void calcularImpostos(String documentoId) throws Exception {
        try (Connection conn = DB.get()) {
            DocumentoFiscalModel doc = docDAO.buscarPorId(conn, documentoId);
            if (doc == null) throw new SQLException("Documento não encontrado: " + documentoId);

            String uf = resolverUfFiscal();
            FiscalCalcService calcService = new FiscalCalcService();
            
            // Para cada item, calcula impostos e atualiza totais
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM documentos_fiscais_itens WHERE documento_id = ?")) {
                ps.setString(1, documentoId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ncm = rs.getString("ncm");
                        String csosn = rs.getString("csosn");
                        String origem = rs.getString("origem");
                        double valorItem = rs.getDouble("total_item");
                        
                        // Calcula todos impostos
                        var impostos = calcService.calcularImpostosSimples(ncm, csosn, origem, valorItem);
                        
                        System.out.println("[FiscalDoc] Item " + rs.getInt("id") + 
                            " NCM=" + ncm + " Valor=" + valorItem + 
                            " ICMS=" + (impostos.icms != null ? impostos.icms.valor : 0));
                    }
                }
            }
        }
    }

    /**
     * Gera XML NFC-e a partir do documento
     */
    public String gerarXml(String documentoId) throws Exception {
        return runWithBusyRetry(() -> {
            synchronized (NFCE_LOCK) {
                try (Connection conn = DB.get()) {
                    conn.setAutoCommit(false);
                    boolean committed = false;
                    try {
                        LogService.info("[NFCE] Gerando XML para doc=" + documentoId);

                        DocumentoFiscalModel doc = docDAO.buscarPorId(conn, documentoId);
                        if (doc == null) throw new SQLException("Documento não encontrado: " + documentoId);

                        ConfigNfceModel config = new ConfigNfceDAO().getConfig(conn);
                        if (config == null) throw new Exception("Configuração fiscal não encontrada");

                        // Busca itens com impostos calculados
                        java.util.List<DocumentoFiscalModel.ItemComImpostos> itens = buscarItensComImpostos(conn, documentoId);

                        // Monta XML
                        XmlBuilderNfce builder = new XmlBuilderNfce(doc, config, itens);
                        String xml = builder.construir();
                        doc.chaveAcesso = builder.getChaveAcesso();

                        // Atualiza documento
                        doc.xml = xml;
                        doc.status = DocumentoFiscalStatus.XML_GERADO;
                        docDAO.atualizarStatus(conn, documentoId, DocumentoFiscalStatus.XML_GERADO, null, null, doc.chaveAcesso, null);

                        conn.commit();
                        committed = true;

                        try {
                            exportarXmlPadrao(conn, doc, xml, "pre");
                        } catch (Exception e) {
                            LogService.warn("[NFCE] Não foi possível exportar XML gerado: " + e.getMessage());
                        }

                        LogService.info("[NFCE] XML gerado doc=" + documentoId + " tam=" + xml.length());
                        return xml;
                    } catch (Exception e) {
                        if (!committed) {
                            conn.rollback();
                        }
                        LogService.error("[NFCE] Falha ao gerar XML doc=" + documentoId, e);
                        throw e;
                    } finally {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException ignored) {
                        }
                    }
                }
            }
        });
    }

    /**
     * Assina XML com certificado A1
     */
    public String assinarXml(String documentoId, String certificadoPath, String certificadoSenha) throws Exception {
        try (Connection conn = DB.get()) {
            DocumentoFiscalModel doc = docDAO.buscarPorId(conn, documentoId);
            if (doc == null) throw new SQLException("Documento não encontrado");

            String xmlOrig = carregarXmlDocumento(conn, doc);
            if (xmlOrig == null || xmlOrig.isBlank()) throw new SQLException("XML base não encontrado");

            // Carrega certificado e assina
            XmlAssinaturaService signer = new XmlAssinaturaService(certificadoPath, certificadoSenha);
            signer.validarCertificado();
            String xmlAssinado = signer.assinarXml(xmlOrig);

            // Atualiza
            docDAO.atualizarStatus(conn, documentoId, DocumentoFiscalStatus.ASSINADA, null, null, null, null);

            try {
                exportarXmlPadrao(conn, doc, xmlAssinado, "assinado");
            } catch (Exception e) {
                System.out.println("[FiscalDoc] Aviso: nÃ£o foi possÃ­vel exportar XML assinado: " + e.getMessage());
            }

            System.out.println("[FiscalDoc] XML assinado");
            return xmlAssinado;
        }
    }

    /**
     * Envia documento para SEFAZ
     */
    public SefazClientSoap.RespostaSefaz enviarSefaz(String documentoId, String certificadoPath, String certificadoSenha, boolean producao) throws Exception {
        try (Connection conn = DB.get()) {
            DocumentoFiscalModel doc = docDAO.buscarPorId(conn, documentoId);
            if (doc == null) throw new SQLException("Documento não encontrado");

            ConfigNfceModel config = new ConfigNfceDAO().getConfig();
            if (config == null) throw new Exception("Configuração fiscal não encontrada");

            // Cliente SEFAZ
            SefazClientSoap sefazClient = new SefazClientSoap(
                "https://sefaz.rs.gov.br/webservice/",
                certificadoPath,
                certificadoSenha
            );

            // Envia XML assinado
            String xmlAssinado = carregarXmlDocumento(conn, doc);
            if (xmlAssinado == null || xmlAssinado.isBlank()) {
                throw new SQLException("XML assinado não encontrado para envio");
            }
            SefazClientSoap.RespostaSefaz resposta = sefazClient.enviarLoteNfce(xmlAssinado, producao);

            // Atualiza documento conforme resposta
            String novoStatus;
            String erro = null;
            String protocolo = null;
            String chave = null;

            if (resposta.eAutorizada()) {
                novoStatus = DocumentoFiscalStatus.AUTORIZADA;
                protocolo = resposta.protocolo;
                chave = resposta.chaveAcesso;
                System.out.println("[FiscalDoc] ✅ AUTORIZADO - Protocolo: " + protocolo);
            } else if (resposta.ehRejeitada()) {
                novoStatus = DocumentoFiscalStatus.REJEITADA;
                erro = resposta.mensagemErro;
                System.out.println("[FiscalDoc] ❌ REJEITADO - " + erro);
            } else {
                novoStatus = DocumentoFiscalStatus.ERRO;
                erro = resposta.mensagemErro;
                System.out.println("[FiscalDoc] ⚠️ ERRO - " + erro);
            }

            docDAO.atualizarStatus(conn, documentoId, novoStatus, erro, null, chave, protocolo);
            return resposta;
        }
    }

    /**
     * Imprime DANFE em arquivo
     */
    public void imprimirDanfe(String documentoId, String caminhoSaida) throws Exception {
        try (Connection conn = DB.get()) {
            DocumentoFiscalModel doc = docDAO.buscarPorId(conn, documentoId);
            if (doc == null) throw new SQLException("Documento não encontrado");

            ConfigNfceModel config = new ConfigNfceDAO().getConfig();
            if (config == null) throw new Exception("Configuração fiscal não encontrada");

            java.util.List<DocumentoFiscalModel.ItemComImpostos> itens = buscarItensComImpostos(conn, documentoId);

            // Gera DANFE
            DanfeNfceGenerator danfeGen = new DanfeNfceGenerator(doc, config, itens);
            danfeGen.salvarEmArquivo(caminhoSaida);

            System.out.println("[FiscalDoc] DANFE salvo: " + caminhoSaida);
        }
    }

    /**
     * Busca itens do documento com impostos calculados
     */
    private java.util.List<DocumentoFiscalModel.ItemComImpostos> buscarItensComImpostos(Connection conn, String documentoId) throws Exception {
        var itens = new java.util.ArrayList<DocumentoFiscalModel.ItemComImpostos>();
        FiscalCalcService calcService = new FiscalCalcService();
        String uf = resolverUfFiscal();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM documentos_fiscais_itens WHERE documento_id = ?")) {
            ps.setString(1, documentoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var item = new DocumentoFiscalModel.ItemComImpostos();
                    item.setProdutoId(rs.getString("produto_id"));
                    item.setNcm(rs.getString("ncm"));
                    item.setDescricao(rs.getString("descricao"));
                    item.setQuantidade(rs.getDouble("quantidade"));
                    item.setValorUnit(rs.getDouble("valor_unit"));
                    item.setDesconto(rs.getDouble("desconto"));
                    item.setAcrescimo(rs.getDouble("acrescimo"));
                    item.setTotalItem(rs.getDouble("total_item"));
                    item.setCfop(rs.getString("cfop"));
                    item.setCsosn(rs.getString("csosn"));
                    item.setOrigem(rs.getString("origem"));
                    item.setUnidade(rs.getString("unidade"));
                    
                    // Calcula impostos
                    item.setImpostos(calcService.calcularImpostosSimples(
                        item.getNcm(), item.getCsosn(), item.getOrigem(), item.getTotalItem()
                    ));
                    
                    itens.add(item);
                }
            }
        }

        return itens;
    }

    private String resolverUfFiscal() {
        try {
            ConfigNfceModel cfg = new ConfigNfceDAO().getConfig();
            String uf = (cfg != null) ? cfg.getUf() : null;
            uf = (uf == null) ? "" : uf.trim().toUpperCase();
            if (uf.equals("MS") || uf.equals("SP") || uf.equals("MT") || uf.equals("PR")) {
                return uf;
            }
        } catch (Exception ignored) {
        }
        return "SP";
    }

    private void exportarXmlPadrao(Connection conn, DocumentoFiscalModel doc, String xml, String tipo) throws IOException, SQLException {
        if (doc == null || xml == null || xml.isBlank()) return;

        String ambiente = (doc.ambiente != null && !doc.ambiente.isBlank())
                ? doc.ambiente.trim().toLowerCase()
                : "desconhecido";
        String mes = LocalDate.now().format(YM);

        Path base = EXPORT_DIR.resolve(ambiente).resolve(mes);
        Files.createDirectories(base);

        String kind = (tipo != null && !tipo.isBlank()) ? tipo.trim().toLowerCase() : "xml";
        String nome = "NFCe_" + doc.id + "_" + kind + ".xml";
        Path out = base.resolve(nome);
        Files.writeString(out, xml, StandardCharsets.UTF_8);

        String sha = sha256(xml);
        int tamanho = xml.getBytes(StandardCharsets.UTF_8).length;
        docDAO.atualizarXmlInfo(conn, doc.id, out.toString(), sha, tamanho);
    }

    private String carregarXmlDocumento(Connection conn, DocumentoFiscalModel doc) throws IOException, SQLException {
        if (doc.xml != null && !doc.xml.isBlank()) return doc.xml;
        if (doc.xmlPath == null || doc.xmlPath.isBlank()) return null;
        Path p = Paths.get(doc.xmlPath);
        if (!Files.exists(p)) return null;
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SqlOp<T> {
        T run() throws Exception;
    }

    private <T> T runWithBusyRetry(SqlOp<T> op) throws SQLException {
        int attempt = 0;
        while (true) {
            try {
                return op.run();
            } catch (SQLException e) {
                attempt++;
                if (!isBusySnapshot(e) || attempt >= BUSY_RETRY_MAX) {
                    throw e;
                }
                long wait = BUSY_RETRY_BASE_MS * attempt;
                LogService.warn("[NFCE] database busy (attempt " + attempt + "), aguardando " + wait + "ms. Erro: " + e.getMessage());
                sleepQuiet(wait);
            } catch (Exception e) {
                if (e instanceof SQLException se) {
                    attempt++;
                    if (!isBusySnapshot(se) || attempt >= BUSY_RETRY_MAX) {
                        throw se;
                    }
                    long wait = BUSY_RETRY_BASE_MS * attempt;
                    LogService.warn("[NFCE] database busy (attempt " + attempt + "), aguardando " + wait + "ms. Erro: " + se.getMessage());
                    sleepQuiet(wait);
                } else {
                    throw new SQLException("Erro fiscal: " + e.getMessage(), e);
                }
            }
        }
    }

    private boolean isBusySnapshot(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("sqlite_busy_snapshot") || m.contains("database is locked");
    }

    private boolean isUniqueVendaDoc(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("unique") && (m.contains("documentos_fiscais.venda_id") || m.contains("ux_doc_fiscal_venda"));
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
