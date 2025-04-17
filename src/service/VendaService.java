package service;

import dao.VendaDAO;
import dao.VendaItemDAO;
import model.VendaModel;
import model.VendaItemModel;
import util.DB;
import util.LogService;
import util.PDFGenerator;

import java.sql.Connection;
import java.util.List;

public class VendaService {
    private EstoqueService estoqueService = new EstoqueService();
    private VendaDAO vendaDAO = new VendaDAO();
    private VendaItemDAO itemDAO = new VendaItemDAO();

    /**
     * Finaliza a venda em uma transação única:
     *   – valida estoque
     *   – insere venda
     *   – insere itens + baixa estoque
     *   – faz commit
     *   – gera PDF
     */
    public int finalizarVenda(VendaModel venda, List<VendaItemModel> itens) throws Exception {
        if (itens.isEmpty()) {
            throw new Exception("Carrinho vazio");
        }

        int vendaId;
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            // 1) valida estoques
            for (VendaItemModel it : itens) {
                if (!estoqueService.possuiEstoque(c, it.getCartaId(), it.getQtd())) {
                    throw new Exception("Estoque insuficiente para " + it.getCartaId());
                }
            }

            // 2) insere venda (agora com o método que aceita Connection)
            vendaId = vendaDAO.insert(venda, c);

            // 3) insere itens + baixa estoque
            for (VendaItemModel it : itens) {
                itemDAO.insert(it, vendaId, c);
                estoqueService.baixarEstoque(c, it.getCartaId(), it.getQtd());
            }

            // 4) commit
            c.commit();
            LogService.info("Venda " + vendaId + " finalizada com sucesso");

        } catch (Exception ex) {
            throw new Exception("Erro ao finalizar venda: " + ex.getMessage(), ex);
        }

        // 5) só depois do commit geramos o PDF para não travar o DB
        PDFGenerator.gerarComprovanteVenda(venda, itens);

        return vendaId;
    }
}
