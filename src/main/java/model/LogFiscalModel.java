package model;

/**
 * Modelo para Log Fiscal
 * Registro de todas as operações de documentos fiscais
 */
public class LogFiscalModel {

    private long id;
    private String documentoFiscalId;
    private String etapa;              // CALC_IMPOSTOS, GERAR_XML, ASSINAR, ENVIAR_SEFAZ, IMPRIMIR_DANFE
    private String tipoLog;             // INFO, WARN, ERROR, DEBUG
    private String mensagem;            // Descrição do evento
    private String payloadResumido;     // Resumo dos dados processados
    private String stackTrace;          // Stack trace se houve erro
    private String timestamp;           // Data/hora do evento

    public LogFiscalModel() {}

    public LogFiscalModel(String documentoFiscalId, String etapa, String tipoLog, String mensagem) {
        this.documentoFiscalId = documentoFiscalId;
        this.etapa = etapa;
        this.tipoLog = tipoLog;
        this.mensagem = mensagem;
    }

    // Getters e Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDocumentoFiscalId() { return documentoFiscalId; }
    public void setDocumentoFiscalId(String documentoFiscalId) { this.documentoFiscalId = documentoFiscalId; }

    public String getEtapa() { return etapa; }
    public void setEtapa(String etapa) { this.etapa = etapa; }

    public String getTipoLog() { return tipoLog; }
    public void setTipoLog(String tipoLog) { this.tipoLog = tipoLog; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getPayloadResumido() { return payloadResumido; }
    public void setPayloadResumido(String payloadResumido) { this.payloadResumido = payloadResumido; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + etapa + " - " + tipoLog + ": " + mensagem;
    }
}
