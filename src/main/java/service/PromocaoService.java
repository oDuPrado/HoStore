package service;

import dao.ClienteDAO;
import dao.ClienteVipDAO;
import dao.PromocaoDAO;
import dao.PromocaoProdutoDAO;
import model.AplicaEm;
import model.ClienteModel;
import model.PromocaoModel;
import model.ProdutoModel;
import model.TipoDesconto;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Contem toda a regra de negocio de promocoes.
 */
public class PromocaoService {

    private final PromocaoDAO promocaoDAO = new PromocaoDAO();
    private final PromocaoProdutoDAO promoProdDAO = new PromocaoProdutoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ClienteVipDAO vipDAO = new ClienteVipDAO();

    public static class PromocaoAplicada {
        public String promocaoId;
        public String promocaoNome;
        public TipoDesconto tipoDesconto;
        public double descontoPercent;
        public double descontoValor;
        public double precoFinalUnit;
        public int prioridade;
    }

    /**
     * Retorna a promocao ativa mais vantajosa para o item (ou empty).
     */
    public Optional<PromocaoAplicada> calcularPromocao(ProdutoModel produto, int qtd, double precoUnit,
            String clienteId) throws Exception {
        if (produto == null || qtd <= 0 || precoUnit <= 0)
            return Optional.empty();

        List<PromocaoModel> ativas = promocaoDAO.listarAtivas();
        if (ativas == null || ativas.isEmpty())
            return Optional.empty();

        final String categoria = resolveCategoria(produto);

        boolean vip = isClienteVip(clienteId);

        return ativas.stream()
                .filter(p -> aplicaParaProduto(p, produto.getId(), categoria, vip))
                .map(p -> calcular(p, qtd, precoUnit))
                .max(Comparator
                        .comparingDouble((PromocaoAplicada a) -> a.descontoValor)
                        .thenComparingInt(a -> a.prioridade).thenComparingDouble(a -> a.descontoPercent));
    }

    public boolean isProdutoEmPromocao(ProdutoModel produto) throws Exception {
        if (produto == null)
            return false;
        final String categoria = resolveCategoria(produto);
        List<PromocaoModel> ativas = promocaoDAO.listarAtivas();
        return ativas.stream().anyMatch(p -> aplicaParaProduto(p, produto.getId(), categoria, false));
    }

    public List<PromocaoModel> listarPromocoesAtivas() throws Exception {
        return promocaoDAO.listarAtivas();
    }

    private boolean aplicaParaProduto(PromocaoModel p, String produtoId, String categoria, boolean clienteVip) {
        if (p == null || p.getAplicaEm() == null)
            return false;
        AplicaEm alvo = p.getAplicaEm();
        try {
            if (alvo == AplicaEm.PRODUTO) {
                return promoProdDAO.existeVinculo(p.getId(), produtoId);
            }
            if (alvo == AplicaEm.CATEGORIA) {
                if (categoria == null || categoria.isBlank())
                    return false;
                return categoria.equalsIgnoreCase(p.getCategoria());
            }
            if (alvo == AplicaEm.CLIENTE_VIP) {
                return clienteVip;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private PromocaoAplicada calcular(PromocaoModel p, int qtd, double precoUnit) {
        double bruto = precoUnit * qtd;
        double descontoValor = 0.0;
        double precoFinalUnit = precoUnit;

        if (p.getTipoDesconto() == TipoDesconto.PORCENTAGEM) {
            descontoValor = bruto * (p.getDesconto() / 100.0);
        } else if (p.getTipoDesconto() == TipoDesconto.VALOR) {
            double descontoUnit = Math.min(precoUnit, p.getDesconto());
            descontoValor = descontoUnit * qtd;
        } else if (p.getTipoDesconto() == TipoDesconto.PRECO_FIXO) {
            double precoFixo = Math.max(0.0, p.getDesconto());
            precoFinalUnit = Math.min(precoUnit, precoFixo);
            descontoValor = Math.max(0.0, (precoUnit - precoFinalUnit) * qtd);
        }

        double descontoPercent = (bruto > 0.0) ? (descontoValor / bruto) * 100.0 : 0.0;
        PromocaoAplicada ap = new PromocaoAplicada();
        ap.promocaoId = p.getId();
        ap.promocaoNome = p.getNome();
        ap.tipoDesconto = p.getTipoDesconto();
        ap.descontoValor = descontoValor;
        ap.descontoPercent = Math.min(100.0, Math.max(0.0, descontoPercent));
        ap.precoFinalUnit = precoUnit - (descontoValor / Math.max(1, qtd));
        ap.prioridade = (p.getPrioridade() == null) ? 0 : p.getPrioridade();
        return ap;
    }

    private boolean isClienteVip(String clienteId) {
        if (clienteId == null || clienteId.isBlank())
            return false;
        ClienteModel c = clienteDAO.buscarPorId(clienteId);
        if (c == null)
            return false;
        return vipDAO.isVipPorCpf(c.getCpf());
    }

    private String resolveCategoria(ProdutoModel produto) {
        if (produto == null)
            return null;
        String categoria = produto.getCategoria();
        if (categoria == null || categoria.isBlank()) {
            categoria = produto.getTipo();
        }
        return categoria;
    }
}
