package ui.clientes.painel;

import model.ClienteModel;
import service.ClienteService;
import service.CreditoLojaService;
import ui.clientes.dialog.ClienteCadastroDialog;
import ui.clientes.dialog.CreditoLojaDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PainelClientes (visual alinhado ao UiKit)
 *
 * Mantém a lógica original. Só muda layout/estilo.
 */
public class PainelClientes extends JPanel {

    private JTextField txtNome;
    private JTextField txtCpf;
    private JTextField txtCidade;
    private JComboBox<String> comboTipo;

    private JTable tabela;
    private DefaultTableModel modelo;
    private List<ClienteModel> lista;

    private final CreditoLojaService creditoService = new CreditoLojaService();

    public PainelClientes() {
        setLayout(new BorderLayout(10, 10));
        UiKit.applyPanelBase(this);

        // Carrega lista inicial
        lista = ClienteService.loadAll();

        add(criarTopo(), BorderLayout.NORTH);
        add(criarCentro(), BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);

        atualizarTabela();
    }

    /* ===================== TOP (FILTROS) ===================== */

    private JComponent criarTopo() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("👥 Clientes"));
        left.add(UiKit.hint("Filtre por nome/CPF/cidade/tipo. Crédito aparece na listagem."));
        card.add(left, BorderLayout.WEST);

        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        txtNome = new JTextField(12);
        txtNome.putClientProperty("JTextField.placeholderText", "Nome do cliente");

        txtCpf = new JTextField(12);
        txtCpf.putClientProperty("JTextField.placeholderText", "CPF");

        txtCidade = new JTextField(12);
        txtCidade.putClientProperty("JTextField.placeholderText", "Cidade");

        comboTipo = new JComboBox<>(new String[] { "Todos", "Colecionador", "Jogador", "Ambos" });

        JButton filtrar = UiKit.primary("🔍 Filtrar");
        filtrar.addActionListener(e -> atualizarTabela());

        // Linha única, com pesos equilibrados
        gc.gridx = 0;
        gc.weightx = 0;
        filtros.add(new JLabel("Nome:"), gc);
        gc.gridx = 1;
        gc.weightx = 0.30;
        filtros.add(txtNome, gc);

        gc.gridx = 2;
        gc.weightx = 0;
        filtros.add(new JLabel("CPF:"), gc);
        gc.gridx = 3;
        gc.weightx = 0.18;
        filtros.add(txtCpf, gc);

        gc.gridx = 4;
        gc.weightx = 0;
        filtros.add(new JLabel("Cidade:"), gc);
        gc.gridx = 5;
        gc.weightx = 0.18;
        filtros.add(txtCidade, gc);

        gc.gridx = 6;
        gc.weightx = 0;
        filtros.add(new JLabel("Tipo:"), gc);
        gc.gridx = 7;
        gc.weightx = 0.16;
        filtros.add(comboTipo, gc);

        gc.gridx = 8;
        gc.weightx = 0;
        filtros.add(filtrar, gc);

        card.add(filtros, BorderLayout.SOUTH);

        // UX: Enter em qualquer campo aciona filtro
        ActionListener act = e -> atualizarTabela();
        txtNome.addActionListener(act);
        txtCpf.addActionListener(act);
        txtCidade.addActionListener(act);

        return card;
    }

    /* ===================== CENTER (TABELA) ===================== */

    private JComponent criarCentro() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.title("Lista de Clientes"), BorderLayout.WEST);
        header.add(UiKit.hint("Clique em ✏️ para editar, 🗑️ para excluir"), BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        // Agora com coluna "Crédito" antes de Editar/Excluir
        modelo = new DefaultTableModel(
                new String[] { "Nome", "CPF", "Cidade", "Tipo", "Crédito", "Editar", "Excluir" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col >= 5;
            }
        };

        tabela = new JTable(modelo);
        UiKit.tableDefaults(tabela);

        // Zebra geral
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Renderer Crédito (direita, bold) mantendo zebra
        DefaultTableCellRenderer creditoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) zebra.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        };
        tabela.getColumn("Crédito").setCellRenderer(creditoRenderer);

        // Botões
        tabela.getColumn("Editar").setCellRenderer(new BtnRenderer("✏️"));
        tabela.getColumn("Excluir").setCellRenderer(new BtnRenderer("🗑️"));
        tabela.getColumn("Editar").setCellEditor(new BtnEditor(true));
        tabela.getColumn("Excluir").setCellEditor(new BtnEditor(false));

        // Larguras
        TableColumnModel tcm = tabela.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(220); // Nome
        tcm.getColumn(1).setPreferredWidth(120); // CPF
        tcm.getColumn(2).setPreferredWidth(140); // Cidade
        tcm.getColumn(3).setPreferredWidth(120); // Tipo
        tcm.getColumn(4).setPreferredWidth(110); // Crédito
        tcm.getColumn(5).setMaxWidth(60);
        tcm.getColumn(5).setMinWidth(60);
        tcm.getColumn(6).setMaxWidth(60);
        tcm.getColumn(6).setMinWidth(60);

        tabela.setRowHeight(30);
        tabela.setAutoCreateRowSorter(true);

        card.add(UiKit.scroll(tabela), BorderLayout.CENTER);

        return card;
    }

    /* ===================== BOTTOM (AÇÕES) ===================== */

    private JComponent criarRodape() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.setOpaque(false);

        JButton novo = UiKit.primary("➕ Novo Cliente");
        novo.addActionListener(e -> abrirDialog(null));

        JButton btnCredito = UiKit.ghost("💰 Gerenciar Crédito");
        btnCredito.addActionListener(e -> abrirCredito());

        JButton importarCsv = UiKit.ghost("📥 Importar CSV");
        importarCsv.addActionListener(e -> importarClientes());

        JButton importarJson = UiKit.ghost("📥 Importar JSON");
        importarJson.addActionListener(e -> importarJson());

        JButton exportarCsv = UiKit.ghost("📤 Exportar CSV");
        exportarCsv.addActionListener(e -> exportarCsv());

        JButton exportarJson = UiKit.ghost("📤 Exportar JSON");
        exportarJson.addActionListener(e -> exportarJson());

        actions.add(novo);
        actions.add(btnCredito);
        actions.add(importarCsv);
        actions.add(importarJson);
        actions.add(exportarCsv);
        actions.add(exportarJson);

        card.add(actions, BorderLayout.EAST);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.hint("Dica: selecione um cliente para gerenciar crédito."));
        card.add(left, BorderLayout.WEST);

        return card;
    }

    /* ===================== LÓGICA (IGUAL) ===================== */

    private void atualizarTabela() {
        String fNome = txtNome.getText().toLowerCase();
        String fCpf = txtCpf.getText().toLowerCase();
        String fCid = txtCidade.getText().toLowerCase();
        String fTipo = (String) comboTipo.getSelectedItem();

        List<ClienteModel> filtrados = lista.stream().filter(c -> {
            boolean okNome = c.getNome().toLowerCase().contains(fNome);
            boolean okCpf = c.getCpf().toLowerCase().contains(fCpf);
            boolean okCid = c.getCidade().toLowerCase().contains(fCid);
            boolean okTipo = fTipo.equals("Todos") || c.getTipo().equalsIgnoreCase(fTipo);
            return okNome && okCpf && okCid && okTipo;
        }).collect(Collectors.toList());

        modelo.setRowCount(0);
        for (ClienteModel c : filtrados) {
            double saldo = creditoService.consultarSaldo(c.getId());
            modelo.addRow(new Object[] {
                    c.getNome(),
                    c.getCpf(),
                    c.getCidade(),
                    c.getTipo(),
                    String.format("R$ %.2f", saldo),
                    "✏️",
                    "🗑️"
            });
        }
    }

    private void abrirDialog(ClienteModel existente) {
        ClienteCadastroDialog d = new ClienteCadastroDialog(
                SwingUtilities.getWindowAncestor(this), existente);
        d.setVisible(true);
        if (d.isSalvou()) {
            ClienteService.upsert(d.getClienteModel());
            lista = ClienteService.loadAll();
            atualizarTabela();
        }
    }

    private void abrirCredito() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um cliente para gerenciar crédito.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tabela.convertRowIndexToModel(row);
        String cpf = (String) tabela.getValueAt(modelRow, 1);

        ClienteModel cli = lista.stream()
                .filter(c -> c.getCpf().equals(cpf))
                .findFirst().orElse(null);

        if (cli != null) {
            new CreditoLojaDialog(
                    SwingUtilities.getWindowAncestor(this),
                    cli).setVisible(true);
            atualizarTabela();
        }
    }

    private void importarClientes() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar clientes CSV");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ClienteService.importCsv(fc.getSelectedFile());
                lista = ClienteService.loadAll();
                atualizarTabela();
                JOptionPane.showMessageDialog(this, "Importação concluída.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void importarJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar clientes JSON");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ClienteService.importJson(fc.getSelectedFile());
                lista = ClienteService.loadAll();
                atualizarTabela();
                JOptionPane.showMessageDialog(this, "Importação concluída.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void exportarCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar clientes");
        fc.setSelectedFile(new File("clientes.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ClienteService.exportCsv(fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Exportação concluída.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void exportarJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar JSON");
        fc.setSelectedFile(new File("clientes.json"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ClienteService.exportJson(fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Exportação concluída.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    /*
     * ===================== BOTÕES NA TABELA (MESMA LÓGICA) =====================
     */

    private class BtnRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        BtnRenderer(String emoji) {
            setText(emoji);
            setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            setHorizontalAlignment(SwingConstants.CENTER);
            putClientProperty("JButton.buttonType", "square");
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            return this;
        }
    }

    private class BtnEditor extends DefaultCellEditor {
        private final boolean editar;
        private final JButton editorComponent;

        BtnEditor(boolean editar) {
            super(new JCheckBox());
            this.editar = editar;

            JButton btn = new JButton(editar ? "✏️" : "🗑️");
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            btn.putClientProperty("JButton.buttonType", "square");

            editorComponent = btn;
            btn.addActionListener(delegate);
        }

        private final EditorDelegate delegate = new EditorDelegate() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tabela.getSelectedRow();
                if (row < 0)
                    return;

                int modelRow = tabela.convertRowIndexToModel(row);
                String cpf = (String) tabela.getValueAt(modelRow, 1);

                ClienteModel cli = lista.stream()
                        .filter(c -> c.getCpf().equals(cpf))
                        .findFirst().orElse(null);

                if (cli == null)
                    return;

                if (editar) {
                    abrirDialog(cli);
                } else {
                    if (JOptionPane.showConfirmDialog(
                            PainelClientes.this,
                            "Excluir cliente " + cli.getNome() + "?",
                            "Confirmação",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        ClienteService.deleteById(cli.getId());
                        lista = ClienteService.loadAll();
                        atualizarTabela();
                    }
                }

                fireEditingStopped();
            }
        };

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    // ATUALIZAÇÃO EXTERNA
    public void atualizarCliente(String clienteId) {
        this.lista = ClienteService.loadAll();
        atualizarTabela();
    }
}
