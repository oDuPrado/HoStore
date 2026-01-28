package service;

import model.ConfigNfceModel;
import model.DocumentoFiscalModel;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Constroi XML de NFC-e (modelo 65) conforme layout 4.00.
 * Estrutura: Identificacao, Emitente, Destinatario (opcional), Detalhes, Totais, Pagamentos.
 */
public class XmlBuilderNfce {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Locale LOCALE_XML = Locale.US;

    private final DocumentoFiscalModel doc;
    private final ConfigNfceModel config;
    private final List<DocumentoFiscalModel.ItemComImpostos> itens;
    private final String cNF;
    private final String chaveAcesso;
    private final String cDV;
    private final OffsetDateTime dhEmi;

    public XmlBuilderNfce(DocumentoFiscalModel doc, ConfigNfceModel config,
                          List<DocumentoFiscalModel.ItemComImpostos> itens) {
        this.doc = doc;
        this.config = config;
        this.itens = itens;
        this.dhEmi = OffsetDateTime.now();
        this.cNF = gerarCNFAleatorio();
        this.chaveAcesso = gerarChaveAcesso();
        this.cDV = chaveAcesso.substring(chaveAcesso.length() - 1);
    }

    /**
     * Constroi XML completo de NFC-e
     */
    public String construir() {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">\n");

        String infNfeId = "NFe" + chaveAcesso;
        xml.append("  <infNFe Id=\"").append(infNfeId).append("\" versao=\"4.00\">\n");

        xml.append(buildIde());
        xml.append(buildEmit());

        // Destinatario opcional (somente se identificado)
        xml.append(buildDest());

        for (int idx = 0; idx < itens.size(); idx++) {
            xml.append(buildDetItem(idx + 1, itens.get(idx)));
        }

        xml.append(buildTotal());
        xml.append(buildPag());
        xml.append(buildTransp());
        xml.append(buildInfAdic());

        xml.append("  </infNFe>\n");
        xml.append(buildInfNFeSupl());
        xml.append("</NFe>");

        return xml.toString();
    }

    private String buildIde() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <ide>\n");
        String uf = (config.getUf() != null && !config.getUf().isBlank()) ? config.getUf() : "RS";
        sb.append("      <cUF>").append(ufParaCodigo(uf)).append("</cUF>\n");
        sb.append("      <cNF>").append(cNF).append("</cNF>\n");
        sb.append("      <natOp>").append(escapeXml(resolveNatOp())).append("</natOp>\n");
        sb.append("      <mod>65</mod>\n");
        sb.append("      <serie>").append(doc.serie).append("</serie>\n");
        sb.append("      <nNF>").append(doc.numero).append("</nNF>\n");
        sb.append("      <dhEmi>").append(dhEmi.format(DT_FORMAT)).append("</dhEmi>\n");
        sb.append("      <tpAmb>").append(isProducao() ? "1" : "2").append("</tpAmb>\n");
        sb.append("      <tpEmis>1</tpEmis>\n");
        sb.append("      <cDV>").append(cDV).append("</cDV>\n");
        sb.append("      <tpNF>1</tpNF>\n");
        sb.append("      <idDest>1</idDest>\n");
        sb.append("      <cMunFG>").append(obterCodigoMunicipio(uf, config.getEnderecoMunicipio())).append("</cMunFG>\n");
        sb.append("      <tpImp>4</tpImp>\n");
        sb.append("      <finNFe>1</finNFe>\n");
        sb.append("      <indFinal>1</indFinal>\n");
        sb.append("      <indPres>1</indPres>\n");
        sb.append("      <procEmi>0</procEmi>\n");
        sb.append("      <verProc>HoStore</verProc>\n");
        sb.append("    </ide>\n");
        return sb.toString();
    }

    private String buildEmit() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <emit>\n");
        sb.append("      <CNPJ>").append(limpaCNPJ(config.getCnpj())).append("</CNPJ>\n");
        if (config.getInscricaoEstadual() != null && !config.getInscricaoEstadual().isBlank()) {
            sb.append("      <IE>").append(config.getInscricaoEstadual()).append("</IE>\n");
        } else {
            sb.append("      <IE>ISENTO</IE>\n");
        }
        sb.append("      <xNome>").append(escapeXml(config.getNomeEmpresa() != null ? config.getNomeEmpresa() : "HoStore")).append("</xNome>\n");
        sb.append("      <xFant>").append(escapeXml(config.getNomeFantasia() != null ? config.getNomeFantasia() : "HoStore")).append("</xFant>\n");

        sb.append("      <enderEmit>\n");
        sb.append("        <xLgr>").append(escapeXml(config.getEnderecoLogradouro() != null ? config.getEnderecoLogradouro() : "Rua Principal")).append("</xLgr>\n");
        sb.append("        <nro>").append(config.getEnderecoNumero() != null ? config.getEnderecoNumero() : "1").append("</nro>\n");
        if (config.getEnderecoComplemento() != null && !config.getEnderecoComplemento().isBlank()) {
            sb.append("        <xCpl>").append(escapeXml(config.getEnderecoComplemento())).append("</xCpl>\n");
        }
        sb.append("        <xBairro>").append(escapeXml(config.getEnderecoBairro() != null ? config.getEnderecoBairro() : "Centro")).append("</xBairro>\n");
        sb.append("        <cMun>").append(obterCodigoMunicipio(config.getUf(), config.getEnderecoMunicipio())).append("</cMun>\n");
        sb.append("        <xMun>").append(escapeXml(config.getEnderecoMunicipio() != null ? config.getEnderecoMunicipio() : "Porto Alegre")).append("</xMun>\n");
        sb.append("        <UF>").append(config.getUf() != null ? config.getUf() : "RS").append("</UF>\n");
        sb.append("        <CEP>").append(limpaNumeros(config.getEnderecoCep() != null ? config.getEnderecoCep() : "00000000")).append("</CEP>\n");
        sb.append("      </enderEmit>\n");

        sb.append("    </emit>\n");
        return sb.toString();
    }

    private String buildDest() {
        // Para NFC-e, destinatario e opcional quando consumidor final nao identificado.
        return "";
    }

    private String buildDetItem(int nItem, DocumentoFiscalModel.ItemComImpostos item) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <det nItem=\"").append(nItem).append("\">\n");

        sb.append("      <prod>\n");
        sb.append("        <CProd>").append(item.getProdutoId() != null ? item.getProdutoId() : "0").append("</CProd>\n");
        sb.append("        <cBarra/>\n");
        sb.append("        <xProd>").append(escapeXml(item.getDescricao())).append("</xProd>\n");
        sb.append("        <NCM>").append(item.getNcm() != null ? item.getNcm() : "00000000").append("</NCM>\n");
        sb.append("        <CFOP>").append(item.getCfop() != null ? item.getCfop() : "5102").append("</CFOP>\n");
        sb.append("        <uCom>").append(item.getUnidade() != null ? item.getUnidade() : "UN").append("</uCom>\n");
        sb.append("        <qCom>").append(fmt4(item.getQuantidade())).append("</qCom>\n");
        sb.append("        <vUnCom>").append(fmt2(item.getValorUnit())).append("</vUnCom>\n");
        sb.append("        <vProd>").append(fmt2(item.getTotalItem())).append("</vProd>\n");

        if (item.getDesconto() > 0) {
            sb.append("        <vDesc>").append(fmt2(item.getDesconto())).append("</vDesc>\n");
        }

        if (item.getAcrescimo() > 0) {
            sb.append("        <vOutro>").append(fmt2(item.getAcrescimo())).append("</vOutro>\n");
        }

        sb.append("        <indTot>1</indTot>\n");
        sb.append("      </prod>\n");

        sb.append("      <imposto>\n");

        if (item.getImpostos() != null && item.getImpostos().icms != null) {
            sb.append(buildICMSSN(item.getOrigem(), item.getImpostos().icms.cst));
        }

        if (item.getImpostos() != null && item.getImpostos().ipi != null && item.getImpostos().ipi.valor > 0) {
            sb.append("        <IPI>\n");
            sb.append("          <IPITrib>\n");
            sb.append("            <CST>49</CST>\n");
            sb.append("            <vIPI>").append(fmt2(item.getImpostos().ipi.valor)).append("</vIPI>\n");
            sb.append("          </IPITrib>\n");
            sb.append("        </IPI>\n");
        }

        sb.append(buildPIS());
        sb.append(buildCOFINS());

        sb.append("      </imposto>\n");
        sb.append("    </det>\n");

        return sb.toString();
    }

    private String buildICMSSN(String origem, String csosn) {
        StringBuilder sb = new StringBuilder();
        sb.append("        <ICMS>\n");
        String orig = (origem == null || origem.isBlank()) ? "0" : origem;
        String cs = (csosn == null || csosn.isBlank()) ? "102" : csosn;
        sb.append("          <ICMSSN102>\n");
        sb.append("            <orig>").append(orig).append("</orig>\n");
        sb.append("            <CSOSN>").append(cs).append("</CSOSN>\n");
        sb.append("          </ICMSSN102>\n");
        sb.append("        </ICMS>\n");
        return sb.toString();
    }

    private String buildPIS() {
        StringBuilder sb = new StringBuilder();
        sb.append("        <PIS>\n");
        sb.append("          <PISNT>\n");
        sb.append("            <CST>04</CST>\n");
        sb.append("          </PISNT>\n");
        sb.append("        </PIS>\n");
        return sb.toString();
    }

    private String buildCOFINS() {
        StringBuilder sb = new StringBuilder();
        sb.append("        <COFINS>\n");
        sb.append("          <COFINSNT>\n");
        sb.append("            <CST>04</CST>\n");
        sb.append("          </COFINSNT>\n");
        sb.append("        </COFINS>\n");
        return sb.toString();
    }

    private String buildTotal() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <total>\n");

        double vProd = doc.totalProdutos != null ? doc.totalProdutos : 0;
        double vDesc = doc.totalDesconto != null ? doc.totalDesconto : 0;
        double vOutro = doc.totalAcrescimo != null ? doc.totalAcrescimo : 0;
        double vNF = doc.totalFinal != null ? doc.totalFinal : (vProd - vDesc + vOutro);

        sb.append("      <ICMSTot>\n");
        sb.append("        <vBC>").append(fmt2(vProd)).append("</vBC>\n");
        sb.append("        <vICMS>0.00</vICMS>\n");
        sb.append("        <vBCST>0.00</vBCST>\n");
        sb.append("        <vST>0.00</vST>\n");
        sb.append("        <vProd>").append(fmt2(vProd)).append("</vProd>\n");
        sb.append("        <vFrete>0.00</vFrete>\n");
        sb.append("        <vSeg>0.00</vSeg>\n");
        sb.append("        <vDesc>").append(fmt2(vDesc)).append("</vDesc>\n");
        sb.append("        <vII>0.00</vII>\n");
        sb.append("        <vIPI>0.00</vIPI>\n");
        sb.append("        <vPIS>0.00</vPIS>\n");
        sb.append("        <vCOFINS>0.00</vCOFINS>\n");
        sb.append("        <vOutro>").append(fmt2(vOutro)).append("</vOutro>\n");
        sb.append("        <vNF>").append(fmt2(vNF)).append("</vNF>\n");
        sb.append("      </ICMSTot>\n");

        sb.append("    </total>\n");
        return sb.toString();
    }

    private String buildPag() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <pag>\n");
        sb.append("      <detPag>\n");
        sb.append("        <tPag>01</tPag>\n");
        sb.append("        <vPag>").append(fmt2(doc.totalFinal != null ? doc.totalFinal : 0.0)).append("</vPag>\n");
        sb.append("      </detPag>\n");
        sb.append("    </pag>\n");
        return sb.toString();
    }

    private String buildTransp() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <transp modFrete=\"9\">\n");
        sb.append("    </transp>\n");
        return sb.toString();
    }

    private String buildInfAdic() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <infAdic>\n");
        String vendaInfo = doc.vendaId != null ? " | VENDA ID: " + doc.vendaId : "";
        sb.append("      <infCpl>NFC-e gerada pelo HoStore").append(vendaInfo).append("</infCpl>\n");
        sb.append("    </infAdic>\n");
        return sb.toString();
    }

    private String buildInfNFeSupl() {
        String urlChave = gerarUrlChave();
        String qrCode = gerarUrlQrCode();
        if (urlChave == null || qrCode == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  <infNFeSupl>\n");
        sb.append("    <qrCode><![CDATA[").append(qrCode).append("]]></qrCode>\n");
        sb.append("    <urlChave>").append(urlChave).append("</urlChave>\n");
        sb.append("  </infNFeSupl>\n");
        return sb.toString();
    }

    private String gerarCNFAleatorio() {
        SecureRandom r = new SecureRandom();
        int n = r.nextInt(100_000_000);
        return String.format("%08d", n);
    }

    private boolean isProducao() {
        return "PRODUCAO".equalsIgnoreCase(config.getAmbiente()) ||
               "producao".equalsIgnoreCase(config.getAmbiente());
    }

    private String ufParaCodigo(String uf) {
        return switch (uf != null ? uf.toUpperCase() : "RS") {
            case "AC" -> "12";
            case "AL" -> "27";
            case "AP" -> "16";
            case "AM" -> "13";
            case "BA" -> "29";
            case "CE" -> "23";
            case "DF" -> "53";
            case "ES" -> "32";
            case "GO" -> "52";
            case "MA" -> "21";
            case "MT" -> "51";
            case "MS" -> "50";
            case "MG" -> "31";
            case "PA" -> "15";
            case "PB" -> "25";
            case "PR" -> "41";
            case "PE" -> "26";
            case "PI" -> "22";
            case "RJ" -> "33";
            case "RN" -> "24";
            case "RS" -> "43";
            case "RO" -> "11";
            case "RR" -> "14";
            case "SC" -> "42";
            case "SP" -> "35";
            case "SE" -> "28";
            case "TO" -> "17";
            default -> "43";
        };
    }

    private String limpaCNPJ(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : "00000000000000";
    }

    private String limpaNumeros(String valor) {
        return valor != null ? valor.replaceAll("\\D", "") : "";
    }

    private String escapeXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private int obterCodigoMunicipio(String uf, String municipio) {
        // Fallback: codigo do IBGE simplificado (em producao, usar tabela completa)
        return 4314902;
    }

    private String fmt2(double v) {
        return String.format(LOCALE_XML, "%.2f", v);
    }

    private String fmt4(double v) {
        return String.format(LOCALE_XML, "%.4f", v);
    }

    private String resolveNatOp() {
        return "VENDA";
    }

    private String gerarChaveAcesso() {
        String uf = (config.getUf() != null && !config.getUf().isBlank()) ? config.getUf() : "RS";
        String cUF = ufParaCodigo(uf);
        String aamm = String.format("%02d%02d", dhEmi.getYear() % 100, dhEmi.getMonthValue());
        String cnpj = limpaCNPJ(config.getCnpj());
        String mod = "65";
        String serie = String.format("%03d", doc.serie);
        String nNF = String.format("%09d", doc.numero);
        String tpEmis = "1";
        String chave43 = cUF + aamm + cnpj + mod + serie + nNF + tpEmis + cNF;
        String dv = calcularDVChave(chave43);
        return chave43 + dv;
    }

    private String calcularDVChave(String chave43) {
        int peso = 2;
        int soma = 0;
        for (int i = chave43.length() - 1; i >= 0; i--) {
            int num = Character.digit(chave43.charAt(i), 10);
            soma += num * peso;
            peso++;
            if (peso > 9) peso = 2;
        }
        int mod = soma % 11;
        int dv = 11 - mod;
        if (dv == 10 || dv == 11) dv = 0;
        return String.valueOf(dv);
    }

    private String gerarUrlChave() {
        String base = getBaseConsultaUrl();
        if (base == null) return null;
        return base + "/consulta?chNFe=" + chaveAcesso;
    }

    private String gerarUrlQrCode() {
        String base = getBaseConsultaUrl();
        if (base == null) return null;
        String csc = config.getCsc();
        Integer idCsc = config.getIdCsc();
        if (csc == null || csc.isBlank() || idCsc == null || idCsc <= 0) {
            return null;
        }
        String assinatura = gerarHashCSC(chaveAcesso, csc, idCsc);
        return base + "/qrcode?chNFe=" + chaveAcesso + "&idToken=" + idCsc + "&cHashQRCode=" + assinatura;
    }

    private String getBaseConsultaUrl() {
        String uf = config.getUf();
        if (uf == null || uf.isBlank()) return null;
        String ufLower = uf.trim().toLowerCase();
        return "https://www.sefaz" + ufLower + ".gov.br/nfce";
    }

    private String gerarHashCSC(String chave, String csc, Integer idCsc) {
        try {
            String entrada = chave + csc + (idCsc != null ? idCsc : "");
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(entrada.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }
}
