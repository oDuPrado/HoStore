// src/ui/estoque/dialog/CriarPedidoEstoqueDialog.java
package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import javax.swing.border.*;
import dao.PedidoEstoqueProdutoDAO;
import dao.FornecedorDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.FornecedorModel;
import model.ProdutoModel;
import com.toedter.calendar.JDateChooser;
import util.UiKit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CriarPedidoEstoqueDialog extends JDialog {

    private final PedidoCompraDAO pedidoDAO = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();

    private final List<ProdutoModel> produtos;
    private final Map<String, ProdutoModel> produtoPorId = new HashMap<>();
    private final List<FornecedorOption> fornecedores = new ArrayList<>();
    private final Map<String, FornecedorOption> fornecedorPorId = new HashMap<>();
    private PedidoCompraModel pedido;

    private final JTextField tfNomePedido = new JTextField(26);
    private final JDateChooser dcData = new JDateChooser(new Date());
    private final JTextField tfFiltroNome = new JTextField(18);
    private final JComboBox<String> cbCategoria;
    private final NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final DefaultTableModel model;
    private final JTable tabela;
    private final TableRowSorter<DefaultTableModel> sorter;

    public CriarPedidoEstoqueDialog(Frame owner, List<ProdutoModel> todosProdutos) {
        super(owner, "🧾 Criar Pedido de Estoque", true);
        UiKit.applyDialogBase(this);

        this.produtos = todosProdutos;
        for (ProdutoModel p : produtos) {
            produtoPorId.put(p.getId(), p);
        }
        carregarFornecedores();

        List<String> categorias = produtos.stream()
                .map(ProdutoModel::getTipo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        categorias.add(0, "Todas");
        cbCategoria = new JComboBox<>(categorias.toArray(new String[0]));

        model = new DefaultTableModel(
                new Object[][] {},
                new String[] { "X", "ID", "Nome", "Categoria", "Estoque Atual", "Fornecedor", "Custo", "Preço",
                        "Qtd a Pedir" }) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 5 || col == 6 || col == 7 || col == 8;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                if (columnIndex == 4 || columnIndex == 8)
                    return Integer.class;
                if (columnIndex == 6 || columnIndex == 7)
                    return Double.class;
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

        // renderer zebra so pras colunas que NAO sao checkbox
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            if (i == 0)
                continue; // coluna de checkbox
            cm.getColumn(i).setCellRenderer(zebra);
        }

        // coluna 0 = checkbox de verdade
        cm.getColumn(0).setCellRenderer(tabela.getDefaultRenderer(Boolean.class));
        cm.getColumn(0).setCellEditor(tabela.getDefaultEditor(Boolean.class));

        // fornecedor por item
        JComboBox<FornecedorOption> cbFornecedor = new JComboBox<>(fornecedores.toArray(new FornecedorOption[0]));
        cm.getColumn(5).setCellEditor(new DefaultCellEditor(cbFornecedor));

        // editores de moeda
        cm.getColumn(6).setCellEditor(moneyEditor("Custo"));
        cm.getColumn(7).setCellEditor(moneyEditor("Preco"));

        DefaultTableCellRenderer moeda = currencyRenderer(zebra);
        cm.getColumn(6).setCellRenderer(moeda);
        cm.getColumn(7).setCellRenderer(moeda);

        // larguras
        cm.getColumn(0).setMaxWidth(42);
        cm.getColumn(4).setMaxWidth(120);
        cm.getColumn(5).setMaxWidth(180);
        cm.getColumn(6).setMaxWidth(110);
        cm.getColumn(7).setMaxWidth(110);
        cm.getColumn(8).setMaxWidth(110);

        tableCard.add(UiKit.scroll(tabela), BorderLayout.CENTER);

        // Mini ações
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.setOpaque(false);
        JButton marcarTudo = UiKit.ghost("Marcar tudo");
        JButton desmarcarTudo = UiKit.ghost("Desmarcar");
        JButton manterAtual = UiKit.ghost("Manter valores atuais");
        quick.add(marcarTudo);
        quick.add(desmarcarTudo);
        quick.add(manterAtual);
        tableCard.add(quick, BorderLayout.NORTH);

        marcarTudo.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++)
                model.setValueAt(true, i, 0);
        });
        desmarcarTudo.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++)
                model.setValueAt(false, i, 0);
        });
        manterAtual.addActionListener(e -> aplicarValoresAtuaisSelecionados());

        body.add(tableCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // Footer card
        JPanel footer = UiKit.card();
        footer.setLayout(new BorderLayout());

        footer.add(UiKit.hint("Dica: ajuste fornecedor/custo/preÃ§o ou use 'Manter valores atuais'."),
                BorderLayout.WEST);

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
            FornecedorOption forn = fornecedorPorId.getOrDefault(p.getFornecedorId(), fornecedorPorId.get(null));
            double custo = sanitizeMoney(p.getPrecoCompra());
            double preco = sanitizeMoney(p.getPrecoVenda());
            model.addRow(new Object[] { false, p.getId(), p.getNome(), p.getTipo(), p.getQuantidade(), forn, custo,
                    preco, 0 });
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
            JOptionPane.showMessageDialog(this, "Preencha o nome e a data do pedido.", "Atencao",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Integer> selecionados = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object marcado = model.getValueAt(i, 0);
            if (marcado instanceof Boolean b && b) {
                selecionados.add(i);
            }
        }

        if (selecionados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Marque ao menos um produto para o pedido.", "Atencao",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int row : selecionados) {
            Object v = model.getValueAt(row, 8);
            int qtd;

            try {
                qtd = Integer.parseInt(String.valueOf(v));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantidade invalida na linha " + (row + 1), "Atencao",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String nomeProd = String.valueOf(model.getValueAt(row, 2));

            if (qtd <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Quantidade deve ser > 0 para o produto:\n" + nomeProd,
                        "Atencao",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                parseMoney(model.getValueAt(row, 6), "Custo", nomeProd);
                parseMoney(model.getValueAt(row, 7), "Preco", nomeProd);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Atencao", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Erro ao criar pedido:\\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int row : selecionados) {
            String prodId = model.getValueAt(row, 1).toString();
            String nomeProd = model.getValueAt(row, 2).toString();
            int qtd = Integer.parseInt(model.getValueAt(row, 8).toString());
            String linkId = UUID.randomUUID().toString();

            FornecedorOption forn = (FornecedorOption) model.getValueAt(row, 5);
            String fornecedorId = (forn != null) ? forn.id : null;
            double custo = parseMoney(model.getValueAt(row, 6), "Custo", nomeProd);
            double preco = parseMoney(model.getValueAt(row, 7), "Preco", nomeProd);

            PedidoEstoqueProdutoModel item = new PedidoEstoqueProdutoModel(
                    linkId,
                    idPedido,
                    prodId,
                    fornecedorId,
                    custo,
                    preco,
                    qtd,
                    0,
                    "pendente");
            try {
                itemDAO.inserir(item);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao adicionar item:\\n" + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this, "Pedido criado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void carregarFornecedores() {
        fornecedores.clear();
        fornecedorPorId.clear();
        FornecedorOption vazio = new FornecedorOption(null, "Sem fornecedor");
        fornecedores.add(vazio);
        fornecedorPorId.put(null, vazio);

        try {
            List<FornecedorModel> lista = new FornecedorDAO().listar(null, null, null, null);
            for (FornecedorModel f : lista) {
                FornecedorOption opt = new FornecedorOption(f.getId(), f.getNome());
                fornecedores.add(opt);
                fornecedorPorId.put(opt.id, opt);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar fornecedores:\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarValoresAtuaisSelecionados() {
        for (int i = 0; i < model.getRowCount(); i++) {
            Object marcado = model.getValueAt(i, 0);
            if (!(marcado instanceof Boolean b && b))
                continue;
            String prodId = model.getValueAt(i, 1).toString();
            ProdutoModel p = produtoPorId.get(prodId);
            if (p == null)
                continue;
            FornecedorOption forn = fornecedorPorId.getOrDefault(p.getFornecedorId(), fornecedorPorId.get(null));
            model.setValueAt(forn, i, 5);
            model.setValueAt(sanitizeMoney(p.getPrecoCompra()), i, 6);
            model.setValueAt(sanitizeMoney(p.getPrecoVenda()), i, 7);
        }
    }

    private DefaultTableCellRenderer currencyRenderer(DefaultTableCellRenderer base) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                double v = 0.0;
                if (value instanceof Number n) {
                    v = n.doubleValue();
                } else if (value != null) {
                    try {
                        v = Double.parseDouble(value.toString().replace(",", "."));
                    } catch (Exception ignore) {
                        v = 0.0;
                    }
                }
                l.setText(brl.format(v));
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        };
    }

    private DefaultCellEditor moneyEditor(String campo) {
        JTextField tf = new JTextField();
        return new DefaultCellEditor(tf) {
            @Override
            public boolean stopCellEditing() {
                try {
                    parseMoney(getCellEditorValue(), campo, null);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CriarPedidoEstoqueDialog.this, ex.getMessage(), "Atencao",
                            JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                return super.stopCellEditing();
            }
        };
    }

    private double parseMoney(Object value, String campo, String produtoNome) {
        if (value == null)
            throw new IllegalArgumentException(msgCampoInvalido(campo, produtoNome));
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        String s = value.toString().trim();
        if (s.isEmpty())
            throw new IllegalArgumentException(msgCampoInvalido(campo, produtoNome));
        s = s.replace("R$", "").replace(" ", "");
        if (s.contains(",") && s.contains(".")) {
            s = s.replace(".", "").replace(",", ".");
        } else {
            s = s.replace(",", ".");
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(msgCampoInvalido(campo, produtoNome));
        }
    }

    private String msgCampoInvalido(String campo, String produtoNome) {
        if (produtoNome == null || produtoNome.isBlank())
            return campo + " invalido.";
        return campo + " invalido para o produto: " + produtoNome;
    }

    private double sanitizeMoney(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < 0)
            return 0.0;
        return valor;
    }

    private static class FornecedorOption {
        public final String id;
        public final String nome;

        public FornecedorOption(String id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        @Override
        public String toString() {
            return nome == null ? "" : nome;
        }
    }
}
