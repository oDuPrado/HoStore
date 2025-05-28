package ui.estoque.dialog;

import dao.PedidoCompraDAO;
import dao.PedidoEstoqueProdutoDAO;
import dao.ProdutoDAO;
import model.PedidoCompraModel;
import model.PedidoEstoqueProdutoModel;
import model.ProdutoModel;

import com.toedter.calendar.JDateChooser;

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

    private final PedidoCompraDAO pedidoDAO       = new PedidoCompraDAO();
    private final PedidoEstoqueProdutoDAO itemDAO = new PedidoEstoqueProdutoDAO();

    private final List<ProdutoModel> produtos;
    private PedidoCompraModel pedido;

    private final JTextField tfNomePedido    = new JTextField(25);
    private final JDateChooser dcData         = new JDateChooser(new Date());
    private final JTextField tfFiltroNome     = new JTextField(15);
    private final JComboBox<String> cbCategoria;

    private final DefaultTableModel model;
    private final JTable tabela;
    private final TableRowSorter<DefaultTableModel> sorter;

    public CriarPedidoEstoqueDialog(Frame owner, List<ProdutoModel> todosProdutos) {
        super(owner, "Criar Pedido de Estoque", true);
        this.produtos = todosProdutos;

        // Prepara combo de categorias (Todos + distintas)
        List<String> categorias = produtos.stream()
            .map(ProdutoModel::getTipo)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        categorias.add(0, "Todas");
        cbCategoria = new JComboBox<>(categorias.toArray(new String[0]));

        // Cria model com coluna de seleção (checkbox) + ID oculto
        model = new DefaultTableModel(
            new Object[][] {},
            new String[] { "✓", "ID", "Nome", "Categoria", "Estoque Atual", "Qtd a Pedir" }
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Coluna 0 = checkbox, coluna 5 = Qtd a Pedir
                return col == 0 || col == 5;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };

        tabela = new JTable(model);
        tabela.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sorter = new TableRowSorter<>(model);
        tabela.setRowSorter(sorter);

        initComponents();
        carregarProdutos();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // ── Topo: Nome do Pedido e Data ─────────────────
        JPanel pnlTopo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        dcData.setDateFormatString("dd/MM/yyyy");
        tfNomePedido.setText("Pedido de " +
            new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        pnlTopo.add(new JLabel("Nome do Pedido:"));
        pnlTopo.add(tfNomePedido);
        pnlTopo.add(new JLabel("Data:"));
        pnlTopo.add(dcData);

        // ── Filtros: Busca por nome e categoria ─────────
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        pnlFiltros.add(new JLabel("Filtrar Nome:"));
        pnlFiltros.add(tfFiltroNome);
        pnlFiltros.add(new JLabel("Categoria:"));
        pnlFiltros.add(cbCategoria);

        tfFiltroNome.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { aplicarFiltros(); }
            public void removeUpdate(DocumentEvent e) { aplicarFiltros(); }
            public void changedUpdate(DocumentEvent e) { aplicarFiltros(); }
        });
        cbCategoria.addActionListener(e -> aplicarFiltros());

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.add(pnlTopo, BorderLayout.NORTH);
        pnlHeader.add(pnlFiltros, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // ── Centro: Tabela ────────────────────────────
        esconderColunaID();
        JScrollPane scroll = new JScrollPane(tabela);
        tabela.setPreferredScrollableViewportSize(new Dimension(700, 300));
        add(scroll, BorderLayout.CENTER);

        // ── Rodapé: Botões ────────────────────────────
        JPanel pnlRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        JButton btSalvar   = new JButton("💾 Salvar Pedido");
        JButton btCancelar = new JButton("Cancelar");
        pnlRodape.add(btSalvar);
        pnlRodape.add(btCancelar);
        add(pnlRodape, BorderLayout.SOUTH);

        btSalvar.addActionListener(e -> salvarPedido());
        btCancelar.addActionListener(e -> dispose());
    }

    private void aplicarFiltros() {
        String texto = tfFiltroNome.getText().trim();
        String cat   = (String) cbCategoria.getSelectedItem();

        List<RowFilter<Object,Object>> filters = new ArrayList<>();
        if (!texto.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 2)); // coluna Nome
        }
        if (!"Todas".equals(cat)) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(cat) + "$", 3)); // coluna Categoria
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void carregarProdutos() {
        model.setRowCount(0);
        for (ProdutoModel p : produtos) {
            model.addRow(new Object[]{
                false,           // checkbox desmarcado
                p.getId(),
                p.getNome(),
                p.getTipo(),
                p.getQuantidade(),
                0                // qtd a pedir inicial = 0
            });
        }
    }

    private void esconderColunaID() {
        TableColumnModel cm = tabela.getColumnModel();
        // checkbox = coluna 0 visível; ID = coluna 1 oculta
        cm.getColumn(1).setMinWidth(0);
        cm.getColumn(1).setMaxWidth(0);
        cm.getColumn(1).setPreferredWidth(0);
    }

    private void salvarPedido() {
        String nome = tfNomePedido.getText().trim();
        Date data   = dcData.getDate();
        if (nome.isEmpty() || data == null) {
            JOptionPane.showMessageDialog(this,
                "Preencha o nome e a data do pedido.",
                "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Coleta índices dos produtos marcados (checkbox = true)
        List<Integer> selecionados = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object marcado = model.getValueAt(i, 0);
            if (marcado instanceof Boolean b && b) {
                selecionados.add(i);
            }
        }
        if (selecionados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Marque ao menos um produto para o pedido.",
                "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validação de quantidade em cada selecionado
        for (int row : selecionados) {
            Object v = model.getValueAt(row, 5); // coluna Qtd a Pedir
            int qtd;
            try {
                qtd = Integer.parseInt(v.toString());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                    "Quantidade inválida na linha " + (row + 1),
                    "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (qtd <= 0) {
                String nomeProd = model.getValueAt(row, 2).toString();
                JOptionPane.showMessageDialog(this,
                    "Quantidade deve ser > 0 para o produto:\n" + nomeProd,
                    "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Gera e salva PedidoCompraModel
        String idPedido = UUID.randomUUID().toString();
        String dataIso  = new SimpleDateFormat("yyyy-MM-dd").format(data);
        pedido = new PedidoCompraModel(
            idPedido,
            nome,
            dataIso,
            "rascunho",
            null,
            ""
        );
        try {
            pedidoDAO.inserir(pedido);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao criar pedido:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Salva cada item marcado
        for (int row : selecionados) {
            String prodId = model.getValueAt(row, 1).toString(); // coluna ID
            int qtd       = Integer.parseInt(model.getValueAt(row, 5).toString());
            String linkId = UUID.randomUUID().toString();

            PedidoEstoqueProdutoModel item = new PedidoEstoqueProdutoModel(
                linkId,
                idPedido,
                prodId,
                qtd,
                0,
                "pendente"
            );
            try {
                itemDAO.inserir(item);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Erro ao adicionar item:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this,
            "Pedido criado com sucesso!",
            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
