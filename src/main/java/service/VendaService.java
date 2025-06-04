// src/service/VendaService.java
package service;

import dao.VendaDAO;
import dao.VendaItemDAO;
import model.VendaItemModel;
import model.VendaModel;
import util.DB;
import util.LogService;
import util.PDFGenerator;

import java.sql.Connection;
import java.util.List;

import dao.VendaDevolucaoDAO;
import model.VendaDevolucaoModel;

import service.ContaReceberService;

/**
 * Serviço que garante transação única na finalização da venda.
 * Suporta produtos genéricos (não apenas cartas).
 */
public class VendaService {

    private final EstoqueService estoqueService = new EstoqueService();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final VendaItemDAO itemDAO = new VendaItemDAO();
    private final ContaReceberService contaReceberService = new ContaReceberService(); // <— ADICIONADO

    /**
     * Finaliza a venda:
     * - valida estoque
     * - grava cabeçalho, itens e baixa estoque
     * - gera PDF
     * - cria título e parcelas em contas a receber
     * Retorna o ID da venda.
     */
    public int finalizarVenda(VendaModel venda, List<VendaItemModel> itens) throws Exception {

        if (itens == null || itens.isEmpty()) {
            throw new Exception("Carrinho vazio");
        }

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            // 1) valida estoque
            for (VendaItemModel it : itens) {
                String produtoId = it.getProdutoId(); // <--- AGORA genérico
                if (!estoqueService.possuiEstoque(c, produtoId, it.getQtd())) {
                    throw new Exception("Estoque insuficiente para o produto " + produtoId);
                }
            }

            // 2) grava venda
            int vendaId = vendaDAO.insert(venda, c);

            // 3) grava itens, baixa estoque e registra movimentação
            ProdutoEstoqueService produtoEstoqueService = new ProdutoEstoqueService();

            for (VendaItemModel it : itens) {
                itemDAO.insert(it, vendaId, c);


                // registra a movimentação no histórico
                produtoEstoqueService.registrarSaida(
                        it.getProdutoId(),
                        it.getQtd(),
                        "Venda #" + vendaId,
                        venda.getUsuario() != null ? venda.getUsuario() : "sistema",
                        c);
            }

            // 4) commit
            c.commit();
            LogService.info("Venda " + vendaId + " finalizada com sucesso");

            // =============================
            // @INTEGRACAO: Contas a Receber
            // =============================
            try {
                String tituloId = null;

                // Se for DINHEIRO, cria título à vista e já registra pagamento
                if ("DINHEIRO".equalsIgnoreCase(venda.getFormaPagamento())) {
                    String hoje = java.time.LocalDate.now().toString();
                    tituloId = contaReceberService.criarTituloParcelado(
                            venda.getClienteId(),
                            venda.getTotalLiquido(),
                            1, // parcela única
                            hoje,
                            30, // intervalo irrelevante aqui
                            "venda-" + vendaId); // <- ESSA É A MUDANÇA

                    int parcelaId = contaReceberService.getPrimeiraParcelaId(tituloId);
                    contaReceberService.registrarPagamento(parcelaId, venda.getTotalLiquido(), "DINHEIRO");

                    // Para outras formas de pagamento: gera se dados estiverem preenchidos
                } else if (venda.getDataPrimeiroVencimento() != null &&
                        venda.getParcelas() > 0 &&
                        venda.getIntervaloDias() > 0) {
                    tituloId = contaReceberService.criarTituloParcelado(
                            venda.getClienteId(),
                            venda.getTotalLiquido(),
                            venda.getParcelas(),
                            venda.getDataPrimeiroVencimento(),
                            venda.getIntervaloDias(),
                            "venda-" + vendaId); // <- ESSA É A MUDANÇA
                } else {
                    LogService.info("Forma de pagamento parcelada mas dados incompletos — título não criado.");
                }

            } catch (Exception ex) {
                LogService.error("Venda " + vendaId + " finalizada, mas erro ao gerar contas a receber", ex);
            }

            // =============================

            // 5) PDF
            venda.setItens(itens); // para o gerador ter acesso
            PDFGenerator.gerarComprovanteVenda(venda, itens);

            return vendaId;

        } catch (Exception ex) {
            LogService.error("Erro ao finalizar venda", ex);
            throw ex;
        }
    }

    private final VendaDevolucaoDAO devolucaoDAO = new VendaDevolucaoDAO();

    /**
     * Registra uma devolução:
     * - grava na tabela de devoluções
     * - devolve ao estoque
     */
    public void registrarDevolucao(VendaDevolucaoModel devolucao) throws Exception {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            // 1) grava devolução no banco
            devolucaoDAO.inserir(devolucao);

            // 2) devolve ao estoque
            estoqueService.entrarEstoque(c, devolucao.getProdutoId(), devolucao.getQuantidade());

            // 3) registra movimentação de entrada
            new ProdutoEstoqueService().registrarEntrada(
                    devolucao.getProdutoId(),
                    devolucao.getQuantidade(),
                    "Devolução da venda #" + devolucao.getVendaId(),
                    "sistema", // ou devolucao.getUsuario() se implementar
                    c);

            // 4) commit
            c.commit();
            LogService.info("Devolução registrada para venda " + devolucao.getVendaId());
        }

    }

}
