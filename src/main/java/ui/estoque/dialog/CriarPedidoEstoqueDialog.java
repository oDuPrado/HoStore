// src/ui/estoque/dialog/CriarPedidoEstoqueDialog.java
package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import javax.swing.border.*;
import dao.PedidoEstoqueProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;
import com.toedter.calendar.JDateChooser;
import util.UiKit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CriarPedidoEstoqueDialog extends JDialog {

    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();

    private final List<ProdutoModel> produtos;
    private PedidoCompraModel pedido;

    private final JTextField tfNomePedido = new JTextField(26);
    private final JDateChooser dcData = new JDateChooser(new Date());
    private final JTextField tfFiltroNome = new JTextField(18);
    private final JComboBox<String> cbCategoria;

    private final DefaultTableModel model;
    private final JTable tabela;
    private final TableRowSorter<DefaultTableModel> sorter;

    public CriarPedidoEstoqueDialog(Frame owner, List<ProdutoModel> todosProdutos) {
        super(owner, "🧾 Criar Pedido de Estoque", true);
        UiKit.applyDialogBase(this);

        this.produtos = todosProdutos;

        List<String> categorias = produtos.stream()
                .map(ProdutoModel::getTipo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        categorias.add(0, "Todas");
        cbCategoria = new JComboBox<>(categorias.toArray(new String[0]));

        model = new DefaultTableModel(
                new Object[][] {},
                new String[] { "✓", "ID", "Nome", "Categoria", "Estoque Atual", "Qtd a Pedir" }) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 5;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };

        tabela = new JTable(model);
        sorter = new TableRowSorter<>(model);
        tabela.setRowSorter(sorter);

        initComponents();
        carregarProdutos();

        setSize(980, 620);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));

        // Header card
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(12, 8));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        titleBox.add(UiKit.title("Criar Pedido de Estoque"));
        titleBox.add(UiKit.hint("Selecione produtos, defina quantidades e salve como rascunho."));
        header.add(titleBox, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // Body split: filtros + tabela (card)
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);

        JPanel topCard = UiKit.card();
        topCard.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        dcData.setDateFormatString("dd/MM/yyyy");
        tfNomePedido.setText("Pedido de " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        // linha 0
        g.gridx = 0;
        g.gridy = 0;
        topCard.add(new JLabel("Nome do Pedido:"), g);
        g.gridx = 1;
        g.gridy = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        topCard.add(tfNomePedido, g);

        g.gridx = 2;
        g.gridy = 0;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        topCard.add(new JLabel("Data:"), g);
        g.gridx = 3;
        g.gridy = 0;
        topCard.add(dcData, g);

        // linha 1
        g.gridx = 0;
        g.gridy = 1;
        topCard.add(new JLabel("Buscar:"), g);
        g.gridx = 1;
        g.gridy = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        topCard.add(tfFiltroNome, g);

        g.gridx = 2;
        g.gridy = 1;
        g.fill = GridBagConstraints.NONE;
        topCard.add(new JLabel("Categoria:"), g);
        g.gridx = 3;
        g.gridy = 1;
        topCard.add(cbCategoria, g);

        tfFiltroNome.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            public void removeUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            public void changedUpdate(DocumentEvent e) {
                aplicarFiltros();
            }
        });
        cbCategoria.addActionListener(e -> aplicarFiltros());

        body.add(topCard, BorderLayout.NORTH);

        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(8, 8));

        UiKit.tableDefaults(tabela);
        esconderColunaID();

        TableColumnModel cm = tabela.getColumnModel();

        // renderer zebra só pras colunas que NÃO são checkbox
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            if (i == 0)
                continue; // coluna de checkbox
            cm.getColumn(i).setCellRenderer(zebra);
        }

        // coluna 0 = checkbox de verdade
        cm.getColumn(0).setCellRenderer(tabela.getDefaultRenderer(Boolean.class));
        cm.getColumn(0).setCellEditor(tabela.getDefaultEditor(Boolean.class));

        // larguras
        cm.getColumn(0).setMaxWidth(42);
        cm.getColumn(4).setMaxWidth(120);
        cm.getColumn(5).setMaxWidth(110);

        // Colunas com largura decente
        cm.getColumn(0).setMaxWidth(42);
        cm.getColumn(4).setMaxWidth(120);
        cm.getColumn(5).setMaxWidth(110);

        tableCard.add(UiKit.scroll(tabela), BorderLayout.CENTER);

        // Mini ações
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.setOpaque(false);
        JButton marcarTudo = UiKit.ghost("Marcar tudo");
        JButton desmarcarTudo = UiKit.ghost("Desmarcar");
        quick.add(marcarTudo);
        quick.add(desmarcarTudo);
        tableCard.add(quick, BorderLayout.NORTH);

        marcarTudo.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++)
                model.setValueAt(true, i, 0);
        });
        desmarcarTudo.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++)
                model.setValueAt(false, i, 0);
        });

        body.add(tableCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // Footer card
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Dica: preencha 'Qtd a Pedir' apenas nos selecionados."), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btSalvar = UiKit.primary("💾 Salvar Pedido");
        JButton btCancelar = UiKit.ghost("Cancelar");
        actions.add(btCancelar);
        actions.add(btSalvar);
        footer.add(actions, BorderLayout.EAST);

        btSalvar.addActionListener(e -> salvarPedido());
        btCancelar.addActionListener(e -> dispose());

        add(footer, BorderLayout.SOUTH);
    }

    private void aplicarFiltros() {
        String texto = tfFiltroNome.getText().trim();
        String cat = (String) cbCategoria.getSelectedItem();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!texto.isEmpty())
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 2));
        if (!"Todas".equals(cat))
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(cat) + "$", 3));
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void carregarProdutos() {
        model.setRowCount(0);
        for (ProdutoModel p : produtos) {
            model.addRow(new Object[] { false, p.getId(), p.getNome(), p.getTipo(), p.getQuantidade(), 0 });
        }
    }

    private void esconderColunaID() {
        TableColumnModel cm = tabela.getColumnModel();
        cm.getColumn(1).setMinWidth(0);
        cm.getColumn(1).setMaxWidth(0);
        cm.getColumn(1).setPreferredWidth(0);
    }

    // === sua lógica original, intacta ===
    private void salvarPedido() {
        String nome = tfNomePedido.getText().trim();
        Date data = dcData.getDate();
        if (nome.isEmpty() || data == null) {
            JOptionPane.showMessageDialog(this, "Preencha o nome e a data do pedido.", "Atenção",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Integer> selecionados = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object marcado = model.getValueAt(i, 0);
            if (marcado instanceof Boolean b && b)
                selecionados.add(i);
        }
        if (selecionados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Marque ao menos um produto para o pedido.", "Atenção",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int row : selecionados) {
            Object v = model.getValueAt(row, 5);
            int qtd;
            try {
                qtd = Integer.parseInt(v.toString());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantidade inválida na linha " + (row + 1), "Atenção",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (qtd <= 0) {
                String nomeProd = model.getValueAt(row, 2).toString();
                JOptionPane.showMessageDialog(this, "Quantidade deve ser > 0 para o produto:\n" + nomeProd,
                        "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String idPedido = UUID.randomUUID().toString();
        String dataIso = new SimpleDateFormat("yyyy-MM-dd").format(data);
        pedido = new PedidoCompraModel(idPedido, nome, dataIso, "rascunho", null, "");

        try {
            pedidoDAO.inserir(pedido);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao criar pedido:\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int row : selecionados) {
            String prodId = model.getValueAt(row, 1).toString();
            int qtd = Integer.parseInt(model.getValueAt(row, 5).toString());
            String linkId = UUID.randomUUID().toString();

            PedidoEstoqueProdutoModel item = new PedidoEstoqueProdutoModel(linkId, idPedido, prodId, qtd, 0,
                    "pendente");
            try {
                itemDAO.inserir(item);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao adicionar item:\n" + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this, "Pedido criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
