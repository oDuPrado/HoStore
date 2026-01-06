package service;

import com.google.gson.*;
import model.ColecaoModel;

import java.util.*;

public class ColecaoService {

    /** Usa o JSON do endpoint /sets (API ou cache) e cria coleções (1 por set). */
    public static List<ColecaoModel> parseColecoesFromSetsJson(String json) {
        if (json == null || json.isBlank()) return List.of();

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        if (data == null) return List.of();

        List<ColecaoModel> colecoes = new ArrayList<>(data.size());

        for (JsonElement el : data) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            String id = safeStr(o, "id");
            String nome = safeStr(o, "name");
            String series = safeStr(o, "series");

            if (id == null || id.isBlank()) continue;
            if (nome == null) nome = "";
            if (series == null) series = "";

            ColecaoModel c = new ColecaoModel();
            c.setId(id);
            c.setName(nome); // <-- NOME DA COLEÇÃO = set.name (Phantasmal Flames)
            c.setSeries(series);
            c.setReleaseDate(safeStr(o, "releaseDate"));
            c.setSigla(safeStr(o, "ptcgoCode")); // opcional

            colecoes.add(c);
        }

        return colecoes;
    }

    private static String safeStr(JsonObject o, String key) {
        try {
            if (o == null || !o.has(key) || o.get(key).isJsonNull()) return null;
            return o.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
