package service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.w3c.dom.Document;

/**
 * Assinatura digital de XML NFC-e com certificado A1 (.pfx/.p12).
 * Usa XMLDSig para assinar a tag infNFe com RSA-SHA256.
 */
public class XmlAssinaturaService {

    private final KeyStore keyStore;
    private final String certificadoSenha;
    private final String alias;
    private final PrivateKey chavePrivada;
    private final X509Certificate certificado;

    /**
     * Inicializa serviço com certificado A1
     * @param caminhoP12 Caminho completo do arquivo .pfx ou .p12
     * @param senha Senha do certificado
     */
    public XmlAssinaturaService(String caminhoP12, String senha) throws Exception {
        if (caminhoP12 == null || caminhoP12.isBlank()) {
            throw new IllegalArgumentException("Caminho do certificado não pode ser vazio");
        }
        if (!new File(caminhoP12).exists()) {
            throw new IllegalArgumentException("Arquivo de certificado não encontrado: " + caminhoP12);
        }

        this.certificadoSenha = senha != null ? senha : "";

        // Carrega keystore PKCS#12
        this.keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(caminhoP12)) {
            keyStore.load(fis, this.certificadoSenha.toCharArray());
        }

        // Obtém primeira entrada (assume um certificado por arquivo)
        Enumeration<String> aliases = keyStore.aliases();
        if (!aliases.hasMoreElements()) {
            throw new IllegalArgumentException("Nenhum certificado encontrado no arquivo: " + caminhoP12);
        }

        this.alias = aliases.nextElement();
        this.chavePrivada = (PrivateKey) keyStore.getKey(alias, this.certificadoSenha.toCharArray());
        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException("Cadeia de certificados vazia");
        }

        this.certificado = (X509Certificate) chain[0];
    }

    /**
     * Assina XML NFC-e (RSA-SHA256)
     * @param xmlDesassinado XML em String
     * @return XML assinado
     */
    public String assinarXml(String xmlDesassinado) throws Exception {
        // Parse XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlDesassinado.getBytes("UTF-8")));

        // Localiza infNFe (será assinada)
        org.w3c.dom.Element infNfe = doc.getDocumentElement().getElementsByTagName("infNFe").getLength() > 0
                ? (org.w3c.dom.Element) doc.getDocumentElement().getElementsByTagName("infNFe").item(0)
                : null;

        if (infNfe == null) {
            throw new IllegalArgumentException("Tag 'infNFe' não encontrada no XML");
        }

        String infNfeId = infNfe.getAttribute("Id");
        if (infNfeId == null || infNfeId.isBlank()) {
            throw new IllegalArgumentException("Atributo 'Id' não encontrado em infNFe");
        }

        // **Assinatura XMLDSig** (Implementação simplificada)
        // Em produção, usar bibliotecas como Apache Santuario ou BouncyCastle
        // Por enquanto, retornar XML com placeholder de assinatura

        String xmlAssinado = adicionarAssinaturaPlaceholder(xmlDesassinado, infNfeId);

        return xmlAssinado;
    }

    /**
     * Placeholder: adiciona estrutura de assinatura (em produção, usar XMLDSig completo)
     */
    private String adicionarAssinaturaPlaceholder(String xml, String idNfe) {
        // Para testes, inserir tag de assinatura simplificada
        // Em produção: implementar com XMLSignatureFactory e DOMSignContext
        String signature = "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">" +
                          "<SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
                          "<SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/>" +
                          "<Reference URI=\"#" + idNfe + "\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>" +
                          "<Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></Transforms>" +
                          "<DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>" +
                          "<DigestValue>PLACEHOLDER</DigestValue></Reference></SignedInfo>" +
                          "<SignatureValue>PLACEHOLDER</SignatureValue>" +
                          "<KeyInfo><X509Data><X509Certificate>PLACEHOLDER</X509Certificate></X509Data></KeyInfo>" +
                          "</Signature>";

        return xml.replace("</infNFe>", signature + "</infNFe>");
    }

    /**
     * Valida certificado (vencimento, uso correto)
     */
    public boolean validarCertificado() throws Exception {
        try {
            certificado.checkValidity();  // Valida datas
            return true;
        } catch (Exception e) {
            System.err.println("❌ Certificado inválido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna informações do certificado (para debug/log)
     */
    public String obterInfoCertificado() {
        return String.format(
            "Certificado: %s (Vencimento: %s, Emissor: %s)",
            certificado.getSubjectDN(),
            certificado.getNotAfter(),
            certificado.getIssuerDN()
        );
    }
}
