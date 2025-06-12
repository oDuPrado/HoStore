package ui.ajustes.painel;

import model.PromocaoModel;
import model.TipoDesconto;
import model.AplicaEm;
import model.TipoPromocaoModel;
import dao.PromocaoDAO;
import dao.TipoPromocaoDAO;
import ui.ajustes.dialog.PromocaoDialog;
import ui.ajustes.dialog.VincularProdutosDialog;
import ui.ajustes.dialog.VerProdutosVinculadosDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

        // Adiciona os botões ao painel de ações
        JPanel painelBotoes = (JPanel) getComponent(1);
        painelBotoes.add(btnVincular);
        painelBotoes.add(btnVer);
        painelBotoes.add(btnTipo);
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
                "Tipo Promo",
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
                LocalDate ini = p.getDataInicio().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate fim = p.getDataFim().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                // formata datas
                String d1 = ini.format(fmt);
                String d2 = fim.format(fmt);

                // calcula duração em dias (inclusive), ou marca "Acabou"/"Inválida"
                String dur;
                if (ini.isAfter(fim)) {
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
                        nomeTipo,
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
}
