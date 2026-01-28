package service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Cliente SOAP para envio/consulta NFC-e em SEFAZ.
 * Implementa: NfeAutorizacao (envio), NfeRetAutorizacao (consulta recibo), NfeCancelamento (cancelamento).
 * 
 * URLs SEFAZ variam por estado. Aqui usamos endpoint simplificado.
 */
public class SefazClientSoap {

    private String urlWebservice;
    private String caminhoP12;
    private String senhaP12;
    private int timeoutSegundos = 30;

    // Endpoints (exemplos para RS em homologação)
    private static final Map<String, String> ENDPOINTS_HOMOLOG = new HashMap<>();
    private static final Map<String, String> ENDPOINTS_PROD = new HashMap<>();

    static {
        // RS Homologação
        ENDPOINTS_HOMOLOG.put("nfe_autorizacao", 
            "https://nfe-homolog.svrs.rs.gov.br/webservices/NfeAutorizacao4/NfeAutorizacao4.asmx");
        ENDPOINTS_HOMOLOG.put("nfe_ret_autorizacao",
            "https://nfe-homolog.svrs.rs.gov.br/webservices/NfeRetAutorizacao4/NfeRetAutorizacao4.asmx");
        ENDPOINTS_HOMOLOG.put("nfe_cancelamento",
            "https://nfe-homolog.svrs.rs.gov.br/webservices/NfeCancelamento4/NfeCancelamento4.asmx");

        // RS Produção
        ENDPOINTS_PROD.put("nfe_autorizacao",
            "https://nfe.svrs.rs.gov.br/webservices/NfeAutorizacao4/NfeAutorizacao4.asmx");
        ENDPOINTS_PROD.put("nfe_ret_autorizacao",
            "https://nfe.svrs.rs.gov.br/webservices/NfeRetAutorizacao4/NfeRetAutorizacao4.asmx");
        ENDPOINTS_PROD.put("nfe_cancelamento",
            "https://nfe.svrs.rs.gov.br/webservices/NfeCancelamento4/NfeCancelamento4.asmx");
    }

    public SefazClientSoap(String urlWebservice, String caminhoP12, String senhaP12) {
        this.urlWebservice = urlWebservice;
        this.caminhoP12 = caminhoP12;
        this.senhaP12 = senhaP12;
    }

    /**
     * Envia lote NFC-e para autorização
     * @param xmlAssinado XML assinado
     * @return RespostaSefaz com protocolo/recibo ou erro
     */
    public RespostaSefaz enviarLoteNfce(String xmlAssinado, boolean producao) throws Exception {
        String endpoint = producao 
            ? ENDPOINTS_PROD.get("nfe_autorizacao")
            : ENDPOINTS_HOMOLOG.get("nfe_autorizacao");

        String soapRequest = montarRequestSoap("NfeAutorizacao4", xmlAssinado);

        return executarRequisicaoSoap(endpoint, soapRequest);
    }

    /**
     * Consulta recibo de autorização
     * @param nRec Número do recibo
     */
    public RespostaSefaz consultarRecibo(String nRec, boolean producao) throws Exception {
        String endpoint = producao
            ? ENDPOINTS_PROD.get("nfe_ret_autorizacao")
            : ENDPOINTS_HOMOLOG.get("nfe_ret_autorizacao");

        String soapRequest = montarRequestConsultaRecibo(nRec);

        return executarRequisicaoSoap(endpoint, soapRequest);
    }

    /**
     * Consulta status de NF-e por chave de acesso
     */
    public RespostaSefaz consultarChave(String chaveAcesso, boolean producao) throws Exception {
        // Implementação similar (usar endpoint específico)
        RespostaSefaz resposta = new RespostaSefaz();
        resposta.status = "3";  // Não implementado ainda
        return resposta;
    }

    /**
     * Cancela NF-e autorizada
     */
    public RespostaSefaz cancelarNfe(String chaveAcesso, String protocolo, String justificativa,
                                     boolean producao) throws Exception {
        String endpoint = producao
            ? ENDPOINTS_PROD.get("nfe_cancelamento")
            : ENDPOINTS_HOMOLOG.get("nfe_cancelamento");

        String soapRequest = montarRequestCancelamento(chaveAcesso, protocolo, justificativa);

        return executarRequisicaoSoap(endpoint, soapRequest);
    }

    /**
     * Executa requisição SOAP genérica
     */
    private RespostaSefaz executarRequisicaoSoap(String endpoint, String soapRequest) throws Exception {
        RespostaSefaz resposta = new RespostaSefaz();

        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Headers SOAP
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "");
            conn.setReadTimeout(timeoutSegundos * 1000);
            conn.setConnectTimeout(timeoutSegundos * 1000);

            // Send request
            conn.setDoOutput(true);
            conn.getOutputStream().write(soapRequest.getBytes("UTF-8"));
            conn.getOutputStream().flush();

            // Get response
            int statusCode = conn.getResponseCode();
            resposta.httpStatus = statusCode;

            if (statusCode >= 200 && statusCode < 300) {
                // Parse XML de retorno
                String xmlResposta = lerInputStream(conn.getInputStream());
                resposta.xmlResposta = xmlResposta;
                resposta = parseXmlResposta(xmlResposta, resposta);
                resposta.sucesso = true;
            } else {
                String erro = lerInputStream(conn.getErrorStream());
                resposta.mensagemErro = "HTTP " + statusCode + ": " + erro;
                resposta.sucesso = false;
            }

        } catch (java.net.SocketTimeoutException e) {
            resposta.sucesso = false;
            resposta.mensagemErro = "Timeout ao conectar SEFAZ: " + e.getMessage();
            resposta.ehRetentavel = true;
        } catch (Exception e) {
            resposta.sucesso = false;
            resposta.mensagemErro = "Erro na comunicação SEFAZ: " + e.getMessage();
            resposta.ehRetentavel = true;
        }

        return resposta;
    }

    private String montarRequestSoap(String operacao, String xmlAssinado) {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <NfeAutorizacao4 xmlns="http://www.sefaz.rs.gov.br/">
                  <nfeDados>XMLFISCAL_PLACEHOLDER</nfeDados>
                </NfeAutorizacao4>
              </soap:Body>
            </soap:Envelope>
            """.replace("XMLFISCAL_PLACEHOLDER", xmlAssinado);
    }

    private String montarRequestConsultaRecibo(String nRec) {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <NfeRetAutorizacao4 xmlns="http://www.sefaz.rs.gov.br/">
                  <nRec>NREC_PLACEHOLDER</nRec>
                </NfeRetAutorizacao4>
              </soap:Body>
            </soap:Envelope>
            """.replace("NREC_PLACEHOLDER", nRec);
    }

    private String montarRequestCancelamento(String chaveAcesso, String protocolo, String justificativa) {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <NfeCancelamento4 xmlns="http://www.sefaz.rs.gov.br/">
                  <chNFe>CHAVE_PLACEHOLDER</chNFe>
                  <nProt>PROTOCOLO_PLACEHOLDER</nProt>
                  <xJust>JUSTIFICATIVA_PLACEHOLDER</xJust>
                </NfeCancelamento4>
              </soap:Body>
            </soap:Envelope>
            """.replace("CHAVE_PLACEHOLDER", chaveAcesso)
               .replace("PROTOCOLO_PLACEHOLDER", protocolo)
               .replace("JUSTIFICATIVA_PLACEHOLDER", justificativa);
    }

    private RespostaSefaz parseXmlResposta(String xmlResposta, RespostaSefaz resposta) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new java.io.ByteArrayInputStream(xmlResposta.getBytes()));

            // Extrai cStatus
            Element cStatus = (Element) doc.getDocumentElement()
                .getElementsByTagName("cStat").item(0);
            if (cStatus != null) {
                resposta.status = cStatus.getTextContent();
            }

            // Extrai nProt (protocolo)
            Element nProt = (Element) doc.getDocumentElement()
                .getElementsByTagName("nProt").item(0);
            if (nProt != null) {
                resposta.protocolo = nProt.getTextContent();
            }

            // Extrai nRec (recibo)
            Element nRec = (Element) doc.getDocumentElement()
                .getElementsByTagName("nRec").item(0);
            if (nRec != null) {
                resposta.recibo = nRec.getTextContent();
            }

            // Extrai xMotivo (mensagem)
            Element xMotivo = (Element) doc.getDocumentElement()
                .getElementsByTagName("xMotivo").item(0);
            if (xMotivo != null) {
                resposta.mensagemErro = xMotivo.getTextContent();
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erro ao fazer parse de resposta SEFAZ: " + e.getMessage());
        }

        return resposta;
    }

    private String lerInputStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len));
        }
        is.close();
        return sb.toString();
    }

    /**
     * Modelo de resposta SEFAZ
     */
    public static class RespostaSefaz {
        public boolean sucesso = false;
        public boolean ehRetentavel = false;
        public int httpStatus;
        public String status;      // cStatus (100=autorizado, 110=processando, 2xx=rejeição)
        public String protocolo;   // nProt
        public String recibo;      // nRec
        public String chaveAcesso;
        public String xmlResposta;
        public String mensagemErro;

        public boolean eAutorizada() {
            return "100".equals(status);
        }

        public boolean ehRejeitada() {
            return status != null && (status.startsWith("2") || status.startsWith("3"));
        }

        public boolean ehProcessando() {
            return "110".equals(status);
        }
    }
}
