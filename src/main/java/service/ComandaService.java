package service;

import dao.ComandaDAO;
import dao.ComandaItemDAO;
import dao.ComandaPagamentoDAO;
import model.ComandaItemModel;
import model.ComandaModel;
import model.ComandaPagamentoModel;
import model.ComandaResumoModel;
import util.DB;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class ComandaService {

    private final ComandaDAO comandaDAO = new ComandaDAO();
    private final ComandaItemDAO itemDAO = new ComandaItemDAO();
    private final ComandaPagamentoDAO pagamentoDAO = new ComandaPagamentoDAO();

    // Estoque
    private final ProdutoEstoqueService estoqueService = new ProdutoEstoqueService();

    public int abrirComanda(String clienteId, String nomeCliente, String mesa, String obs, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = new ComandaModel();
            c.setClienteId((clienteId == null || clienteId.isBlank()) ? null : clienteId);
            c.setNomeCliente((nomeCliente == null || nomeCliente.isBlank()) ? null : nomeCliente);
            c.setMesa((mesa == null || mesa.isBlank()) ? null : mesa);
            c.setStatus("aberta");
            c.setObservacoes(obs);
            c.setCriadoEm(LocalDateTime.now());
            c.setCriadoPor(usuario);

            int id = comandaDAO.inserir(c, conn);

            conn.commit();
            return id;
        }
    }

    public ComandaModel getComanda(int id) throws Exception {
        return comandaDAO.buscarPorId(id);
    }

    public List<ComandaResumoModel> listarResumo(String status) throws Exception {
        return comandaDAO.listarResumo(status);
    }

    public List<ComandaItemModel> listarItens(int comandaId, Connection conn) throws Exception {
        return itemDAO.listarPorComanda(comandaId, conn);
    }

    public List<ComandaPagamentoModel> listarPagamentos(int comandaId, Connection conn) throws Exception {
        return pagamentoDAO.listarPorComanda(comandaId, conn);
    }

    public void adicionarItem(int comandaId, String produtoId, int qtd, double preco, double desconto, double acrescimo,
                              String obs, String usuario) throws Exception {

        if (qtd <= 0) throw new Exception("Quantidade inválida.");
        if (preco < 0) throw new Exception("Preço inválido.");
        if (desconto < 0) desconto = 0;
        if (acrescimo < 0) acrescimo = 0;

        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null) throw new Exception("Comanda não encontrada.");
            if (c.getStatus() == null) c.setStatus("aberta");

            if (c.getStatus().equalsIgnoreCase("fechada") || c.getStatus().equalsIgnoreCase("cancelada")) {
                throw new Exception("Comanda não permite alterações (status: " + c.getStatus() + ").");
            }

            ComandaItemModel it = new ComandaItemModel();
            it.setComandaId(comandaId);
            it.setProdutoId(produtoId);
            it.setQtd(qtd);
            it.setPreco(preco);
            it.setDesconto(desconto);
            it.setAcrescimo(acrescimo);
            it.setObservacoes(obs);
            it.setCriadoEm(LocalDateTime.now());
            it.setCriadoPor(usuario);
            it.recalcularTotal();

            itemDAO.inserir(it, conn);

            // Estoque: baixa imediatamente
            estoqueService.registrarSaida(produtoId, qtd, "Comanda #" + comandaId, usuario, conn);

            // Recalcula e persiste totais
            recomputarTotaisEAtualizar(comandaId, conn);

            conn.commit();
        }
    }

    public void removerItem(int itemId, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaItemModel it = itemDAO.buscarPorId(itemId, conn);
            if (it == null) throw new Exception("Item não encontrado.");

            ComandaModel c = comandaDAO.buscarPorId(it.getComandaId(), conn);
            if (c == null) throw new Exception("Comanda não encontrada.");

            if (c.getStatus().equalsIgnoreCase("fechada") || c.getStatus().equalsIgnoreCase("cancelada")) {
                throw new Exception("Comanda não permite alterações (status: " + c.getStatus() + ").");
            }

            itemDAO.deletar(itemId, conn);

            // Estoque: devolve imediatamente
            estoqueService.registrarEntrada(it.getProdutoId(), it.getQtd(), "Remoção item Comanda #" + it.getComandaId(), usuario, conn);

            recomputarTotaisEAtualizar(it.getComandaId(), conn);

            conn.commit();
        }
    }

    public void registrarPagamento(int comandaId, String tipo, double valor, String usuario) throws Exception {
        if (valor <= 0) throw new Exception("Valor inválido.");

        if (tipo == null || tipo.isBlank()) tipo = "OUTRO";
        tipo = tipo.trim().toUpperCase();

        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null) throw new Exception("Comanda não encontrada.");
            if (c.getStatus().equalsIgnoreCase("cancelada")) throw new Exception("Comanda cancelada não recebe pagamento.");

            ComandaPagamentoModel pg = new ComandaPagamentoModel();
            pg.setComandaId(comandaId);
            pg.setTipo(tipo);
            pg.setValor(valor);
            pg.setUsuario(usuario);
            pg.setData(LocalDateTime.now());

            pagamentoDAO.inserir(pg, conn);

            // Atualiza total_pago (sem depender de somas pesadas na tela)
            c.setTotalPago(c.getTotalPago() + valor);

            // Ajusta status automaticamente
            if (c.getTotalPago() >= c.getTotalLiquido() && c.getTotalLiquido() > 0) {
                c.setStatus("fechada");
                c.setFechadoEm(LocalDateTime.now());
                c.setFechadoPor(usuario);
            } else {
                // se já estava pendente, continua; senão mantém aberta
                if (!"pendente".equalsIgnoreCase(c.getStatus())) c.setStatus("aberta");
            }

            comandaDAO.atualizarTotaisEStatus(c, conn);

            conn.commit();
        }
    }

    public void fecharComanda(int comandaId, boolean permitirPendente, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null) throw new Exception("Comanda não encontrada.");
            if (c.getStatus().equalsIgnoreCase("cancelada")) throw new Exception("Comanda cancelada não pode ser fechada.");

            // sempre recalcula antes de fechar
            recomputarTotaisEAtualizar(comandaId, conn);
            c = comandaDAO.buscarPorId(comandaId, conn);

            double saldo = c.getTotalLiquido() - c.getTotalPago();
            if (saldo <= 0.0001) {
                c.setStatus("fechada");
                c.setFechadoEm(LocalDateTime.now());
                c.setFechadoPor(usuario);
                comandaDAO.atualizarTotaisEStatus(c, conn);
                conn.commit();
                return;
            }

            if (!permitirPendente) {
                throw new Exception("Saldo em aberto: R$ " + String.format("%.2f", saldo) + ". Registre pagamento ou feche como pendente.");
            }

            // Pendente: trava itens, mas permite pagar depois
            c.setStatus("pendente");
            c.setFechadoEm(LocalDateTime.now());
            c.setFechadoPor(usuario);
            comandaDAO.atualizarTotaisEStatus(c, conn);

            conn.commit();
        }
    }

    public void cancelarComanda(int comandaId, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null) throw new Exception("Comanda não encontrada.");
            if (c.getStatus().equalsIgnoreCase("cancelada")) return;

            // Reverte estoque de todos os itens
            List<ComandaItemModel> itens = itemDAO.listarPorComanda(comandaId, conn);
            for (ComandaItemModel it : itens) {
                estoqueService.registrarEntrada(it.getProdutoId(), it.getQtd(), "Cancelamento Comanda #" + comandaId, usuario, conn);
            }

            // Marca cancelada (não deleta, porque histórico importa)
            c.setStatus("cancelada");
            c.setCanceladoEm(LocalDateTime.now());
            c.setCanceladoPor(usuario);

            // Zera valores (opcional, mas deixa a leitura óbvia)
            c.setTotalBruto(0);
            c.setDesconto(0);
            c.setAcrescimo(0);
            c.setTotalLiquido(0);

            comandaDAO.atualizarTotaisEStatus(c, conn);

            conn.commit();
        }
    }

    private void recomputarTotaisEAtualizar(int comandaId, Connection conn) throws Exception {
        ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
        if (c == null) throw new Exception("Comanda não encontrada.");

        List<ComandaItemModel> itens = itemDAO.listarPorComanda(comandaId, conn);

        double bruto = 0, desc = 0, acre = 0, liquido = 0;
        for (ComandaItemModel it : itens) {
            double itBruto = it.getQtd() * it.getPreco();
            bruto += itBruto;
            desc += it.getDesconto();
            acre += it.getAcrescimo();
            liquido += Math.max(0.0, itBruto - it.getDesconto() + it.getAcrescimo());
        }

        c.setTotalBruto(bruto);
        c.setDesconto(desc);
        c.setAcrescimo(acre);
        c.setTotalLiquido(liquido);

        // Status automático baseado em pagamento (sem ser “burro”)
        if (!"cancelada".equalsIgnoreCase(c.getStatus())) {
            if (c.getTotalLiquido() > 0 && c.getTotalPago() >= c.getTotalLiquido()) {
                c.setStatus("fechada");
            } else {
                // se foi fechado pendente, mantém pendente; senão, aberta
                if (!"pendente".equalsIgnoreCase(c.getStatus())) c.setStatus("aberta");
            }
        }

        comandaDAO.atualizarTotaisEStatus(c, conn);
    }
}
