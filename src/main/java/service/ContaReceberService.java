package service;

import dao.*;
import model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @CR Service: regras de negócio das contas a receber.
 *
 * 👉 Responsabilidades principais
 * 1. Criar título + parcelas (manual ou a partir de venda)
 * 2. Registrar pagamento de parcela
 * 3. Atualizar status de parcela e título
 */
public class ContaReceberService {

    private final TituloContaReceberDAO tituloDAO = new TituloContaReceberDAO();
    private final ParcelaContaReceberDAO parcelaDAO = new ParcelaContaReceberDAO();
    private final PagamentoContaReceberDAO pagamentoDAO = new PagamentoContaReceberDAO();

    /* ─────────────────────────────────────────────────────────────── */
    /* 1. Criação de título + parcelas                                */
    /* ─────────────────────────────────────────────────────────────── */

    /**
     * Gera um novo título em aberto e N parcelas iguais.
     *
     * @param clienteId  FK do cliente
     * @param total      Valor total
     * @param numParcelas Número de parcelas
     * @param primeiroVenc  yyyy-MM-dd do 1º vencimento
     * @param intervaloDias intervalo entre parcelas
     */
    public String criarTituloParcelado(
            String clienteId, double total, int numParcelas,
            String primeiroVenc, int intervaloDias,
            String obs) throws SQLException {

        /* Validação: numParcelas deve ser > 0 */
        if (numParcelas <= 0) {
            throw new IllegalArgumentException("Número de parcelas deve ser maior que zero");
        }

        /* Cria título */
        TituloContaReceberModel titulo = new TituloContaReceberModel();
        titulo.setClienteId(clienteId);
        titulo.setDataGeracao(hojeISO());
        titulo.setValorTotal(total);
        titulo.setObservacoes(obs);
        titulo.setCodigoSelecao(obs);
        tituloDAO.inserir(titulo);

        /* Gera parcelas */
        double parcelaValor = arredondar(total / numParcelas);
        LocalDate venc = LocalDate.parse(primeiroVenc);
        for (int n = 1; n <= numParcelas; n++) {
            ParcelaContaReceberModel p = new ParcelaContaReceberModel();
            p.setTituloId(titulo.getId());
            p.setNumeroParcela(n);
            p.setVencimento(venc.toString());
            p.setValorNominal(parcelaValor);
            parcelaDAO.inserir(p);
            venc = venc.plusDays(intervaloDias);
        }
        return titulo.getId();
    }

    /* ─────────────────────────────────────────────────────────────── */
    /* 2. Registrar pagamento de parcela                               */
    /* ─────────────────────────────────────────────────────────────── */

    /**
     * Registra um pagamento numa parcela e cuida do status.
     */
    public void registrarPagamento(
            int parcelaId, double valorPago, String forma) throws SQLException {

        ParcelaContaReceberModel parcela = parcelaDAO.buscarPorId(parcelaId);
        if (parcela == null) throw new IllegalArgumentException("Parcela não encontrada");

        /* Salva pagamento granular */
        PagamentoContaReceberModel pg = new PagamentoContaReceberModel();
        pg.setParcelaId(parcelaId);
        pg.setValorPago(valorPago);
        pg.setFormaPagamento(forma);
        pg.setDataPagamento(hojeISO());
        pagamentoDAO.inserir(pg);

        /* Atualiza parcela */
        parcela.setValorPago(parcela.getValorPago() + valorPago);
        double totalDevido = parcela.getValorNominal() + parcela.getValorJuros()
                + parcela.getValorAcrescimo() - parcela.getValorDesconto();
        // ✅ Tolerância de R$ 0,01 (não R$ 0,009) para evitar centavos pendentes
        if (parcela.getValorPago() >= totalDevido - 0.01) {
            parcela.setStatus("pago");
            parcela.setDataPagamento(hojeISO());
        }
        parcelaDAO.atualizar(parcela);

        /* Atualiza título */
        atualizarStatusTitulo(parcela.getTituloId());
    }

    /* ─────────────────────────────────────────────────────────────── */
    /* 3. Consistência de status                                       */
    /* ─────────────────────────────────────────────────────────────── */

    /**
     * Percorre parcelas de um título e define:
     * - quitado  → todas pagas
     * - vencido  → ao menos 1 vencida e aberta
     * - aberto   → caso contrário
     */
    private void atualizarStatusTitulo(String tituloId) throws SQLException {
        List<ParcelaContaReceberModel> parcelas = parcelaDAO.listarPorTitulo(tituloId);
        boolean allPaid = parcelas.stream().allMatch(p -> "pago".equals(p.getStatus()));
        boolean anyOverdue = parcelas.stream().anyMatch(p ->
                "aberto".equals(p.getStatus()) &&
                LocalDate.parse(p.getVencimento()).isBefore(LocalDate.now()));

        TituloContaReceberModel titulo = tituloDAO.buscarPorId(tituloId);
        if (allPaid)       titulo.setStatus("quitado");
        else if (anyOverdue) titulo.setStatus("vencido");
        else               titulo.setStatus("aberto");
        tituloDAO.atualizar(titulo);
    }

    /* ─────────────────────────────────────────────────────────────── */
    /* Utilidades internas                                             */
    /* ─────────────────────────────────────────────────────────────── */
    private static String hojeISO() {
        return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }

    private static double arredondar(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /**
 * Retorna o ID da primeira parcela vinculada ao título.
 * Útil para marcar como paga em vendas à vista (DINHEIRO).
 */
public int getPrimeiraParcelaId(String tituloId) throws SQLException {
    List<ParcelaContaReceberModel> parcelas = parcelaDAO.listarPorTitulo(tituloId);
    if (parcelas == null || parcelas.isEmpty()) {
        throw new SQLException("Nenhuma parcela encontrada para o título: " + tituloId);
    }
    return parcelas.get(0).getId();
}

}
