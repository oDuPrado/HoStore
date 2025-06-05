package service;

import api.PokeTcgApi;
import com.google.gson.*;
import model.ColecaoModel;

import java.util.ArrayList;
import java.util.List;

public class ColecaoService {

    public static List<ColecaoModel> listarColecoes() throws Exception {
        String json = PokeTcgApi.listarColecoes();

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        JsonArray data = obj.getAsJsonArray("data");

        List<ColecaoModel> colecoes = new ArrayList<>();
        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();

            ColecaoModel c = new ColecaoModel();
            c.setId(o.get("id").getAsString());
            c.setName(o.get("name").getAsString());
            c.setSeries(o.get("series").getAsString());
            c.setReleaseDate(o.get("releaseDate").getAsString());
            c.setSigla(o.has("ptcgoCode") ? o.get("ptcgoCode").getAsString() : null); // <- esse campo é o "sigla"

            colecoes.add(c);

            colecoes.add(c);
        }
        return colecoes;
    }
}
