
package factory;
import model.*;
import java.util.List;

public class VendaFactory {
    public static VendaModel criarVenda(String clienteId,double total,double desconto,String forma,int parcelas){
        return new VendaModel(java.time.LocalDateTime.now().toString(), clienteId,total,desconto,forma,parcelas,"fechada");
    }
}
