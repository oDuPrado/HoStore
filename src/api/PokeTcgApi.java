package api;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class PokeTcgApi {

    private static final String BASE_URL = "https://api.pokemontcg.io/v2";
    private static final String API_KEY = "8d293a2a-4949-4d04-a06c-c20672a7a12c"; // opcional – pode deixar vazio
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /** Consulta uma rota da API com GET e retorna o JSON bruto */
    public static String get(String endpoint) throws Exception {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(new URI(BASE_URL + endpoint))
            .timeout(Duration.ofSeconds(15))
            .GET();

        if (!API_KEY.isBlank()) {
            reqBuilder.header("X-Api-Key", API_KEY);
        }

        HttpRequest req = reqBuilder.build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            return res.body();
        } else {
            throw new RuntimeException("Erro na API: " + res.statusCode() + "\n" + res.body());
        }
    }

    // ==================== FUNÇÕES PRINCIPAIS ====================

    /** Lista todas as coleções e sets disponíveis */
    public static String listarColecoes() throws Exception {
        return get("/sets"); // retorna JSON completo
    }

    // public static String buscarCartasPorNome(String nome) throws Exception {
    //     return get("/cards?q=name:" + URLEncoder.encode(nome, "UTF-8"));
    // }
}
