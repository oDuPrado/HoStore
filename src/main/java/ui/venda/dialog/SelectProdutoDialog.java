package ui.venda.dialog;

import dao.ProdutoDAO;
import model.ProdutoModel;
import util.AlertUtils;
import util.ScannerUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Seletor genérico de produtos (multi-check).
 * Usa ProdutoDAO.listAll() e ProdutoDAO.findById(),
 * agora com sistema de busca por código de barras.
 */
public class SelectProdutoDialog extends JDialog {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final List<ProdutoModel> todosProdutos; // cache de todos

    // Inicializamos model e table em linha, para satisfazer o final
    private final DefaultTableModel model = new DefaultTableModel(new String[] {
            "✓", "ID", "Nome", "Tipo", "Estoque", "R$ Venda"
    }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 0; // apenas checkbox
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

    private final JTextField txtNome = new JTextField(15);
    private final JComboBox<String> cboTipo = new JComboBox<>();
    private final JComboBox<String> cboOrder = new JComboBox<>(
            new String[] { "Mais novo", "Mais antigo", "Maior preço", "Menor preço", "Maior estoque",
                    "Menor estoque" });

    private List<ProdutoModel> selecionados = Collections.emptyList();

    public SelectProdutoDialog(JFrame owner) {
        super(owner, "Selecionar Produtos", true);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // carrega tudo uma vez
        todosProdutos = produtoDAO.listAll();

        // --- filtros ---
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtros.add(new JLabel("Nome:"));
        filtros.add(txtNome);
        filtros.add(new JLabel("Tipo:"));
        filtros.add(cboTipo);
        filtros.add(new JLabel("Ordenar por:"));
        filtros.add(cboOrder);

        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.addActionListener(e -> carregarTabela());
        filtros.add(btnBuscar);

        // 📷 Botão de leitura de código de barras
        JButton btnScan = new JButton("📷 Ler Código de Barras");
        btnScan.addActionListener(e -> {
            ScannerUtils.lerCodigoBarras(this, "Ler Código de Barras", codigo -> {
                List<ProdutoModel> encontrados = todosProdutos.stream()
                    .filter(p -> p.getCodigoBarras() != null && p.getCodigoBarras().equalsIgnoreCase(codigo))
                    .collect(Collectors.toList());

                if (encontrados.isEmpty()) {
                    AlertUtils.warn("Nenhum produto com este código de barras foi encontrado.");
                } else if (encontrados.size() == 1) {
                    marcarProdutoNaTabela(encontrados.get(0));
                } else {
                    // múltiplas opções, pergunta ao usuário
                    ProdutoModel escolhido = (ProdutoModel) JOptionPane.showInputDialog(
                        this,
                        "Múltiplos produtos encontrados com este código. Escolha o correto:",
                        "Selecionar Produto",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        encontrados.toArray(),
                        encontrados.get(0)
                    );
                    if (escolhido != null) {
                        marcarProdutoNaTabela(escolhido);
                    }
                }
            });
        });
        filtros.add(btnScan);

        add(filtros, BorderLayout.NORTH);

        // --- tabela ---
        personalizarTabela();
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- rodapé ---
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JButton btnAdd = new JButton("Adicionar Selecionados");
        btnAdd.addActionListener(e -> confirmarSelecao());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rodape.add(btnCancelar);
        rodape.add(btnAdd);
        add(rodape, BorderLayout.SOUTH);

        popularTipos();
        carregarTabela();

        // Atalhos de teclado
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
                for (ActionListener al : btnScan.getActionListeners()) {
                    al.actionPerformed(new ActionEvent(btnScan, ActionEvent.ACTION_PERFORMED, ""));
                }
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

    }

    private void personalizarTabela() {
        // formata moeda na coluna 5
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        TableColumnModel cols = table.getColumnModel();
        cols.getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object v) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                super.setText(cf.format((Double) v));
            }
        });
        // oculta coluna ID visualmente
        cols.removeColumn(cols.getColumn(1));
        // checkbox estreito
        cols.getColumn(0).setMaxWidth(40);
    }

    private void popularTipos() {
        // extrai tipos únicos e ordena
        Set<String> tiposSet = todosProdutos.stream()
                .map(ProdutoModel::getTipo)
                .collect(Collectors.toSet());
        List<String> tipos = new ArrayList<>(tiposSet);
        Collections.sort(tipos);
        cboTipo.addItem("Todos");
        tipos.forEach(cboTipo::addItem);
    }

    private void carregarTabela() {
        model.setRowCount(0);
        // cópia para filtrar/ordenar
        List<ProdutoModel> lista = new ArrayList<>(todosProdutos);

        // filtro por nome
        String txt = txtNome.getText().trim().toLowerCase();
        if (!txt.isEmpty()) {
            lista = lista.stream()
                    .filter(p -> p.getNome().toLowerCase().contains(txt))
                    .collect(Collectors.toList());
        }

        // filtro por tipo
        String tipoSel = (String) cboTipo.getSelectedItem();
        if (tipoSel != null && !"Todos".equals(tipoSel)) {
            lista = lista.stream()
                    .filter(p -> p.getTipo().equals(tipoSel))
                    .collect(Collectors.toList());
        }

        // ordenação
        switch (cboOrder.getSelectedIndex()) {
            case 0 -> lista.sort(Comparator.comparing(ProdutoModel::getCriadoEm).reversed());
            case 1 -> lista.sort(Comparator.comparing(ProdutoModel::getCriadoEm));
            case 2 -> lista.sort(Comparator.comparing(ProdutoModel::getPrecoVenda).reversed());
            case 3 -> lista.sort(Comparator.comparing(ProdutoModel::getPrecoVenda));
            case 4 -> lista.sort(Comparator.comparing(ProdutoModel::getQuantidade).reversed());
            case 5 -> lista.sort(Comparator.comparing(ProdutoModel::getQuantidade));
        }

        // popula linhas (ignora estoque = 0)
        for (ProdutoModel p : lista) {
            if (p.getQuantidade() > 0) {
                model.addRow(new Object[] {
                        false, p.getId(), p.getNome(), p.getTipo(),
                        p.getQuantidade(), p.getPrecoVenda()
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
        // resumo de confirmação
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

    /** Retorna os produtos marcados após fechar o diálogo */
    public List<ProdutoModel> getSelecionados() {
        return selecionados;
    }

    /**
     * Marca na tabela o produto correspondente ao modelo fornecido.
     */
    private void marcarProdutoNaTabela(ProdutoModel p) {
        for (int i = 0; i < table.getRowCount(); i++) {
            String idTabela = (String) model.getValueAt(i, 1); // coluna ID oculta
            if (idTabela.equals(p.getId())) {
                model.setValueAt(true, i, 0); // marca checkbox
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
        AlertUtils.warn("Produto encontrado, mas está sem estoque ou não visível na tabela.");
    }
}
