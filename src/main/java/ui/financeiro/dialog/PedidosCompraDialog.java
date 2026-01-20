// PedidosCompraDialog – Financeiro (UiKit)
package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import dao.FornecedorDAO;
import dao.PedidoCompraDAO;
import model.FornecedorModel;
import model.PedidoCompraModel;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog de Gerenciamento de Pedidos de Compra.
 * Suporta dois modos:
 * - gerenciamento completo (criar/editar/excluir)
 * - seleção apenas (para vincular em outro diálogo)
 */
public class PedidosCompraDialog extends JDialog {

    private final PedidoCompraDAO dao = new PedidoCompraDAO();
    private final FornecedorDAO fornecedorDAO = new FornecedorDAO();

    private final boolean modoSelecao;
    private final Consumer<PedidoCompraModel> onSelecionar;

    // tabela
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "ID", "Nome", "Data", "Status", "Fornecedor", "Obs" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    // filtros
    private final JComboBox<String> cbStatus = new JComboBox<>(
            new String[] { "Todos", "rascunho", "enviado", "recebido" });
    private final JComboBox<FornecedorModel> cbFornecedor = new JComboBox<>();
    private final JDateChooser dtInicio = new JDateChooser(dateFrom(LocalDate.now()));
    private final JDateChooser dtFim = new JDateChooser(dateFrom(LocalDate.now().plusDays(365))); // padrão: hoje -> 1
                                                                                                  // ano

    private final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");

    public PedidosCompraDialog(Window owner) {
        this(owner, false, null);
    }

    public PedidosCompraDialog(Window owner, boolean modoSelecao, Consumer<PedidoCompraModel> onSelecionar) {
        super(owner, "Gerenciar Pedidos de Compra", ModalityType.APPLICATION_MODAL);
        this.modoSelecao = modoSelecao;
        this.onSelecionar = onSelecionar;

        UiKit.applyDialogBase(this);
        setContentPane(buildUI());

        pack();
        setMinimumSize(new Dimension(980, 560));
        setLocationRelativeTo(owner);

        loadTable();
    }

    private JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title(modoSelecao ? "Selecionar Pedido de Compra" : "Gerenciar Pedidos de Compra"));
        header.add(UiKit.hint(modoSelecao
                ? "Selecione um pedido e clique em Selecionar (ou dê duplo clique)."
                : "Filtre, crie, edite e exclua pedidos de compra."));
        root.add(header, BorderLayout.NORTH);

        // Card filtros
        JPanel filtrosCard = UiKit.card();
        filtrosCard.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // fornecedores no combo
        cbFornecedor.removeAllItems();
        cbFornecedor.addItem(null); // "Todos"
        try {
            fornecedorDAO.listar(null, null, null, null).forEach(cbFornecedor::addItem);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        cbFornecedor.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FornecedorModel f)
                    setText(f.getNome());
                else
                    setText("Todos");
                return this;
            }
        });

        // estilo / tamanhos
        cbStatus.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbFornecedor.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        prepararDateChooser(dtInicio);
        prepararDateChooser(dtFim);

        JButton btFiltrar = UiKit.primary("Filtrar");
        JButton btLimpar = UiKit.ghost("Limpar");

        btFiltrar.addActionListener(e -> loadTable());
        btLimpar.addActionListener(e -> {
            cbStatus.setSelectedIndex(0);
            cbFornecedor.setSelectedItem(null);
            dtInicio.setDate(dateFrom(LocalDate.now()));
            dtFim.setDate(dateFrom(LocalDate.now().plusDays(365)));
            loadTable();
        });

        // linha 0
        addField(filtrosCard, gc, 0, "Status", cbStatus);
        addField(filtrosCard, gc, 1, "Fornecedor", cbFornecedor);

        // linha 1 (datas)
        addField(filtrosCard, gc, 2, "De", dtInicio);
        addField(filtrosCard, gc, 3, "Até", dtFim);

        // botões
        GridBagConstraints gb = (GridBagConstraints) gc.clone();
        gb.gridx = 4;
        gb.gridy = 0;
        gb.gridheight = 2;
        gb.weightx = 0;
        gb.fill = GridBagConstraints.NONE;

        JPanel box = new JPanel(new GridLayout(2, 1, 6, 6));
        box.setOpaque(false);
        box.add(btFiltrar);
        box.add(btLimpar);

        filtrosCard.add(box, gb);

        root.add(filtrosCard, BorderLayout.BEFORE_FIRST_LINE);

        // Card tabela
        JPanel tableCard = UiKit.card();
        tableCard.setLayout(new BorderLayout(10, 10));

        setupTable();
        tableCard.add(UiKit.scroll(table), BorderLayout.CENTER);

        root.add(tableCard, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btRefresh = UiKit.ghost("Atualizar");
        btRefresh.addActionListener(e -> loadTable());
        footer.add(btRefresh);

        if (!modoSelecao) {
            JButton btNovo = UiKit.primary("Novo");
            JButton btEditar = UiKit.ghost("Editar");
            JButton btExcluir = UiKit.ghost("Excluir");

            btNovo.addActionListener(e -> onNovo());
            btEditar.addActionListener(e -> editarPedido());
            btExcluir.addActionListener(e -> excluirPedido());

            footer.add(btNovo);
            footer.add(btEditar);
            footer.add(btExcluir);
        } else {
            JButton btSelecionar = UiKit.primary("Selecionar");
            JButton btCancelar = UiKit.ghost("Cancelar");

            btSelecionar.addActionListener(e -> selecionarPedido());
            btCancelar.addActionListener(e -> dispose());

            footer.add(btSelecionar);
            footer.add(btCancelar);
        }

        root.add(footer, BorderLayout.SOUTH);

        return root;
    }

    private void setupTable() {
        UiKit.tableDefaults(table);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // larguras
        esconderColunaID(table);
        setColWidth(1, 220); // Nome
        setColWidth(2, 95); // Data
        setColWidth(3, 110); // Status
        setColWidth(4, 180); // Fornecedor
        setColWidth(5, 260); // Obs

        // zebra base
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(zebra);
        }

        // alinhamentos
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(2).setCellRenderer(new DelegatingRenderer(zebra, center)); // data
        table.getColumnModel().getColumn(4).setCellRenderer(new DelegatingRenderer(zebra, center)); // fornecedor (texto
                                                                                                    // central dá "ERP
                                                                                                    // vibes")

        // status com badge (usa o do UiKit, que já tem casos rascunho/enviado/recebido)
        table.getColumnModel().getColumn(3).setCellRenderer(UiKit.badgeStatusRenderer());

        // duplo clique
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (modoSelecao)
                        selecionarPedido();
                    else
                        editarPedido();
                }
            }
        });
    }

    private void loadTable() {
        model.setRowCount(0);

        try {
            String stFil = (String) cbStatus.getSelectedItem();
            FornecedorModel fFil = (FornecedorModel) cbFornecedor.getSelectedItem();

            Date dIni = dtInicio.getDate();
            Date dFim = dtFim.getDate();

            for (PedidoCompraModel p : dao.listarTodos()) {
                if (!"Todos".equals(stFil) && !safe(p.getStatus()).equalsIgnoreCase(stFil))
                    continue;
                if (fFil != null && !safe(p.getFornecedorId()).equals(fFil.getId()))
                    continue;

                Date dt = optDate(p.getData());
                if (dt == null)
                    continue;

                // filtro data (seguro)
                if (dIni != null && dt.before(dIni))
                    continue;
                if (dFim != null && dt.after(dFim))
                    continue;

                String fornNome = "";
                try {
                    fornNome = fornecedorDAO.buscarPorId(p.getFornecedorId()).getNome();
                } catch (Exception ignored) {
                }

                model.addRow(new Object[] {
                        p.getId(),
                        p.getNome(),
                        visFmt.format(dt),
                        safe(p.getStatus()),
                        fornNome,
                        safe(p.getObservacoes())
                });
            }

            // ordena por data desc (mais recente primeiro)
            @SuppressWarnings("unchecked")
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.DESCENDING)));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onNovo() {
        PedidoCompraModel p = showForm(null);
        if (p == null)
            return;

        try {
            dao.inserir(p);
            loadTable();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao salvar:\n" + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluirPedido() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = String.valueOf(model.getValueAt(modelRow, 0));

        if (JOptionPane.showConfirmDialog(
                this,
                "Excluir este pedido?",
                "Confirmar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                dao.excluir(id);
                loadTable();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao excluir:\n" + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editarPedido() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = String.valueOf(model.getValueAt(modelRow, 0));

        try {
            PedidoCompraModel p0 = dao.buscarPorId(id);
            PedidoCompraModel p1 = showForm(p0);
            if (p1 != null) {
                dao.atualizar(p1);
                loadTable();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void selecionarPedido() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma linha.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = String.valueOf(model.getValueAt(modelRow, 0));

        try {
            PedidoCompraModel p = dao.buscarPorId(id);
            if (onSelecionar != null)
                onSelecionar.accept(p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        dispose();
    }

    /**
     * Formulário de criação/edição de pedido.
     * Mantive JOptionPane para não virar uma novela, mas estilizado com UiKit.
     */
    private PedidoCompraModel showForm(PedidoCompraModel existing) {
        JTextField tfNome = new JTextField(22);
        JDateChooser dcData = new JDateChooser(dateFrom(LocalDate.now()));
        prepararDateChooser(dcData);

        JComboBox<String> cbSt = new JComboBox<>(new String[] { "rascunho", "enviado", "recebido" });
        cbSt.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        JComboBox<FornecedorModel> cbForn = new JComboBox<>();
        cbForn.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbForn.addItem(null);

        JTextArea taObs = new JTextArea(4, 26);
        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);
        taObs.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Observações (opcional)...");

        JScrollPane spObs = UiKit.scroll(taObs);
        spObs.setPreferredSize(new Dimension(420, 110));

        // fornecedores
        try {
            fornecedorDAO.listar(null, null, null, null).forEach(cbForn::addItem);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        cbForn.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FornecedorModel f)
                    setText(f.getNome());
                else
                    setText("Selecione...");
                return this;
            }
        });

        // edição
        if (existing != null) {
            tfNome.setText(safe(existing.getNome()));
            try {
                dcData.setDate(optDate(existing.getData()));
            } catch (Exception ignored) {
            }

            cbSt.setSelectedItem(safe(existing.getStatus()));

            for (int i = 0; i < cbForn.getItemCount(); i++) {
                FornecedorModel f = cbForn.getItemAt(i);
                if (f != null && safe(existing.getFornecedorId()).equals(f.getId())) {
                    cbForn.setSelectedIndex(i);
                    break;
                }
            }
            taObs.setText(safe(existing.getObservacoes()));
        }

        JPanel panel = UiKit.card();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        addField(panel, gc, 0, "Nome", tfNome);
        addField(panel, gc, 1, "Data", dcData);
        addField(panel, gc, 2, "Status", cbSt);
        addField(panel, gc, 3, "Fornecedor", cbForn);

        // obs (multi-linha)
        GridBagConstraints gl = (GridBagConstraints) gc.clone();
        gl.gridx = 0;
        gl.gridy = 4;
        gl.weightx = 0;
        gl.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Observações"), gl);

        GridBagConstraints gf = (GridBagConstraints) gc.clone();
        gf.gridx = 1;
        gf.gridy = 4;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.BOTH;
        gf.weighty = 1;
        panel.add(spObs, gf);

        int op = JOptionPane.showConfirmDialog(
                this,
                panel,
                existing == null ? "Novo Pedido" : "Editar Pedido",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (op != JOptionPane.OK_OPTION)
            return null;

        if (tfNome.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe um nome.");
            return null;
        }
        if (cbForn.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor.");
            return null;
        }
        if (dcData.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Informe a data.");
            return null;
        }

        String id = existing != null ? existing.getId() : UUID.randomUUID().toString();
        String dataIso = isoFmt.format(dcData.getDate());

        return new PedidoCompraModel(
                id,
                tfNome.getText().trim(),
                dataIso,
                (String) cbSt.getSelectedItem(),
                ((FornecedorModel) cbForn.getSelectedItem()).getId(),
                taObs.getText().trim());
    }

    /* ─────────────────────────── helpers ─────────────────────────── */

    private void esconderColunaID(JTable t) {
        TableColumn col = t.getColumnModel().getColumn(0);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }

    private void setColWidth(int col, int w) {
        TableColumn c = table.getColumnModel().getColumn(col);
        c.setPreferredWidth(w);
    }

    private Date optDate(String iso) {
        try {
            return iso == null ? null : isoFmt.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Date dateFrom(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void prepararDateChooser(JDateChooser dc) {
        dc.setDateFormatString("dd/MM/yyyy");
        dc.setPreferredSize(new Dimension(140, 30));

        if (dc.getDateEditor() != null) {
            JComponent editor = dc.getDateEditor().getUiComponent();
            if (editor instanceof JComponent) {
                editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
            }
        }

        JButton calBtn = dc.getCalendarButton();
        if (calBtn != null) {
            calBtn.setText("📅");
            calBtn.setFocusPainted(false);
            calBtn.setMargin(new Insets(2, 8, 2, 8));
            calBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0; font: +1;");
            calBtn.setToolTipText("Selecionar data");
        }
    }

    private void addField(JPanel parent, GridBagConstraints base, int col, String label, Component field) {
        int row = (col >= 2) ? 1 : 0;
        int x = (col % 2) * 2;

        GridBagConstraints gl = (GridBagConstraints) base.clone();
        gl.gridx = x;
        gl.gridy = row;
        gl.weightx = 0;
        gl.fill = GridBagConstraints.NONE;
        parent.add(new JLabel(label + ":"), gl);

        GridBagConstraints gf = (GridBagConstraints) base.clone();
        gf.gridx = x + 1;
        gf.gridy = row;
        gf.weightx = 1;
        parent.add(field, gf);
    }

    static class DelegatingRenderer extends DefaultTableCellRenderer {
        private final DefaultTableCellRenderer base;
        private final DefaultTableCellRenderer aligner;

        DelegatingRenderer(DefaultTableCellRenderer base, DefaultTableCellRenderer aligner) {
            this.base = base;
            this.aligner = aligner;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel l) {
                l.setHorizontalAlignment(aligner.getHorizontalAlignment());
            }
            return c;
        }
    }
}
