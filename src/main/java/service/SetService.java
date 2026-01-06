package service;

import api.PokeTcgApi;
import com.google.gson.*;
import model.SetModel;

import java.util.ArrayList;
import java.util.List;

/**
 * SetService (Pokémon)
 * - Busca JSON (API/cache/fallback) via PokeTcgApi
 * - Faz parse robusto via parseSetsFromJson(json)
 */
public class SetService {

    public static List<SetModel> listarSets() {
        String json;
        try {
            // PokeTcgApi.listarColecoes() na prática devolve /sets (sets do Pokémon)
            json = PokeTcgApi.listarColecoes();
        } catch (Exception e) {
            System.err.println("⚠ SetService: falha ao obter JSON de sets Pokémon: " + e.getMessage());
            return List.of();
        }
        return parseSetsFromJson(json);
    }

    /**
     * Parser robusto: recebe o JSON bruto da PokémonTCG API (endpoint /sets)
     * e converte em List<SetModel>.
     *
     * Regras:
     * - Não lança exceção (não derruba init)
     * - Ignora itens inválidos
     * - Usa defaults seguros
     *
     * Observação importante:
     * Seu SetModel atual parece: new SetModel(id, nome, series, colecao_id, data_lancamento)
     * Aqui, colecao_id é DERIVADO DA SERIES pra ficar estável e evitar FK maluca.
     */
    public static List<SetModel> parseSetsFromJson(String json) {
        List<SetModel> sets = new ArrayList<>();
        if (json == null || json.isBlank()) return sets;

        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("WARN: Parser de sets Pokémon: JSON inválido.");
            return sets;
        }

        JsonArray data = root.getAsJsonArray("data");
        if (data == null) {
            System.err.println("WARN: Parser de sets Pokémon: campo 'data' não encontrado.");
            return sets;
        }

        for (JsonElement el : data) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            String id = safeStr(o, "id");
            String nome = safeStr(o, "name");
            String series = safeStr(o, "series");
            String releaseDate = safeStr(o, "releaseDate");

            // sem id ou nome, não existe set
            if (id == null || id.isBlank() || nome == null || nome.isBlank()) continue;

            // colecao_id estável derivado da series (não usa id do set)
            String colecaoId = buildColecaoId(series);

            // Ajuste caso seu modelo não suporte null
            if (releaseDate == null) releaseDate = "";

            // Constrói seu SetModel
            // Assumindo construtor: (id, nome, series, colecaoId, dataLancamento)
            SetModel s = new SetModel(id, nome, series, colecaoId, releaseDate);
            sets.add(s);
        }

        return sets;
    }

    // ---------------- helpers ----------------

    private static String safeStr(JsonObject o, String key) {
        try {
            if (o == null || !o.has(key) || o.get(key).isJsonNull()) return null;
            String v = o.get(key).getAsString();
            return (v != null && v.isBlank()) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildColecaoId(String series) {
        if (series == null || series.isBlank()) return "SERIES_UNKNOWN";
        return "SERIES_" + normalize(series);
    }

    private static String normalize(String s) {
        return s.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_]", "");
    }
}
