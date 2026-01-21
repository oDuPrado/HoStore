package ui.estoque.dialog;

import dao.EstoqueLoteDAO;
import model.ProdutoModel;
import service.ProdutoEstoqueService;
import util.DB;
import util.LogService;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.sql.Connection;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class LotesProdutoDialog extends JDialog {

    private final ProdutoModel produto;
    private final DefaultTableModel tabelaModel;
    private final JTable tabela;

    public LotesProdutoDialog(Window owner, ProdutoModel produto) {
        super(owner, "Lotes do Produto", ModalityType.APPLICATION_MODAL);
        this.produto = produto;

        UiKit.applyDialogBase(this);
        setSize(980, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildFooter(), BorderLayout.SOUTH);

        tabelaModel = new DefaultTableModel(
                new String[] { "ID", "Codigo", "Fornecedor", "Entrada", "Validade", "Custo", "Preco",
                        "Qtd Inicial", "Qtd Disp", "Status", "Obs" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0 || col == 7 || col == 8)
                    return Integer.class;
                if (col == 5 || col == 6)
                    return Double.class;
                return String.class;
            }
        };

        tabela = new JTable(tabelaModel);
        UiKit.tableDefaults(tabela);
        configurarColunas();

        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));
        card.add(UiKit.scroll(tabela), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        carregarLotes();
    }

    private JPanel buildHeader() {
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 6));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);
        left.add(UiKit.title("Lotes do Produto"));
        left.add(UiKit.hint(produto.getNome() + " • SKU " + produto.getId()));
        header.add(left, BorderLayout.WEST);

        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btAtualizar = UiKit.ghost("Atualizar");
        JButton btAjustarLote = UiKit.ghost("Ajustar Lote");
        JButton btAjusteGeral = UiKit.ghost("Ajuste Geral");
        JButton btFechar = UiKit.primary("Fechar");

        btAtualizar.addActionListener(e -> carregarLotes());
        btAjustarLote.addActionListener(e -> ajustarLoteSelecionado());
        btAjusteGeral.addActionListener(e -> ajusteGeral());
        btFechar.addActionListener(e -> dispose());

        actions.add(btAtualizar);
        actions.add(btAjustarLote);
        actions.add(btAjusteGeral);
        actions.add(btFechar);

        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void configurarColunas() {
        TableColumnModel cm = tabela.getColumnModel();
        cm.getColumn(0).setMaxWidth(80);
        cm.getColumn(5).setMaxWidth(90);
        cm.getColumn(6).setMaxWidth(90);
        cm.getColumn(7).setMaxWidth(90);
        cm.getColumn(8).setMaxWidth(90);
        cm.getColumn(9).setMaxWidth(90);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setBorder(new EmptyBorder(0, 8, 0, 8));
        cm.getColumn(0).setCellRenderer(center);
        cm.getColumn(7).setCellRenderer(center);
        cm.getColumn(8).setCellRenderer(center);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setCellRenderer(zebra);
        }
        cm.getColumn(0).setCellRenderer(center);
        cm.getColumn(7).setCellRenderer(center);
        cm.getColumn(8).setCellRenderer(center);

        cm.getColumn(5).setCellRenderer(currencyRenderer(zebra));
        cm.getColumn(6).setCellRenderer(currencyRenderer(zebra));
    }

    private void carregarLotes() {
        tabelaModel.setRowCount(0);
        try (Connection c = DB.get()) {
            EstoqueLoteDAO dao = new EstoqueLoteDAO();
            List<EstoqueLoteDAO.LoteDetalhe> lotes = dao.listarLotesDetalhados(produto.getId(), c);
            for (EstoqueLoteDAO.LoteDetalhe l : lotes) {
                tabelaModel.addRow(new Object[] {
                        l.id,
                        l.codigoLote,
                        l.fornecedorNome,
                        formatDate(l.dataEntrada),
                        formatDate(l.validade),
                        l.custoUnit,
                        l.precoVendaUnit,
                        l.qtdInicial,
                        l.qtdDisponivel,
                        l.status,
                        l.observacoes
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar lotes:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajustarLoteSelecionado() {
        int viewRow = tabela.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um lote.");
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(viewRow);
        int loteId = (int) tabelaModel.getValueAt(modelRow, 0);

        String qtdStr = JOptionPane.showInputDialog(this,
                "Quantidade para ajustar (use negativo para diminuir):");
        if (qtdStr == null)
            return;

        int delta;
        try {
            delta = Integer.parseInt(qtdStr.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Quantidade invalida.");
            return;
        }

        if (delta == 0) {
            JOptionPane.showMessageDialog(this, "Quantidade nao pode ser zero.");
            return;
        }

        String motivo = JOptionPane.showInputDialog(this, "Motivo do ajuste:");
        if (motivo == null || motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Motivo obrigatorio.");
            return;
        }

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            ProdutoEstoqueService service = new ProdutoEstoqueService();
            service.ajustarLote(produto.getId(), loteId, delta, motivo.trim(), "sistema", c);
            LogService.audit("UI_AJUSTE_LOTE", "lote", String.valueOf(loteId),
                    "produto=" + produto.getId() + " delta=" + delta + " motivo=" + motivo.trim());
            c.commit();
            carregarLotes();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao ajustar lote:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajusteGeral() {
        String qtdStr = JOptionPane.showInputDialog(this, "Quantidade para ajuste geral:");
        if (qtdStr == null)
            return;

        int qtd;
        try {
            qtd = Integer.parseInt(qtdStr.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Quantidade invalida.");
            return;
        }

        if (qtd <= 0) {
            JOptionPane.showMessageDialog(this, "Quantidade deve ser maior que zero.");
            return;
        }

        String motivo = JOptionPane.showInputDialog(this, "Motivo do ajuste geral:");
        if (motivo == null || motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Motivo obrigatorio.");
            return;
        }

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            ProdutoEstoqueService service = new ProdutoEstoqueService();
            service.criarLoteAjuste(produto.getId(), qtd, motivo.trim(), "sistema", c);
            LogService.audit("UI_AJUSTE_GERAL", "produto", produto.getId(),
                    "qtd=" + qtd + " motivo=" + motivo.trim());
            c.commit();
            carregarLotes();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao criar lote de ajuste:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String formatDate(String raw) {
        if (raw == null || raw.isBlank())
            return "";
        try {
            if (raw.contains("T")) {
                LocalDateTime dt = LocalDateTime.parse(raw);
                return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            if (raw.contains(" ")) {
                LocalDateTime dt = LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            LocalDate d = LocalDate.parse(raw);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return raw;
        }
    }

    private static DefaultTableCellRenderer currencyRenderer(DefaultTableCellRenderer zebra) {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebra.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
                l.setText(cf.format(v));
                return l;
            }
        };
    }
}
