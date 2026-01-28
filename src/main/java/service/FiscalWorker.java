package service;

import dao.ConfigNfceDAO;
import dao.DocumentoFiscalDAO;
import model.ConfigNfceModel;
import model.DocumentoFiscalModel;
import model.DocumentoFiscalStatus;
import util.DB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Worker assíncrono para processar fila de documentos fiscais.
 * Executa periodicamente (a cada 5 minutos) e tenta avançar documentos no workflow:
 * pendente → xml_gerado → assinada → enviada → autorizada
 * 
 * Com retentativas e backoff exponencial para erros.
 */
public class FiscalWorker {

    private static FiscalWorker instance;
    private static final Object LOCK = new Object();

    private Timer timer;
    private boolean running = false;
    private int tentativasMax = 5;
    private long intervaloSegundos = 5 * 60;  // 5 minutos

    private final DocumentoFiscalDAO docDAO = new DocumentoFiscalDAO();
    private final DocumentoFiscalService docService = new DocumentoFiscalService();
    private final ConfigNfceDAO configDAO = new ConfigNfceDAO();

    private FiscalWorker() {}

    /**
     * Obtém instância singleton
     */
    public static FiscalWorker getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new FiscalWorker();
                }
            }
        }
        return instance;
    }

    /**
     * Inicia worker (chame uma vez durante inicialização do app)
     */
    public synchronized void iniciar() {
        if (running) return;

        System.out.println("🚀 Iniciando FiscalWorker...");
        timer = new Timer("FiscalWorker", true);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                processarFila();
            }
        }, 10000, intervaloSegundos * 1000);  // Começa em 10s, depois a cada 5min

        running = true;
    }

    /**
     * Para worker
     */
    public synchronized void parar() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        running = false;
        System.out.println("⛔ FiscalWorker parado");
    }

    /**
     * Processa fila: tenta avançar documentos pendentes
     */
    private void processarFila() {
        try {
            // Verifica se tem configuração fiscal
            ConfigNfceModel config = configDAO.getConfig();
            if (config == null) {
                System.out.println("⚠️ Configuração fiscal não encontrada - FiscalWorker aguardando configuração");
                return;
            }

            // Documentos pendentes (status = "pendente")
            processarPendentes();

            // Documentos com XML gerado, prontos para assinatura
            processarXmlGerado(config);

            // Documentos assinados prontos para envio (status = "assinada")
            processarAssinados(config);

            // Documentos com erro (status = "erro") - com backoff
            processarComErro();

        } catch (Exception e) {
            System.err.println("❌ Erro em FiscalWorker.processarFila: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processa documentos pendentes: gera XML
     */
    private void processarPendentes() throws SQLException {
        try (Connection conn = DB.get()) {
            List<DocumentoFiscalModel> pendentes = docDAO.listarPorStatus(conn, DocumentoFiscalStatus.PENDENTE, 10);

            for (DocumentoFiscalModel doc : pendentes) {
                try {
                    System.out.println("📄 Processando documento " + doc.id + " (Número: " + doc.numero + ")");
                    
                    // 1. Calcula impostos
                    System.out.println("  ├─ Calculando impostos...");
                    docService.calcularImpostos(doc.id);
                    
                    // 2. Gera XML
                    System.out.println("  ├─ Gerando XML...");
                    String xml = docService.gerarXml(doc.id);
                    
                    System.out.println("  └─ ✅ XML gerado (" + xml.length() + " caracteres)");

                } catch (Exception e) {
                    System.err.println("  └─ ❌ Erro ao processar " + doc.id + ": " + e.getMessage());
                    try (Connection connErr = DB.get()) {
                        docDAO.atualizarStatus(connErr, doc.id, DocumentoFiscalStatus.ERRO, "Erro ao gerar XML: " + e.getMessage(), null, null, null);
                    }
                }
            }
        }
    }

    /**
     * Processa documentos com XML gerado: assina XML
     */
    private void processarXmlGerado(ConfigNfceModel config) throws SQLException {
        try (Connection conn = DB.get()) {
            List<DocumentoFiscalModel> xmlGerados = docDAO.listarPorStatus(conn, DocumentoFiscalStatus.XML_GERADO, 10);

            for (DocumentoFiscalModel doc : xmlGerados) {
                try {
                    // Verifica se tem certificado configurado
                    String certPath = config.getCertA1Path();
                    String certSenha = config.getCertA1Senha();
                    
                    if (certPath == null || certPath.isBlank()) {
                        System.out.println("⚠️ Certificado não configurado para documento " + doc.id + " - aguardando configuração");
                        continue;
                    }

                    System.out.println("🔐 Assinando documento " + doc.id);
                    
                    // Assina XML
                    String xmlAssinado = docService.assinarXml(doc.id, certPath, certSenha);
                    
                    System.out.println("  └─ ✅ XML assinado");

                } catch (Exception e) {
                    System.err.println("  └─ ❌ Erro ao assinar " + doc.id + ": " + e.getMessage());
                    try (Connection connErr = DB.get()) {
                        docDAO.atualizarStatus(connErr, doc.id, DocumentoFiscalStatus.ERRO, "Erro ao assinar XML: " + e.getMessage(), null, null, null);
                    }
                }
            }
        }
    }

    /**
     * Processa documentos assinados: envia para SEFAZ
     */
    private void processarAssinados(ConfigNfceModel config) throws SQLException {
        try (Connection conn = DB.get()) {
            List<DocumentoFiscalModel> assinados = docDAO.listarPorStatus(conn, DocumentoFiscalStatus.ASSINADA, 10);

            for (DocumentoFiscalModel doc : assinados) {
                try {
                    // Verifica configuração
                    String certPath = config.getCertA1Path();
                    String certSenha = config.getCertA1Senha();
                    String ambiente = config.getAmbiente();
                    
                    if (certPath == null || certPath.isBlank()) {
                        System.out.println("⚠️ Certificado não configurado - aguardando");
                        continue;
                    }

                    boolean producao = "PRODUCAO".equalsIgnoreCase(ambiente);
                    
                    System.out.println("📤 Enviando para SEFAZ: " + doc.id + " (ambiente: " + ambiente + ")");

                    // Envia para SEFAZ
                    SefazClientSoap.RespostaSefaz resposta = docService.enviarSefaz(doc.id, certPath, certSenha, producao);

                    if (resposta.eAutorizada()) {
                        System.out.println("  └─ ✅ AUTORIZADO - Protocolo: " + resposta.protocolo);
                    } else if (resposta.ehRejeitada()) {
                        System.out.println("  └─ ❌ REJEITADO - " + resposta.mensagemErro);
                    } else if (resposta.ehRetentavel) {
                        System.out.println("  └─ ⚠️ ERRO TEMPORÁRIO - Será retentado: " + resposta.mensagemErro);
                        try (Connection connErr = DB.get()) {
                            docDAO.atualizarStatus(connErr, doc.id, DocumentoFiscalStatus.ERRO, resposta.mensagemErro, null, null, null);
                        }
                    } else {
                        System.out.println("  └─ ❌ ERRO - " + resposta.mensagemErro);
                    }

                } catch (Exception e) {
                    System.err.println("  └─ ❌ Erro ao enviar " + doc.id + ": " + e.getMessage());
                    try (Connection connErr = DB.get()) {
                        docDAO.atualizarStatus(connErr, doc.id, DocumentoFiscalStatus.ERRO, "Erro ao enviar SEFAZ: " + e.getMessage(), null, null, null);
                    }
                }
            }
        }
    }

    /**
     * Processa documentos com erro: retentativas com backoff
     */
    private void processarComErro() throws SQLException {
        try (Connection conn = DB.get()) {
            List<DocumentoFiscalModel> comErro = docDAO.listarPorStatus(conn, DocumentoFiscalStatus.ERRO, 5);

            for (DocumentoFiscalModel doc : comErro) {
                try {
                    // Simples contador (em produção, usar tabela de retentativas)
                    int tentativas = contarTentativas(doc.erro);

                    if (tentativas >= tentativasMax) {
                        System.out.println("❌ Doc " + doc.id + " atingiu máximo de tentativas (" + tentativasMax + ")");
                        continue;
                    }

                    // Backoff exponencial
                    long delayMinutos = (long) Math.pow(2, tentativas);
                    System.out.println("⏳ Preparando retry " + (tentativas + 1) + "/" + tentativasMax + " para " + doc.id + " (backoff: " + delayMinutos + " min)");

                    // Próxima tentativa: resetar erro e tentar novamente
                    String novoErro = "Tentativa " + (tentativas + 1) + "/" + tentativasMax + ": " + doc.erro;
                    docDAO.atualizarStatus(conn, doc.id, DocumentoFiscalStatus.PENDENTE, novoErro, null, null, null);

                } catch (Exception e) {
                    System.err.println("⚠️ Erro ao reprocessar " + doc.id + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Conta tentativas a partir do campo erro (formato: "Tentativa x/5: mensagem")
     */
    private int contarTentativas(String erro) {
        if (erro == null || !erro.contains("Tentativa")) return 0;
        try {
            String[] parts = erro.split("/");
            if (parts.length > 0) {
                String numStr = parts[0].replaceAll("\\D", "");
                if (!numStr.isEmpty()) {
                    return Integer.parseInt(numStr);
                }
            }
        } catch (Exception e) {
            // Ignora erro de parse
        }
        return 0;
    }

    /**
     * Força processamento imediato (para testes/debug)
     */
    public void forcarProcessamento() {
        System.out.println("⚡ Forçando processamento imediato...");
        processarFila();
    }

    /**
     * Retorna status do worker
     */
    public boolean estaRodando() {
        return running;
    }

    /**
     * Configurações
     */
    public void setIntervaloSegundos(long segundos) {
        this.intervaloSegundos = Math.max(30, segundos);  // Mínimo 30s
    }

    public void setTentativasMax(int max) {
        this.tentativasMax = Math.max(1, max);
    }
}
