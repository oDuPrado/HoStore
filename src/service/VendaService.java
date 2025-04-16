
package service;
import dao.*;
import model.*;
import util.*;
import java.sql.SQLException;
import java.util.*;

public class VendaService {
    private EstoqueService estoqueService = new EstoqueService();
    private VendaDAO vendaDAO = new VendaDAO();
    private VendaItemDAO itemDAO = new VendaItemDAO();

    public int finalizarVenda(VendaModel venda,List<VendaItemModel> itens) throws Exception{
        // regra: validar itens
        if(itens.isEmpty()) throw new Exception("Carrinho vazio");
        for(VendaItemModel it: itens){
            if(!estoqueService.possuiEstoque(it.getCartaId(),it.getQtd()))
                throw new Exception("Estoque insuficiente para "+it.getCartaId());
        }
        // salva venda
        int id = vendaDAO.insert(venda);
        // salva itens + baixa estoque
        for(VendaItemModel it: itens){
            itemDAO.insert(it,id);
            estoqueService.baixarEstoque(it.getCartaId(),it.getQtd());
        }
        LogService.info("Venda "+id+" finalizada");
        PDFGenerator.gerarComprovanteVenda(venda,itens);
        return id;
    }
}
