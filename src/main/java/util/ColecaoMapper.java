package util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilitário para gerenciar o mapeamento entre:
 *   - "nome PT da Edição" (campo vindo da planilha da Liga, ex: "Evoluções em Paldea")
 *   - valores oficiais de colecao (ex: "Paldean Fates") e set (ex: "Scarlet & Violet")
 *
 * O arquivo JSON (data/colecao_mapping.json) armazena um map:
 *   { 
 *     "Evoluções em Paldea": { "colecao": "Paldean Fates", "set": "Scarlet & Violet" },
 *     ...
 *   }
 *
 * Fluxo de uso:
 * 1) Sempre chamar ColecaoMapper.carregar() uma vez (por exemplo, no início da importação).
 * 2) Para cada "nome PT" lido da planilha, verificar se já existe no mapa (get(nomePt)).
 * 3) Se não existir, coletar todos os nomes PT únicos e passar para VincularColecaoDialog para vincular.
 * 4) Após o usuário definir os pares, chamar ColecaoMapper.set(...) para gravar no JSON.
 *
 * CTRL+F:
 *  - Para encontrar onde carregar/carregar, busque "CARREGAR_HASH" ou "SALVAR_HASH".
 *  - Para encontrar a classe ColecaoMatch, use "COLECAO_MATCH".
 */
public class ColecaoMapper {

    // --- CAMINHO DO ARQUIVO JSON QUE GUARDARÁ O CACHE ---
    private static final String PATH = "data/colecao_mapping.json";

    /**
     * Representa um par de valores oficiais que mapeiam o nome PT da "Edição":
     *   - colecao: nome oficial de coleção (ex: "Paldean Fates")
     *   - set:    nome oficial de série/set (ex: "Scarlet & Violet")
     *
     * Usamos campos públicos para facilitar serialização com Gson.
     *
     * CTRL+F: COLECAO_MATCH
     */
    public static class ColecaoMatch {
        public String colecao;  // Nome oficial da coleção, igual ao campo colecao.nome no BD
        public String set;      // Nome oficial do set (campo set.nome no BD)

        // Construtor sem-arg para o Gson
        public ColecaoMatch() {}

        public ColecaoMatch(String colecao, String set) {
            this.colecao = colecao;
            this.set = set;
        }
    }

    // --- MAPA EM MEMÓRIA: chave = nomePT (ex: "Evoluções em Paldea") → valor = ColecaoMatch ---
    private static Map<String, ColecaoMatch> mapa = new HashMap<>();

    /**
     * CARREGAR_HASH:
     * Carrega o arquivo JSON para popular o mapa em memória. Se o arquivo não existir ou falhar
     * ao ler, cria um mapa vazio (sem lançar exceção ao usuário).
     *
     * Chame sempre no início do fluxo de importação.
     */
    public static void carregar() {
        try {
            String json = Files.readString(Paths.get(PATH));
            Type type = new TypeToken<Map<String, ColecaoMatch>>() {}.getType();
            mapa = new Gson().fromJson(json, type);
            if (mapa == null) {
                // Se o JSON estiver vazio ou mal formatado, garante que o mapa não seja null
                mapa = new HashMap<>();
            }
            System.out.println("[LOG] ColecaoMapper: cache carregado com " + mapa.size() + " entradas.");
        } catch (IOException e) {
            // Se não encontrou o arquivo ou houve erro, inicializa mapa vazio
            mapa = new HashMap<>();
            System.out.println("[LOG] ColecaoMapper: nenhum cache encontrado, iniciando vazio.");
        }
    }

    /**
     * SALVAR_HASH:
     * Salva o map atual em disco (arquivo JSON). Chamamos toda vez que adicionamos um novo vínculo.
     *
     * CTRL+F: SALVAR_HASH
     */
    public static void salvar() {
        try (FileWriter writer = new FileWriter(PATH)) {
            String json = new Gson().toJson(mapa);
            writer.write(json);
            System.out.println("[LOG] ColecaoMapper: cache salvo em " + PATH);
        } catch (IOException e) {
            // Se falhar ao salvar, imprime stack trace para debug
            System.err.println("[ERROR] ColecaoMapper: falha ao salvar cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retorna o ColecaoMatch (colecao + set) para um determinado nome PT da edição.
     * Se não existir, retorna null.
     *
     * CTRL+F: GET_MATCH
     */
    public static ColecaoMatch get(String nomeEdicaoPt) {
        return mapa.get(nomeEdicaoPt);
    }

    /**
     * Insere (ou sobrescreve) o mapeamento entre nome PT e os valores oficiais de colecao/set.
     * Após inserir, salva imediatamente em disco para persistir para próximas execuções.
     *
     * CTRL+F: SET_MATCH
     */
    public static void set(String nomeEdicaoPt, ColecaoMatch match) {
        mapa.put(nomeEdicaoPt, match);
        salvar();
    }

    /**
     * Recebe uma lista de todos os nomes PT lidos na planilha e retorna apenas aqueles
     * que ainda NÃO estão mapeados no cache. Essa lista servirá para exibir no dialog
     * para o usuário escolher manualmente.
     *
     * CTRL+F: NOMES_NAO_MAPEADOS
     */
    public static Set<String> nomesNaoMapeados(List<String> nomesDaPlanilha) {
        return nomesDaPlanilha.stream()
                .filter(nome -> !mapa.containsKey(nome))
                .collect(Collectors.toSet());
    }
}
