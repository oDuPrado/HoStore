package service;

import model.VendaDevolucaoModel;

/**
 * Service responsavel por registrar devolucoes de venda.
 * Encaminha para VendaService, que executa o fluxo transacional por lote.
 */
public class VendaDevolucaoService {

    public void registrarDevolucao(VendaDevolucaoModel dev) throws Exception {
        new VendaService().registrarDevolucao(dev);
    }
}
