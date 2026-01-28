package dao;

import model.LogFiscalModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para Logs Fiscais
 * Tabela: logs_fiscal
 * Registra todas as operações de documentos fiscais para auditoria
 */
public class LogFiscalDAO {

    /**
     * Cria tabela se não existir
     */
    public static void criarTabelaSenaoExiste() {
        String sql = """
            CREATE TABLE IF NOT EXISTS logs_fiscal (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                documento_fiscal_id TEXT NOT NULL,
                etapa TEXT NOT NULL,
                tipo_log TEXT NOT NULL,
                mensagem TEXT,
                payload_resumido TEXT,
                stack_trace TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (documento_fiscal_id) REFERENCES documentos_fiscais(id)
            )
        """;

        try (Connection conn = DB.get();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Índices para performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_fiscal_doc ON logs_fiscal(documento_fiscal_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_fiscal_timestamp ON logs_fiscal(timestamp DESC)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registra um evento de log
     * Tipos: INFO, WARN, ERROR, DEBUG
     */
    public void inserir(LogFiscalModel log) throws Exception {
        String sql = """
            INSERT INTO logs_fiscal (documento_fiscal_id, etapa, tipo_log, mensagem, payload_resumido, stack_trace)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, log.getDocumentoFiscalId());
            ps.setString(2, log.getEtapa());
            ps.setString(3, log.getTipoLog());
            ps.setString(4, log.getMensagem());
            ps.setString(5, log.getPayloadResumido());
            ps.setString(6, log.getStackTrace());
            ps.executeUpdate();
        }
    }

    /**
     * Busca logs de um documento fiscal
     */
    public List<LogFiscalModel> buscarPorDocumento(String documentoId) throws Exception {
        List<LogFiscalModel> logs = new ArrayList<>();
        String sql = """
            SELECT id, documento_fiscal_id, etapa, tipo_log, mensagem, payload_resumido, stack_trace, timestamp
            FROM logs_fiscal
            WHERE documento_fiscal_id = ?
            ORDER BY timestamp DESC
        """;

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, documentoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogFiscalModel log = new LogFiscalModel();
                    log.setId(rs.getLong("id"));
                    log.setDocumentoFiscalId(rs.getString("documento_fiscal_id"));
                    log.setEtapa(rs.getString("etapa"));
                    log.setTipoLog(rs.getString("tipo_log"));
                    log.setMensagem(rs.getString("mensagem"));
                    log.setPayloadResumido(rs.getString("payload_resumido"));
                    log.setStackTrace(rs.getString("stack_trace"));
                    log.setTimestamp(rs.getString("timestamp"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    /**
     * Busca últimos N logs
     */
    public List<LogFiscalModel> buscarUltimos(int limite) throws Exception {
        List<LogFiscalModel> logs = new ArrayList<>();
        String sql = """
            SELECT id, documento_fiscal_id, etapa, tipo_log, mensagem, payload_resumido, stack_trace, timestamp
            FROM logs_fiscal
            ORDER BY timestamp DESC
            LIMIT ?
        """;

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogFiscalModel log = new LogFiscalModel();
                    log.setId(rs.getLong("id"));
                    log.setDocumentoFiscalId(rs.getString("documento_fiscal_id"));
                    log.setEtapa(rs.getString("etapa"));
                    log.setTipoLog(rs.getString("tipo_log"));
                    log.setMensagem(rs.getString("mensagem"));
                    log.setPayloadResumido(rs.getString("payload_resumido"));
                    log.setStackTrace(rs.getString("stack_trace"));
                    log.setTimestamp(rs.getString("timestamp"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    /**
     * Busca logs por etapa e tipo
     */
    public List<LogFiscalModel> buscarPorEtapaETipo(String etapa, String tipoLog) throws Exception {
        List<LogFiscalModel> logs = new ArrayList<>();
        String sql = """
            SELECT id, documento_fiscal_id, etapa, tipo_log, mensagem, payload_resumido, stack_trace, timestamp
            FROM logs_fiscal
            WHERE etapa = ? AND tipo_log = ?
            ORDER BY timestamp DESC
        """;

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, etapa);
            ps.setString(2, tipoLog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogFiscalModel log = new LogFiscalModel();
                    log.setId(rs.getLong("id"));
                    log.setDocumentoFiscalId(rs.getString("documento_fiscal_id"));
                    log.setEtapa(rs.getString("etapa"));
                    log.setTipoLog(rs.getString("tipo_log"));
                    log.setMensagem(rs.getString("mensagem"));
                    log.setPayloadResumido(rs.getString("payload_resumido"));
                    log.setStackTrace(rs.getString("stack_trace"));
                    log.setTimestamp(rs.getString("timestamp"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    /**
     * Limpa logs com mais de N dias
     */
    public void limparLogsAntigos(int dias) throws Exception {
        String sql = "DELETE FROM logs_fiscal WHERE timestamp < datetime('now', '-" + dias + " days')";
        try (Connection conn = DB.get();
             Statement stmt = conn.createStatement()) {
            int deletados = stmt.executeUpdate(sql);
            System.out.println("✓ Deletados " + deletados + " logs fiscais antigos");
        }
    }

    /**
     * Retorna resumo de erros por etapa
     */
    public String gerarRelatorioErros() throws Exception {
        String sql = """
            SELECT etapa, tipo_log, COUNT(*) as total, 
                   GROUP_CONCAT(DISTINCT mensagem, ', ') as erros
            FROM logs_fiscal
            WHERE tipo_log IN ('ERROR', 'WARN')
            GROUP BY etapa, tipo_log
            ORDER BY total DESC
        """;

        StringBuilder relatorio = new StringBuilder();
        try (Connection conn = DB.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                relatorio.append("Etapa: ").append(rs.getString("etapa"))
                    .append(" | Tipo: ").append(rs.getString("tipo_log"))
                    .append(" | Total: ").append(rs.getInt("total"))
                    .append(" | Erros: ").append(rs.getString("erros"))
                    .append("\n");
            }
        }
        return relatorio.toString();
    }
}
