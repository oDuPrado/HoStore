package ui.clientes.painel;

import model.ClienteModel;
import service.ClienteService;
import service.CreditoLojaService;
import ui.clientes.dialog.ClienteCadastroDialog;
import ui.clientes.dialog.CreditoLojaDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PainelClientes (versão definitiva ajustada para FlatLaf)
 *
 * Inclui agora coluna "Crédito" e botão para gerenciar crédito de loja.
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
        setLayout(new BorderLayout());

        // Carrega lista inicial de clientes
        lista = ClienteService.loadAll();

        // Adiciona painel de filtros (com placeholders) no topo
        add(criarPainelFiltro(), BorderLayout.NORTH);
        // Adiciona painel de tabela no centro
        add(criarPainelTabela(), BorderLayout.CENTER);

        // Popula tabela inicialmente
        atualizarTabela();
    }

    /* ---------- PAINEL DE FILTRO COM GRIDBAGLAYOUT ---------- */
    private JPanel criarPainelFiltro() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Filtros de Busca"),
                new EmptyBorder(8, 8, 8, 8)));
        
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        txtNome = new JTextField(12);
        txtNome.putClientProperty("JTextField.placeholderText", "Nome do cliente");
        gc.gridx = 0; gc.weightx = 0; p.add(new JLabel("Nome:"), gc);
        gc.gridx = 1; gc.weightx = 0.25; p.add(txtNome, gc);

        txtCpf = new JTextField(12);
        txtCpf.putClientProperty("JTextField.placeholderText", "CPF");
        gc.gridx = 2; gc.weightx = 0; p.add(new JLabel("CPF:"), gc);
        gc.gridx = 3; gc.weightx = 0.2; p.add(txtCpf, gc);

        txtCidade = new JTextField(12);
        txtCidade.putClientProperty("JTextField.placeholderText", "Cidade");
        gc.gridx = 4; gc.weightx = 0; p.add(new JLabel("Cidade:"), gc);
        gc.gridx = 5; gc.weightx = 0.2; p.add(txtCidade, gc);

        comboTipo = new JComboBox<>(new String[] { "Todos", "Colecionador", "Jogador", "Ambos" });
        gc.gridx = 6; gc.weightx = 0; p.add(new JLabel("Tipo:"), gc);
        gc.gridx = 7; gc.weightx = 0.15; p.add(comboTipo, gc);

        JButton filtrar = criarBotao("🔍 Filtrar", e -> atualizarTabela());
        gc.gridx = 8; gc.weightx = 0; p.add(filtrar, gc);

        return p;
    }

    /* ---------- PAINEL DA TABELA DE CLIENTES ---------- */
    private JPanel criarPainelTabela() {
        // Agora com coluna "Crédito" antes de Editar/Excluir
        modelo = new DefaultTableModel(
                new String[] { "Nome", "CPF", "Cidade", "Tipo", "Crédito", "Editar", "Excluir" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Só as colunas de Editar(5) e Excluir(6) são editáveis (botões)
                return col >= 5;
            }
        };

        tabela = new JTable(modelo);
        tabela.setRowHeight(24);
        tabela.setShowHorizontalLines(true);
        tabela.setShowVerticalLines(false);
        tabela.setFont(tabela.getFont().deriveFont(10f));
        tabela.setGridColor(new Color(200, 200, 200));
        
        // Renderer para coluna de Crédito (alinhado à direita e formatado)
        DefaultTableCellRenderer creditoRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        };
        tabela.getColumn("Crédito").setCellRenderer(creditoRenderer);

        // Renderizadores e editores para os botões
        tabela.getColumn("Editar").setCellRenderer(new BtnRenderer("✏️"));
        tabela.getColumn("Excluir").setCellRenderer(new BtnRenderer("🗑️"));
        tabela.getColumn("Editar").setCellEditor(new BtnEditor(true));
        tabela.getColumn("Excluir").setCellEditor(new BtnEditor(false));
        
        // Redimensionar colunas de botões
        tabela.getColumn("Editar").setMaxWidth(50);
        tabela.getColumn("Editar").setMinWidth(50);
        tabela.getColumn("Excluir").setMaxWidth(50);
        tabela.getColumn("Excluir").setMinWidth(50);

        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(BorderFactory.createTitledBorder("Clientes Cadastrados"));

        // Botões organizados em grid
        JButton novo = criarBotao("➕ Novo Cliente", e -> abrirDialog(null));
        JButton importarCsv = criarBotao("📥 Importar CSV", e -> importarClientes());
        JButton importarJson = criarBotao("📥 Importar JSON", e -> importarJson());
        JButton exportarCsv = criarBotao("📤 Exportar CSV", e -> exportarCsv());
        JButton exportarJson = criarBotao("📤 Exportar JSON", e -> exportarJson());
        JButton btnCredito = criarBotao("💰 Gerenciar Crédito", e -> abrirCredito());

        JPanel rodape = new JPanel(new GridBagLayout());
        rodape.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        gc.weightx = 0.16;

        gc.gridx = 0; rodape.add(novo, gc);
        gc.gridx = 1; rodape.add(importarCsv, gc);
        gc.gridx = 2; rodape.add(importarJson, gc);
        gc.gridx = 3; rodape.add(exportarCsv, gc);
        gc.gridx = 4; rodape.add(exportarJson, gc);
        gc.gridx = 5; rodape.add(btnCredito, gc);

        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(new EmptyBorder(8, 8, 8, 8));
        painel.add(sp, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);

        return painel;
    }

    /* ---------- ATUALIZAÇÃO DA TABELA ---------- */
    private void atualizarTabela() {
        String fNome = txtNome.getText().toLowerCase();
        String fCpf = txtCpf.getText().toLowerCase();
        String fCid = txtCidade.getText().toLowerCase();
        String fTipo = (String) comboTipo.getSelectedItem();

        // Filtra a lista
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
                    String.format("R$ %.2f", saldo), // coluna Crédito
                    "✏️",
                    "🗑️"
            });
        }
    }

    /* ---------- DIÁLOGOS ---------- */
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
        String cpf = (String) tabela.getValueAt(row, 1);
        ClienteModel cli = lista.stream()
                .filter(c -> c.getCpf().equals(cpf))
                .findFirst().orElse(null);
        if (cli != null) {
            new CreditoLojaDialog(
                    SwingUtilities.getWindowAncestor(this),
                    cli).setVisible(true);
            // Atualiza a tabela para refletir novo saldo
            atualizarTabela();
        }
    }

    /* ---------- AÇÕES DE IMPORTAR/EXPORTAR ---------- */
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

    /* ---------- CRIADOR DE BOTÃO ESTILIZADO ---------- */
    private JButton criarBotao(String texto, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setFont(b.getFont().deriveFont(11f));
        b.addActionListener(acao);
        return b;
    }

    /* ---------- RENDERER PARA BOTÕES ---------- */
    private class BtnRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        BtnRenderer(String emoji) {
            setText(emoji);
            setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            setHorizontalAlignment(SwingConstants.CENTER);
            putClientProperty("JButton.buttonType", "square");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            return this;
        }
    }

    /* ---------- EDITOR PARA BOTÕES ---------- */
    private class BtnEditor extends DefaultCellEditor {
        private final boolean editar;
        private final JButton editorComponent;

        BtnEditor(boolean editar) {
            super(new JCheckBox());
            this.editar = editar;
            JButton btn = new JButton(editar ? "✏️" : "🗑️");
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            putClientProperty("JButton.buttonType", "square");
            editorComponent = btn;
            btn.addActionListener(delegate);
        }

        private final EditorDelegate delegate = new EditorDelegate() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tabela.getSelectedRow();
                if (row < 0)
                    return;
                String cpf = (String) tabela.getValueAt(row, 1);
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
        // Recarrega todos os clientes da base (ou poderia otimizar para só esse)
        this.lista = ClienteService.loadAll();
        atualizarTabela();
    }

}
