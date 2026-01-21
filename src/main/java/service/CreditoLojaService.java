package service;

import dao.CreditoLojaDAO;
import model.CreditoLojaModel;
import model.CreditoLojaMovimentacaoModel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import util.DB;

/**
 * Service para regras de negócio de crédito de loja:
 * valida saldo, encapsula transação e usa o DAO.
 */
public class CreditoLojaService {

    private final CreditoLojaDAO dao = new CreditoLojaDAO();

    /**
     * Consulta o saldo atual de crédito do cliente.
     * Se não existir registro, retorna 0.
     */
    public double consultarSaldo(String clienteId) {
        try {
            CreditoLojaModel m = dao.getByClienteId(clienteId);
            return (m != null ? m.getValor() : 0.0);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar saldo de crédito", e);
        }
    }

    /**
     * Adiciona crédito ao cliente e registra no histórico.
     */
    public void adicionarCredito(String clienteId, double valor, String referencia) {
        adicionarCredito(clienteId, valor, referencia, null);
    }

    /**
     * Adiciona crédito ao cliente e registra no histórico com vínculo opcional de evento.
     */
    public void adicionarCredito(String clienteId, double valor, String referencia, String eventoId) {
        adicionarCreditoComRetorno(clienteId, valor, referencia, eventoId);
    }

    /**
     * Adiciona crédito ao cliente e retorna o ID da movimentação criada.
     */
    public String adicionarCreditoComRetorno(String clienteId, double valor, String referencia, String eventoId) {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);  // início da transação

            // 1) obtém ou cria registro de crédito
            CreditoLojaModel m = dao.getByClienteId(clienteId);
            if (m == null) {
                m = new CreditoLojaModel(clienteId, valor);
                dao.insertCredito(m);
            } else {
                m.setValor(m.getValor() + valor);
                dao.updateCredito(m);
            }

            // 2) registra movimentação
            CreditoLojaMovimentacaoModel mov = new CreditoLojaMovimentacaoModel(
                    clienteId, valor, "entrada", referencia);
            mov.setEventoId(eventoId);
            dao.insertMovimentacao(mov);

            conn.commit();  // confirma tudo
            return mov.getId();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar crédito", e);
        }
    }

    /**
     * Usa crédito do cliente (debita), validando saldo e registrando histórico.
     * Lança IllegalArgumentException se saldo insuficiente.
     */
    public void usarCredito(String clienteId, double valor, String referencia) {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            // 1) checa saldo
            CreditoLojaModel m = dao.getByClienteId(clienteId);
            double saldo = (m != null ? m.getValor() : 0.0);
            if (saldo < valor) {
                throw new IllegalArgumentException("Saldo insuficiente para usar crédito: "
                        + saldo + " < " + valor);
            }

            // 2) atualiza saldo
            m.setValor(saldo - valor);
            dao.updateCredito(m);

            // 3) registra movimentação de uso (valor negativo ou positivo, conforme sua convenção)
            CreditoLojaMovimentacaoModel mov = new CreditoLojaMovimentacaoModel(
                    clienteId, -valor, "uso", referencia);
            dao.insertMovimentacao(mov);

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao usar crédito", e);
        }
    }

    /**
     * Lista o histórico de movimentações de crédito do cliente.
     */
    public List<CreditoLojaMovimentacaoModel> listarMovimentacoes(String clienteId) {
        try {
            return dao.getMovimentacoes(clienteId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar movimentações de crédito", e);
        }
    }
}
