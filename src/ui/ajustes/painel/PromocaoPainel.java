package ui.ajustes.painel;

import dao.CadastroGenericoDAO;
import ui.ajustes.dialog.PromocaoDialog;
import ui.ajustes.dialog.VincularProdutosDialog;
import ui.ajustes.dialog.VerProdutosVinculadosDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class PromocaoPainel extends AbstractCrudPainel {

    private final CadastroGenericoDAO dao = new CadastroGenericoDAO(
        "promocoes",
        "id", "nome", "desconto", "data_inicio", "data_fim", "observacoes"
    );

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PromocaoPainel() {
        super();
        carregarTabela();

        JButton btnVincular = new JButton("🔗 Vincular Produtos");
        btnVincular.addActionListener(e -> onVincularProdutos());

        JButton btnVer = new JButton("📋 Ver Vinculados");
        btnVer.addActionListener(e -> onVerVinculados());

        JButton btnTipo = new JButton("🧩 Tipos de Promoção");
        btnTipo.addActionListener(e -> new TipoPromocaoPainel().abrir());

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
        // adicionado 5ª coluna: Duração (dias)
        return new String[]{
            "Nome",
            "Desconto (%)",
            "Data Início",
            "Data Fim",
            "Duração (dias)"
        };
    }

    @Override
    protected void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<Map<String, String>> lista = dao.listar();
            LocalDate hoje = LocalDate.now();
            for (Map<String, String> row : lista) {
                String nome     = row.get("nome");
                String desc     = row.get("desconto");
                LocalDate ini   = LocalDate.parse(row.get("data_inicio"));
                LocalDate fim   = LocalDate.parse(row.get("data_fim"));
                String d1       = ini.format(fmt);
                String d2       = fim.format(fmt);

                String duracao;
                if (hoje.isAfter(fim)) {
                    duracao = "Acabou";
                } else {
                    long dias = ChronoUnit.DAYS.between(ini, fim) + 1;
                    duracao = String.valueOf(dias);
                }

                modelo.addRow(new Object[]{
                    nome,
                    desc,
                    d1,
                    d2,
                    duracao
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar promoções:\n" + e.getMessage()
            );
        }
    }

    @Override
    protected void onAdicionar() {
        new PromocaoDialog(null, null).setVisible(true);
        carregarTabela();
    }

    @Override
    protected void onEditar() {
        int row = tabela.getSelectedRow();
        if (row == -1) return;

        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (Map<String, String> item : dao.listar()) {
                if (item.get("nome").equals(nome)) {
                    new PromocaoDialog(null, item).setVisible(true);
                    carregarTabela();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao editar:\n" + e.getMessage()
            );
        }
    }

    @Override
    protected void onRemover() {
        int row = tabela.getSelectedRow();
        if (row == -1) return;

        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (Map<String, String> item : dao.listar()) {
                if (item.get("nome").equals(nome)) {
                    int op = JOptionPane.showConfirmDialog(
                        this,
                        "Remover a promoção “" + nome + "”?",
                        "Confirmação",
                        JOptionPane.YES_NO_OPTION
                    );
                    if (op == JOptionPane.YES_OPTION) {
                        dao.excluir(item.get("id"));
                        carregarTabela();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao excluir:\n" + e.getMessage()
            );
        }
    }

    private void onVincularProdutos() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Selecione uma promoção primeiro."
            );
            return;
        }
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (Map<String, String> item : dao.listar()) {
                if (item.get("nome").equals(nome)) {
                    new VincularProdutosDialog(item.get("id")).setVisible(true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao abrir vinculação:\n" + e.getMessage()
            );
        }
    }

    private void onVerVinculados() {
        int row = tabela.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Selecione uma promoção primeiro."
            );
            return;
        }
        String nome = modelo.getValueAt(row, 0).toString();
        try {
            for (Map<String, String> item : dao.listar()) {
                if (item.get("nome").equals(nome)) {
                    new VerProdutosVinculadosDialog(item.get("id")).setVisible(true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao abrir visualização:\n" + e.getMessage()
            );
        }
    }

    @Override
    protected DefaultTableModel criarModelo() {
        return new DefaultTableModel(getColunas(), 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return false; // tabela somente leitura
            }
        };
    }
}
