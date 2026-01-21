package ui.venda.dialog;

import dao.ProdutoDAO;
import model.ProdutoModel;
import util.AlertUtils;
import util.LogService;
import util.ScannerUtils;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Seletor genérico de produtos (multi-check).
 * Usa ProdutoDAO.listAll() e ProdutoDAO.findById(),
 * com busca por código de barras.
 */
public class SelectProdutoDialog extends JDialog {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final List<ProdutoModel> todosProdutos; // cache

    private final DefaultTableModel model = new DefaultTableModel(new String[] {
            "✓", "ID", "Nome", "Tipo", "Estoque", "R$ Venda"
    }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 0;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            if (c == 0)
                return Boolean.class;
            if (c == 4)
                return Integer.class;
            if (c == 5)
                return Double.class;
            return String.class;
        }
    };

    private final JTable table = new JTable(model);

    private final JTextField txtNome = new JTextField(18);
    private final JComboBox<String> cboTipo = new JComboBox<>();
    private final JComboBox<String> cboOrder = new JComboBox<>(
            new String[] { "Mais novo", "Mais antigo", "Maior preço", "Menor preço", "Maior estoque",
                    "Menor estoque" });

    private List<ProdutoModel> selecionados = Collections.emptyList();

    public SelectProdutoDialog(JFrame owner) {
        super(owner, "Selecionar Produtos", true);

        UiKit.applyDialogBase(this);

        setMinimumSize(new Dimension(980, 640));
        setLayout(new BorderLayout(10, 10));

        // carrega tudo uma vez
        todosProdutos = produtoDAO.listAll();

        /* ===================== TOP (CARD) ===================== */
        JPanel top = UiKit.card();
        top.setLayout(new BorderLayout(10, 10));

        JPanel title = new JPanel(new GridLayout(0, 1, 0, 2));
        title.setOpaque(false);
        title.add(UiKit.title("Selecionar Produtos"));
        title.add(UiKit.hint("F2 Buscar | F3 Ler código | ENTER Confirmar | ESC Fechar | Ctrl+F Buscar nome"));
        top.add(title, BorderLayout.WEST);

        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 4, 2, 4);
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0;
        filtros.add(new JLabel("Nome:"), gc);
        gc.gridx = 1;
        filtros.add(txtNome, gc);

        gc.gridx = 2;
        filtros.add(new JLabel("Tipo:"), gc);
        gc.gridx = 3;
        filtros.add(cboTipo, gc);

        gc.gridx = 4;
        filtros.add(new JLabel("Ordenar:"), gc);
        gc.gridx = 5;
        filtros.add(cboOrder, gc);

        JButton btnBuscar = UiKit.primary("🔍 Buscar (F2)");
        btnBuscar.addActionListener(e -> carregarTabela());

        JButton btnScan = UiKit.ghost("📷 Ler Código (F3)");
        btnScan.addActionListener(e -> lerCodigoBarras());

        gc.gridx = 6;
        gc.weightx = 0;
        filtros.add(btnBuscar, gc);
        gc.gridx = 7;
        filtros.add(btnScan, gc);

        top.add(filtros, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        /* ===================== CENTER (CARD + TABLE) ===================== */
        JPanel center = UiKit.card();
        center.setLayout(new BorderLayout(8, 8));

        JPanel centerHeader = new JPanel(new BorderLayout());
        centerHeader.setOpaque(false);
        centerHeader.add(UiKit.title("Resultados"), BorderLayout.WEST);
        center.add(centerHeader, BorderLayout.NORTH);

        personalizarTabela();
        UiKit.tableDefaults(table);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        applyZebra(table, zebra);

        // moeda na coluna 5 (R$ Venda) mantendo zebra
        table.getColumnModel().getColumn(5).setCellRenderer(currencyRendererZebra(zebra));

        center.add(UiKit.scroll(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        /* ===================== FOOTER (CARD) ===================== */
        JPanel bottom = UiKit.card();
        bottom.setLayout(new BorderLayout(10, 10));

        bottom.add(UiKit.hint("Dica: você pode marcar vários itens e confirmar."), BorderLayout.WEST);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodape.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar (ESC)");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnAdd = UiKit.primary("Adicionar Selecionados (ENTER)");
        btnAdd.addActionListener(e -> confirmarSelecao());

        rodape.add(btnCancelar);
        rodape.add(btnAdd);
        bottom.add(rodape, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        popularTipos();
        carregarTabela();

        // atalhos
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "buscar");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "lerCodigo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirmar");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focoNome");

        am.put("buscar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                carregarTabela();
            }
        });
        am.put("lerCodigo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                lerCodigoBarras();
            }
        });
        am.put("confirmar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmarSelecao();
            }
        });
        am.put("cancelar", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        am.put("focoNome", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                txtNome.requestFocusInWindow();
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    private void personalizarTabela() {
        TableColumnModel cols = table.getColumnModel();

        // checkbox estreito
        cols.getColumn(0).setMaxWidth(42);
        cols.getColumn(0).setMinWidth(42);

        // “ocultar” ID sem removeColumn (evita treta entre model e view)
        TableColumn idCol = cols.getColumn(1);
        idCol.setMinWidth(0);
        idCol.setMaxWidth(0);
        idCol.setPreferredWidth(0);

        // alinhamentos
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        cols.getColumn(4).setCellRenderer(center); // estoque
        cols.getColumn(5).setCellRenderer(right); // venda (vai ser sobrescrito pelo renderer zebra-moeda)
    }

    private void popularTipos() {
        cboTipo.removeAllItems();
        cboTipo.addItem("Todos");

        Set<String> tiposSet = todosProdutos.stream()
                .map(ProdutoModel::getTipo)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new)); // ordenado

        for (String t : tiposSet)
            cboTipo.addItem(t);
        cboTipo.setSelectedIndex(0);
    }

    private void carregarTabela() {
        model.setRowCount(0);

        List<ProdutoModel> lista = new ArrayList<>(todosProdutos);

        // filtro por nome
        String txt = txtNome.getText().trim().toLowerCase();
        if (!txt.isEmpty()) {
            lista = lista.stream()
                    .filter(p -> p.getNome() != null && p.getNome().toLowerCase().contains(txt))
                    .collect(Collectors.toList());
        }

        // filtro por tipo
        String tipoSel = (String) cboTipo.getSelectedItem();
        if (tipoSel != null && !"Todos".equals(tipoSel)) {
            lista = lista.stream()
                    .filter(p -> tipoSel.equals(p.getTipo()))
                    .collect(Collectors.toList());
        }

        // ordenação
        switch (cboOrder.getSelectedIndex()) {
            case 0 -> lista.sort(Comparator
                    .comparing(ProdutoModel::getCriadoEm, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            case 1 -> lista.sort(
                    Comparator.comparing(ProdutoModel::getCriadoEm, Comparator.nullsLast(Comparator.naturalOrder())));
            case 2 -> lista.sort(
                    Comparator.comparing(ProdutoModel::getPrecoVenda, Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed());
            case 3 -> lista.sort(
                    Comparator.comparing(ProdutoModel::getPrecoVenda, Comparator.nullsLast(Comparator.naturalOrder())));
            case 4 -> lista.sort(Comparator.comparing(ProdutoModel::getQuantidade).reversed());
            case 5 -> lista.sort(Comparator.comparing(ProdutoModel::getQuantidade));
        }

        // popula linhas (estoque > 0)
        for (ProdutoModel p : lista) {
            if (p != null && p.getQuantidade() > 0) {
                model.addRow(new Object[] {
                        false,
                        p.getId(),
                        p.getNome(),
                        p.getTipo(),
                        p.getQuantidade(),
                        p.getPrecoVenda()
                });
            }
        }
    }

    private void confirmarSelecao() {
        List<ProdutoModel> sel = new ArrayList<>();

        for (int r = 0; r < model.getRowCount(); r++) {
            if (Boolean.TRUE.equals(model.getValueAt(r, 0))) {
                String id = (String) model.getValueAt(r, 1);
                ProdutoModel p = produtoDAO.findById(id);
                if (p != null)
                    sel.add(p);
            }
        }

        if (sel.isEmpty()) {
            AlertUtils.info("Nenhum produto selecionado.");
            return;
        }

        String resumo = sel.stream()
                .map(p -> "- " + p.getNome() + " (Qtde: " + p.getQuantidade() + ")")
                .collect(Collectors.joining("\n"));

        int op = JOptionPane.showConfirmDialog(this,
                "Você está adicionando:\n\n" + resumo + "\n\nConfirmar?",
                "Confirmar produtos",
                JOptionPane.YES_NO_OPTION);

        if (op == JOptionPane.YES_OPTION) {
            selecionados = sel;
            dispose();
        }
    }

    public List<ProdutoModel> getSelecionados() {
        return selecionados;
    }

    private void lerCodigoBarras() {
        ScannerUtils.lerCodigoBarras(this, "Ler Codigo de Barras", codigo -> {
            if (codigo == null || codigo.isBlank())
                return;

            List<ProdutoModel> encontrados = produtoDAO.findByCodigoBarrasList(codigo, false);

            LogService.audit("BARCODE_SCAN", "produto", null, "codigo=" + codigo + " encontrados=" + encontrados.size());

            if (encontrados.isEmpty()) {
                AlertUtils.warn("Nenhum produto com este codigo de barras foi encontrado.");
                return;
            }

            ProdutoModel escolhido;
            if (encontrados.size() == 1) {
                escolhido = encontrados.get(0);
            } else {
                escolhido = escolherProdutoPorCodigo(encontrados);
            }

            if (escolhido != null) {
                LogService.audit("BARCODE_SELECT", "produto", escolhido.getId(),
                        "codigo=" + codigo + " nome=" + escolhido.getNome());
                marcarProdutoNaTabela(escolhido);
            }
        });
    }

    private ProdutoModel escolherProdutoPorCodigo(List<ProdutoModel> encontrados) {
        DefaultTableModel m = new DefaultTableModel(
                new String[] { "Nome", "Tipo", "Fornecedor", "Preco Venda", "Estoque" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 3) return Double.class;
                if (c == 4) return Integer.class;
                return String.class;
            }
        };

        for (ProdutoModel p : encontrados) {
            m.addRow(new Object[] {
                    p.getNome(),
                    p.getTipo(),
                    p.getFornecedorNome(),
                    p.getPrecoVenda(),
                    p.getQuantidade()
            });
        }

        JTable t = new JTable(m);
        UiKit.tableDefaults(t);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (m.getRowCount() > 0)
            t.setRowSelectionInterval(0, 0);

        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        applyZebra(t, zebra);
        t.getColumnModel().getColumn(3).setCellRenderer(currencyRendererZebra(zebra));

        JPanel panel = UiKit.card();
        panel.setLayout(new BorderLayout(8, 8));
        panel.add(UiKit.hint("Selecione o SKU correto para este codigo de barras."), BorderLayout.NORTH);
        panel.add(UiKit.scroll(t), BorderLayout.CENTER);

        int op = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Selecionar Produto",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (op != JOptionPane.OK_OPTION)
            return null;

        int viewRow = t.getSelectedRow();
        if (viewRow < 0)
            return null;

        int modelRow = t.convertRowIndexToModel(viewRow);
        return encontrados.get(modelRow);
    }

    private void marcarProdutoNaTabela(ProdutoModel p) {
        for (int i = 0; i < model.getRowCount(); i++) {
            String idTabela = (String) model.getValueAt(i, 1); // ID oculto, mas existe no model
            if (Objects.equals(idTabela, p.getId())) {
                model.setValueAt(true, i, 0);
                Rectangle rect = table.getCellRect(i, 0, true);
                table.scrollRectToVisible(rect);
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
        AlertUtils.warn("Produto encontrado, mas está sem estoque ou não visível na tabela.");
    }

    private static void applyZebra(JTable t, DefaultTableCellRenderer zebra) {
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
    }

    private static TableCellRenderer currencyRendererZebra(DefaultTableCellRenderer zebra) {
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return (table, value, isSelected, hasFocus, row, column) -> {
            JLabel l = (JLabel) zebra.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            double v = (value instanceof Number) ? ((Number) value).doubleValue() : 0.0;
            l.setText(cf.format(v));
            return l;
        };
    }
}
