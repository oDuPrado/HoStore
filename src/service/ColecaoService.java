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
            ColecaoModel c = new Gson().fromJson(el, ColecaoModel.class);
            colecoes.add(c);
        }
        return colecoes;
    }
}
