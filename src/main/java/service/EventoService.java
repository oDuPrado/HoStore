package service;

import dao.EventoDAO;
import dao.EventoParticipanteDAO;
import dao.EventoPremiacaoDAO;
import dao.EventoPremiacaoRegraDAO;
import dao.EventoRankingDAO;
import dao.ProdutoDAO;
import model.*;
import util.DB;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventoService {

    private final EventoDAO eventoDAO = new EventoDAO();
    private final EventoParticipanteDAO participanteDAO = new EventoParticipanteDAO();
    private final EventoRankingDAO rankingDAO = new EventoRankingDAO();
    private final EventoPremiacaoRegraDAO regraDAO = new EventoPremiacaoRegraDAO();
    private final EventoPremiacaoDAO premiacaoDAO = new EventoPremiacaoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final ProdutoEstoqueService estoqueService = new ProdutoEstoqueService();
    private final CreditoLojaService creditoService = new CreditoLojaService();
    private static final String PRODUTO_INSCRICAO_ID = "EVT-INSCRICAO";

    public List<EventoModel> listarEventos() {
        try {
            return eventoDAO.listarTodos();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar eventos", e);
        }
    }

    public EventoModel buscarEvento(String eventoId) {
        try {
            return eventoDAO.buscarPorId(eventoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar evento", e);
        }
    }

    public void salvarEvento(EventoModel e, String usuario) {
        try {
            if (e.getStatus() == null || e.getStatus().isBlank()) {
                e.setStatus("rascunho");
            }

            if (e.getId() == null || e.getId().isBlank()) {
                e.setId(UUID.randomUUID().toString());
                e.setCriadoEm(LocalDateTime.now().toString());
                e.setCriadoPor(usuario);

                e.setProdutoInscricaoId(garantirProdutoInscricao());

                eventoDAO.inserir(e);
                return;
            }

            e.setAlteradoEm(LocalDateTime.now().toString());
            e.setAlteradoPor(usuario);

            String antigoProduto = e.getProdutoInscricaoId();
            e.setProdutoInscricaoId(garantirProdutoInscricao());
            if (antigoProduto != null && !antigoProduto.isBlank()
                    && !PRODUTO_INSCRICAO_ID.equals(antigoProduto)) {
                try {
                    produtoDAO.inativar(antigoProduto, usuario);
                } catch (Exception ignored) {
                }
            }

            eventoDAO.atualizar(e);
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao salvar evento", ex);
        }
    }

    private String garantirProdutoInscricao() throws SQLException {
        ProdutoModel existente = produtoDAO.findById(PRODUTO_INSCRICAO_ID, true);
        if (existente != null) {
            return existente.getId();
        }
        ProdutoModel p = new ProdutoModel(PRODUTO_INSCRICAO_ID, "Inscricao", "SERVICO", 0, 0.0, 0.0);
        produtoDAO.insert(p);
        produtoDAO.inativar(PRODUTO_INSCRICAO_ID, null);
        return p.getId();
    }

    public EventoParticipanteModel inscreverParticipante(String eventoId, String clienteId, String nomeAvulso,
            Integer comandaId, Integer comandaItemId, String usuario) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            EventoModel evento = eventoDAO.buscarPorId(eventoId);
            if (evento == null) {
                throw new RuntimeException("Evento nao encontrado.");
            }

            if (evento.getLimiteParticipantes() != null) {
                int total = participanteDAO.contarPorEvento(eventoId);
                if (total >= evento.getLimiteParticipantes()) {
                    throw new RuntimeException("Limite de participantes atingido.");
                }
            }

            EventoParticipanteModel p = new EventoParticipanteModel();
            p.setId(UUID.randomUUID().toString());
            p.setEventoId(eventoId);
            p.setClienteId((clienteId != null && !clienteId.isBlank()) ? clienteId : null);
            p.setNomeAvulso((clienteId == null || clienteId.isBlank()) ? nomeAvulso : null);
            p.setStatus("inscrito");
            p.setComandaId(comandaId);
            p.setComandaItemId(comandaItemId);
            p.setCriadoEm(LocalDateTime.now().toString());
            p.setCriadoPor(usuario);

            participanteDAO.inserir(p, c);
            c.commit();
            return p;
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao inscrever participante", ex);
        }
    }

    public List<EventoParticipanteModel> listarParticipantes(String eventoId) {
        try {
            return participanteDAO.listarPorEvento(eventoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar participantes", e);
        }
    }

    public void atualizarStatusParticipante(String participanteId, String status, String usuario) {
        try {
            participanteDAO.atualizarStatus(participanteId, status, LocalDateTime.now().toString(), usuario);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status do participante", e);
        }
    }

    public void registrarCheckin(String participanteId, String usuario) {
        try (Connection c = DB.get()) {
            EventoParticipanteModel p = participanteDAO.buscarPorId(participanteId);
            if (p == null) {
                throw new RuntimeException("Participante nao encontrado.");
            }
            p.setStatus("presente");
            p.setDataCheckin(LocalDateTime.now().toString());
            p.setAlteradoEm(LocalDateTime.now().toString());
            p.setAlteradoPor(usuario);
            participanteDAO.atualizar(p, c);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar check-in", e);
        }
    }

    public void vincularVendaParticipante(String participanteId, int vendaId, String usuario) {
        try {
            participanteDAO.vincularVenda(participanteId, vendaId, "pago",
                    LocalDateTime.now().toString(), usuario);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao vincular venda ao participante", e);
        }
    }

    public void vincularComandaParticipante(String participanteId, int comandaId, int comandaItemId, String usuario) {
        try (Connection c = DB.get()) {
            EventoParticipanteModel p = participanteDAO.buscarPorId(participanteId);
            if (p == null) {
                throw new RuntimeException("Participante nao encontrado.");
            }
            p.setComandaId(comandaId);
            p.setComandaItemId(comandaItemId);
            p.setStatus("inscrito_comanda");
            p.setAlteradoEm(LocalDateTime.now().toString());
            p.setAlteradoPor(usuario);
            participanteDAO.atualizar(p, c);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao vincular comanda ao participante", e);
        }
    }

    public List<EventoRankingModel> listarRanking(String eventoId) {
        try {
            return rankingDAO.listarPorEvento(eventoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar ranking", e);
        }
    }

    public void salvarRanking(String eventoId, List<EventoRankingModel> itens) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            rankingDAO.deleteByEvento(eventoId, c);
            for (EventoRankingModel r : itens) {
                if (r.getId() == null || r.getId().isBlank()) {
                    r.setId(UUID.randomUUID().toString());
                }
                r.setEventoId(eventoId);
                rankingDAO.inserir(r, c);
            }
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar ranking", e);
        }
    }

    public List<EventoPremiacaoRegraModel> listarRegrasPremiacao(String eventoId) {
        try {
            return regraDAO.listarPorEvento(eventoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar regras de premiacao", e);
        }
    }

    public void salvarRegrasPremiacao(String eventoId, List<EventoPremiacaoRegraModel> regras) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            regraDAO.deleteByEvento(eventoId, c);
            for (EventoPremiacaoRegraModel r : regras) {
                if (r.getId() == null || r.getId().isBlank()) {
                    r.setId(UUID.randomUUID().toString());
                }
                r.setEventoId(eventoId);
                regraDAO.inserir(r, c);
            }
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar regras de premiacao", e);
        }
    }

    public List<EventoPremiacaoModel> listarPremiacoes(String eventoId) {
        try {
            return premiacaoDAO.listarPorEvento(eventoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar premiacoes", e);
        }
    }

    public void atualizarPremiacoes(List<EventoPremiacaoModel> premios) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            for (EventoPremiacaoModel p : premios) {
                premiacaoDAO.atualizarDadosBasicos(p, c);
            }
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar premiacoes", e);
        }
    }

    public void removerPremiacoes(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            for (String id : ids) {
                premiacaoDAO.deleteById(id, c);
            }
            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao remover premiacoes", e);
        }
    }

    public void aplicarPremiacaoPorRegras(String eventoId) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            List<EventoPremiacaoRegraModel> regras = regraDAO.listarPorEvento(eventoId);
            List<EventoRankingModel> ranking = rankingDAO.listarPorEvento(eventoId);
            ranking.sort((a, b) -> {
                int ca = (a.getColocacao() == null) ? Integer.MAX_VALUE : a.getColocacao();
                int cb = (b.getColocacao() == null) ? Integer.MAX_VALUE : b.getColocacao();
                return Integer.compare(ca, cb);
            });

            premiacaoDAO.deletePendentesByEvento(eventoId, c);

            List<EventoPremiacaoModel> novas = new ArrayList<>();
            for (EventoRankingModel r : ranking) {
                if (r.getColocacao() == null) {
                    continue;
                }
                int coloc = r.getColocacao();
                for (EventoPremiacaoRegraModel regra : regras) {
                    Integer ini = regra.getColocacaoInicio();
                    Integer fim = regra.getColocacaoFim();
                    if (ini != null && coloc < ini) {
                        continue;
                    }
                    if (fim != null && coloc > fim) {
                        continue;
                    }

                    EventoPremiacaoModel p = new EventoPremiacaoModel();
                    p.setId(UUID.randomUUID().toString());
                    p.setEventoId(eventoId);
                    p.setParticipanteId(r.getParticipanteId());
                    p.setTipo(regra.getTipo());
                    p.setProdutoId(regra.getProdutoId());
                    p.setQuantidade(regra.getQuantidade());
                    p.setValorCredito(regra.getValorCredito());
                    p.setStatus("pendente");
                    novas.add(p);
                }
            }

            for (EventoPremiacaoModel p : novas) {
                premiacaoDAO.inserir(p, c);
            }

            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao aplicar premiacao", e);
        }
    }

    public void entregarPremioProduto(String premiacaoId, String usuario) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            EventoPremiacaoModel p = premiacaoDAO.buscarPorId(premiacaoId);
            if (p == null) {
                throw new RuntimeException("Premiacao nao encontrada.");
            }
            if (!"pendente".equalsIgnoreCase(p.getStatus())) {
                throw new RuntimeException("Premiacao nao esta pendente.");
            }
            if (p.getProdutoId() == null || p.getQuantidade() == null) {
                throw new RuntimeException("Premiacao sem produto/quantidade.");
            }

            List<ProdutoEstoqueService.LoteConsumo> consumos = estoqueService.consumirFIFO(
                    p.getProdutoId(),
                    p.getQuantidade(),
                    "Premiacao evento " + p.getEventoId(),
                    usuario,
                    c,
                    p.getEventoId());

            Integer movId = consumos.isEmpty() ? null : consumos.get(0).movimentacaoId;

            p.setStatus("entregue");
            p.setMovimentacaoEstoqueId(movId);
            p.setEntregueEm(LocalDateTime.now().toString());
            p.setEntreguePor(usuario);
            premiacaoDAO.atualizar(p, c);

            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao entregar premio em produto", e);
        }
    }

    public void entregarPremioCredito(String premiacaoId, String usuario) {
        try (Connection c = DB.get()) {
            c.setAutoCommit(false);

            EventoPremiacaoModel p = premiacaoDAO.buscarPorId(premiacaoId);
            if (p == null) {
                throw new RuntimeException("Premiacao nao encontrada.");
            }
            if (!"pendente".equalsIgnoreCase(p.getStatus())) {
                throw new RuntimeException("Premiacao nao esta pendente.");
            }
            if (p.getValorCredito() == null) {
                throw new RuntimeException("Premiacao sem valor de credito.");
            }

            EventoParticipanteModel participante = participanteDAO.buscarPorId(p.getParticipanteId());
            if (participante == null || participante.getClienteId() == null) {
                throw new RuntimeException("Participante sem cliente para credito.");
            }

            String movId = creditoService.adicionarCreditoComRetorno(
                    participante.getClienteId(),
                    p.getValorCredito(),
                    "Premiacao evento " + p.getEventoId(),
                    p.getEventoId());

            p.setStatus("entregue");
            p.setCreditoMovId(movId);
            p.setEntregueEm(LocalDateTime.now().toString());
            p.setEntreguePor(usuario);
            premiacaoDAO.atualizar(p, c);

            c.commit();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao entregar premio em credito", e);
        }
    }
}
