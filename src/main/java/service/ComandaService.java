package service;

import dao.ComandaDAO;
import dao.ComandaItemDAO;
import dao.ComandaPagamentoDAO;
import dao.EventoParticipanteDAO;
import model.ComandaItemModel;
import model.ComandaModel;
import model.ComandaPagamentoModel;
import model.ComandaResumoModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

public class ComandaService {

    private final ComandaDAO comandaDAO = new ComandaDAO();
    private final ComandaItemDAO itemDAO = new ComandaItemDAO();
    private final ComandaPagamentoDAO pagamentoDAO = new ComandaPagamentoDAO();

    public int abrirComanda(String clienteId, String nomeCliente, String mesa, String obs, String usuario)
            throws Exception {
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

    // Atalho pra UI
    public List<ComandaItemModel> listarItens(int comandaId) throws Exception {
        try (Connection conn = DB.get()) {
            return itemDAO.listarPorComanda(comandaId, conn);
        }
    }

    public List<ComandaPagamentoModel> listarPagamentos(int comandaId, Connection conn) throws Exception {
        return pagamentoDAO.listarPorComanda(comandaId, conn);
    }

    public void adicionarItem(int comandaId, String produtoId, int qtd, double preco, double desconto, double acrescimo,
            String obs, String usuario) throws Exception {
        adicionarItemRetornandoId(comandaId, produtoId, qtd, preco, desconto, acrescimo, obs, usuario);
    }

    public int adicionarItemRetornandoId(int comandaId, String produtoId, int qtd, double preco, double desconto,
            double acrescimo, String obs, String usuario) throws Exception {

        if (qtd <= 0)
            throw new Exception("Quantidade inválida.");
        if (preco < 0)
            throw new Exception("Preço inválido.");
        if (desconto < 0)
            desconto = 0;
        if (acrescimo < 0)
            acrescimo = 0;

        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null)
                throw new Exception("Comanda não encontrada.");
            if (c.getStatus() == null)
                c.setStatus("aberta");

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

            int itemId = itemDAO.inserir(it, conn);

            // ✅ Opção A: Comanda NÃO mexe em estoque
            // Estoque será baixado SOMENTE ao virar venda (VendaService.finalizarVenda)

            recomputarTotaisEAtualizar(comandaId, conn);

            conn.commit();
            return itemId;
        }
    }

    public void removerItem(int itemId, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaItemModel it = itemDAO.buscarPorId(itemId, conn);
            if (it == null)
                throw new Exception("Item não encontrado.");

            ComandaModel c = comandaDAO.buscarPorId(it.getComandaId(), conn);
            if (c == null)
                throw new Exception("Comanda não encontrada.");

            if (c.getStatus().equalsIgnoreCase("fechada") || c.getStatus().equalsIgnoreCase("cancelada")) {
                throw new Exception("Comanda não permite alterações (status: " + c.getStatus() + ").");
            }

            itemDAO.deletar(itemId, conn);

            // ✅ Opção A: nada de devolver estoque aqui, porque comanda não baixou estoque

            recomputarTotaisEAtualizar(it.getComandaId(), conn);

            conn.commit();
        }
    }

    public void registrarPagamento(int comandaId, String tipo, double valor, String usuario) throws Exception {
        if (valor <= 0)
            throw new Exception("Valor inválido.");

        if (tipo == null || tipo.isBlank())
            tipo = "OUTRO";
        tipo = tipo.trim().toUpperCase();

        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null)
                throw new Exception("Comanda não encontrada.");
            if (c.getStatus().equalsIgnoreCase("cancelada"))
                throw new Exception("Comanda cancelada não recebe pagamento.");

            ComandaPagamentoModel pg = new ComandaPagamentoModel();
            pg.setComandaId(comandaId);
            pg.setTipo(tipo);
            pg.setValor(valor);
            pg.setUsuario(usuario);
            pg.setData(LocalDateTime.now());

            pagamentoDAO.inserir(pg, conn);

            // Atualiza total_pago
            c.setTotalPago(c.getTotalPago() + valor);

            // Ajusta status automaticamente
            if (c.getTotalPago() >= c.getTotalLiquido() && c.getTotalLiquido() > 0) {
                c.setStatus("fechada");
                c.setFechadoEm(LocalDateTime.now());
                c.setFechadoPor(usuario);
            } else {
                if (!"pendente".equalsIgnoreCase(c.getStatus()))
                    c.setStatus("aberta");
            }

            comandaDAO.atualizarTotaisEStatus(c, conn);

            conn.commit();
        }
    }

    public void fecharComanda(int comandaId, boolean permitirPendente, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null)
                throw new Exception("Comanda não encontrada.");
            if (c.getStatus().equalsIgnoreCase("cancelada"))
                throw new Exception("Comanda cancelada não pode ser fechada.");

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
                throw new Exception("Saldo em aberto: R$ " + String.format("%.2f", saldo)
                        + ". Registre pagamento ou feche como pendente.");
            }

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
            if (c == null)
                throw new Exception("Comanda não encontrada.");
            if (c.getStatus().equalsIgnoreCase("cancelada"))
                return;

            // ✅ Opção A: nada de devolver estoque, porque comanda não baixou estoque

            c.setStatus("cancelada");
            c.setCanceladoEm(LocalDateTime.now());
            c.setCanceladoPor(usuario);

            c.setTotalBruto(0);
            c.setDesconto(0);
            c.setAcrescimo(0);
            c.setTotalLiquido(0);

            comandaDAO.atualizarTotaisEStatus(c, conn);

            conn.commit();
        }
    }

    // Fecha comanda vinculando venda
    public void fecharComandaComVenda(int comandaId, int vendaId, String usuario) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
            if (c == null)
                throw new Exception("Comanda não encontrada.");
            if ("cancelada".equalsIgnoreCase(c.getStatus()))
                throw new Exception("Comanda cancelada não pode virar venda.");

            recomputarTotaisEAtualizar(comandaId, conn);

            String sql = """
                        UPDATE comandas
                           SET status = 'fechada',
                               fechado_em = ?,
                               fechado_por = ?,
                               venda_id = ?,
                               total_pago = total_liquido
                         WHERE id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, LocalDateTime.now().toString());
                ps.setString(2, usuario);
                ps.setInt(3, vendaId);
                ps.setInt(4, comandaId);
                ps.executeUpdate();
            }

            // Vincula participantes de eventos que pagaram via comanda
            new EventoParticipanteDAO().vincularVendaPorComanda(comandaId, vendaId, conn);

            conn.commit();
        }
    }

    private void recomputarTotaisEAtualizar(int comandaId, Connection conn) throws Exception {
        ComandaModel c = comandaDAO.buscarPorId(comandaId, conn);
        if (c == null)
            throw new Exception("Comanda não encontrada.");

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

        if (!"cancelada".equalsIgnoreCase(c.getStatus())) {
            if (c.getTotalLiquido() > 0 && c.getTotalPago() >= c.getTotalLiquido()) {
                c.setStatus("fechada");
            } else {
                if (!"pendente".equalsIgnoreCase(c.getStatus()))
                    c.setStatus("aberta");
            }
        }

        comandaDAO.atualizarTotaisEStatus(c, conn);
    }
}
