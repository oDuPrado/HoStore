package ui.clientes.painel;

import model.ClienteModel;
import service.ClienteService;
import ui.clientes.dialog.ClienteCadastroDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PainelClientes (versão definitiva ajustada para FlatLaf)
 *
 * Este painel NÃO força cores fixas, permitindo que o FlatLaf aplique o tema
 * (claro/escuro) dinamicamente. Os filtros usam placeholder nativo em vez de JLabel.
 */
public class PainelClientes extends JPanel {

    private JTextField txtNome;
    private JTextField txtCpf;
    private JTextField txtCidade;
    private JComboBox<String> comboTipo;
    private JTable tabela;
    private DefaultTableModel modelo;
    private List<ClienteModel> lista;

    public PainelClientes() {
        setLayout(new BorderLayout());
        // Removido setBackground(new Color(245, 245, 245));
        // O tema FlatLaf aplicará o fundo correto (claro/escuro) automaticamente.

        // Carrega lista inicial de clientes
        lista = ClienteService.loadAll();

        // Adiciona painel de filtros (com placeholders) no topo
        add(criarPainelFiltro(), BorderLayout.NORTH);
        // Adiciona painel de tabela no centro
        add(criarPainelTabela(), BorderLayout.CENTER);

        // Popula tabela inicialmente
        atualizarTabela();
    }

    /* ---------- PAINEL DE FILTRO COM PLACEHOLDERS ---------- */
    private JPanel criarPainelFiltro() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        // Removido p.setBackground(getBackground());
        // Deixe o FlatLaf determinar o fundo.

        // Campo de texto para Nome, com placeholder
        txtNome = new JTextField(10);
        txtNome.putClientProperty("JTextField.placeholderText", "Nome");

        // Campo de texto para CPF, com placeholder
        txtCpf = new JTextField(10);
        txtCpf.putClientProperty("JTextField.placeholderText", "CPF");

        // Campo de texto para Cidade, com placeholder
        txtCidade = new JTextField(10);
        txtCidade.putClientProperty("JTextField.placeholderText", "Cidade");

        // Combo para Tipo (Todos/Colecionador/Jogador/Ambos)
        comboTipo = new JComboBox<>(new String[] { "Todos", "Colecionador", "Jogador", "Ambos" });
        comboTipo.setToolTipText("Tipo");

        // Botão Filtrar (estilo herdado do tema)
        JButton filtrar = criarBotao("Filtrar", e -> atualizarTabela());

        // Adiciona componentes no painel de filtro
        p.add(new JLabel("Filtros:")); // Apenas um indicador de seção
        p.add(txtNome);
        p.add(txtCpf);
        p.add(txtCidade);
        p.add(comboTipo);
        p.add(filtrar);

        return p;
    }

    /* ---------- PAINEL DA TABELA DE CLIENTES ---------- */
    private JPanel criarPainelTabela() {
        // Cria modelo com colunas: Nome, CPF, Cidade, Tipo, Editar, Excluir
        modelo = new DefaultTableModel(
                new String[] { "Nome", "CPF", "Cidade", "Tipo", "Editar", "Excluir" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Apenas colunas de Editar(4) e Excluir(5) são editáveis (para botões)
                return col >= 4;
            }
        };

        tabela = new JTable(modelo);
        tabela.setRowHeight(28);

        // Renderizador para botão "Editar" (só exibe emoji ✏️)
        tabela.getColumn("Editar").setCellRenderer(new BtnRenderer("✏️"));
        // Renderizador para botão "Excluir" (emoji 🗑️)
        tabela.getColumn("Excluir").setCellRenderer(new BtnRenderer("🗑️"));

        // Editor para botão "Editar"
        tabela.getColumn("Editar").setCellEditor(new BtnEditor(true));
        // Editor para botão "Excluir"
        tabela.getColumn("Excluir").setCellEditor(new BtnEditor(false));

        JScrollPane sp = new JScrollPane(tabela);

        // Botão "Novo Cliente" no rodapé (estilo herdado do tema)
        JButton novo = criarBotao("Novo Cliente", e -> abrirDialog(null));

        // Botões de Importar/Exportar
        JButton importarCsv = criarBotao("Importar CSV", e -> importarClientes());
        JButton importarJson = criarBotao("Importar JSON", e -> importarJson());
        JButton exportarCsv = criarBotao("Exportar CSV", e -> exportarCsv());
        JButton exportarJson = criarBotao("Exportar JSON", e -> exportarJson());

        // Painel de rodapé para esses botões
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        // Removido rodape.setBackground(getBackground());

        rodape.add(novo);
        rodape.add(importarCsv);
        rodape.add(importarJson);
        rodape.add(exportarCsv);
        rodape.add(exportarJson);

        // Painel completo unindo tabela e rodapé
        JPanel painel = new JPanel(new BorderLayout());
        // Removido painel.setBackground(getBackground());
        painel.add(sp, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);

        return painel;
    }

    /* ---------- AÇÕES DE IMPORTAR/EXPORTAR ---------- */
    private void importarClientes() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar clientes CSV");
        int op = fc.showOpenDialog(this);
        if (op == JFileChooser.APPROVE_OPTION) {
            try {
                int qtd = ClienteService.importCsv(fc.getSelectedFile());
                lista = ClienteService.loadAll();
                atualizarTabela();
                JOptionPane.showMessageDialog(this, "Importados " + qtd + " clientes.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void importarJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Importar clientes JSON");
        int op = fc.showOpenDialog(this);
        if (op == JFileChooser.APPROVE_OPTION) {
            try {
                int qtd = ClienteService.importJson(fc.getSelectedFile());
                lista = ClienteService.loadAll();
                atualizarTabela();
                JOptionPane.showMessageDialog(this, "Importados " + qtd + " clientes.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void exportarCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar clientes");
        fc.setSelectedFile(new File("clientes.csv"));
        int op = fc.showSaveDialog(this);
        if (op == JFileChooser.APPROVE_OPTION) {
            try {
                ClienteService.exportCsv(fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Exportado com sucesso!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    private void exportarJson() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar JSON");
        fc.setSelectedFile(new File("clientes.json"));
        int op = fc.showSaveDialog(this);
        if (op == JFileChooser.APPROVE_OPTION) {
            try {
                ClienteService.exportJson(fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Exportado!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        }
    }

    /* ---------- ATUALIZAÇÃO DA TABELA ---------- */
    private void atualizarTabela() {
        // Obtém valores dos filtros, já em lowercase para comparar
        String fNome = txtNome.getText().toLowerCase();
        String fCpf = txtCpf.getText().toLowerCase();
        String fCid = txtCidade.getText().toLowerCase();
        String fTipo = (String) comboTipo.getSelectedItem();

        // Filtra a lista de clientes
        List<ClienteModel> filtrados = lista.stream().filter(c -> {
            boolean okNome = c.getNome().toLowerCase().contains(fNome);
            boolean okCpf = c.getCpf().toLowerCase().contains(fCpf);
            boolean okCid = c.getCidade().toLowerCase().contains(fCid);
            boolean okTipo = fTipo.equals("Todos") || c.getTipo().equalsIgnoreCase(fTipo);
            return okNome && okCpf && okCid && okTipo;
        }).collect(Collectors.toList());

        // Atualiza linhas da tabela
        modelo.setRowCount(0);
        for (ClienteModel c : filtrados) {
            modelo.addRow(new Object[] { c.getNome(), c.getCpf(), c.getCidade(), c.getTipo(), "✏️", "🗑️" });
        }
    }

    /* ---------- ABRIR DIALOG DE CADASTRO/EDIÇÃO ---------- */
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

    /* ---------- CRIADOR DE BOTÃO NEUTRO (HERDA APARÊNCIA DO TEMA) ---------- */
    private JButton criarBotao(String texto, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(acao);
        // Não define cor de fundo ou texto: FlatLaf cuidará disso
        return b;
    }

    /* ---------- RENDERER PERSONALIZADO PARA BOTÕES NA TABELA ---------- */
    private class BtnRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        BtnRenderer(String emoji) {
            setText(emoji);
            setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            setFocusPainted(false);
            // Não define cor fixa; FlatLaf aplicará estilo adequado
            putClientProperty("JButton.buttonType", "square"); 
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            return this;
        }
    }

    /* ---------- EDITOR PERSONALIZADO PARA BOTÕES NA TABELA ---------- */
    private class BtnEditor extends DefaultCellEditor {
        private final boolean editar;
        private final JButton editorComponent;

        BtnEditor(boolean editar) {
            super(new JCheckBox());
            this.editar = editar;

            JButton btn = new JButton(editar ? "✏️" : "🗑️");
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            // Não define cor fixa; FlatLaf aplicará estilo adequado
            putClientProperty("JButton.buttonType", "square");

            editorComponent = btn;
            btn.addActionListener(delegate);
        }

        private final EditorDelegate delegate = new EditorDelegate() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tabela.getSelectedRow();
                if (row < 0) return;

                String cpf = (String) tabela.getValueAt(row, 1);
                ClienteModel cli = lista.stream()
                        .filter(c -> c.getCpf().equals(cpf))
                        .findFirst().orElse(null);
                if (cli == null) return;

                if (editar) {
                    abrirDialog(cli);
                } else {
                    int opt = JOptionPane.showConfirmDialog(
                            PainelClientes.this,
                            "Excluir cliente " + cli.getNome() + "?",
                            "Confirmação",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (opt == JOptionPane.YES_OPTION) {
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
}
