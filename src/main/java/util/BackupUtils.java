package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * Utilitário de backup local para o HoStore.
 * - Lê/escreve configuração em JSON (data/config/backup.json)
 * - Agenda backups periódicos via ScheduledExecutorService
 * - Executa backup copiando hostore.db para pasta escolhida com timestamp
 */
public class BackupUtils {

    // Caminho do arquivo de config
    private static final String CONFIG_PATH = "data/config/backup.json";
    // Caminho do DB original
    private static final String DB_PATH = "data/hostore.db";

    // Scheduler para tarefas de backup
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduledTask;

    // Gson para (de)serialização do JSON
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Modelo de configuração de backup */
    public static class BackupConfig {
        public boolean enabled;     // se backup automático está ativo
        public String folderPath;   // pasta onde salvar backups
        public long interval;       // intervalo numérico
        public String unit;         // "MINUTES", "HOURS" ou "DAYS"
    }

    /** Carrega config do JSON ou retorna padrão se não existir/der erro */
    public static BackupConfig loadConfig() {
        try {
            File f = new File(CONFIG_PATH);
            if (!f.exists()) return defaultConfig();
            try (FileReader reader = new FileReader(f)) {
                return gson.fromJson(reader, BackupConfig.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return defaultConfig();
        }
    }

    /** Salva config em JSON (cria pasta se necessário) */
    public static void saveConfig(BackupConfig config) {
        try {
            File cfg = new File(CONFIG_PATH);
            cfg.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(cfg)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Erro ao salvar configuração de backup:\n" + e.getMessage());
        }
    }

    /** Aplica a configuração: agenda ou cancela o backup automático */
    public static void applyConfig(BackupConfig config) {
        // Se já está agendado, cancela
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }

        if (config.enabled) {
            // Converte unidade para TimeUnit
            TimeUnit tu = TimeUnit.valueOf(config.unit);
            // Agenda tarefa: primeiro delay = intervalo, depois período = intervalo
            scheduledTask = scheduler.scheduleAtFixedRate(
                () -> doBackup(config.folderPath),
                config.interval,
                config.interval,
                tu
            );
            System.out.println("Backup agendado: a cada " +
                    config.interval + " " + config.unit.toLowerCase());
        } else {
            System.out.println("Backup automático desativado.");
        }
    }

    /** Executa um backup imediato copiando o arquivo .db */
    public static void doBackup(String folder) {
        try {
            File src = new File(DB_PATH);
            if (!src.exists()) {
                System.out.println("DB não encontrado em " + DB_PATH);
                return;
            }
            File destDir = new File(folder);
            destDir.mkdirs();

            // Timestamp no nome: yyyy-MM-dd_HHmmss
            String ts = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
            Path dest = Paths.get(folder, "backup_" + ts + ".db");

            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup criado em: " + dest.toString());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Erro ao realizar backup:\n" + e.getMessage());
        }
    }

    /** Configuração padrão, caso não exista JSON */
    private static BackupConfig defaultConfig() {
        BackupConfig def = new BackupConfig();
        def.enabled = false;
        def.folderPath = "data/backup";
        def.interval = 1;
        def.unit = "DAYS";
        return def;
    }
}
