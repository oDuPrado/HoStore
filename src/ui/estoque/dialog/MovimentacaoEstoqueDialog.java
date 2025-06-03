package ui.estoque.dialog;

import util.DB;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Vector;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * Diálogo para exibir e filtrar todas as movimentações de estoque,
 * mostrando o NOME do produto em vez do ID, e com um detalhe
 * em forma de tabela quando o usuário dá duplo-clique numa linha.
 *
 * Ajustes para legibilidade em temas claro/escuro:
 * - Não força fundo branco em diálogos ou painéis.
 * - Usa getForeground() do renderer padrão para texto “normal”.
 * - Mantém apenas cores específicas para “entrada” (verde) e “saida”
 * (vermelho).
 *
 * Como usar:
 * 1) Copie este arquivo para
 * src/ui/estoque/dialog/MovimentacaoEstoqueDialog.java
 * 2) Certifique-se de ter JCalendar (JDateChooser) no classpath.
 * 3) Em algum menu ou botão, chame:
 * new MovimentacaoEstoqueDialog(seuFramePai).setVisible(true);
 *
 * @author Marco Prado
 */
public class MovimentacaoEstoqueDialog extends JDialog {

    // Formato ISO armazenado no banco: "yyyy-MM-ddTHH:mm:ss"
    private static final DateTimeFormatter FMT_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    // Formato legível para exibição: "dd/MM/yyyy HH:mm:ss"
    private static final DateTimeFormatter FMT_EXIBIR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Componentes principais
    private JTable tabela;
    private DefaultTableModel tabelaModel;
    private JComboBox<String> cboTipo;
    private JDateChooser dtInicio, dtFim;
    private JButton btnFiltrar;

    public MovimentacaoEstoqueDialog(Window owner) {
        super(owner, "Movimentações de Estoque", ModalityType.APPLICATION_MODAL);

        // Configurações iniciais da janela
        setSize(920, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // =========== PAINEL DE FILTROS (NORTH) ===========
        JPanel painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        // Deixa o painel sem setBackground() fixo para herdar o tema ativo

        // Filtro de tipo: Todos / entrada / saida
        painelFiltros.add(new JLabel("Tipo:"));
        cboTipo = new JComboBox<>(new String[] { "Todos", "entrada", "saida" });
        painelFiltros.add(cboTipo);

        // Filtro de data início
        painelFiltros.add(new JLabel("Data Início:"));
        dtInicio = new JDateChooser();
        dtInicio.setDateFormatString("dd/MM/yyyy");
        painelFiltros.add(dtInicio);

        // Filtro de data fim
        painelFiltros.add(new JLabel("Data Fim:"));
        dtFim = new JDateChooser();
        dtFim.setDateFormatString("dd/MM/yyyy");
        painelFiltros.add(dtFim);

        // Botão “Aplicar Filtros”
        btnFiltrar = new JButton("Aplicar Filtros");
        painelFiltros.add(btnFiltrar);

        add(painelFiltros, BorderLayout.NORTH);

        // =========== TABELA DE DADOS (CENTER) ===========
        // Colunas: ID, Produto, Tipo, Quantidade, Motivo, Data/Hora, Usuário
        tabelaModel = new DefaultTableModel(
                new String[] { "ID", "Produto", "Tipo", "Quantidade", "Motivo", "Data/Hora", "Usuário" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // tabela somente leitura
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0 || col == 3)
                    return Integer.class;
                return String.class;
            }
        };

        tabela = new JTable(tabelaModel);
        // Renderer centralizado padrão
        DefaultTableCellRenderer centralizado = new DefaultTableCellRenderer();
        centralizado.setHorizontalAlignment(SwingConstants.CENTER);

        // Aplica centralização para todas as colunas, exceto “Produto” (coluna 1) e
        // “Tipo” (coluna 2)
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            if (i != 1 && i != 2) {
                tabela.getColumnModel().getColumn(i).setCellRenderer(centralizado);
            }
        }

        // Ativa ordenação clicando nos títulos das colunas
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tabelaModel);
        tabela.setRowSorter(sorter);

        // Ordenação padrão: Data/Hora (coluna 5), em ordem DESC
        sorter.toggleSortOrder(5); // primeira vez = ASC
        sorter.toggleSortOrder(5); // segunda vez = DESC

        tabela.setFillsViewportHeight(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderer personalizado para destacar “entrada” em verde e “saida” em vermelho
        tabela.getColumnModel().getColumn(2).setCellRenderer(new TableCellRenderer() {
            private final DefaultTableCellRenderer RENDERER = new DefaultTableCellRenderer();
            {
                RENDERER.setHorizontalAlignment(SwingConstants.CENTER); // 👈 centraliza
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String tipo = (String) value;
                if ("entrada".equalsIgnoreCase(tipo)) {
                    c.setForeground(new Color(0, 220, 0));
                } else if ("saida".equalsIgnoreCase(tipo)) {
                    c.setForeground(new Color(255, 0, 0));
                } else {
                    c.setForeground(RENDERER.getForeground());
                }
                return c;
            }
        });

        // Duplo-clique: abre detalhes em formato de tabela
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() != -1) {
                    exibirDetalhesComTabela();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        add(scroll, BorderLayout.CENTER);

        // =========== BOTÃO DE AÇÃO DO FILTRO ===========
        btnFiltrar.addActionListener(evt -> carregarMovimentacoes());

        // Carrega todas movimentações inicialmente
        carregarMovimentacoes();
    }

    /**
     * Carrega as movimentações do banco aplicando filtros de tipo e data.
     * Realiza JOIN com a tabela 'produtos' para obter o nome do produto.
     */
    private void carregarMovimentacoes() {
        tabelaModel.setRowCount(0); // limpa linhas antigas

        String tipoSelecionado = (String) cboTipo.getSelectedItem();

        LocalDate dataIni = null;
        LocalDate dataFimLocal = null;
        if (dtInicio.getDate() != null) {
            dataIni = dtInicio.getDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (dtFim.getDate() != null) {
            dataFimLocal = dtFim.getDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        }

        // Montagem dinâmica do SQL com JOIN para pegar o nome do produto
        StringBuilder sql = new StringBuilder(
                "SELECT m.id, m.produto_id, p.nome AS produto_nome, " +
                        "m.tipo_mov, m.quantidade, m.motivo, m.data, m.usuario " +
                        "FROM estoque_movimentacoes m " +
                        "LEFT JOIN produtos p ON m.produto_id = p.id " +
                        "WHERE 1=1 ");
        Vector<Object> params = new Vector<>();

        // Filtro de tipo (entrada/saida)
        if (!"Todos".equalsIgnoreCase(tipoSelecionado)) {
            sql.append("AND m.tipo_mov = ? ");
            params.add(tipoSelecionado);
        }

        // Filtro de data (compara somente a parte yyyy-MM-dd)
        if (dataIni != null) {
            sql.append("AND SUBSTR(m.data,1,10) >= ? ");
            params.add(dataIni.toString());
        }
        if (dataFimLocal != null) {
            sql.append("AND SUBSTR(m.data,1,10) <= ? ");
            params.add(dataFimLocal.toString());
        }

        sql.append("ORDER BY m.data DESC");

        // Executa a consulta e popula a tabela
        try (Connection c = DB.get();
                PreparedStatement ps = c.prepareStatement(sql.toString())) {

            // Define parâmetros no PreparedStatement
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String produtoNome = rs.getString("produto_nome");
                    if (produtoNome == null) {
                        // Caso não encontre na tabela 'produtos', exibe "ID:xxxxx"
                        produtoNome = "ID:" + rs.getString("produto_id");
                    }
                    String tipo = rs.getString("tipo_mov");
                    int quantidade = rs.getInt("quantidade");
                    String motivo = rs.getString("motivo");
                    String dataBruta = rs.getString("data");
                    String usuario = rs.getString("usuario");

                    // Formata a data ISO para algo legível: "dd/MM/yyyy HH:mm:ss"
                    String dataFormatada;
                    try {
                        LocalDateTime dt = LocalDateTime.parse(dataBruta, FMT_ISO);
                        dataFormatada = FMT_EXIBIR.format(dt);
                    } catch (Exception ex) {
                        // Se der erro no parse, exibe como veio do banco
                        dataFormatada = dataBruta;
                    }

                    // Adiciona a linha na tabela
                    tabelaModel.addRow(new Object[] {
                            id,
                            produtoNome,
                            tipo,
                            quantidade,
                            motivo,
                            dataFormatada,
                            usuario
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar movimentações:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Ao dar duplo-clique em uma linha, abre um JDialog contendo uma JTable
     * “Campo | Valor” com todos os detalhes da movimentação selecionada.
     */
    private void exibirDetalhesComTabela() {
        int linha = tabela.getSelectedRow();
        if (linha < 0)
            return;

        // Obtém dados da linha selecionada
        Object idObj = tabelaModel.getValueAt(linha, 0);
        Object produtoObj = tabelaModel.getValueAt(linha, 1);
        Object tipoObj = tabelaModel.getValueAt(linha, 2);
        Object qtdObj = tabelaModel.getValueAt(linha, 3);
        Object motivoObj = tabelaModel.getValueAt(linha, 4);
        Object dataHoraObj = tabelaModel.getValueAt(linha, 5);
        Object usuarioObj = tabelaModel.getValueAt(linha, 6);

        // Model para JTable de detalhes: “Campo | Valor”
        DefaultTableModel detalhesModel = new DefaultTableModel(
                new String[] { "Campo", "Valor" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return String.class;
            }
        };

        // Adiciona cada campo como uma linha
        detalhesModel.addRow(new Object[] { "ID", idObj.toString() });
        detalhesModel.addRow(new Object[] { "Produto", produtoObj.toString() });
        detalhesModel.addRow(new Object[] { "Tipo", tipoObj.toString() });
        detalhesModel.addRow(new Object[] { "Quantidade", qtdObj.toString() });
        detalhesModel.addRow(new Object[] { "Motivo", motivoObj.toString() });
        detalhesModel.addRow(new Object[] { "Data/Hora", dataHoraObj.toString() });
        detalhesModel.addRow(new Object[] { "Usuário", usuarioObj.toString() });

        // Cria a JTable de detalhes dentro de JScrollPane
        JTable tabelaDetalhes = new JTable(detalhesModel);
        tabelaDetalhes.setFillsViewportHeight(true);
        tabelaDetalhes.setRowHeight(24);
        tabelaDetalhes.getColumnModel().getColumn(0).setPreferredWidth(100);
        tabelaDetalhes.getColumnModel().getColumn(1).setPreferredWidth(300);

        JScrollPane scrollDetalhes = new JScrollPane(tabelaDetalhes);
        scrollDetalhes.setPreferredSize(new Dimension(420, 200));

        // Cria o JDialog para mostrar detalhes
        JDialog dlg = new JDialog(this, "Detalhes Movimentação #" + idObj, true);
        dlg.setLayout(new BorderLayout(8, 8));
        // Removido dlg.getContentPane().setBackground(Color.WHITE);
        // Agora herda o fundo do tema ativo (claro/escuro)

        // Adiciona o JScrollPane ao centro
        dlg.add(scrollDetalhes, BorderLayout.CENTER);

        // Botão “Fechar”
        JButton btnFechar = new JButton("Fechar");
        btnFechar.addActionListener(e -> dlg.dispose());
        JPanel painelBtn = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // Sem setBackground() fixo
        painelBtn.add(btnFechar);
        dlg.add(painelBtn, BorderLayout.SOUTH);

        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ==================== Método main() para teste independente
    // ====================
    // Você pode remover ou comentar este método no código final se não precisar.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MovimentacaoEstoqueDialog dlg = new MovimentacaoEstoqueDialog(null);
            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dlg.setVisible(true);
        });
    }
}
