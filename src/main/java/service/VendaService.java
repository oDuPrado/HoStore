package service;

import dao.VendaDAO;
import dao.VendaItemDAO;
import dao.VendaItemLoteDAO;
import dao.VendaDevolucaoDAO;
import dao.VendaDevolucaoLoteDAO;
import dao.ProdutoDAO;
import dao.ConfigNfceDAO;

import model.VendaItemModel;
import model.VendaModel;
import model.VendaDevolucaoModel;
import model.ProdutoModel;
import model.ConfigNfceModel;
import model.DocumentoFiscalAmbiente;
import model.DocumentoFiscalModel;

import util.DB;
import util.LogService;
import util.PDFGenerator;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    public int finalizarVenda(VendaModel venda, List<VendaItemModel> itens) throws Exception {

        if (itens == null || itens.isEmpty()) {
            throw new Exception("Carrinho vazio");
        }

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            int vendaId;

            try {
                LogService.audit("VENDA_INICIO", "venda", null, "itens=" + itens.size());
                String fpRaw = venda.getFormaPagamento();
                String fp = (fpRaw == null) ? "" : fpRaw.trim().toUpperCase();

                LogService.info("FinalizarVenda: formaPagamento(raw)='" + fpRaw + "' norm='" + fp + "'");
                LogService.info("FinalizarVenda: clienteId=" + venda.getClienteId()
                        + " totalLiquido=" + venda.getTotalLiquido()
                        + " parcelas=" + venda.getParcelas()
                        + " venc=" + venda.getDataPrimeiroVencimento()
                        + " intervaloDias=" + venda.getIntervaloDias());

                // 1) valida estoque (ignora itens de serviço / não-estoque)
                Map<String, Boolean> naoEstoque = new HashMap<>();
                for (VendaItemModel it : itens) {
                    String produtoId = it.getProdutoId();
                    ProdutoModel produto = produtoDAO.findById(produtoId, c, true);
                    boolean isNaoEstoque = ProdutoEstoqueService.isNaoEstoque(produto);
                    naoEstoque.put(produtoId, isNaoEstoque);
                    if (!isNaoEstoque && !estoqueService.possuiEstoque(c, produtoId, it.getQtd())) {
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

                    if (!naoEstoque.getOrDefault(it.getProdutoId(), false)) {
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
                }

                c.commit();
                LogService.audit("VENDA_OK", "venda", String.valueOf(vendaId), "finalizada");

                // Fora da transação: contas a receber
                gerarContasReceberSafe(venda, vendaId);

                // ===== ETAPA 15: INTEGRAÇÃO NFC-e =====
                // Criar documento fiscal automaticamente (background job processará)
                try {
                    DocumentoFiscalService docFiscalService = new DocumentoFiscalService();
                    String usuario = (venda.getUsuario() != null && !venda.getUsuario().isBlank()) 
                        ? venda.getUsuario() : "sistema";
                    
                    ConfigNfceModel cfg = null;
                    try {
                        cfg = new ConfigNfceDAO().getConfig();
                    } catch (Exception ignored) {
                        // mantém fallback abaixo
                    }

                    if (cfg != null && cfg.getEmitirNfce() == 0) {
                        LogService.info("NFC-e desativada na configuração (venda " + vendaId + ")");
                    } else {
                        String ambiente = cfg != null ? cfg.getAmbiente() : DocumentoFiscalAmbiente.OFF;
                        boolean ok = false;
                        for (int attempt = 1; attempt <= 3 && !ok; attempt++) {
                            try {
                                DocumentoFiscalModel doc = docFiscalService.criarDocumentoPendenteParaVenda(vendaId, usuario, ambiente);
                                try {
                                    docFiscalService.gerarXml(doc.id);
                                } catch (Exception genEx) {
                                    LogService.warn("NFC-e criada, mas falha ao gerar XML offline: " + genEx.getMessage());
                                }
                                LogService.info("NFC-e criada para venda " + vendaId + " (ambiente=" + ambiente + ")");
                                ok = true;
                            } catch (Exception nfceEx) {
                                String msg = nfceEx.getMessage() != null ? nfceEx.getMessage().toLowerCase() : "";
                                boolean busy = msg.contains("sqlite_busy_snapshot") || msg.contains("database is locked");
                                if (!busy || attempt == 3) {
                                    throw nfceEx;
                                }
                                try {
                                    Thread.sleep(150L * attempt);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                    
                } catch (Exception nfceEx) {
                    // NFC-e falha não bloqueia venda
                    LogService.warn("Venda " + vendaId + " finalizada, mas erro ao criar NFC-e: " + nfceEx.getMessage());
                }

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
                LogService.auditError("VENDA_ERRO", "venda", null, "falha ao finalizar", e);
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
                LogService.audit("DEVOLUCAO_INICIO", "venda", String.valueOf(devolucao.getVendaId()),
                        "produto=" + devolucao.getProdutoId() + " qtd=" + devolucao.getQuantidade());
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
                LogService.audit("DEVOLUCAO_OK", "venda", String.valueOf(devolucao.getVendaId()), "devolucao registrada");

            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (Exception rbEx) {
                    LogService.error("Falha ao dar rollback na devolucao", rbEx);
                }
                LogService.auditError("DEVOLUCAO_ERRO", "venda", String.valueOf(devolucao.getVendaId()), "falha ao registrar devolucao", e);
                throw e;
            }
        }
    }
}
