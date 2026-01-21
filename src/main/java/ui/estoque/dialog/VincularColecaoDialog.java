package ui.estoque.dialog;

import util.UiKit;
import dao.ColecaoDAO;
import dao.SetDAO;
import model.ColecaoModel;
import model.SetModel;
import util.ColecaoMapper;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dialog Swing para vincular nomes PT de "Edição" (vindo da planilha da Liga)
 * aos valores oficiais de colecao e set do sistema.
 *
 * Fluxo:
 * 1) Recebe um Set<String> de nomesPt não mapeados (ex: "Evoluções em Paldea").
 * 2) Carrega todas as colecoes e sets existentes via DAO.
 * 3) Exibe uma JTable com colunas:
 *      [Nome PT da Edição]  [ComboBox de Colecao]  [ComboBox de Set]
 * 4) Usuário escolhe, linha a linha, qual ColecaoModel & SetModel aplicar.
 * 5) Ao clicar em "Salvar", grava cada par em ColecaoMapper e fecha.
 *
 * CTRL+F:
 *  - Para encontrar onde salvar o match, busque "CTRL+F: SALVAR_MAPEAMENTO"
 *  - Para saber onde popular a JTable, busque "POPULA_TABELA"
 */
public class VincularColecaoDialog extends JDialog {

    // --- Dados da planilha (nomes PT ainda não mapeados) ---
    private final List<String> nomesPt; // Ex: ["Evoluções em Paldea", "Mascarada Crepuscular", ...]
    // --- Modelos para IHM ---
    private final List<ColecaoModel> listaColecoes = new ArrayList<>();
    private final List<SetModel> listaSets = new ArrayList<>();
    private VincularTableModel tableModel;

    /**
     * Construtor principal.
     *
     * @param parent    Janela pai (pode ser null se não tiver)
     * @param nomesNaoMapeados  Conjunto de Strings contendo todos os nomes PT que ainda não têm mapeamento
     */
    public VincularColecaoDialog(Window parent, Set<String> nomesNaoMapeados) {
    super(parent, "Vincular Coleções e Sets", ModalityType.APPLICATION_MODAL);
        UiKit.applyDialogBase(this);

    this.nomesPt = new ArrayList<>(nomesNaoMapeados);

    try {
        // Tenta carregar os dados do banco
        listaColecoes.addAll(new ColecaoDAO().listarTodas());
        listaSets.addAll(new SetDAO().listarTodas());
    } catch (Exception e) {
        e.printStackTrace();
        AlertUtils.error("Erro ao carregar dados de coleções/sets:\n" + e.getMessage());
        dispose(); // Fecha o dialog se der erro
        return;
    }

    this.tableModel = new VincularTableModel(this.nomesPt, this.listaColecoes, this.listaSets);
    initUI();
    pack();
    setLocationRelativeTo(parent);
}


    /**
     * Configura a interface Swing (JTable + botões).
     */
    private void initUI() {
        // Layout geral
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- 1) JTable central com scroll ---
        JTable tabela = new JTable(tableModel);
        tabela.setRowHeight(25);
        tabela.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(listaColecoes.toArray(new ColecaoModel[0]))));
        tabela.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(listaSets.toArray(new SetModel[0]))));

        JScrollPane scroll = UiKit.scroll(tabela);
        add(scroll, BorderLayout.CENTER);

        // --- 2) Painel de botões ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Salvar Mapeamentos");
        JButton btnCancelar = new JButton("Cancelar");
        btnPanel.add(btnSalvar);
        btnPanel.add(btnCancelar);
        add(btnPanel, BorderLayout.PAGE_END);

        // Listener do botão Salvar
        btnSalvar.addActionListener(e -> {
            // Percorre todas as linhas, coleta o nomePT + seleção de combo
            for (int i = 0; i < nomesPt.size(); i++) {
                String nomePt = nomesPt.get(i);
                ColecaoModel colecaoSel = tableModel.getColecaoSelecionada(i);
                SetModel setSel = tableModel.getSetSelecionado(i);

                if (colecaoSel == null || setSel == null) {
                    // Se algum combo não foi escolhido, alerta e aborta
                    AlertUtils.warn("Por favor, selecione Coleção e Série (Set) para:\n" + nomePt);
                    return;
                }

                // SALVAR_MAPEAMENTO: grava em JSON via ColecaoMapper
                ColecaoMapper.ColecaoMatch match = new ColecaoMapper.ColecaoMatch();
                match.colecao = colecaoSel.getName(); // colecao.nome
                match.set = setSel.getNome();         // set.nome
                ColecaoMapper.set(nomePt, match);
                System.out.printf("[LOG] Mapeamento salvo: \"%s\" → (%s / %s)%n",
                        nomePt, match.colecao, match.set);
            }

            // Após salvar todos, fecha diálogo
            AlertUtils.info("Todos os mapeamentos foram salvos com sucesso.");
            dispose();
        });

        // Listener do botão Cancelar
        btnCancelar.addActionListener(e -> {
            // Apenas fecha sem salvar nada
            dispose();
        });
    }


    /**
     * TableModel customizado para exibir:
     *  COLUNA 0: Nome PT da Edição (String, não editável)
     *  COLUNA 1: ComboBox de ColecaoModel
     *  COLUNA 2: ComboBox de SetModel
     *
     * CTRL+F: POPULA_TABELA
     */
    private static class VincularTableModel extends AbstractTableModel {
        private final List<String> nomesPt;
        private final List<ColecaoModel> colecoesDisponiveis;
        private final List<SetModel> setsDisponiveis;
        // Em paralelo à lista de nomesPt, guardaremos as escolhas do usuário
        private final ColecaoModel[] selectedColecao;
        private final SetModel[] selectedSet;

        // Nomes das colunas que aparecem no JTable
        private final String[] colNames = {"Nome PT da Edição", "Coleção", "Série (Set)"};

        public VincularTableModel(List<String> nomesPt,
                                   List<ColecaoModel> colecoesDisponiveis,
                                   List<SetModel> setsDisponiveis) {
            this.nomesPt = nomesPt;
            this.colecoesDisponiveis = colecoesDisponiveis;
            this.setsDisponiveis = setsDisponiveis;
            this.selectedColecao = new ColecaoModel[nomesPt.size()];
            this.selectedSet = new SetModel[nomesPt.size()];
        }

        @Override
        public int getRowCount() {
            return nomesPt.size();
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            // Coluna 0 é String, Coluna 1 é ColecaoModel, Coluna 2 é SetModel
            if (columnIndex == 0) {
                return String.class;
            } else if (columnIndex == 1) {
                return ColecaoModel.class;
            } else {
                return SetModel.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Somente as colunas de combo (1 e 2) são editáveis
            return columnIndex == 1 || columnIndex == 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    // Exibe apenas o nome PT
                    return nomesPt.get(rowIndex);
                case 1:
                    // Retorna a ColecaoModel selecionada ou null
                    return selectedColecao[rowIndex];
                case 2:
                    // Retorna o SetModel selecionado ou null
                    return selectedSet[rowIndex];
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                // Usuário escolheu uma ColecaoModel no combo
                selectedColecao[rowIndex] = (ColecaoModel) aValue;
            }
            if (columnIndex == 2) {
                // Usuário escolheu um SetModel no combo
                selectedSet[rowIndex] = (SetModel) aValue;
            }
            // Notifica atualização no JTable
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        /**
         * Retorna a ColecaoModel escolhida para a linha 'row'
         * CTRL+F: GET_COLECAO_SELECIONADA
         */
        public ColecaoModel getColecaoSelecionada(int row) {
            return selectedColecao[row];
        }

        /**
         * Retorna o SetModel escolhido para a linha 'row'
         * CTRL+F: GET_SET_SELECIONADO
         */
        public SetModel getSetSelecionado(int row) {
            return selectedSet[row];
        }
    }
}
