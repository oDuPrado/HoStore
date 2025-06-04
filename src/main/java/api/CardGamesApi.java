package api;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/**
 * CardGamesApi: Classe para consultar sets, coleções e cartas de diversos TCGs,
 * excluindo Pokémon (que permanece em PokeTcgApi.java).
 * 
 * Jogos incluídos:
 * - Yu-Gi-Oh!
 * - Magic: The Gathering
 * - Digimon TCG
 * - One Piece Card Game
 * - Dragon Ball Super Card Game (sem endpoint público)
 */
public class CardGamesApi {

    // HTTP client compartilhado para todas as requisições
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Faz GET a uma URL completa e retorna o JSON como String.
     * @param fullUrl URL completa para requisição GET
     * @return corpo da resposta (JSON) em formato de String
     * @throws Exception em caso de erro de conexão ou status code não-2xx
     */
    private static String getFull(String fullUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(fullUrl))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        } else {
            throw new RuntimeException("Erro na API (GET " + fullUrl + "): " 
                + status + "\n" + response.body());
        }
    }

    // ==================== Yu-Gi-Oh! (YGOPRODeck) ====================

    private static final String YGO_BASE_URL = "https://db.ygoprodeck.com/api/v7";

    /**
     * Lista todos os sets/coleções de Yu-Gi-Oh!.
     * Endpoint: https://db.ygoprodeck.com/api/v7/cardsets.php
     * @return JSON contendo array de sets (cada objeto possui set_name, set_code, etc.)
     * @throws Exception em caso de falha na requisição
     */
    public static String listarSetsYgo() throws Exception {
        String url = YGO_BASE_URL + "/cardsets.php";
        return getFull(url);
    }

    /**
     * Lista todas as cartas de um determinado set de Yu-Gi-Oh!.
     * @param setCode código do set (por ex.: "LOB", "SDY", "WCS", etc.)
     * @return JSON contendo informações de todas as cartas desse set
     * Endpoint: https://db.ygoprodeck.com/api/v7/cardinfo.php?set={setCode}
     * @throws Exception em caso de falha na requisição
     */
    public static String listarCardsYgoPorSet(String setCode) throws Exception {
        String encoded = java.net.URLEncoder.encode(setCode, "UTF-8");
        String url = YGO_BASE_URL + "/cardinfo.php?set=" + encoded;
        return getFull(url);
    }

    // ==================== Magic: The Gathering (Scryfall) ====================

    private static final String MAGIC_SETS_URL = "https://api.scryfall.com/sets";

    /**
     * Lista todos os sets/coleções de Magic: The Gathering.
     * Endpoint público: https://api.scryfall.com/sets
     * @return JSON contendo o objeto "data" com array de sets (cada objeto possui code, name, released_at, etc.)
     * @throws Exception em caso de falha na requisição
     */
    public static String listarSetsMagic() throws Exception {
        return getFull(MAGIC_SETS_URL);
    }

    /**
     * Lista todas as cartas de um determinado set de Magic: The Gathering.
     * @param setCode código do set Scryfall (por ex.: "ced", "2ed", "lea", etc.)
     * @return JSON contendo informações de todas as cartas daquele set
     * Endpoint: https://api.scryfall.com/cards/search?order=set&q=e%3A{setCode}&unique=prints
     * @throws Exception em caso de falha na requisição
     */
    public static String listarCardsMagicPorSet(String setCode) throws Exception {
        String encoded = java.net.URLEncoder.encode(setCode, "UTF-8");
        String url = "https://api.scryfall.com/cards/search?order=set&q=e%3A" + encoded + "&unique=prints";
        return getFull(url);
    }

    // ==================== Digimon TCG (DigimonCard.io) ====================

    private static final String DIGIMON_ALL_CARDS_URL = "https://digimoncard.io/api-public/getAllCards.php";

    /**
     * Retorna todas as cartas de Digimon TCG (JSON completo com cada carta e campo 'cardnumber').
     * Para extrair sets/coleções, filtre o prefixo de 'cardnumber' (ex.: "BO-01" → prefixo "BO")
     * ou utilize o campo 'packName' se disponível no JSON.
     * Endpoint: https://digimoncard.io/api-public/getAllCards.php
     * @return JSON contendo array de cartas de Digimon
     * @throws Exception em caso de falha na requisição
     */
    public static String listarCardsDigimon() throws Exception {
        return getFull(DIGIMON_ALL_CARDS_URL);
    }

    /**
     * Mesmo que listarCardsDigimon(), pois não existe endpoint dedicado a “sets” no DigimonCard.io.
     * Deixa a cargo do cliente extrair packs únicos a partir de 'cardnumber' ou 'packName'.
     * @return JSON completo de cartas de Digimon (idêntico a listarCardsDigimon())
     * @throws Exception em caso de falha na requisição
     */
    public static String listarSetsDigimonBruto() throws Exception {
        return listarCardsDigimon();
    }

    // ==================== One Piece Card Game (OPTCG API) ====================

    private static final String ONEPIECE_SETS_URL = "https://optcgapi.com/api/allSets/";

    /**
     * Lista todos os sets/coleções de One Piece Card Game.
     * Endpoint: https://optcgapi.com/api/allSets/
     * @return JSON contendo array de sets (cada objeto possui informações de ID, nome, código, etc.)
     * @throws Exception em caso de falha na requisição
     */
    public static String listarSetsOnePiece() throws Exception {
        return getFull(ONEPIECE_SETS_URL);
    }

    /**
     * Retorna as cartas de One Piece Card Game (não documentado / não disponível oficialmente).
     * Se sua API fornecer um endpoint "/api/allCards/" substitua o throw por getFull("https://optcgapi.com/api/allCards/").
     * @throws UnsupportedOperationException se não existir endpoint público de cartas
     */
    public static String listarCardsOnePieceBruto() {
        throw new UnsupportedOperationException(
            "Endpoint de cartas de One Piece não disponível. " +
            "Use listarSetsOnePiece() e filtre no cliente, " +
            "ou obtenha endpoint de /allCards/ se existir."
        );
    }

    // ==================== Dragon Ball Super Card Game (Fusion World) ====================

    /**
     * Dragon Ball Super TCG não fornece endpoint público sem chave de API.
     * Use campo de texto livre no front-end ou obtenha credenciais para acessar a API de Bandai.
     * @throws UnsupportedOperationException em todas as chamadas
     */
    public static String listarSetsDbz() {
        throw new UnsupportedOperationException(
            "Dragon Ball Super não possui endpoint público sem chave. " +
            "Use campo livre para o usuário digitar a coleção."
        );
    }

    public static String listarCardsDbz() {
        throw new UnsupportedOperationException(
            "Dragon Ball Super não possui endpoint público sem chave. " +
            "Use campo livre para o usuário digitar ou obtenha acesso autorizado."
        );
    }

    // ==================== EXEMPLO DE USO (comentado) ====================
    /*
    public static void main(String[] args) {
        try {
            // Yu-Gi-Oh! - listar sets
            String ygoSets = CardGamesApi.listarSetsYgo();
            System.out.println("YGO Sets JSON:\n" + ygoSets);

            // Yu-Gi-Oh! - listar cartas de um set
            String ygoCards = CardGamesApi.listarCardsYgoPorSet("WCS");
            System.out.println("YGO Cards (WCS):\n" + ygoCards);

            // Magic - listar sets
            String magicSets = CardGamesApi.listarSetsMagic();
            System.out.println("Magic Sets JSON:\n" + magicSets);

            // Magic - listar cartas de um set
            String magicCards = CardGamesApi.listarCardsMagicPorSet("ced");
            System.out.println("Magic Cards (ced):\n" + magicCards);

            // Digimon - listar cartas (extração de sets no cliente)
            String digimonAll = CardGamesApi.listarCardsDigimon();
            System.out.println("Digimon Cards JSON:\n" + digimonAll);

            // One Piece - listar sets
            String onePieceSets = CardGamesApi.listarSetsOnePiece();
            System.out.println("One Piece Sets JSON:\n" + onePieceSets);

            // Dragon Ball Super - lança exceção
            String dbzSets = CardGamesApi.listarSetsDbz();
            System.out.println(dbzSets);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
}
