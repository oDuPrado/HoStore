package util;

import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.io.InputStream;

public class SyncStatusUtil {
    private static final Path SYNC_STATE_FILE =
            Paths.get(System.getProperty("user.dir"), "data", "cache", "sync_state.properties");

    public static String getUltimaSincronizacaoFormatada() {
        try {
            if (!Files.exists(SYNC_STATE_FILE)) return "Nunca";
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(SYNC_STATE_FILE)) {
                props.load(in);
            }
            long epoch = Long.parseLong(props.getProperty("last_sync_epoch", "0"));
            if (epoch <= 0) return "Nunca";
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return "Indisponível";
        }
    }
}
