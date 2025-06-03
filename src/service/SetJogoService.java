package service;

import api.CardGamesApi;
import com.google.gson.*;
import model.SetJogoModel;

import java.util.*;

/**
 * Serviço responsável por converter dados de APIs externas
 * em SetJogoModel para jogos diferentes de Pokémon.
 */
public class SetJogoService {

    /** 🔷 Yu-Gi-Oh! */
    public static List<SetJogoModel> listarSetsYugioh() throws Exception {
        String json = CardGamesApi.listarSetsYgo();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        List<SetJogoModel> lista = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject o = el.getAsJsonObject();
            SetJogoModel set = new SetJogoModel();
            set.setSetId(o.get("set_code").getAsString());
            set.setNome(o.get("set_name").getAsString());
            set.setJogoId("YUGIOH");
            set.setDataLancamento(o.has("tcg_date") ? o.get("tcg_date").getAsString() : null);
            set.setQtdCartas(o.has("num_of_cards") ? o.get("num_of_cards").getAsInt() : null);
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
            JsonObject o = el.getAsJsonObject();
            SetJogoModel set = new SetJogoModel();
            set.setSetId(o.get("code").getAsString());
            set.setNome(o.get("name").getAsString());
            set.setJogoId("MAGIC");
            set.setDataLancamento(o.has("released_at") ? o.get("released_at").getAsString() : null);
            set.setQtdCartas(o.has("card_count") ? o.get("card_count").getAsInt() : null);
            set.setCodigoExterno(o.has("id") ? o.get("id").getAsString() : null); // ID da Scryfall
            lista.add(set);
        }

        return lista;
    }

    /** 🔷 Digimon TCG */
    public static List<SetJogoModel> listarSetsDigimon() throws Exception {
        String json = CardGamesApi.listarCardsDigimon();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        List<SetJogoModel> lista = new ArrayList<>();
        Set<String> codigosUnicos = new HashSet<>();

        for (JsonElement el : array) {
            JsonObject card = el.getAsJsonObject();
            if (!card.has("cardnumber"))
                continue;

            String cardNumber = card.get("cardnumber").getAsString();
            String prefixo = cardNumber.contains("-") ? cardNumber.split("-")[0] : cardNumber;

            if (codigosUnicos.contains(prefixo))
                continue;
            codigosUnicos.add(prefixo);

            SetJogoModel set = new SetJogoModel();
            set.setSetId(prefixo); // Ex: "BT", "EX", "BO"
            set.setNome("Pack " + prefixo);
            set.setJogoId("DIGIMON");
            set.setDataLancamento(null);
            set.setQtdCartas(null);
            set.setCodigoExterno(null);
            lista.add(set);
        }

        return lista;
    }

    /** 🔷 One Piece Card Game */
    public static List<SetJogoModel> listarSetsOnePiece() throws Exception {
        String json = CardGamesApi.listarSetsOnePiece();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        List<SetJogoModel> lista = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject o = el.getAsJsonObject();

            SetJogoModel set = new SetJogoModel();

            set.setSetId(o.has("id") ? o.get("id").getAsString() : UUID.randomUUID().toString());
            set.setNome(o.has("name") ? o.get("name").getAsString() : "Set Desconhecido");
            set.setJogoId("ONEPIECE");
            set.setDataLancamento(o.has("releaseDate") ? o.get("releaseDate").getAsString() : null);
            set.setQtdCartas(o.has("cardCount") ? o.get("cardCount").getAsInt() : null);
            set.setCodigoExterno(o.has("code") ? o.get("code").getAsString() : null);

            lista.add(set);
        }

        return lista;
    }

    /** 🔷 Dragon Ball Super – sem API pública, lista vazia */
    public static List<SetJogoModel> listarSetsDbz() {
        return new ArrayList<>();
    }
}
