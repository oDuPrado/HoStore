package service;

import dao.TaxaCartaoDAO;
import model.TaxaCartaoModel;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negócio para configurações de taxas de cartão.
 */
public class TaxaCartaoService {
    private final TaxaCartaoDAO dao = new TaxaCartaoDAO();

    /** Salva ou atualiza uma configuração de taxa. */
    public void salvar(TaxaCartaoModel m) throws SQLException {
        dao.save(m);
    }

    /** Exclui uma configuração pelo id. */
    public void excluir(int id) throws SQLException {
        dao.delete(id);
    }

    /** Lista todas as configurações de taxas cadastradas. */
    public List<TaxaCartaoModel> listar() throws SQLException {
        return dao.findAll();
    }

    /**
     * Busca a taxa aplicável para a combinação:
     * bandeira, tipo de pagamento, número de parcelas e mês de referência.
     */
    public Optional<Double> buscarTaxa(String bandeira, String tipo,
            int parcelas, YearMonth mesRef) throws SQLException {
        return dao.buscarTaxa(bandeira, tipo, parcelas, mesRef);
    }
}
