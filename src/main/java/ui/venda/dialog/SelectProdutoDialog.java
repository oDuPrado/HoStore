// src/ui/venda/dialog/SelectProdutoDialog.java
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Seletor genérico de produtos (multi-check).
 * Usa ProdutoDAO.listAll() e ProdutoDAO.findById(),
 * agora com sistema de busca por código de barras.
 */
public class SelectProdutoDialog extends JDialog {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final List<ProdutoModel> todosProdutos;   // cache de todos

    // Inicializamos model e table em linha, para satisfazer o final
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
        "✓", "ID", "Nome", "Tipo", "Estoque", "R$ Venda"
    }, 0) {
        @Override public boolean isCellEditable(int r, int c) {
            return c == 0; // apenas checkbox
        }
        @Override public Class<?> getColumnClass(int c) {
            if (c == 0) return Boolean.class;
            if (c == 4) return Integer.class;
            if (c == 5) return Double.class;
            return String.class;
        }
    };
    private final JTable table = new JTable(model);

    private final JTextField txtNome     = new JTextField(15);
    private final JComboBox<String> cboTipo  = new JComboBox<>();
    private final JComboBox<String> cboOrder = new JComboBox<>(
        new String[]{ "Mais novo", "Mais antigo", "Maior preço", "Menor preço", "Maior estoque", "Menor estoque" }
    );

    private List<ProdutoModel> selecionados = Collections.emptyList();

    public SelectProdutoDialog(JFrame owner) {
        super(owner, "Selecionar Produtos", true);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

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
                Optional<ProdutoModel> encontrado = todosProdutos.stream()
                    .filter(p -> p.getCodigoBarras() != null && p.getCodigoBarras().equalsIgnoreCase(codigo))
                    .findFirst();

                if (encontrado.isPresent()) {
                    ProdutoModel p = encontrado.get();

                    // Itera sobre a tabela e marca o checkbox da linha correspondente
                    for (int i = 0; i < table.getRowCount(); i++) {
                        String idTabela = (String) model.getValueAt(i, 1); // coluna ID (oculta)
                        if (idTabela.equals(p.getId())) {
                            model.setValueAt(true, i, 0); // marca o checkbox
                            table.scrollRectToVisible(table.getCellRect(i, 0, true)); // rola até a linha
                            table.setRowSelectionInterval(i, i); // destaca a linha
                            return;
                        }
                    }

                    AlertUtils.warn("Produto encontrado, mas está sem estoque ou não visível na tabela.");
                } else {
                    AlertUtils.warn("Nenhum produto com este código de barras foi encontrado.");
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
    }

    private void personalizarTabela() {
        // formata moeda na coluna 5
        NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
        TableColumnModel cols = table.getColumnModel();
        cols.getColumn(5).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public void setValue(Object v){
                setHorizontalAlignment(SwingConstants.RIGHT);
                super.setText(cf.format((Double)v));
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
        String tipoSel = (String)cboTipo.getSelectedItem();
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
                model.addRow(new Object[]{
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
                if (p != null) sel.add(p);
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
            JOptionPane.YES_NO_OPTION
        );
        if (op == JOptionPane.YES_OPTION) {
            selecionados = sel;
            dispose();
        }
    }

    /** Retorna os produtos marcados após fechar o diálogo */
    public List<ProdutoModel> getSelecionados() {
        return selecionados;
    }
}
