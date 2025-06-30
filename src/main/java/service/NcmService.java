// src/main/java/service/NcmService.java
package service;

import dao.NcmDAO;
import model.NcmModel;

import java.sql.SQLException;
import java.util.List;

/**
 * Facade para operações de NCM.
 */
public class NcmService {
    private static final NcmService INSTANCE = new NcmService();
    private final NcmDAO dao = new NcmDAO();

    private NcmService() {}

    public static NcmService getInstance() {
        return INSTANCE;
    }

    /**
     * Busca todos os NCMs cadastrados.
     */
    public List<NcmModel> findAll() {
        try {
            return dao.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar NCMs", e);
        }
    }

    /**
     * Salva todos os NCMs (modo manual):
     *  - limpa a tabela
     *  - insere cada NCM da lista
     */
    public void saveAll(List<NcmModel> lista) {
        try {
            dao.deleteAll();
            for (NcmModel ncm : lista) {
                dao.insert(ncm);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar NCMs", e);
        }
    }

    /**
     * Operações CRUD independentes, se quiser usar:
     */
    public void insert(NcmModel ncm) {
        try {
            dao.insert(ncm);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir NCM", e);
        }
    }

    public void update(NcmModel ncm) {
        try {
            dao.update(ncm);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar NCM", e);
        }
    }

    public void delete(String codigo) {
        try {
            dao.delete(codigo);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover NCM", e);
        }
    }
}