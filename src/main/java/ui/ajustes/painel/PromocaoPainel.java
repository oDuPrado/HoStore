package ui.ajustes.painel;

import model.PromocaoModel;
import model.TipoDesconto;
import model.AplicaEm;
import model.TipoPromocaoModel;
import dao.PromocaoDAO;
import dao.PromocaoAplicacaoDAO;
import dao.TipoPromocaoDAO;
import ui.ajustes.dialog.PromocaoDialog;
import ui.ajustes.dialog.VincularProdutosDialog;
import ui.ajustes.dialog.VerProdutosVinculadosDialog;
import ui.relatorios.dialog.RelatorioTabelaDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import util.MoedaUtil;
import java.util.List;

/**
 * @TODO: AJUSTAR_PROMOCAO_PAINEL
 *        Painel CRUD de Promoções usando o novo PromocaoModel e PromocaoDAO.
 *        Substitui o uso de CadastroGenericoDAO/Map por DAO e Model tipados.
 */
public class PromocaoPainel extends AbstractCrudPainel {

    // DAO específico para promocoes
    private final PromocaoDAO dao = new PromocaoDAO();
    // Formatter para exibir datas no padrão dd/MM/yyyy
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JButton btnHist = new JButton("ðŸ“Š Historico");

    public PromocaoPainel() {
        super();
        carregarTabela();

        // Botões extras: vincular, ver vinculados e gerenciar tipos de promoção
        JButton btnVincular = new JButton("🔗 Vincular Produtos");
        btnVincular.addActionListener(e -> onVincularProdutos());

        JButton btnVer = new JButton("📋 Ver Vinculados");
        btnVer.addActionListener(e -> onVerVinculados());

        JButton btnTipo = new JButton("🧩 Tipos de Promoção");
        btnTipo.addActionListener(e -> new TipoPromocaoPainel().abrir());
        btnHist.addActionListener(e -> onHistorico());

        // Adiciona os botões ao painel de ações
        JPanel painelBotoes = (JPanel) getComponent(1);
        painelBotoes.add(btnVincular);
        painelBotoes.add(btnVer);
        painelBotoes.add(btnTipo);
        painelBotoes.add(btnHist);
    }

    @Override
    protected String getTitulo() {
        return "Promoções e Descontos";
    }

    @Override
    protected String[] getColunas() {
        // Cabeçalhos das colunas que serão exibidas
        return new String[] {
                "Nome",
                "Tipo Desc",
                "Valor Desc",
                "Aplica em",
                "Categoria",
                "Tipo Promo",
                "Ativo",
                "Início",
                "Fim",
                "Duração (dias)"
        };
    }

    @Override
    protected DefaultTableModel criarModelo() {
        // Modelo de tabela não-editável
        return new DefaultTableModel(getColunas(), 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    @Override
    protected void carregarTabela() {
        modelo.setRowCount(0);
        try {
            // Busca todas as promoções cadastradas
            List<PromocaoModel> lista = dao.listarTodos();
            for (PromocaoModel p : lista) {
                // converte Date para LocalDate
                LocalDate ini = null;
                LocalDate fim = null;
                if (p.getDataInicio() != null) {
                    ini = p.getDataInicio().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                }
                if (p.getDataFim() != null) {
                    fim = p.getDataFim().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                }
                // formata datas
                String d1 = (ini != null) ? ini.format(fmt) : "-";
                String d2 = (fim != null) ? fim.format(fmt) : "-";

                // calcula duração em dias (inclusive), ou marca "Acabou"/"Inválida"
                String dur;
                if (ini == null || fim == null) {
                    dur = "—";
                } else if (ini.isAfter(fim)) {
                    dur = "Inválida";
                } else if (LocalDate.now().isAfter(fim)) {
                    dur = "Acabou";
                } else {
                    dur = String.valueOf(ChronoUnit.DAYS.between(ini, fim) + 1);
                }

                // busca o nome do TipoPromocao associado
                String nomeTipo;
                try {
                    TipoPromocaoModel tipo = new TipoPromocaoDAO().buscarPorId(p.getTipoId());
                    nomeTipo = tipo != null ? tipo.getNome() : "—";
                } catch (Exception e) {
                    nomeTipo = "—";
                }

                // adiciona linha na tabela
                modelo.addRow(new Object[] {
                        p.getNome(),
                        p.getTipoDesconto().name(),
                        p.getDesconto() + (p.getTipoDesconto() == TipoDesconto.PORCENTAGEM ? "%" : " R$"),
                        p.getAplicaEm().name(),
                        p.getCategoria() != null ? p.getCategoria() : "?",
                        nomeTipo,
                        (p.getAtivo() == null || p.getAtivo() == 1) ? "Sim" : "N?o",
                        d1, d2, dur
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar promoções:\n" + e.getMessage());
        }
    }

    @Override
    protected void onAdicionar() {
        try {
            new PromocaoDialog(null, null).setVisible(true);
            carregarTabela();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao abrir cadastro de promoção:\n" + e.getMessage());
        }
    }

    @Override
    protected void onEditar() {
        int row = tabela.getSelectedRow();
        if (row < 0)
            return;
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            // localiza pelo nome (poderia refinar para usar IDs)
            for (PromocaoModel p : dao.listarTodos()) {
                if (p.getNome().equals(nome)) {
                    new PromocaoDialog(null, p.getId()).setVisible(true);
                    carregarTabela();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao editar promoção:\n" + e.getMessage());
        }
    }

    @Override
    protected void onRemover() {
        int row = tabela.getSelectedRow();
        if (row < 0)
            return;
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (PromocaoModel p : dao.listarTodos()) {
                if (p.getNome().equals(nome)) {
                    int op = JOptionPane.showConfirmDialog(this,
                            "Remover promoção \"" + nome + "\"?",
                            "Confirmação", JOptionPane.YES_NO_OPTION);
                    if (op == JOptionPane.YES_OPTION) {
                        dao.excluir(p.getId());
                        carregarTabela();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao remover promoção:\n" + e.getMessage());
        }
    }

    // abre o dialog para vincular produtos à promoção selecionada
    private void onVincularProdutos() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma promoção primeiro.");
            return;
        }
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (PromocaoModel p : dao.listarTodos()) {
                if (p.getNome().equals(nome)) {
                    new VincularProdutosDialog(p.getId()).setVisible(true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao vincular produtos:\n" + e.getMessage());
        }
    }

    // abre o dialog para visualizar/desvincular produtos já vinculados
    private void onVerVinculados() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma promoção primeiro.");
            return;
        }
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (PromocaoModel p : dao.listarTodos()) {
                if (p.getNome().equals(nome)) {
                    new VerProdutosVinculadosDialog(p.getId()).setVisible(true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao ver produtos vinculados:\n" + e.getMessage());
        }
    }

    private void onHistorico() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma promo??o primeiro.");
            return;
        }
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            PromocaoModel promo = null;
            for (PromocaoModel p : dao.listarTodos()) {
                if (p.getNome().equals(nome)) {
                    promo = p;
                    break;
                }
            }
            if (promo == null) {
                JOptionPane.showMessageDialog(this, "Promo??o n?o encontrada.");
                return;
            }

            PromocaoAplicacaoDAO apDao = new PromocaoAplicacaoDAO();
            java.util.List<model.PromocaoAplicacaoModel> hist = apDao.listarPorPromocao(promo.getId());

            RelatorioTabelaDialog d = new RelatorioTabelaDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Hist?rico da Promo??o: " + promo.getNome(),
                    new Object[]{"Data", "Venda", "Item", "Produto", "Cliente", "Qtd", "Pre?o", "Desconto", "Final", "Tipo"});
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            for (model.PromocaoAplicacaoModel a : hist) {
                rows.add(new Object[]{
                        a.getDataAplicacao(),
                        a.getVendaId(),
                        a.getVendaItemId(),
                        a.getProdutoId(),
                        a.getClienteId(),
                        a.getQtd(),
                        MoedaUtil.brl(a.getPrecoOriginal()),
                        MoedaUtil.brl(a.getDescontoValor()),
                        MoedaUtil.brl(a.getPrecoFinal()),
                        a.getDescontoTipo()
                });
            }
            d.setRows(rows);
            d.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir historico:\n" + e.getMessage());
        }
    }

}
