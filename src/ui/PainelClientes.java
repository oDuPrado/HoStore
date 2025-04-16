package ui;

import model.ClienteModel;
import service.ClienteService;
import ui.dialog.ClienteCadastroDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class PainelClientes extends JPanel {

    private JTextField txtNome, txtCpf, txtCidade;
    private JComboBox<String> comboTipo;
    private JTable tabela;
    private DefaultTableModel modelo;
    private List<ClienteModel> lista;

    public PainelClientes() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        lista = ClienteService.loadAll();

        add(criarPainelFiltro(), BorderLayout.NORTH);
        add(criarPainelTabela(), BorderLayout.CENTER);

        atualizarTabela();
    }

    /* ---------- Filtro ---------- */
    private JPanel criarPainelFiltro() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setBackground(getBackground());

        JLabel lbl = new JLabel("Filtros:");
        lbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));

        txtNome = new JTextField(10);
        txtCpf = new JTextField(10);
        txtCidade = new JTextField(10);
        comboTipo = new JComboBox<>(new String[] { "Todos", "Colecionador", "Jogador", "Ambos" });

        p.add(lbl);
        p.add(new JLabel("Nome:"));
        p.add(txtNome);
        p.add(new JLabel("CPF:"));
        p.add(txtCpf);
        p.add(new JLabel("Cidade:"));
        p.add(txtCidade);
        p.add(new JLabel("Tipo:"));
        p.add(comboTipo);

        JButton filtrar = criarBotaoDark("Filtrar");
        filtrar.addActionListener(e -> atualizarTabela());
        p.add(filtrar);
        return p;
    }

    /* ---------- Tabela ---------- */
    private JPanel criarPainelTabela() {
        modelo = new DefaultTableModel(
                new String[] { "Nome", "CPF", "Cidade", "Tipo", "Editar", "Excluir" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return c >= 4;
            }
        };

        tabela = new JTable(modelo);
        tabela.setRowHeight(28);

        // render / editor dos botões
        tabela.getColumn("Editar").setCellRenderer(new BtnRenderer("✏️"));
        tabela.getColumn("Excluir").setCellRenderer(new BtnRenderer("🗑️"));
        tabela.getColumn("Editar").setCellEditor(new BtnEditor(true));
        tabela.getColumn("Excluir").setCellEditor(new BtnEditor(false));

        JScrollPane sp = new JScrollPane(tabela);

        JButton novo = criarBotaoDark("Novo Cliente");
        novo.addActionListener(e -> abrirDialog(null));

        // ... dentro criarPainelTabela(), depois botao 'Novo Cliente' ...

        JButton exportar = criarBotaoDark("Exportar");
        exportar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Exportar clientes");
            fc.setSelectedFile(new File("clientes.csv"));
            fc.setApproveButtonText("Salvar CSV");
            int op = fc.showSaveDialog(this);
            if(op == JFileChooser.APPROVE_OPTION){
                File f = fc.getSelectedFile();
                try { ClienteService.exportCsv(f);
                    JOptionPane.showMessageDialog(this,"Exportado para "+f.getName()); }
                catch(Exception ex){ JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage()); }
            }
        });

        JButton exportJson = criarBotaoDark("Exportar JSON");
        exportJson.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Exportar JSON");
            fc.setSelectedFile(new File("clientes.json"));
            int op = fc.showSaveDialog(this);
            if(op == JFileChooser.APPROVE_OPTION){
                try { ClienteService.exportJson(fc.getSelectedFile());
                    JOptionPane.showMessageDialog(this,"Exportado!"); }
                catch(Exception ex){ JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage()); }
            }
        });

        JButton importar = criarBotaoDark("Importar CSV");
        importar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Importar clientes CSV");
            int op = fc.showOpenDialog(this);
            if(op == JFileChooser.APPROVE_OPTION){
                try {
                    int qtd = ClienteService.importCsv(fc.getSelectedFile());
                    lista = ClienteService.loadAll();
                    atualizarTabela();
                    JOptionPane.showMessageDialog(this,"Importados "+qtd+" clientes.");
                } catch(Exception ex){
                    JOptionPane.showMessageDialog(this,"Erro: "+ex.getMessage());
                }
            }
        });
        
        

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBackground(getBackground());
        
        // Botões no estilo escuro
        rodape.add(criarBotaoDark("Novo Cliente", e -> abrirDialog(null)));
        rodape.add(criarBotaoDark("Importar CSV", e -> importarClientes()));
        rodape.add(criarBotaoDark("Importar JSON", e -> importarJson()));
        rodape.add(criarBotaoDark("Exportar CSV", e -> exportarCsv()));
        rodape.add(criarBotaoDark("Exportar JSON", e -> exportarJson()));
        

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(getBackground());
        painel.add(sp, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

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

    private JButton criarBotaoDark(String texto, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(acao);
        return b;
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

    /* ---------- Atualiza tabela ---------- */
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
            modelo.addRow(new Object[] { c.getNome(), c.getCpf(), c.getCidade(), c.getTipo(), "✏️", "🗑️" });
        }
    }

    /* ---------- Helpers ---------- */
    private JButton criarBotaoDark(String texto) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void abrirDialog(ClienteModel existente) {
        ClienteCadastroDialog d = new ClienteCadastroDialog(SwingUtilities.getWindowAncestor(this), existente);
        d.setVisible(true);
        if (d.isSalvou()) {
            ClienteService.upsert(d.getClienteModel());
            lista = ClienteService.loadAll();
            atualizarTabela();
        }
    }

    /* ---------- Render / Editor internos ---------- */
    private class BtnRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        BtnRenderer(String emoji) {
            setText(emoji);
            setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            setFocusPainted(false);
            setBackground(new Color(60, 63, 65));
            setForeground(Color.WHITE);
        }

        public Component getTableCellRendererComponent(JTable t, Object o, boolean s, boolean f, int r, int c) {
            return this;
        }
    }

    private class BtnEditor extends DefaultCellEditor {
        private final boolean editar;

        BtnEditor(boolean editar) {
            super(new JCheckBox());
            this.editar = editar;

            JButton btn = new JButton(editar ? "✏️" : "🗑️");
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(60, 63, 65));
            btn.setForeground(Color.WHITE);
            editorComponent = btn;

            btn.addActionListener(delegate); // VINCULA ao delegate
        }

        private final EditorDelegate delegate = new EditorDelegate() {
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
                    int opt = JOptionPane.showConfirmDialog(PainelClientes.this,
                            "Excluir cliente " + cli.getNome() + "?", "Confirma",
                            JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) {
                        ClienteService.deleteById(cli.getId());
                        lista = ClienteService.loadAll();
                        atualizarTabela();
                    }
                }
            }
        };
    }
}
