package service;

import api.PokeTcgApi;
import com.google.gson.*;
import model.SetModel;

import java.util.ArrayList;
import java.util.List;

public class SetService {

    public static List<SetModel> listarSets() throws Exception {
        String json = PokeTcgApi.listarColecoes();
    
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        JsonArray data = obj.getAsJsonArray("data");
    
        List<SetModel> sets = new ArrayList<>();
        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
    
            String id = o.get("id").getAsString();
            String nome = o.get("name").getAsString();
            String series = o.get("series").getAsString();
            String releaseDate = o.has("releaseDate") ? o.get("releaseDate").getAsString() : "";
    
            SetModel s = new SetModel(id, nome, series, id, releaseDate); // 👈 id como colecao_id
            sets.add(s);
        }
    
        return sets;
    }
    
}
