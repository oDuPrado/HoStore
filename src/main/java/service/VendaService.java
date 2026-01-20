package service;

import dao.VendaDAO;
import dao.VendaItemDAO;
import dao.VendaItemLoteDAO;
import dao.VendaDevolucaoDAO;
import dao.VendaDevolucaoLoteDAO;

import model.VendaItemModel;
import model.VendaModel;
import model.VendaDevolucaoModel;

import util.DB;
import util.LogService;
import util.PDFGenerator;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Serviço transacional:
 * - Finaliza venda (itens + movimentação de saída)
 * - Registra devolução (devolução + movimentação de entrada)
 *
 * Regra: Estoque/movimentação tem UM dono: ProdutoEstoqueService.
 */
public class VendaService {

    private final EstoqueService estoqueService = new EstoqueService(); // usado para validar estoque antes
    private final VendaDAO vendaDAO = new VendaDAO();
    private final VendaItemDAO itemDAO = new VendaItemDAO();
    private final VendaDevolucaoDAO devolucaoDAO = new VendaDevolucaoDAO();
    private final ContaReceberService contaReceberService = new ContaReceberService();

    public int finalizarVenda(VendaModel venda, List<VendaItemModel> itens) throws Exception {

        if (itens == null || itens.isEmpty()) {
            throw new Exception("Carrinho vazio");
        }

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            int vendaId;

            try {
                String fpRaw = venda.getFormaPagamento();
                String fp = (fpRaw == null) ? "" : fpRaw.trim().toUpperCase();

                LogService.info("FinalizarVenda: formaPagamento(raw)='" + fpRaw + "' norm='" + fp + "'");
                LogService.info("FinalizarVenda: clienteId=" + venda.getClienteId()
                        + " totalLiquido=" + venda.getTotalLiquido()
                        + " parcelas=" + venda.getParcelas()
                        + " venc=" + venda.getDataPrimeiroVencimento()
                        + " intervaloDias=" + venda.getIntervaloDias());

                // 1) valida estoque
                for (VendaItemModel it : itens) {
                    String produtoId = it.getProdutoId();
                    if (!estoqueService.possuiEstoque(c, produtoId, it.getQtd())) {
                        throw new Exception("Estoque insuficiente para o produto " + produtoId);
                    }
                }

                // 2) grava venda
                vendaId = vendaDAO.insert(venda, c);

                // 3) grava itens + registra saida por lote (FIFO)
                ProdutoEstoqueService produtoEstoqueService = new ProdutoEstoqueService();
                VendaItemLoteDAO itemLoteDAO = new VendaItemLoteDAO();

                for (VendaItemModel it : itens) {
                    int vendaItemId = itemDAO.insert(it, vendaId, c);

                    List<ProdutoEstoqueService.LoteConsumo> consumos = produtoEstoqueService.consumirFIFO(
                            it.getProdutoId(),
                            it.getQtd(),
                            "Venda #" + vendaId,
                            (venda.getUsuario() != null && !venda.getUsuario().isBlank()) ? venda.getUsuario()
                                    : "sistema",
                            c);

                    for (ProdutoEstoqueService.LoteConsumo consumo : consumos) {
                        itemLoteDAO.inserirConsumo(
                                vendaItemId,
                                consumo.loteId,
                                consumo.qtdConsumida,
                                consumo.custoUnit,
                                c);
                    }

                    produtoEstoqueService.atualizarQuantidadeCache(it.getProdutoId(), c);
                }

                c.commit();
                LogService.info("Venda " + vendaId + " finalizada com sucesso");

                // Fora da transação: contas a receber
                gerarContasReceberSafe(venda, vendaId);

                // Fora da transação: PDF
                try {
                    venda.setItens(itens);
                    PDFGenerator.gerarComprovanteVenda(venda, itens);
                } catch (Exception pdfEx) {
                    LogService.error("Venda " + vendaId + " finalizada, mas erro ao gerar PDF", pdfEx);
                }

                return vendaId;

            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (Exception rbEx) {
                    LogService.error("Falha ao dar rollback na venda", rbEx);
                }
                LogService.error("Erro ao finalizar venda", e);
                throw e;
            }
        }
    }

    private void gerarContasReceberSafe(VendaModel venda, int vendaId) {
        try {
            String fpRaw = venda.getFormaPagamento();
            String fp = (fpRaw == null) ? "" : fpRaw.trim().toUpperCase();

            if ("DINHEIRO".equals(fp)) {
                String hoje = LocalDate.now().toString();

                String tituloId = contaReceberService.criarTituloParcelado(
                        venda.getClienteId(),
                        venda.getTotalLiquido(),
                        1,
                        hoje,
                        0,
                        "venda-" + vendaId);

                int parcelaId = contaReceberService.getPrimeiraParcelaId(tituloId);
                contaReceberService.registrarPagamento(parcelaId, venda.getTotalLiquido(), "DINHEIRO");

            } else if (venda.getDataPrimeiroVencimento() != null &&
                    venda.getParcelas() > 0 &&
                    venda.getIntervaloDias() > 0) {

                contaReceberService.criarTituloParcelado(
                        venda.getClienteId(),
                        venda.getTotalLiquido(),
                        venda.getParcelas(),
                        venda.getDataPrimeiroVencimento(),
                        venda.getIntervaloDias(),
                        "venda-" + vendaId);

            } else {
                LogService.info("Contas a receber não gerado: formaPagamento='" + fpRaw
                        + "' (norm='" + fp + "') e dados de parcelamento incompletos.");
            }

        } catch (Exception ex) {
            LogService.error("Venda " + vendaId + " finalizada, mas erro ao gerar contas a receber", ex);
        }
    }

    /**
     * Registra devolução de forma transacional:
     * 1) grava devolução
     * 2) registra entrada (estoque + histórico) via ProdutoEstoqueService
     */
    public void registrarDevolucao(VendaDevolucaoModel devolucao) throws Exception {
        if (devolucao == null)
            throw new Exception("Devolucao nula");

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            try {
                String usuario = (devolucao.getUsuario() != null && !devolucao.getUsuario().isBlank())
                        ? devolucao.getUsuario()
                        : "sistema";

                int devolucaoId = devolucaoDAO.inserir(devolucao, c);

                ProdutoEstoqueService produtoEstoqueService = new ProdutoEstoqueService();
                VendaItemLoteDAO itemLoteDAO = new VendaItemLoteDAO();
                VendaDevolucaoLoteDAO devolucaoLoteDAO = new VendaDevolucaoLoteDAO();

                int restante = devolucao.getQuantidade();
                List<VendaItemLoteDAO.ConsumoLote> consumos = itemLoteDAO.listarConsumoPorLote(
                        devolucao.getVendaId(),
                        devolucao.getProdutoId(),
                        c);

                for (VendaItemLoteDAO.ConsumoLote consumo : consumos) {
                    if (restante <= 0)
                        break;

                    int devolvido = itemLoteDAO.somarDevolvidoNoLote(
                            devolucao.getVendaId(),
                            devolucao.getProdutoId(),
                            consumo.loteId,
                            c);
                    int disponivel = consumo.qtdConsumida - devolvido;
                    if (disponivel <= 0)
                        continue;

                    int qtd = Math.min(restante, disponivel);
                    produtoEstoqueService.reporNoLote(
                            devolucao.getProdutoId(),
                            consumo.loteId,
                            qtd,
                            "Devolucao da venda #" + devolucao.getVendaId(),
                            usuario,
                            c);

                    devolucaoLoteDAO.inserir(devolucaoId, consumo.loteId, qtd, consumo.custoUnit, c);
                    restante -= qtd;
                }

                if (restante > 0) {
                    throw new Exception("Quantidade devolvida excede o consumido nos lotes");
                }

                c.commit();
                LogService.info("Devolucao registrada para venda " + devolucao.getVendaId());

            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (Exception rbEx) {
                    LogService.error("Falha ao dar rollback na devolucao", rbEx);
                }
                LogService.error("Erro ao registrar devolucao", e);
                throw e;
            }
        }
    }
}
