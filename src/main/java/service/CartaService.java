package service;

import dao.CartaDAO;
import model.Carta;
import model.ProdutoModel;
import controller.ProdutoEstoqueController;

/**
 * Serviço responsável por gerenciar a persistência de Cartas e sua integração
 * com a tabela de produtos. Ao salvar ou atualizar uma carta, este serviço
 * garante que exista um registro correspondente na tabela "produtos", de modo
 * que a carta possa ser tratada como um produto comum (para vendas, estoque,
 * etc.).
 *
 * Observação: Por enquanto, definimos jogoId = "POKEMON" para todas as cartas,
 * mas no futuro poderá ser parametrizado conforme outros TCGs (YUGIOH, MAGIC,
 * etc.).
 */
public class CartaService {

    // DAO para acessar a tabela "cartas"
    private final CartaDAO cartaDAO = new CartaDAO();

    // Controller para inserir/atualizar dados na tabela "produtos"
    private final ProdutoEstoqueController produtoController = new ProdutoEstoqueController();

    /**
     * Salva uma nova carta no banco e cria o produto correspondente em "produtos".
     *
     * @param carta objeto Carta contendo todos os campos necessários para
     *              persistência
     * @throws Exception caso ocorra erro de SQL ou de persistência do produto
     */
    public void salvarNovaCarta(Carta carta) throws Exception {
        // 1) Insere a carta na tabela "cartas"
        cartaDAO.insert(carta);

        // 2) Gera o ProdutoModel a partir dos dados da carta e salva em "produtos"
        ProdutoModel produto = gerarProdutoModel(carta);
        produtoController.salvar(produto);
    }

    /**
     * Atualiza uma carta existente no banco e atualiza o produto correspondente em
     * "produtos".
     *
     * @param carta objeto Carta com o mesmo ID de uma carta já existente, contendo
     *              todos os campos atualizados
     * @throws Exception caso ocorra erro de SQL ou de persistência do produto
     */
    public void atualizarCarta(Carta carta) throws Exception {
        // 1) Atualiza todos os campos editáveis da carta na tabela "cartas"
        cartaDAO.update(carta);

        // 2) Gera novamente o ProdutoModel (com quantidade, preço, etc.) e salva em
        // "produtos"
        ProdutoModel produto = gerarProdutoModel(carta);
        produtoController.salvar(produto);
    }

    /**
     * Busca uma carta no banco pelo seu ID.
     *
     * @param id identificador único da carta (por exemplo, "PAL-011-R" ou "001")
     * @return objeto Carta completo (ou null se não existir)
     * @throws Exception caso ocorra erro de SQL durante a busca
     */
    public Carta buscarPorId(String id) throws Exception {
        return cartaDAO.buscarPorId(id);
    }

    /**
     * Constrói um ProdutoModel a partir dos atributos de uma carta.
     * O produto resultante terá:
     * - mesmo ID da carta (id único da tabela "produtos")
     * - nome idêntico ao da carta
     * - tipo fixo "Carta"
     * - quantidade = quantidade da carta (qtd)
     * - precoCompra = custo da carta
     * - precoVenda = preço de loja da carta
     * - jogoId fixo "POKEMON" (para futuras customizações, pode-se inferir de
     * setId)
     * - codigoBarras = null (não aplicável a cartas no momento)
     *
     * @param carta objeto Carta contendo todos os dados necessários
     * @return ProdutoModel pronto para ser persistido na tabela "produtos"
     */
    private ProdutoModel gerarProdutoModel(Carta carta) {
        // Cria instância de ProdutoModel usando construtor que seta criadoEm e
        // alteradoEm automaticamente
        ProdutoModel p = new ProdutoModel(
                carta.getId(), // id do produto = id da carta
                carta.getNome(), // nome do produto = nome da carta
                "Carta", // tipo fixo para distinguir no catálogo
                carta.getQtd(), // quantidade de estoque
                carta.getCusto(), // precoCompra = custo de aquisição
                carta.getPrecoLoja() // precoVenda = preço praticado na loja
        );

        // Definimos jogoId = "POKEMON" para todas as cartas por enquanto
        p.setJogoId("POKEMON");

        // Não há código de barras para cartas, mantemos null
        p.setCodigoBarras(null);

        return p;
    }

    public void salvarOuAtualizarCarta(Carta carta) throws Exception {
        CartaDAO dao = new CartaDAO();
        if (dao.buscarPorId(carta.getId()) != null) {
            atualizarCarta(carta); // já existe → atualiza
        } else {
            salvarNovaCarta(carta); // novo → insere
        }
    }

}
