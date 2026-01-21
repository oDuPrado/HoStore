
package util;

import model.UsuarioModel;
import service.SessaoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogService {
    private static final Logger logger = Logger.getLogger("HoStore");
    static {
        try {
            Files.createDirectories(Paths.get("data"));
            FileHandler file = new FileHandler("data/hos_logs.txt", true);
            file.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord r) {
                    String ts = Instant.ofEpochMilli(r.getMillis()).toString();
                    return ts + " | " + r.getLevel() + " | " + r.getMessage() + System.lineSeparator();
                }
            });
            logger.setUseParentHandlers(false);
            logger.addHandler(file);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void warn(String msg) {
        logger.warning(msg);
    }

    public static void error(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    public static void audit(String action, String entity, String entityId, String msg) {
        log(Level.INFO, action, entity, entityId, msg, null);
    }

    public static void auditError(String action, String entity, String entityId, String msg, Throwable t) {
        log(Level.SEVERE, action, entity, entityId, msg, t);
    }

    private static void log(Level level, String action, String entity, String entityId, String msg, Throwable t) {
        String user = currentUserTag();
        String line = "user=" + user
                + " action=" + safe(action)
                + " entity=" + safe(entity)
                + " id=" + safe(entityId)
                + " msg=" + safe(msg);
        if (t != null) {
            logger.log(level, line, t);
        } else {
            logger.log(level, line);
        }
    }

    private static String currentUserTag() {
        UsuarioModel u = SessaoService.get();
        if (u == null)
            return "anon";
        String user = (u.getUsuario() == null || u.getUsuario().isBlank()) ? "usuario" : u.getUsuario();
        String id = (u.getId() == null || u.getId().isBlank()) ? "sem_id" : u.getId();
        return user + "(" + id + ")";
    }

    private static String safe(String s) {
        if (s == null || s.isBlank())
            return "-";
        return s.replace('\n', ' ').replace('\r', ' ');
    }
}
