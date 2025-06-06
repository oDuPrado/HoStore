package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.CfopModel;
import model.CsosnModel;
import model.NcmModel;
import model.OrigemModel;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável por buscar e cachear dados fiscais (NCM, CFOP, CSOSN, Origem).
 * 
 * A estratégia é:
 *  1. Tentar ler do cache local (data/cache/*.json).
 *  2. Se o cache estiver “antigo” (>24h) ou faltar, buscar da API remota.
 *  3. Atualizar o cache no disco e retornar a lista de objetos Java.
 * 
 * ATENÇÃO: Você deve definir URLs válidas para cada endpoint. Neste exemplo, usaremos URIs fictícias:
 *   - https://api.exemplo.com/fiscal/ncm
 *   - https://api.exemplo.com/fiscal/cfop
 *   - https://api.exemplo.com/fiscal/csosn
 *   - https://api.exemplo.com/fiscal/origem
 * 
 * Se qualquer chamada falhar, retorne apenas o cache local (mesmo que antigo).
 */
public class FiscalApiService {

    // ==== CONFIGURAÇÃO PRINCIPAL ====
    private static final String CACHE_DIR = "data/cache/";

    // URLs fictícias; substitua pelas reais
    private static final String URL_NCM    = "https://api.exemplo.com/fiscal/ncm";
    private static final String URL_CFOP   = "https://api.exemplo.com/fiscal/cfop";
    private static final String URL_CSOSN  = "https://api.exemplo.com/fiscal/csosn";
    private static final String URL_ORIGEM = "https://api.exemplo.com/fiscal/origem";

    // Nome dos arquivos de cache no disco
    private static final String FILE_CACHE_NCM    = CACHE_DIR + "ncm.json";
    private static final String FILE_CACHE_CFOP   = CACHE_DIR + "cfop.json";
    private static final String FILE_CACHE_CSOSN  = CACHE_DIR + "csosn.json";
    private static final String FILE_CACHE_ORIGEM = CACHE_DIR + "origem.json";

    private static final long CACHE_EXPIRATION_SECONDS = Duration.ofHours(24).getSeconds();

    private static final Gson gson = new Gson();

    // ==== MÉTODOS PÚBLICOS ====

    /**
     * Retorna a lista de NCMs. Tenta usar cache; se for necessário, baixa da API.
     */
    public static List<NcmModel> listarNcms() {
        return fetchOrLoadCache(
            URL_NCM,
            FILE_CACHE_NCM,
            new TypeToken<List<NcmModel>>(){}.getType()
        );
    }

    /**
     * Retorna a lista de CFOPs. Tenta usar cache; se for necessário, baixa da API.
     */
    public static List<CfopModel> listarCfops() {
        return fetchOrLoadCache(
            URL_CFOP,
            FILE_CACHE_CFOP,
            new TypeToken<List<CfopModel>>(){}.getType()
        );
    }

    /**
     * Retorna a lista de CSOSNs. Tenta usar cache; se for necessário, baixa da API.
     */
    public static List<CsosnModel> listarCsosns() {
        return fetchOrLoadCache(
            URL_CSOSN,
            FILE_CACHE_CSOSN,
            new TypeToken<List<CsosnModel>>(){}.getType()
        );
    }

    /**
     * Retorna a lista de Origens. Tenta usar cache; se for necessário, baixa da API.
     */
    public static List<OrigemModel> listarOrigens() {
        return fetchOrLoadCache(
            URL_ORIGEM,
            FILE_CACHE_ORIGEM,
            new TypeToken<List<OrigemModel>>(){}.getType()
        );
    }

    // ==== MÉTODOS PRIVADOS AUXILIARES ====

    /**
     * 1. Verifica se o arquivo de cache existe e está “fresquinho” (menos de 24h).
     * 2. Se não existir ou for antigo, faz HTTP GET na URL fornecida e escreve o retorno no cache.
     * 3. Lê do cache (arquivo JSON) e converte para List<T> com Gson.
     */
    private static <T> List<T> fetchOrLoadCache(String apiUrl, String cacheFilePath, Type listaTipo) {
        try {
            File cacheFile = new File(cacheFilePath);

            // Se não existir ou estiver expirado...
            if (!cacheFile.exists() || isExpired(cacheFile)) {
                // → Buscar da API
                String jsonRemoto = fetchUrl(apiUrl);
                if (jsonRemoto != null) {
                    writeFile(cacheFilePath, jsonRemoto);
                }
            }
            // Agora, mesmo que API tenha falhado, lemos o cache local (se existir)
            if (cacheFile.exists()) {
                String conteudo = new String(Files.readAllBytes(Paths.get(cacheFilePath)), StandardCharsets.UTF_8);
                return gson.fromJson(conteudo, listaTipo);
            } else {
                // Cache não existe (e a API falhou), retorna lista vazia para não quebrar
                return new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Verifica se o arquivo de cache está “antigo” (> CACHE_EXPIRATION_SECONDS).
     */
    private static boolean isExpired(File file) {
        long ultimo = file.lastModified();
        long agora = Instant.now().toEpochMilli();
        long diff = (agora - ultimo) / 1000; // diferença em segundos
        return diff > CACHE_EXPIRATION_SECONDS;
    }

    /**
     * Faz um HTTP GET simples na URL e retorna o corpo da resposta (JSON) como String.
     * Se falhar (timeout, 404, etc.), retorna null.
     */
    private static String fetchUrl(String apiUrl) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000); // 5s para conectar
            conn.setReadTimeout(5_000);    // 5s para ler
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("❌ Falha ao buscar API: " + apiUrl + " (HTTP " + status + ")");
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("❌ Erro ao conectar na API: " + apiUrl);
            e.printStackTrace();
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Escreve o conteúdo (JSON) no disco, criando pastas se necessário.
     */
    private static void writeFile(String path, String conteudo) {
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write(conteudo);
            }
        } catch (IOException e) {
            System.err.println("❌ Erro ao escrever cache em " + path);
            e.printStackTrace();
        }
    }
}
