package service;

import api.CardGamesApi;
import com.google.gson.*;
import model.SetJogoModel;

import java.util.*;

/**
 * Serviço responsável por converter dados de APIs externas
 * em SetJogoModel para jogos diferentes de Pokémon.
 *
 * Regra: se API falhar, a camada DAO/DB deve aplicar cache/fallback.
 * Aqui a gente só garante que parsing não explode.
 */
public class SetJogoService {

    /** 🔷 Yu-Gi-Oh! */
    public static List<SetJogoModel> listarSetsYugioh() throws Exception {
        String json = CardGamesApi.listarSetsYgo();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        List<SetJogoModel> lista = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            String setId = safeStr(o, "set_code");
            String nome = safeStr(o, "set_name");
            if (setId == null || nome == null) continue;

            SetJogoModel set = new SetJogoModel();
            set.setSetId(setId);
            set.setNome(nome);
            set.setJogoId("YUGIOH");
            set.setDataLancamento(safeStr(o, "tcg_date"));
            set.setQtdCartas(safeInt(o, "num_of_cards"));
            set.setCodigoExterno(null);

            lista.add(set);
        }
        return lista;
    }

    /** 🔷 Magic: The Gathering */
    public static List<SetJogoModel> listarSetsMagic() throws Exception {
        String json = CardGamesApi.listarSetsMagic();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray array = root.getAsJsonArray("data");

        List<SetJogoModel> lista = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            String code = safeStr(o, "code");
            String name = safeStr(o, "name");
            if (code == null || name == null) continue;

            SetJogoModel set = new SetJogoModel();
            set.setSetId(code);
            set.setNome(name);
            set.setJogoId("MAGIC");
            set.setDataLancamento(safeStr(o, "released_at"));
            set.setQtdCartas(safeInt(o, "card_count"));
            set.setCodigoExterno(safeStr(o, "id")); // Scryfall id

            lista.add(set);
        }
        return lista;
    }

    /** 🔷 Digimon TCG */
    public static List<SetJogoModel> listarSetsDigimon() throws Exception {
        String json = CardGamesApi.listarCardsDigimon();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        // Preferir "packName" se existir. Se não, extrair prefixo do cardnumber.
        Map<String, SetJogoModel> map = new LinkedHashMap<>();

        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            JsonObject card = el.getAsJsonObject();

            String packName = safeStr(card, "packName");
            String cardNumber = safeStr(card, "cardnumber");

            String setId;
            String nome;

            if (packName != null && !packName.isBlank()) {
                setId = normalize(packName);
                nome = packName;
            } else if (cardNumber != null && !cardNumber.isBlank()) {
                String prefixo = cardNumber.contains("-") ? cardNumber.split("-")[0] : cardNumber;
                setId = prefixo;
                nome = "Pack " + prefixo;
            } else {
                continue;
            }

            String key = "DIGIMON:" + setId;
            if (map.containsKey(key)) continue;

            SetJogoModel set = new SetJogoModel();
            set.setSetId(setId);
            set.setNome(nome);
            set.setJogoId("DIGIMON");
            set.setDataLancamento(null);
            set.setQtdCartas(null);
            set.setCodigoExterno(null);

            map.put(key, set);
        }

        return new ArrayList<>(map.values());
    }

    /** 🔷 One Piece Card Game */
    public static List<SetJogoModel> listarSetsOnePiece() throws Exception {
        String json = CardGamesApi.listarSetsOnePiece();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        List<SetJogoModel> lista = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            String name = safeStr(o, "name");
            String code = safeStr(o, "code");
            String id = safeStr(o, "id");

            // setId estável pra deduplicar:
            // 1) code, 2) id, 3) nome normalizado
            String setId = (code != null && !code.isBlank())
                    ? code
                    : (id != null && !id.isBlank())
                        ? id
                        : (name != null ? normalize(name) : null);

            if (setId == null) continue;

            SetJogoModel set = new SetJogoModel();
            set.setSetId(setId);
            set.setNome(name != null ? name : "Set Desconhecido");
            set.setJogoId("ONEPIECE");
            set.setDataLancamento(safeStr(o, "releaseDate"));
            set.setQtdCartas(safeInt(o, "cardCount"));
            set.setCodigoExterno(code);

            lista.add(set);
        }

        return lista;
    }

    /** 🔷 Dragon Ball Super – sem API pública, lista vazia */
    public static List<SetJogoModel> listarSetsDbz() {
        return new ArrayList<>();
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

    private static Integer safeInt(JsonObject o, String key) {
        try {
            if (o == null || !o.has(key) || o.get(key).isJsonNull()) return null;
            return o.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String s) {
        if (s == null) return null;
        return s.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_]", "")
                .toUpperCase();
    }
}
