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

/**
 * Serviço que garante transação única na finalização da venda.
 * Suporta produtos genéricos (não apenas cartas).
 */
public class VendaService {

    private final EstoqueService estoqueService = new EstoqueService();
    private final VendaDAO       vendaDAO       = new VendaDAO();
    private final VendaItemDAO   itemDAO        = new VendaItemDAO();

    /**
     * Finaliza a venda:
     *  - valida estoque
     *  - grava cabeçalho, itens e baixa estoque
     *  - gera PDF
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

            // 3) grava itens e baixa estoque
            for (VendaItemModel it : itens) {
                itemDAO.insert(it, vendaId, c);
                estoqueService.baixarEstoque(c, it.getProdutoId(), it.getQtd());
            }

            // 4) commit
            c.commit();
            LogService.info("Venda " + vendaId + " finalizada com sucesso");

            // 5) PDF
            venda.setItens(itens);      // para o gerador ter acesso
            PDFGenerator.gerarComprovanteVenda(venda, itens);

            return vendaId;

        } catch (Exception ex) {
            LogService.error("Erro ao finalizar venda", ex);
            throw ex;
        }
    }
}
