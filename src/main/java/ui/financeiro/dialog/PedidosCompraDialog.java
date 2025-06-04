// PedidosCompraDialog – Financeiro
package ui.financeiro.dialog;

import dao.PedidoCompraDAO;
import dao.FornecedorDAO;
import model.PedidoCompraModel;
import model.FornecedorModel;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog de Gerenciamento de Pedidos de Compra.
 * Suporta dois modos:
 *  - gerenciamento completo (criar/editar/excluir)
 *  - seleção apenas (para vincular em outro diálogo)
 */
public class PedidosCompraDialog extends JDialog {

    private final PedidoCompraDAO dao           = new PedidoCompraDAO();
    private final FornecedorDAO fornecedorDAO   = new FornecedorDAO();

    private final boolean modoSelecao;
    private final Consumer<PedidoCompraModel> onSelecionar;

    // modelo da tabela
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "ID", "Nome", "Data", "Status", "Fornecedor", "Obs" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    // filtros
    private final JComboBox<String> cbStatus     = new JComboBox<>(
            new String[] { "Todos", "rascunho", "enviado", "recebido" });
    private final JComboBox<FornecedorModel> cbFornecedor = new JComboBox<>();
    private final JDateChooser dtInicio          = new JDateChooser(new Date());
    private final JDateChooser dtFim             = new JDateChooser(new Date());
    private final SimpleDateFormat isoFmt        = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt        = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Construtor (modo gerenciamento completo).
     */
    public PedidosCompraDialog(Window owner) {
        this(owner, false, null);
    }

    /**
     * Construtor principal.
     *
     * @param owner        janela pai
     * @param modoSelecao  se true, habilita apenas seleção
     * @param onSelecionar callback no modo seleção
     */
    public PedidosCompraDialog(Window owner, boolean modoSelecao, Consumer<PedidoCompraModel> onSelecionar) {
        super(owner, "Gerenciar Pedidos de Compra", ModalityType.APPLICATION_MODAL);
        this.modoSelecao  = modoSelecao;
        this.onSelecionar = onSelecionar;
        initComponents();
        loadTable();
        setSize(820, 450);               // aumenta um pouco para dar folga
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // ── Painel de filtros ───────────────────────────────────────────────
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        // Ajusta tamanhos para não cortar
        cbStatus.setPreferredSize(new Dimension(100, cbStatus.getPreferredSize().height));
        dtInicio.setDateFormatString("dd/MM/yyyy");
        dtInicio.setPreferredSize(new Dimension(100, dtInicio.getPreferredSize().height));
        dtFim.setDateFormatString("dd/MM/yyyy");
        dtFim.setPreferredSize(new Dimension(100, dtFim.getPreferredSize().height));

        cbFornecedor.addItem(null);
        try {
            fornecedorDAO.listar(null,null,null,null)
                         .forEach(cbFornecedor::addItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cbFornecedor.setPreferredSize(new Dimension(160, cbFornecedor.getPreferredSize().height));

        filtros.add(new JLabel("Status:"));   filtros.add(cbStatus);
        filtros.add(new JLabel("Fornecedor:")); filtros.add(cbFornecedor);
        filtros.add(new JLabel("De:"));       filtros.add(dtInicio);
        filtros.add(new JLabel("Até:"));      filtros.add(dtFim);

        JButton btFiltrar = new JButton("Filtrar");
        btFiltrar.addActionListener(e -> loadTable());
        filtros.add(btFiltrar);

        add(filtros, BorderLayout.NORTH);

        // ── Tabela de resultados ────────────────────────────────────────────
        // desativa redimensionamento automático
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // define larguras de colunas
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Nome
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // Data
        table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Status
        table.getColumnModel().getColumn(4).setPreferredWidth(140); // Fornecedor
        table.getColumnModel().getColumn(5).setPreferredWidth(200); // Obs

        // centraliza colunas Data, Status, Fornecedor, Obs
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // duplo clique na tabela
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (modoSelecao) selecionarPedido();
                    else editarPedido();
                }
            }
        });

        // ── Painel de botões ────────────────────────────────────────────────
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btRefresh = new JButton("Atualizar");
        btRefresh.addActionListener(e -> loadTable());

        botoes.add(btRefresh);

        if (!modoSelecao) {
            JButton btNovo   = new JButton("Novo");
            JButton btEditar = new JButton("Editar");
            JButton btExcluir= new JButton("Excluir");

            btNovo.addActionListener(e -> {
                PedidoCompraModel p = showForm(null);
                if (p != null) {
                    try { dao.inserir(p); loadTable(); }
                    catch (Exception ex) { ex.printStackTrace(); }
                }
            });
            btEditar.addActionListener(e -> editarPedido());
            btExcluir.addActionListener(e -> {
                int sel = table.getSelectedRow();
                if (sel < 0) return;
                String id = (String) model.getValueAt(sel, 0);
                if (JOptionPane.showConfirmDialog(this,
                        "Excluir este pedido?", "Confirmar",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try { dao.excluir(id); loadTable(); }
                    catch (Exception ex) { ex.printStackTrace(); }
                }
            });

            botoes.add(btNovo);
            botoes.add(btEditar);
            botoes.add(btExcluir);

        } else {
            JButton btSelecionar = new JButton("Selecionar");
            btSelecionar.addActionListener(e -> selecionarPedido());
            JButton btCancelar = new JButton("Cancelar");
            btCancelar.addActionListener(e -> dispose());

            botoes.add(btSelecionar);
            botoes.add(btCancelar);
        }

        add(botoes, BorderLayout.SOUTH);
    }

    /**
     * Carrega dados na tabela aplicando filtros.
     */
    private void loadTable() {
        model.setRowCount(0);
        try {
            String stFil = (String) cbStatus.getSelectedItem();
            FornecedorModel fFil = (FornecedorModel) cbFornecedor.getSelectedItem();
            Date dIni = dtInicio.getDate(), dFim = dtFim.getDate();

            for (PedidoCompraModel p : dao.listarTodos()) {
                if (!"Todos".equals(stFil) && !p.getStatus().equalsIgnoreCase(stFil))
                    continue;
                if (fFil != null && !p.getFornecedorId().equals(fFil.getId()))
                    continue;
                Date dt = isoFmt.parse(p.getData());
                if (dt.before(dIni) || dt.after(dFim)) continue;

                String fornNome = fornecedorDAO.buscarPorId(p.getFornecedorId()).getNome();
                model.addRow(new Object[]{
                    p.getId(),
                    p.getNome(),
                    visFmt.format(dt),
                    p.getStatus(),
                    fornNome,
                    p.getObservacoes()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Formulário de criação/edição de pedido.
     */
    private PedidoCompraModel showForm(PedidoCompraModel existing) {
        JTextField tfNome = new JTextField(20);
        JDateChooser dcData = new JDateChooser(new Date());
        dcData.setDateFormatString("dd/MM/yyyy");
    
        JComboBox<String> cbSt = new JComboBox<>(new String[] { "rascunho", "enviado", "recebido" });
        JComboBox<FornecedorModel> cbForn = new JComboBox<>();
        JTextArea taObs = new JTextArea(3, 20);
    
        // ── Carrega fornecedores no combo
        try {
            fornecedorDAO.listar(null, null, null, null).forEach(cbForn::addItem);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    
        // ── Mostra nomes dos fornecedores no combo
        cbForn.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FornecedorModel f) {
                    setText(f.getNome());
                } else {
                    setText("⛔ Selecione...");
                }
                return this;
            }
        });
    
        // ── Se for edição, preenche campos
        if (existing != null) {
            tfNome.setText(existing.getNome());
            try {
                dcData.setDate(visFmt.parse(existing.getData()));
            } catch (Exception ignored) {}
    
            cbSt.setSelectedItem(existing.getStatus());
    
            // Pré-seleciona fornecedor correspondente
            for (int i = 0; i < cbForn.getItemCount(); i++) {
                FornecedorModel f = cbForn.getItemAt(i);
                if (f != null && f.getId().equals(existing.getFornecedorId())) {
                    cbForn.setSelectedIndex(i);
                    break;
                }
            }
    
            taObs.setText(existing.getObservacoes());
        }
    
        // ── Layout visual com GroupLayout
        JPanel panel = new JPanel();
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);
    
        JLabel lNome = new JLabel("Nome:");
        JLabel lData = new JLabel("Data:");
        JLabel lStatus = new JLabel("Status:");
        JLabel lForn = new JLabel("Fornecedor:");
        JLabel lObs = new JLabel("Observações:");
        JScrollPane spObs = new JScrollPane(taObs);
    
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(lNome)
                    .addComponent(lData)
                    .addComponent(lStatus)
                    .addComponent(lForn)
                    .addComponent(lObs))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(tfNome, 250, 250, 250)
                    .addComponent(dcData, 150, 150, 150)
                    .addComponent(cbSt, 150, 150, 150)
                    .addComponent(cbForn, 250, 250, 250)
                    .addComponent(spObs, 250, 250, 250)))
        );
    
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lNome).addComponent(tfNome))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lData).addComponent(dcData))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lStatus).addComponent(cbSt))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lForn).addComponent(cbForn))
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lObs).addComponent(spObs))
        );
    
        int op = JOptionPane.showConfirmDialog(
            this, panel,
            existing == null ? "Novo Pedido" : "Editar Pedido",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
    
        // ── Validação de campos
        if (op != JOptionPane.OK_OPTION) return null;
    
        if (cbForn.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Selecione um fornecedor.");
            return null;
        }
    
        String id = existing != null ? existing.getId() : UUID.randomUUID().toString();
        String dataIso = new SimpleDateFormat("yyyy-MM-dd").format(dcData.getDate());
    
        return new PedidoCompraModel(
            id,
            tfNome.getText().trim(),
            dataIso,
            (String) cbSt.getSelectedItem(),
            ((FornecedorModel) cbForn.getSelectedItem()).getId(),
            taObs.getText().trim()
        );
    }
    
    
    /**
     * Edição no modo gerenciamento.
     */
    private void editarPedido() {
        int sel = table.getSelectedRow();
        if (sel < 0) return;
        String id = (String) model.getValueAt(sel, 0);
        try {
            PedidoCompraModel p0 = dao.buscarPorId(id);
            PedidoCompraModel p1 = showForm(p0);
            if (p1 != null) {
                dao.atualizar(p1);
                loadTable();
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
     * Seleciona no modo seleção e dispara callback.
     */
    private void selecionarPedido() {
        int sel = table.getSelectedRow();
        if (sel < 0) return;
        String id = (String) model.getValueAt(sel, 0);
        try {
            PedidoCompraModel p = dao.buscarPorId(id);
            if (onSelecionar != null) onSelecionar.accept(p);
        } catch (Exception ex) { ex.printStackTrace(); }
        dispose();
    }
}
