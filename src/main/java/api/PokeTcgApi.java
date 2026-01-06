package api;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * PokeTcgApi (v2)
 * - Paginação robusta (page/pageSize)
 * - Retry/backoff (429 + 5xx + timeouts)
 * - Cache/fallback: salva JSON bruto consolidado (data[])
 *
 * Nota prática:
 * - O endpoint /sets é instável e às vezes dá 504 no Cloudflare se você não paginar.
 * - Evite depender de orderBy em /sets. Se quiser “mais recente”, ordene localmente depois.
 */
public class PokeTcgApi {

    private static final String BASE_URL = "https://api.pokemontcg.io/v2";
    private static final String API_KEY = "8d293a2a-4949-4d04-a06c-c20672a7a12c"; // pode deixar vazio

    // Cache/fallback
    private static final Path CACHE_DIR = Paths.get(System.getProperty("user.dir"), "data", "cache");
    private static final Path SETS_CACHE_FILE = CACHE_DIR.resolve("pokemontcg_sets.json");

    // Circuit breaker simples
    private static Instant lastFailureAt = null;
    private static final Duration COOLDOWN_AFTER_FAILURE = Duration.ofMinutes(30);

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            // HTTP_2 não é necessário aqui e às vezes atrapalha mais do que ajuda dependendo do caminho/CDN.
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // Você já viu resposta levando ~56s no Postman. Então 120s é mais realista.
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    // Retries
    private static final int MAX_RETRIES = 3;

    // Paginação
    private static final int DEFAULT_PAGE_SIZE = 255;     // sets não são tantos; 100 costuma ser ok
    private static final int MAX_PAGES_HARD_LIMIT = 280;  // trava de segurança

    /** GET com retry/backoff e logs mais úteis */
    public static String get(String endpointWithQuery) throws IOException, InterruptedException {
        // Proteção: endpoint precisa começar com "/"
        String ep = endpointWithQuery.startsWith("/") ? endpointWithQuery : ("/" + endpointWithQuery);
        URI uri = URI.create(BASE_URL + ep);

        long backoffMs = 1200;
        IOException lastIo = null;

        for (int attempt = 1; attempt <= (MAX_RETRIES + 1); attempt++) {
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .GET()
                        .header("Accept", "application/json")
                        // ajuda alguns CDNs/WAFs a não tratarem request como “genérico estranho”
                        .header("User-Agent", "HoStore/1.0 (Java HttpClient)");

                if (API_KEY != null && !API_KEY.isBlank()) {
                    reqBuilder.header("X-Api-Key", API_KEY);
                }

                HttpResponse<String> res = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                int code = res.statusCode();

                if (code >= 200 && code < 300) {
                    return res.body();
                }

                // Retry-worthy (429 + 5xx)
                if (code == 429 || (code >= 500 && code <= 599)) {
                    long retryAfterMs = parseRetryAfterMs(res);
                    long sleep = retryAfterMs > 0 ? retryAfterMs : backoffMs;
                    System.err.println("⚠ PokémonTCG HTTP " + code + " em " + uri + " (tentativa " + attempt + "). Aguardando " + sleep + "ms...");
                    sleepQuietly(sleep);
                    backoffMs = Math.min(backoffMs * 2, 12_000);
                    continue;
                }

                // 404 normalmente seria definitivo, mas você está vendo 404 em cenários onde deveria existir.
                // Então tratamos como "possivelmente transiente" UMA vez: faz retry curto e depois explode.
                if (code == 404 && attempt <= MAX_RETRIES) {
                    System.err.println("⚠ PokémonTCG HTTP 404 em " + uri + " (tentativa " + attempt + "). Pode ser instabilidade/rota. Retry em " + backoffMs + "ms...");
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 12_000);
                    continue;
                }

                // Definitivo
                throw new RuntimeException("Erro na PokémonTCG API: HTTP " + code + " | " + safeSnippet(res.body()));

            } catch (HttpTimeoutException e) {
                lastIo = new IOException("Timeout ao chamar " + uri + " (tentativa " + attempt + ")", e);
                System.err.println("⚠ " + lastIo.getMessage());
                if (attempt <= MAX_RETRIES) {
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 12_000);
                }
            } catch (IOException e) {
                lastIo = e;
                System.err.println("⚠ Erro de rede ao chamar " + uri + " (tentativa " + attempt + "): " + e.getMessage());
                if (attempt <= MAX_RETRIES) {
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 12_000);
                }
            }
        }

        if (lastIo != null) throw lastIo;
        throw new IOException("Falha desconhecida ao chamar " + uri);
    }

    /**
     * Lista sets com paginação e loop.
     * Estratégia:
     * - Sempre paginar
     * - NÃO usar orderBy em /sets (instável/variável). Se você quiser “mais recente”, ordene localmente depois.
     * - Cache incremental: se cair no meio, retorna parcial.
     * - Se falhar antes de pegar qualquer coisa, usa cache local.
     */
    public static String listarSetsPaginado() {
        // Circuit breaker: se falhou recentemente, nem tenta
        if (lastFailureAt != null) {
            Duration since = Duration.between(lastFailureAt, Instant.now());
            if (since.compareTo(COOLDOWN_AFTER_FAILURE) < 0) {
                System.err.println("⚠ PokémonTCG: falhou recentemente (" + since.toMinutes() + " min). Usando cache/fallback.");
                String cached = readCache(SETS_CACHE_FILE);
                return (cached != null && !cached.isBlank()) ? cached : "{\"data\":[]}";
            }
        }

        int page = 1;
        int pageSize = DEFAULT_PAGE_SIZE;

        List<String> dataObjects = new ArrayList<>();
        Integer totalCount = null;

        while (page <= MAX_PAGES_HARD_LIMIT) {
            String endpoint = buildSetsEndpoint(page, pageSize);

            try {
                String json = get(endpoint);

                // Extrai "data" (array) e "count" (se existir)
                String dataArray = JsonMini.extractArray(json, "data");
                if (dataArray == null) dataArray = "[]";

                if (totalCount == null) {
                    totalCount = JsonMini.extractInt(json, "count");
                }

                List<String> items = JsonMini.splitTopLevelArrayItems(dataArray);
                if (items.isEmpty()) {
                    break; // acabou
                }

                dataObjects.addAll(items);

                // cache incremental
                saveCache(SETS_CACHE_FILE, JsonMini.buildDataOnlyPayload(dataObjects, page, pageSize, totalCount));

                System.out.println("✅ Pokémon: sets página " + page + " (" + items.size() + "). Total: " + dataObjects.size());

                // se veio menos que pageSize, provavelmente acabou
                if (items.size() < pageSize) {
                    break;
                }

                page++;

            } catch (Exception e) {
                lastFailureAt = Instant.now();
                System.err.println("⚠ Pokémon: falha em sets página " + page + " (" + endpoint + "). Motivo: " + e.getMessage());

                // Se já temos algo, retorna parcial
                if (!dataObjects.isEmpty()) {
                    System.err.println("✅ Pokémon: retornando parcial (" + dataObjects.size() + " sets).");
                    return JsonMini.buildDataOnlyPayload(dataObjects, page, pageSize, totalCount);
                }

                // Se não temos nada, tenta cache local
                String cached = readCache(SETS_CACHE_FILE);
                if (cached != null && !cached.isBlank()) {
                    System.err.println("✅ Pokémon: fallback aplicado (cache local).");
                    return cached;
                }

                System.err.println("❌ Pokémon: sem cache. Retornando vazio pra não travar inicialização.");
                return "{\"data\":[]}";
            }
        }

        // Sucesso
        String finalPayload = JsonMini.buildDataOnlyPayload(dataObjects, page, pageSize, totalCount);
        saveCache(SETS_CACHE_FILE, finalPayload);
        return finalPayload;
    }

    /** Mantém compatibilidade com seu nome antigo */
    public static String listarColecoes() {
        return listarSetsPaginado();
    }

    // ----------------- endpoint builder -----------------

    private static String buildSetsEndpoint(int page, int pageSize) {
        // Sem orderBy para /sets. É aqui que você está se queimando.
        return "/sets?page=" + page + "&pageSize=" + pageSize;
    }

    // ----------------- cache -----------------

    private static void saveCache(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("⚠ Não foi possível salvar cache PokémonTCG em " + file + ": " + e.getMessage());
        }
    }

    private static String readCache(Path file) {
        try {
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("⚠ Não foi possível ler cache PokémonTCG em " + file + ": " + e.getMessage());
        }
        return null;
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private static long parseRetryAfterMs(HttpResponse<?> res) {
        try {
            return res.headers()
                    .firstValue("Retry-After")
                    .map(String::trim)
                    .map(Long::parseLong)
                    .map(sec -> sec * 1000L)
                    .orElse(0L);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String safeSnippet(String body) {
        if (body == null) return "";
        String s = body.replaceAll("\\s+", " ").trim();
        return s.length() > 240 ? s.substring(0, 240) + "..." : s;
    }

    // ----------------- JSON helper "mínimo viável" (sem libs) -----------------
    static class JsonMini {

        static Integer extractInt(String json, String key) {
            if (json == null) return null;
            String needle = "\"" + key + "\"";
            int p = json.indexOf(needle);
            if (p < 0) return null;
            int colon = json.indexOf(':', p);
            if (colon < 0) return null;

            int i = colon + 1;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            StringBuilder sb = new StringBuilder();
            while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '-')) {
                sb.append(json.charAt(i));
                i++;
            }

            try {
                if (sb.isEmpty()) return null;
                return Integer.parseInt(sb.toString());
            } catch (Exception e) {
                return null;
            }
        }

        static String extractArray(String json, String key) {
            if (json == null) return null;

            String needle = "\"" + key + "\"";
            int p = json.indexOf(needle);
            if (p < 0) return null;

            int colon = json.indexOf(':', p);
            if (colon < 0) return null;

            int start = json.indexOf('[', colon);
            if (start < 0) return null;

            int end = findMatchingBracket(json, start, '[', ']');
            if (end < 0) return null;

            return json.substring(start, end + 1);
        }

        static List<String> splitTopLevelArrayItems(String arrayJson) {
            List<String> items = new ArrayList<>();
            if (arrayJson == null) return items;

            String s = arrayJson.trim();
            if (s.length() < 2 || s.charAt(0) != '[') return items;

            int i = 1;
            int depthObj = 0;
            int depthArr = 0;
            boolean inString = false;
            int itemStart = -1;

            while (i < s.length() - 1) {
                char c = s.charAt(i);

                if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                    inString = !inString;
                }

                if (!inString) {
                    if (c == '{') {
                        if (depthObj == 0 && depthArr == 0) itemStart = i;
                        depthObj++;
                    } else if (c == '}') {
                        depthObj--;
                        if (depthObj == 0 && depthArr == 0 && itemStart >= 0) {
                            items.add(s.substring(itemStart, i + 1).trim());
                            itemStart = -1;
                        }
                    } else if (c == '[') {
                        depthArr++;
                    } else if (c == ']') {
                        depthArr--;
                    }
                }

                i++;
            }

            return items;
        }

        static String buildDataOnlyPayload(List<String> dataObjects, int lastPage, int pageSize, Integer count) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"data\":[");
            for (int i = 0; i < dataObjects.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(dataObjects.get(i));
            }
            sb.append("]");

            if (count != null) sb.append(",\"count\":").append(count);
            sb.append(",\"page\":").append(Math.max(1, lastPage));
            sb.append(",\"pageSize\":").append(pageSize);
            sb.append("}");
            return sb.toString();
        }

        private static int findMatchingBracket(String s, int openIndex, char open, char close) {
            int depth = 0;
            boolean inString = false;

            for (int i = openIndex; i < s.length(); i++) {
                char c = s.charAt(i);

                if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                    inString = !inString;
                }

                if (!inString) {
                    if (c == open) depth++;
                    else if (c == close) {
                        depth--;
                        if (depth == 0) return i;
                    }
                }
            }
            return -1;
        }
    }
}
