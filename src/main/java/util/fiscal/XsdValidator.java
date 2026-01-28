package util.fiscal;

import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXParseException;

/**
 * XsdValidator: Valida XML contra XSD locais (offline, sem internet)
 * 
 * Objetivo:
 * - Carregar XSD embarcado no projeto
 * - Validar XML gerado contra schema oficial
 * - Retornar erros com linha/coluna para debug
 */
public final class XsdValidator {

    private static final String XSD_BASE_PATH = "/fiscal/xsd/nfce/";
    private static final String XSD_PRINCIPAL = "NFe_v4.00.xsd";

    /**
     * Valida XML contra XSD principal (NFC-e v4.00)
     * 
     * @param xmlContent conteúdo do XML a validar
     * @return true se válido, false se inválido
     * @throws Exception se houver erro na validação
     */
    public static boolean validarXml(String xmlContent) throws Exception {
        return validarXml(xmlContent, XSD_PRINCIPAL);
    }

    /**
     * Valida XML contra um XSD específico
     * 
     * @param xmlContent conteúdo do XML a validar
     * @param xsdResourceName nome do arquivo XSD em /fiscal/xsd/nfce/
     * @return true se válido
     * @throws Exception se houver erro (incluindo XSD inválido)
     */
    public static boolean validarXml(String xmlContent, String xsdResourceName) throws Exception {
        try {
            // Carregar XSD do classpath
            String xsdPath = XSD_BASE_PATH + xsdResourceName;
            InputStream xsdStream = XsdValidator.class.getResourceAsStream(xsdPath);
            
            if (xsdStream == null) {
                throw new Exception("❌ XSD não encontrado: " + xsdPath + 
                                   "\n   Verifique se 'src/main/resources" + xsdPath + "' existe");
            }

            // Criar Schema
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.ACCESS_EXTERNAL_DTD, false);
            factory.setFeature(XMLConstants.ACCESS_EXTERNAL_SCHEMA, false);
            
            Schema schema = factory.newSchema(new StreamSource(xsdStream));

            // Criar validador
            Validator validator = schema.newValidator();
            
            // Callback customizado para capturar erros
            StringBuilder erroDetalhado = new StringBuilder();
            validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    erroDetalhado.append("⚠️ AVISO (linha ").append(e.getLineNumber())
                                 .append(", col ").append(e.getColumnNumber()).append("): ")
                                 .append(e.getMessage()).append("\n");
                }

                @Override
                public void error(org.xml.sax.SAXParseException e) {
                    erroDetalhado.append("❌ ERRO (linha ").append(e.getLineNumber())
                                 .append(", col ").append(e.getColumnNumber()).append("): ")
                                 .append(e.getMessage()).append("\n");
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException e) {
                    erroDetalhado.append("❌ ERRO FATAL (linha ").append(e.getLineNumber())
                                 .append(", col ").append(e.getColumnNumber()).append("): ")
                                 .append(e.getMessage()).append("\n");
                }
            });

            // Validar
            validator.validate(new StreamSource(new java.io.StringReader(xmlContent)));
            
            // Se chegou aqui, é válido
            return true;

        } catch (Exception e) {
            String msgErro = "❌ XSD FAIL: " + e.getMessage();
            if (e instanceof org.xml.sax.SAXParseException) {
                SAXParseException spe = (SAXParseException) e;
                msgErro = "❌ XSD FAIL (linha " + spe.getLineNumber() + ", col " + 
                         spe.getColumnNumber() + "): " + spe.getMessage();
            }
            throw new Exception(msgErro, e);
        }
    }

    /**
     * Valida XML e retorna relatório detalhado
     * 
     * @param xmlContent XML a validar
     * @return relatório com sucesso/erro
     */
    public static RelatorioValidacao validarComRelatorio(String xmlContent) {
        RelatorioValidacao relatorio = new RelatorioValidacao();
        relatorio.xmlTamanho = xmlContent.length();
        relatorio.dataValidacao = java.time.LocalDateTime.now();
        
        try {
            if (validarXml(xmlContent)) {
                relatorio.valido = true;
                relatorio.mensagem = "✅ XML válido contra XSD";
            }
        } catch (Exception e) {
            relatorio.valido = false;
            relatorio.mensagem = e.getMessage();
            relatorio.erroTecnico = e.toString();
        }
        
        return relatorio;
    }

    /**
     * Classe para retornar relatório estruturado
     */
    public static class RelatorioValidacao {
        public boolean valido;
        public String mensagem;
        public String erroTecnico;
        public int xmlTamanho;
        public java.time.LocalDateTime dataValidacao;

        @Override
        public String toString() {
            return (valido ? "✅ " : "❌ ") + mensagem + 
                   (erroTecnico != null ? "\n" + erroTecnico : "");
        }
    }
}
