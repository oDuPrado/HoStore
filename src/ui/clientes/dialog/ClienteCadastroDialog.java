package ui.clientes.dialog;

import model.ClienteModel;
import service.ClienteService;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

/**
 * ClienteCadastroDialog (versão definitiva ajustada para FlatLaf)
 *
 * - Não força cores fixas em painéis ou botões.
 * - Deixa o FlatLaf (claro/escuro) aplicar o estilo nativo.
 * - Mantém posicional (null layout) para reproduzir o layout original.
 */
public class ClienteCadastroDialog extends JDialog {

    private JTextField txtNome;
    private JFormattedTextField txtTelefone;
    private JFormattedTextField txtCPF;
    private JFormattedTextField txtDataNasc;
    private JComboBox<String> comboTipo;
    private JTextField txtEndereco;
    private JTextField txtCidade;
    private JComboBox<String> comboEstado;
    private JTextArea txtObservacoes;

    private boolean salvou;
    private ClienteModel clienteModel;

    /* ----------------- CONSTRUTOR ----------------- */
    public ClienteCadastroDialog(Window parent, ClienteModel existente) {
        super(parent, "Cadastro de Cliente", ModalityType.APPLICATION_MODAL);

        // Deixa a janela sem decoração (só ficará a borda que definimos abaixo)
        setUndecorated(true);
        setSize(500, 520);
        setLocationRelativeTo(parent);

        // Coloca uma borda cinza clara ao redor da janela para destacar no tema escuro/claro
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));

        // O setBackground abaixo era usado para tornar o dialog transparente, 
        // mas não vamos forçar cores. Comentado para herdar tema:
        // setBackground(new Color(0, 0, 0, 0));

        // Vamos usar um painel com layout null (posicional) para reproduzir o layout original,
        // mas NÃO definiremos cor de fundo: o FlatLaf decidirá.
        JPanel content = new JPanel(null);
        // Removido: content.setBackground(Color.WHITE);
        add(content);

        /* ----- Carrega ou cria o model ----- */
        clienteModel = (existente == null) ? novoModelo() : existente;

        /* ----- Variáveis de posição e tamanhofixo ----- */
        int h = 30;   // altura padrão dos campos
        int y = 20;   // posição Y inicial
        int dy = 10;  // espaçamento vertical entre campos

        int lx = 20;  // posição X de labels
        int cx = 150; // posição X de campos
        int lw = 120; // largura dos labels
        int cw = 300; // largura dos campos

        /* ------------------ CAMPO NOME ------------------ */
        content.add(label("Nome:", lx, y, lw, h));
        txtNome = new JTextField(clienteModel.getNome());
        txtNome.setBounds(cx, y, cw, h);
        content.add(txtNome);
        y += h + dy;

        /* ------------------ CAMPO TELEFONE ------------------ */
        content.add(label("Telefone:", lx, y, lw, h));
        txtTelefone = campoMascara("(##) #####-####", clienteModel.getTelefone());
        txtTelefone.setBounds(cx, y, cw, h);
        content.add(txtTelefone);
        y += h + dy;

        /* ------------------ CAMPO CPF ------------------ */
        content.add(label("CPF:", lx, y, lw, h));
        txtCPF = campoMascara("###.###.###-##", clienteModel.getCpf());
        txtCPF.setBounds(cx, y, cw, h);
        content.add(txtCPF);
        y += h + dy;

        /* ------------------ CAMPO DATA NASCIMENTO ------------------ */
        content.add(label("Data Nasc.:", lx, y, lw, h));
        txtDataNasc = campoMascara("##/##/####", clienteModel.getDataNasc());
        txtDataNasc.setBounds(cx, y, cw, h);
        content.add(txtDataNasc);
        y += h + dy;

        /* ------------------ CAMPO TIPO ------------------ */
        content.add(label("Tipo:", lx, y, lw, h));
        comboTipo = new JComboBox<>(new String[] { "Colecionador", "Jogador", "Ambos" });
        comboTipo.setSelectedItem(clienteModel.getTipo());
        comboTipo.setBounds(cx, y, cw, h);
        content.add(comboTipo);
        y += h + dy;

        /* ------------------ CAMPO ENDEREÇO ------------------ */
        content.add(label("Endereço:", lx, y, lw, h));
        txtEndereco = new JTextField(clienteModel.getEndereco());
        txtEndereco.setBounds(cx, y, cw, h);
        content.add(txtEndereco);
        y += h + dy;

        /* ------------------ CAMPO CIDADE ------------------ */
        content.add(label("Cidade:", lx, y, lw, h));
        txtCidade = new JTextField(clienteModel.getCidade());
        txtCidade.setBounds(cx, y, cw, h);
        content.add(txtCidade);
        y += h + dy;

        /* ------------------ CAMPO ESTADO ------------------ */
        content.add(label("Estado:", lx, y, lw, h));
        comboEstado = new JComboBox<>(UF);
        comboEstado.setSelectedItem(clienteModel.getEstado());
        comboEstado.setBounds(cx, y, cw, h);
        content.add(comboEstado);
        y += h + dy;

        /* ------------------ CAMPO OBSERVAÇÕES ------------------ */
        content.add(label("Observações:", lx, y, lw, h));
        txtObservacoes = new JTextArea(clienteModel.getObservacoes());
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        JScrollPane scObs = new JScrollPane(txtObservacoes);
        // Removido: scObs.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scObs.setBounds(cx, y, cw, 60);
        content.add(scObs);

        /* --------- PAINEL DE BOTÕES (RODAPÉ) --------- */
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rodape.setBounds(0, 420, 500, 50);
        // Removido: rodape.setBackground(Color.WHITE);
        content.add(rodape);

        // Botões agora usam método criarBotao para herdar estilo do tema
        rodape.add(criarBotao("Salvar", e -> salvar(false)));
        rodape.add(criarBotao("Salvar + Novo", e -> salvar(true)));
        rodape.add(criarBotao("Cancelar", e -> {
            salvou = false;
            dispose();
        }));

        /* --------- Efeito de Fade‐In --------- */
        Timer t = new Timer(30, null);
        final float[] opacidade = { 0f };
        t.addActionListener(e -> {
            opacidade[0] += 0.08f;
            if (opacidade[0] >= 1f) {
                opacidade[0] = 1f;
                t.stop();
            }
            setOpacity(opacidade[0]);
        });
        setOpacity(0f);
        t.start();
    }

    /* ----------------- SALVAR (CRIAR OU ATUALIZAR) ----------------- */
    private void salvar(boolean novoApos) {
        String nome = txtNome.getText().trim();
        String cpf = txtCPF.getText().replaceAll("\\D", "");

        // Validações básicas
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome é obrigatório!");
            return;
        }
        if (cpf.length() != 11) {
            JOptionPane.showMessageDialog(this, "CPF inválido.");
            return;
        }
        // Verifica duplicidade de CPF em outro cliente
        List<ClienteModel> todos = ClienteService.loadAll();
        boolean duplicado = todos.stream()
                .anyMatch(c -> c.getCpf().replaceAll("\\D", "").equals(cpf)
                        && !c.getId().equals(clienteModel.getId()));
        if (duplicado) {
            JOptionPane.showMessageDialog(this, "CPF já cadastrado para outro cliente.");
            return;
        }

        // Preenche o model com os valores dos campos
        clienteModel.setNome(nome);
        clienteModel.setCpf(txtCPF.getText());
        clienteModel.setTelefone(txtTelefone.getText());
        clienteModel.setDataNasc(txtDataNasc.getText());
        clienteModel.setTipo((String) comboTipo.getSelectedItem());
        clienteModel.setEndereco(txtEndereco.getText());
        clienteModel.setCidade(txtCidade.getText());
        clienteModel.setEstado((String) comboEstado.getSelectedItem());
        clienteModel.setObservacoes(txtObservacoes.getText());
        // Exemplo de preenchimento de metadata (data/hora fixa)
        clienteModel.setAlteradoEm("2025-04-16 10:10");
        clienteModel.setAlteradoPor("admin");

        // Persiste no serviço/DAO
        ClienteService.upsert(clienteModel);
        salvou = true;

        if (novoApos) {
            // Fecha este e abre outro novo
            dispose();
            new ClienteCadastroDialog(
                    SwingUtilities.getWindowAncestor(getParent()), null
            ).setVisible(true);
        } else {
            dispose();
        }
    }

    /* ----------------- HELPER: cria JLabel posicionado ----------------- */
    private JLabel label(String texto, int x, int y, int w, int h) {
        JLabel l = new JLabel(texto);
        l.setBounds(x, y, w, h);
        return l;
    }

    /* ----------------- HELPER: cria JFormattedTextField com máscara ----------------- */
    private JFormattedTextField campoMascara(String mask, String valorInicial) {
        try {
            MaskFormatter mf = new MaskFormatter(mask);
            mf.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mf);
            f.setText(valorInicial == null ? "" : valorInicial);
            return f;
        } catch (ParseException e) {
            // Se der erro na máscara, retorna um campo genérico
            return new JFormattedTextField(valorInicial);
        }
    }

    /* ----------------- HELPER: cria JButton neutro (herdando estilo do tema) ----------------- */
    private JButton criarBotao(String texto, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(acao);
        // Não definimos setBackground nem setForeground:
        // → o FlatLaf aplicará automaticamente o estilo correto (claro/escuro).
        return b;
    }

    /* ----------------- AUXILIAR: cria um novo model vazio ----------------- */
    private ClienteModel novoModelo() {
        ClienteModel m = new ClienteModel();
        m.setId("C-" + UUID.randomUUID().toString().substring(0, 5));
        m.setCriadoEm("2025-04-16 10:00");
        m.setCriadoPor("admin");
        return m;
    }

    public boolean isSalvou() {
        return salvou;
    }

    public ClienteModel getClienteModel() {
        return clienteModel;
    }

    /* ----------------- LISTA DE UNIDADES FEDERATIVAS ----------------- */
    private static final String[] UF = {
        "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS",
        "MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC",
        "SE","SP","TO"
    };
}
