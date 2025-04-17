package factory;

import model.VendaModel;
import util.DateUtils;

public class VendaFactory {

    /**
     * Cria um VendaModel com total bruto, desconto em R$, total líquido e status "fechada".
     */
    public static VendaModel criarVenda(
            String clienteId,
            double totalBruto,
            double desconto,
            String formaPagamento,
            int parcelas
    ) {
        double totalLiquido = totalBruto - desconto;
        String dataVenda = DateUtils.now();
        return new VendaModel(
            dataVenda,
            clienteId,
            totalBruto,
            desconto,
            totalLiquido,
            formaPagamento,
            parcelas,
            "fechada"
        );
    }
}
