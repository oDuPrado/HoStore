
package controller;
import service.VendaService;
import model.*;
import java.util.*;

public class VendaController {
    private VendaService vendaService = new VendaService();
    private List<VendaItemModel> carrinho = new ArrayList<>();

    public void adicionarItem(VendaItemModel it){ carrinho.add(it); }
    public void limparCarrinho(){ carrinho.clear(); }
    public int finalizar(String clienteId,String forma,int parcelas) throws Exception{
        double total = carrinho.stream().mapToDouble(i->i.getPreco()*i.getQtd()).sum();
        VendaModel venda = factory.VendaFactory.criarVenda(clienteId,total,0,forma,parcelas);
        return vendaService.finalizarVenda(venda,carrinho);
    }
    public List<VendaItemModel> getCarrinho(){return carrinho;}
}
