package service;

import dao.PlanoContaDAO;
import model.PlanoContaModel;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PlanoContaService {

    private final PlanoContaDAO dao = new PlanoContaDAO();

    public List<PlanoContaModel> listarTodos() throws SQLException {
        return dao.listarTodos();
    }

    public PlanoContaModel buscarPorId(String id) throws SQLException {
        return dao.buscarPorId(id);
    }

    /**
     * Salva ou atualiza o Plano de Contas.
     * Se for novo, gera UUID e codigo automático antes de inserir.
     */
    public void salvar(PlanoContaModel p) throws SQLException {
        System.out.println("[SERVICE] Entrou em salvar");
    
        if (p.getId() == null || p.getId().isBlank()) {
            System.out.println("[SERVICE] Criando novo ID e gerando código...");
            p.setId(UUID.randomUUID().toString());
            String novoCodigo = generateNextCodigo(p.getParentId());
            p.setCodigo(novoCodigo);
            dao.inserir(p);
            System.out.println("[SERVICE] Inserção feita no DAO");
        } else {
            System.out.println("[SERVICE] Atualizando existente...");
            dao.atualizar(p);
        }
    }
    

    public void excluir(String id) throws SQLException {
        dao.excluir(id);
    }

    /**
     * Conta quantas contas existem com o mesmo parentId e retorna o próximo índice:
     * Ex.: sem parent => "1","2",...
     *      com parent código "1" => "1.1","1.2",...
     */
    private String generateNextCodigo(String parentId) throws SQLException {
        List<PlanoContaModel> todas = dao.listarTodos();
        int count = 0;
        for (PlanoContaModel pc : todas) {
            boolean sameParent = (parentId == null && pc.getParentId() == null)
                              || (parentId != null && parentId.equals(pc.getParentId()));
            if (sameParent) count++;
        }
        int next = count + 1;
        if (parentId == null) {
            return String.valueOf(next);
        } else {
            PlanoContaModel pai = dao.buscarPorId(parentId);
            String parentCode = (pai != null ? pai.getCodigo() : "0");
            return parentCode + "." + next;
        }
    }
}
