package service;

import model.DocumentoFiscalModel;
import model.ConfigNfceModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gerador de DANFE NFC-e em PDF com QRCode.
 * Formato: papel 80mm x 120mm (largura x altura).
 * Conteúdo: cabeçalho, itens, totais, forma de pagamento, QRCode, chave de acesso.
 * 
 * Para PDF real, usar iText, Apache PDFBox ou DynamicReports.
 * Aqui: versão simplificada em texto (placeholder para impressão).
 */
public class DanfeNfceGenerator {

    private final DocumentoFiscalModel doc;
    private final ConfigNfceModel config;
    private final List<DocumentoFiscalModel.ItemComImpostos> itens;

    // Constantes de formatação
    private static final String FORMAT_80 = "%-80s%n";
    private static final String FORMAT_60_10 = "%-60s %10s%n";
    private static final String CURRENCY = "R$ %.2f";

    public DanfeNfceGenerator(DocumentoFiscalModel doc, ConfigNfceModel config,
                             List<DocumentoFiscalModel.ItemComImpostos> itens) {
        this.doc = doc;
        this.config = config;
        this.itens = itens;
    }

    /**
     * Gera DANFE em formato texto (para impressão térmica 80mm)
     * Em produção, gerar PDF real
     */
    public String gerarDANFETexto() {
        StringBuilder danfe = new StringBuilder();

        // Linhas de separação
        String linha = "=" + "=".repeat(76) + "=\n";
        String sublinha = "-" + "-".repeat(76) + "-\n";

        // Cabeçalho
        danfe.append(linha);
        danfe.append(String.format(FORMAT_80, centralizarTexto("NOTA FISCAL ELETRÔNICA DO CONSUMIDOR")));
        danfe.append(String.format(FORMAT_80, centralizarTexto("(NFCE)")));
        danfe.append(linha);

        // Dados da empresa
        danfe.append(String.format(FORMAT_80, "EMPRESA: " + (config.getNomeEmpresa() != null ? config.getNomeEmpresa() : "HoStore")));
        danfe.append(String.format(FORMAT_80, "CNPJ: " + config.getCnpj()));
        danfe.append(String.format(FORMAT_80, "IE: " + (config.getInscricaoEstadual() != null ? config.getInscricaoEstadual() : "ISENTO")));
        danfe.append(sublinha);

        // Dados do documento
        danfe.append(String.format("%-40s%40s%n", "NFC-e: " + doc.numero, "Série: " + doc.serie));
        danfe.append(String.format("%-40s%40s%n", 
            "Emissão: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
            "Autorização: " + (doc.protocolo != null ? doc.protocolo : "PENDENTE")));
        danfe.append(sublinha);

        // Itens
        danfe.append(String.format(FORMAT_80, "ITENS"));
        danfe.append(sublinha);

        for (DocumentoFiscalModel.ItemComImpostos item : itens) {
            danfe.append(String.format(FORMAT_60_10, 
                item.getDescricao(), 
                String.format(CURRENCY, item.getTotalItem())));
            danfe.append(String.format("  Qtd: %.0f x " + CURRENCY + "%n", 
                item.getQuantidade(), item.getValorUnit()));
        }

        danfe.append(sublinha);

        // Totais
        danfe.append(String.format(FORMAT_60_10, 
            "SUBTOTAL", String.format(CURRENCY, doc.totalProdutos != null ? doc.totalProdutos : 0.0)));
        
        if (doc.totalDesconto != null && doc.totalDesconto > 0) {
            danfe.append(String.format(FORMAT_60_10, 
                "DESCONTO", String.format(CURRENCY, doc.totalDesconto)));
        }
        
        if (doc.totalAcrescimo != null && doc.totalAcrescimo > 0) {
            danfe.append(String.format(FORMAT_60_10, 
                "ACRÉSCIMO", String.format(CURRENCY, doc.totalAcrescimo)));
        }

        danfe.append(linha);
        danfe.append(String.format(FORMAT_60_10, 
            "TOTAL", String.format(CURRENCY, doc.totalFinal != null ? doc.totalFinal : 0.0)));
        danfe.append(linha);

        // Formas de pagamento
        danfe.append(String.format(FORMAT_80, "FORMA DE PAGAMENTO"));
        danfe.append(String.format(FORMAT_80, "Dinheiro / PIX / Cartão"));
        danfe.append(sublinha);

                // QRCode (URL)
        String qrUrl = gerarURLQRCode(config.getCsc(), config.getIdCsc());
        danfe.append(String.format(FORMAT_80, "QR-CODE (Consulte em)"));
        danfe.append(String.format(FORMAT_80, qrUrl));
        danfe.append(sublinha);

        // Chave de acesso
        if (doc.chaveAcesso != null && !doc.chaveAcesso.isBlank()) {
            String chaveFormatada = formatarChave(doc.chaveAcesso);
            danfe.append(String.format(FORMAT_80, centralizarTexto("CHAVE DE ACESSO")));
            danfe.append(String.format(FORMAT_80, centralizarTexto(chaveFormatada)));
        } else {
            danfe.append(String.format(FORMAT_80, centralizarTexto("CHAVE DE ACESSO PENDENTE")));
        }

        danfe.append(sublinha);

        // Rodapé
        danfe.append(String.format(FORMAT_80, centralizarTexto("EMITIDO PELO HOSTORE")));
        danfe.append(String.format(FORMAT_80, centralizarTexto("DATA: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))));
        danfe.append(linha);

        return danfe.toString();
    }

    /**
     * Gera DANFE em PDF (placeholder - em produção, usar iText ou PDFBox)
     */
    public byte[] gerarDANFEPdf() throws Exception {
        // Placeholder: em produção, usar biblioteca PDF
        String textoDanfe = gerarDANFETexto();
        return textoDanfe.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Salva DANFE em arquivo
     */
    public void salvarEmArquivo(String caminhoSaida) throws IOException {
        String danfe = gerarDANFETexto();
        try (FileWriter fw = new FileWriter(caminhoSaida)) {
            fw.write(danfe);
        }
    }

    /**
     * Gera URL do QRCode da NFC-e
     */
    public String gerarURLQRCode(String csc, Integer idCSC) {
        if (doc.chaveAcesso == null || doc.chaveAcesso.isBlank() || csc == null || csc.isBlank() || idCSC == null || idCSC <= 0) {
            return "QRCode indisponivel (CSC ou chave faltando)";
        }

        String uf = config.getUf() != null ? config.getUf().trim().toLowerCase() : "rs";
        String assinatura = gerarHashCSC(doc.chaveAcesso, csc, idCSC);

        return String.format(
            "https://www.sefaz%s.gov.br/nfce/qrcode?chNFe=%s&idToken=%s&cHashQRCode=%s",
            uf,
            doc.chaveAcesso,
            idCSC,
            assinatura
        );
    }

    /**
     * Gera hash CSC para QRCode (SHA-256)
     */
    private String gerarHashCSC(String chaveAcesso, String csc, Integer idCSC) {
        try {
            String entrada = chaveAcesso + csc + (idCSC != null ? idCSC : "");
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(entrada.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    // ==================== Helpers ====================

    private String centralizarTexto(String texto) {
        int totalLength = 80;
        int padding = (totalLength - texto.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + texto;
    }

    private String formatarChave(String chave) {
        // Formata chave: XXXX XXXX XXXX XXXX XXXX XXXX XXXX XXXX XXXX
        if (chave == null || chave.length() < 44) return chave;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chave.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(" ");
            sb.append(chave.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Modelo para dados de DANFE em JSON (para integração com APIs)
     */
    public static class DadosDANFE {
        private String empresa;
        private String cnpj;
        private String nfce;
        private int serie;
        private String dataEmissao;
        private String chaveAcesso;
        private double totalFinal;
        private String statusNFce;
        private String protocolo;
        private String qrcodeUrl;

        // Getters and Setters
        public String getEmpresa() {
            return empresa;
        }

        public void setEmpresa(String empresa) {
            this.empresa = empresa;
        }

        public String getCnpj() {
            return cnpj;
        }

        public void setCnpj(String cnpj) {
            this.cnpj = cnpj;
        }

        public String getNfce() {
            return nfce;
        }

        public void setNfce(String nfce) {
            this.nfce = nfce;
        }

        public int getSerie() {
            return serie;
        }

        public void setSerie(int serie) {
            this.serie = serie;
        }

        public String getDataEmissao() {
            return dataEmissao;
        }

        public void setDataEmissao(String dataEmissao) {
            this.dataEmissao = dataEmissao;
        }

        public String getChaveAcesso() {
            return chaveAcesso;
        }

        public void setChaveAcesso(String chaveAcesso) {
            this.chaveAcesso = chaveAcesso;
        }

        public double getTotalFinal() {
            return totalFinal;
        }

        public void setTotalFinal(double totalFinal) {
            this.totalFinal = totalFinal;
        }

        public String getStatusNFce() {
            return statusNFce;
        }

        public void setStatusNFce(String statusNFce) {
            this.statusNFce = statusNFce;
        }

        public String getProtocolo() {
            return protocolo;
        }

        public void setProtocolo(String protocolo) {
            this.protocolo = protocolo;
        }

        public String getQrcodeUrl() {
            return qrcodeUrl;
        }

        public void setQrcodeUrl(String qrcodeUrl) {
            this.qrcodeUrl = qrcodeUrl;
        }
    }
}
