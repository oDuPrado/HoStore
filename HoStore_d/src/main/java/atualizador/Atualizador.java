package atualizador;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.JOptionPane;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Atualizador {

    // ===== CONFIGURÁVEIS =====================================================
    private static final String UPDATE_JSON_URL = "https://hostore-7c455.web.app/update.json";
    // ← troque pela sua URL
    private static final String VERSAO_PROPS = "versao.properties";
    private static final String NOVO_JAR = "HoStore-novo.jar";
    private static final int TIMEOUT_MS = 12_000; // 12 s
    // ========================================================================

    public static void main(String[] args) {
        boolean modoSilent = args.length > 0 && "silent".equalsIgnoreCase(args[0]);
        try {
            verificarAtualizacao(modoSilent);
        } catch (Exception e) {
            if (!modoSilent) {
                JOptionPane.showMessageDialog(null,
                        "Falha ao buscar atualização:\n" + e.getMessage(),
                        "HoStore – Atualizador", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static void verificarAtualizacao(boolean silent) throws Exception {

        // 1) Lê a versão local
        Properties props = new Properties();
        Path propsPath = Paths.get(VERSAO_PROPS);
        if (Files.exists(propsPath))
            try (InputStream in = Files.newInputStream(propsPath)) {
                props.load(in);
            }
        String versaoLocal = props.getProperty("versao_atual", "0.0.0");

        // 2) Busca o JSON remoto
        JsonObject json = obterJsonRemoto();
        String ultimaVersao = json.get("latest_version").getAsString();
        String urlDownload = json.get("download_url").getAsString();
        String changelog = json.has("changelog") ? json.get("changelog").getAsString() : "";
        String sha256 = json.has("checksum_sha256") ? json.get("checksum_sha256").getAsString() : null;

        // 3) Compara
        if (ultimaVersao.equals(versaoLocal)) {
            if (!silent)
                JOptionPane.showMessageDialog(null,
                        "Você já está na última versão (" + versaoLocal + ").",
                        "HoStore – Atualizado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 4) Baixa o novo .jar
        if (!silent)
            JOptionPane.showMessageDialog(null,
                    "Nova versão disponível: " + ultimaVersao +
                            "\nBaixando atualização...\n\n" + changelog,
                    "HoStore – Atualizando", JOptionPane.INFORMATION_MESSAGE);

        baixarArquivo(urlDownload, NOVO_JAR);

        // 5) (Opcional) Valida integridade
        if (sha256 != null && !sha256.isBlank()) {
            String calculado = sha256DeArquivo(NOVO_JAR);
            if (!calculado.equalsIgnoreCase(sha256)) {
                Files.deleteIfExists(Paths.get(NOVO_JAR));
                throw new RuntimeException("Hash SHA-256 não confere! (" + calculado + ")");
            }
        }

        // 6) Atualiza versao.properties
        props.setProperty("versao_atual", ultimaVersao);
        props.setProperty("ultimo_update_em",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        try (OutputStream out = Files.newOutputStream(propsPath, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Gerado automaticamente");
        }

        // 7) Conclui
        String msg = "Atualização baixada com sucesso!\n"
                + "Feche o HoStore e abra novamente para aplicar.";
        if (silent)
            System.out.println("[Atualizador] " + msg.replace("\n", " "));
        else
            JOptionPane.showMessageDialog(null, msg,
                    "HoStore – Atualização pronta", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------------
    private static JsonObject obterJsonRemoto() throws IOException {
        URL url = new URL(UPDATE_JSON_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        try (Reader reader = new InputStreamReader(conn.getInputStream())) {
            return new Gson().fromJson(reader, JsonObject.class);
        }
    }

    private static void baixarArquivo(String srcUrl, String destino) throws IOException {
        try (InputStream in = new URL(srcUrl).openStream()) {
            Files.copy(in, Paths.get(destino), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256DeArquivo(String arquivo) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(Paths.get(arquivo))) {
            byte[] buf = new byte[8192];
            int l;
            while ((l = in.read(buf)) > 0)
                md.update(buf, 0, l);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest())
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
