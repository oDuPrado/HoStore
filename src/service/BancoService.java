package service;

import dao.BancoDAO;
import model.BancoModel;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class BancoService {

    private final BancoDAO dao = new BancoDAO();

    public List<BancoModel> listarTodos() throws SQLException {
        return dao.listarTodos();
    }

    public void criar(String nome, String agencia, String conta, String observacoes) throws SQLException {
        String id = UUID.randomUUID().toString();
        dao.inserir(new BancoModel(id, nome, agencia, conta, observacoes));
    }

    public void atualizar(String id, String nome, String agencia, String conta, String observacoes) throws SQLException {
        dao.atualizar(new BancoModel(id, nome, agencia, conta, observacoes));
    }

    public void excluir(String id) throws SQLException {
        dao.excluir(id);
    }

    public BancoModel buscarPorId(String id) throws SQLException {
        return dao.buscarPorId(id);
    }
}
