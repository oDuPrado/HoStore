package api;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;

/**
 * CardGamesApi: Consulta sets/coleções/cartas de diversos TCGs (exceto Pokémon).
 *
 * APIs públicas validadas:
 * - Yu-Gi-Oh! (YGOPRODeck): https://db.ygoprodeck.com/api/v7/cardsets.php  | cardinfo.php
 * - Magic (Scryfall): https://api.scryfall.com/sets | cards/search (paginado)
 * - Digimon (digimoncard.io): https://digimoncard.io/api-public/getAllCards  (sem .php)
 * - One Piece (optcgapi.com): https://optcgapi.com/api/allSets/ | /api/allSetCards/
 * - Dragon Ball Super: sem endpoint público (fallback local)
 */
public class CardGamesApi {

    // -------------------- HTTP CLIENT ROBUSTO --------------------

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL) // resolve 301/302 automaticamente
            .version(HttpClient.Version.HTTP_2)
            .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    private static final int MAX_RETRIES = 3;

    // -------------------- CACHE / FALLBACK (JSON) --------------------

    private static final Path CACHE_DIR = Paths.get(System.getProperty("user.dir"), "data", "cache");

    private static final Path YGO_SETS_CACHE = CACHE_DIR.resolve("yugioh_sets.json");
    private static final Path MAGIC_SETS_CACHE = CACHE_DIR.resolve("magic_sets.json");
    private static final Path DIGIMON_ALL_CARDS_CACHE = CACHE_DIR.resolve("digimon_all_cards.json");
    private static final Path ONEPIECE_SETS_CACHE = CACHE_DIR.resolve("onepiece_sets.json");
    private static final Path ONEPIECE_ALL_CARDS_CACHE = CACHE_DIR.resolve("onepiece_all_cards.json");
    private static final Path DBZ_FALLBACK_CACHE = CACHE_DIR.resolve("dragonball_sets.json");

    // -------------------- YU-GI-OH! (YGOPRODeck) --------------------

    private static final String YGO_BASE_URL = "https://db.ygoprodeck.com/api/v7";
    private static final String YGO_SETS_URL = YGO_BASE_URL + "/cardsets.php";
    // Observação: cardsetsinfo.php existe e precisa setcode, mas aqui você usa lista geral.
    // Docs: https://ygoprodeck.com/api-guide/

    public static String listarSetsYgo() {
        return fetchWithCache(
                YGO_SETS_URL,
                YGO_SETS_CACHE,
                // fallback mínimo
                "[]",
                "Yu-Gi-Oh! sets"
        );
    }

    /**
     * Lista cartas por set (YGOPRODeck suporta filtro via parâmetro "set").
     * Ex.: /cardinfo.php?set=Metal%20Raiders
     */
    public static String listarCardsYgoPorSet(String setCodeOrName) {
        String encoded = urlEncode(setCodeOrName);
        String url = YGO_BASE_URL + "/cardinfo.php?set=" + encoded;

        // Cache por set (pra não virar um ransomware de rede)
        Path perSetCache = CACHE_DIR.resolve("yugioh_cards_set_" + safeFileName(setCodeOrName) + ".json");

        // fallback mínimo compatível com o formato do YGOPRODeck (geralmente {data:[...]})
        String empty = "{\"data\":[]}";
        return fetchWithCache(url, perSetCache, empty, "Yu-Gi-Oh! cards by set");
    }

    // -------------------- MAGIC (Scryfall) --------------------

    private static final String MAGIC_SETS_URL = "https://api.scryfall.com/sets";

    public static String listarSetsMagic() {
        // Resposta Scryfall é um objeto com "data":[...]
        String empty = "{\"object\":\"list\",\"has_more\":false,\"data\":[]}";
        return fetchWithCache(MAGIC_SETS_URL, MAGIC_SETS_CACHE, empty, "Magic sets");
    }

    /**
     * Lista cartas de um set do Scryfall (paginado).
     * Endpoint: /cards/search?order=set&q=e:{setCode}&unique=prints
     *
     * IMPORTANTE: isso pode gerar várias páginas. Aqui eu agrego até maxPages e retorno um JSON único:
     * { "data":[...], "truncated":true/false }
     */
    public static String listarCardsMagicPorSet(String setCode, int maxPages) {
        if (maxPages <= 0) maxPages = 5;

        String encoded = urlEncode(setCode);
        String firstUrl = "https://api.scryfall.com/cards/search?order=set&q=e%3A" + encoded + "&unique=prints";

        Path cache = CACHE_DIR.resolve("magic_cards_set_" + safeFileName(setCode) + ".json");
        try {
            String aggregated = fetchScryfallPaginated(firstUrl, maxPages);
            saveCache(cache, aggregated);
            return aggregated;
        } catch (Exception e) {
            System.err.println("⚠ Falha Magic cards (" + setCode + "). Tentando fallback cache. Motivo: " + e.getMessage());
            String cached = readCache(cache);
            if (cached != null && !cached.isBlank()) {
                System.err.println("✅ Fallback Magic aplicado: usando cache local do set " + setCode);
                return cached;
            }
            return "{\"data\":[],\"truncated\":false}";
        }
    }

    // -------------------- DIGIMON (digimoncard.io) --------------------

    /**
     * Endpoint documentado (SEM .php): https://digimoncard.io/api-public/getAllCards
     * A doc também mostra variante via /index.php/api-public/getAllCards
     */
    private static final String DIGIMON_ALL_CARDS_URL = "https://digimoncard.io/api-public/getAllCards";

    public static String listarCardsDigimon() {
        // Resposta costuma ser array JSON.
        return fetchWithCache(
                DIGIMON_ALL_CARDS_URL,
                DIGIMON_ALL_CARDS_CACHE,
                "[]",
                "Digimon all cards"
        );
    }

    // -------------------- ONE PIECE (optcgapi.com) --------------------

    private static final String ONEPIECE_SETS_URL = "https://optcgapi.com/api/allSets/";
    private static final String ONEPIECE_ALL_CARDS_URL = "https://optcgapi.com/api/allSetCards/";

    public static String listarSetsOnePiece() {
        return fetchWithCache(
                ONEPIECE_SETS_URL,
                ONEPIECE_SETS_CACHE,
                "[]",
                "One Piece sets"
        );
    }

    /**
     * Lista todas as cartas (set cards) do One Piece via endpoint documentado.
     * Isso pode ser grande. Por isso cache obrigatório.
     */
    public static String listarCardsOnePieceBruto() {
        return fetchWithCache(
                ONEPIECE_ALL_CARDS_URL,
                ONEPIECE_ALL_CARDS_CACHE,
                "[]",
                "One Piece all set cards"
        );
    }

    // -------------------- DRAGON BALL SUPER --------------------

    /**
     * Sem endpoint público confiável/documentado: retorna fallback local (cache) ou vazio.
     * Você pode preencher esse JSON manualmente (ou via import futuro).
     */
    public static String listarSetsDbz() {
        String cached = readCache(DBZ_FALLBACK_CACHE);
        return (cached != null && !cached.isBlank()) ? cached : "[]";
    }

    public static String listarCardsDbz() {
        // mesmo raciocínio: sem API pública, sem milagre
        return "[]";
    }

    // -------------------- CORE: GET COM RETRY + CACHE --------------------

    private static String fetchWithCache(String url, Path cacheFile, String emptyFallbackJson, String label) {
        try {
            String json = getFull(url);
            saveCache(cacheFile, json);
            return json;
        } catch (Exception e) {
            System.err.println("⚠ Falha " + label + ". Tentando fallback cache. Motivo: " + e.getMessage());
            String cached = readCache(cacheFile);
            if (cached != null && !cached.isBlank()) {
                System.err.println("✅ Fallback aplicado (" + label + "): usando cache local.");
                return cached;
            }
            System.err.println("❌ Sem cache disponível (" + label + "). Retornando fallback vazio.");
            return emptyFallbackJson;
        }
    }

    /**
     * GET robusto: redirect + retry/backoff para 429/5xx + rede/timeout.
     */
    private static String getFull(String fullUrl) throws IOException, InterruptedException {
        URI uri = URI.create(fullUrl);

        long backoffMs = 800;
        IOException lastIo = null;
        InterruptedException lastInterrupted = null;
        RuntimeException lastRuntime = null;

        for (int attempt = 1; attempt <= (MAX_RETRIES + 1); attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    return response.body();
                }

                // retry útil: rate limit / servidor instável
                if (status == 429 || (status >= 500 && status <= 599)) {
                    long retryAfterMs = parseRetryAfterMs(response);
                    long sleep = retryAfterMs > 0 ? retryAfterMs : backoffMs;
                    System.err.println("⚠ HTTP " + status + " (tentativa " + attempt + "). Retry em " + sleep + "ms: " + fullUrl);
                    sleepQuietly(sleep);
                    backoffMs = Math.min(backoffMs * 2, 8000);
                    continue;
                }

                // erro definitivo: não adianta retry
                throw new RuntimeException("Erro na API (GET " + fullUrl + "): HTTP " + status +
                        (response.body() != null ? "\n" + safeSnippet(response.body()) : ""));

            } catch (HttpTimeoutException e) {
                lastIo = new IOException("Timeout ao chamar " + fullUrl + " (tentativa " + attempt + ")", e);
                System.err.println("⚠ " + lastIo.getMessage());
                if (attempt <= MAX_RETRIES) {
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 8000);
                    continue;
                }
            } catch (IOException e) {
                lastIo = e;
                System.err.println("⚠ Erro de rede ao chamar " + fullUrl + " (tentativa " + attempt + "): " + e.getMessage());
                if (attempt <= MAX_RETRIES) {
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 8000);
                    continue;
                }
            } catch (InterruptedException e) {
                lastInterrupted = e;
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException e) {
                lastRuntime = e;
                break;
            }
        }

        if (lastRuntime != null) throw lastRuntime;
        if (lastInterrupted != null) throw lastInterrupted;
        if (lastIo != null) throw lastIo;

        throw new IOException("Falha desconhecida ao chamar " + fullUrl);
    }

    // -------------------- Scryfall pagination aggregator --------------------

    /**
     * Agrega páginas do Scryfall (cards/search) até maxPages ou até has_more = false.
     * Retorna:
     * { "data":[...], "truncated": true/false }
     *
     * Sem dependência de JSON lib: faz parsing "na unha" (não perfeito, mas pragmático).
     * Se você quiser 100% correto, usa Gson/Jackson e parseia o JSON direito.
     */
    private static String fetchScryfallPaginated(String firstUrl, int maxPages) throws IOException, InterruptedException {
        String next = firstUrl;
        StringBuilder allDataArrays = new StringBuilder();
        boolean truncated = false;
        int pages = 0;

        while (next != null && !next.isBlank() && pages < maxPages) {
            pages++;
            String body = getFull(next);

            // Extrai o array "data":[ ... ] (bem básico)
            String dataArray = extractJsonArray(body, "\"data\"");
            if (dataArray == null) {
                // Se falhar extração, devolve corpo inteiro como fallback bruto
                return "{\"data\":[],\"truncated\":false,\"raw\":" + quoteJson(body) + "}";
            }

            // Concatena arrays: remove [ ] e junta com vírgula
            String inner = stripBrackets(dataArray).trim();
            if (!inner.isEmpty()) {
                if (allDataArrays.length() > 0) allDataArrays.append(",");
                allDataArrays.append(inner);
            }

            boolean hasMore = body.contains("\"has_more\":true");
            if (!hasMore) {
                next = null;
                break;
            }

            // next_page é URL string
            String nextPage = extractJsonString(body, "\"next_page\"");
            next = nextPage;
        }

        if (next != null && !next.isBlank()) {
            truncated = true;
        }

        return "{\"data\":[" + allDataArrays + "],\"truncated\":" + truncated + "}";
    }

    // -------------------- cache helpers --------------------

    private static void saveCache(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("⚠ Não foi possível salvar cache em " + file + ": " + e.getMessage());
        }
    }

    private static String readCache(Path file) {
        try {
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("⚠ Não foi possível ler cache em " + file + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------- misc helpers --------------------

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
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
        return s.length() > 250 ? s.substring(0, 250) + "..." : s;
    }

    private static String urlEncode(String v) {
        try {
            return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }

    private static String safeFileName(String s) {
        if (s == null) return "null";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    // --------- “parsing” simples sem biblioteca JSON (pragmático) ---------

    private static String stripBrackets(String array) {
        String a = array.trim();
        if (a.startsWith("[")) a = a.substring(1);
        if (a.endsWith("]")) a = a.substring(0, a.length() - 1);
        return a;
    }

    private static String extractJsonArray(String json, String key) {
        int k = json.indexOf(key);
        if (k < 0) return null;
        int colon = json.indexOf(':', k);
        if (colon < 0) return null;

        int start = json.indexOf('[', colon);
        if (start < 0) return null;

        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private static String extractJsonString(String json, String key) {
        int k = json.indexOf(key);
        if (k < 0) return null;
        int colon = json.indexOf(':', k);
        if (colon < 0) return null;

        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;

        // não trata escapes complexos, mas resolve a maior parte
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static String quoteJson(String s) {
        if (s == null) return "null";
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}
